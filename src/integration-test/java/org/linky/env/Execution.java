package org.linky.env;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.ListAssert;
import org.linky.files.FileTree;
import org.linky.files.FileTree.Diff;

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

        @NonNull
        private final Execution execution;

        public OutputAssert succeed() {
            assertThat(execution.exitCode())
                    .withFailMessage(
                            "Execution exited with code %s%nand errors %s",
                            execution.exitCode(),
                            execution.stdErr())
                    .isZero();
            return new OutputAssert(execution);
        }

        public OutputAssert fail() {
            assertThat(execution.exitCode())
                    .withFailMessage(
                            "Command exited with code %s%nand errors %s",
                            execution.exitCode(),
                            execution.stdErr())
                    .isEqualTo(2);
            return new OutputAssert(execution);
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

        public OutputAssert withMessage(String message, Object... objects) {
            return withMessage(String.format(message, objects));
        }

        public OutputAssert withErrorMessage(String message) {
            assertThat(execution.stdErr()).contains(message);
            return this;
        }

        public OutputAssert withErrorMessage(String message, Object... objects) {
            return withErrorMessage(String.format(message, objects));
        }

        public void withFileTreeDiff(Diff diff) {
            Diff actual = execution.fileSystemEntriesDiff();
            assertThat(actual.getCreated()).containsExactlyInAnyOrderElementsOf(diff.getCreated());
            assertThat(actual.getDeleted()).containsExactlyInAnyOrderElementsOf(diff.getDeleted());
        }

        public ListAssert<String> andWorkingDirLayout() {
            return assertThat(FileTree.fromPath(execution.workingDir()).getLayout());
        }

    }
}
