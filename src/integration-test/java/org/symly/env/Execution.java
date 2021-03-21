package org.symly.env;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.files.FileTree;
import org.symly.files.FileTree.Diff;

@RequiredArgsConstructor
public class Execution {

    @NonNull
    private final FileTree snapshot;
    @NonNull
    private final Path rootDir;
    @NonNull
    private final Path workingDir;
    private final int exitCode;
    @NonNull
    private final List<String> stdOut;
    @NonNull
    private final List<String> stdErr;

    private Diff fileSystemEntriesDiff() {
        return snapshot.diff(FileTree.fromPath(rootDir));
    }

    public Path workingDir() {
        return workingDir;
    }

    public int exitCode() {
        return this.exitCode;
    }

    public List<String> stdOut() {
        return this.stdOut;
    }

    public List<String> stdErr() {
        return this.stdErr;
    }

    public static Execution of(FileTree rootFileTreeSnapshot, Path rootDir,
            Path workingDir, Process process) {
        return new Execution(
                rootFileTreeSnapshot,
                rootDir,
                workingDir,
                process.exitValue(),
                lines(process.getInputStream()),
                lines(process.getErrorStream())
        );
    }

    private static List<String> lines(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .collect(Collectors.toList());
    }

    public ExitCodeAssert thenItShould() {
        return new ExitCodeAssert(this);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ExitCodeAssert {

        private static final int SUCCESS = 0;
        private static final int RUN_ERROR = 1;
        private static final int CONFIGURATION_ERROR = 2;

        @NonNull
        private final Execution execution;

        public OutputAssert succeed() {
            OutputAssert outputAssert = assertExitCodeIs(SUCCESS);
            assertThat(execution.stdErr()).isEmpty();
            return outputAssert;
        }

        public OutputAssert failWithConfigurationError() {
            return assertExitCodeIs(CONFIGURATION_ERROR);
        }

        public OutputAssert failWithError() {
            return assertExitCodeIs(RUN_ERROR);
        }

        private OutputAssert assertExitCodeIs(int exitCode) {
            assertThat(execution.exitCode())
                    .withFailMessage(
                            "Command exited with code %s%nand errors:%n%s%nand output:%n%s%n",
                            execution.exitCode(),
                            lines(execution.stdErr()),
                            lines(execution.stdOut()))
                    .isEqualTo(exitCode);
            return new OutputAssert(execution);
        }

        private String lines(Collection<String> lines) {
            return lines.stream().map(line -> "\t" + line).collect(Collectors.joining("\n"));
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OutputAssert {

        @NonNull
        private final Execution execution;

        public OutputAssert withMessage(String message) {
            assertThat(execution.stdOut()).contains(message);
            return this;
        }

        public OutputAssert withMessages(List<String> messages) {
            messages.forEach(this::withMessage);
            return this;
        }

        public OutputAssert withErrorMessage(String message) {
            assertThat(execution.stdErr()).contains(message);
            return this;
        }

        public OutputAssert withErrorMessages(List<String> messages) {
            messages.forEach(this::withErrorMessage);
            return this;
        }

        public void withFileTreeDiff(Diff diff) {
            Diff actual = execution.fileSystemEntriesDiff();
            assertThat(actual.getNewPaths())
                    .describedAs("Should create the following file system entries")
                    .containsExactlyInAnyOrderElementsOf(diff.getNewPaths());
            assertThat(actual.getRemovedPaths())
                    .describedAs("Should remove the following file system entries")
                    .containsExactlyInAnyOrderElementsOf(diff.getRemovedPaths());
        }
    }
}
