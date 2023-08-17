package org.symly.env;

import static org.symly.testing.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.symly.doc.ExecutionDocReport;
import org.symly.files.FileTree;
import org.symly.files.FileTree.Diff;

@SuppressWarnings({"java:S5960" // Assertions should not be used in production code (this is test code)
})
public record Execution(
        FileTree snapshot,
        Path rootDir,
        Path workingDir,
        List<String> command,
        int exitCode,
        List<String> stdOut,
        List<String> stdErr) {

    public Diff fileSystemEntriesDiff() {
        return snapshot.diff(currentFileTree());
    }

    public FileTree currentFileTree() {
        return FileTree.fromPath(rootDir);
    }

    public static Execution of(
            FileTree rootFileTreeSnapshot,
            Path rootDir,
            Path workingDir,
            List<String> command,
            int exitCode,
            Reader stdOut,
            Reader stdErr) {
        return new Execution(
                rootFileTreeSnapshot, rootDir, workingDir, command, exitCode, lines(stdOut), lines(stdErr));
    }

    private static List<String> lines(Reader reader) {
        return new BufferedReader(reader).lines().toList();
    }

    public ExitCodeAssert thenItShould() {
        return new ExitCodeAssert(this);
    }

    public static class ExitCodeAssert {

        private static final int SUCCESS = 0;
        private static final int RUN_ERROR = 1;
        private static final int CONFIGURATION_ERROR = 2;

        private final Execution execution;

        public ExitCodeAssert(Execution execution) {
            this.execution = Objects.requireNonNull(execution);
        }

        public OutputAssert succeed() {
            OutputAssert outputAssert = assertExitCodeIs(SUCCESS);
            assertThat(execution.stdErr())
                    .withFailMessage(
                            """
                        Expected no messages on stderr but got:
                        %s
                        stdout was:
                        %s
                        """,
                            lines(execution.stdErr()), lines(execution.stdOut()))
                    .isEmpty();
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
                            execution.exitCode(), lines(execution.stdErr()), lines(execution.stdOut()))
                    .isEqualTo(exitCode);
            return new OutputAssert(execution);
        }

        private String lines(Collection<String> lines) {
            return lines.stream().map(line -> "\t" + line).collect(Collectors.joining("\n"));
        }
    }

    public record OutputAssert(Execution execution) {

        @SuppressWarnings("CanIgnoreReturnValueSuggester")
        public OutputAssert withMessage(String message) {
            assertThat(execution.stdOut()).contains(message);
            return this;
        }

        @SuppressWarnings("CanIgnoreReturnValueSuggester")
        public OutputAssert withoutMessage(String message) {
            assertThat(execution.stdOut()).doesNotContain(message);
            return this;
        }

        @SuppressWarnings("CanIgnoreReturnValueSuggester")
        public OutputAssert withMessages(List<String> messages) {
            messages.forEach(this::withMessage);
            return this;
        }

        @SuppressWarnings("CanIgnoreReturnValueSuggester")
        public OutputAssert withErrorMessage(String message) {
            assertThat(execution.stdErr()).contains(message);
            return this;
        }

        @SuppressWarnings("CanIgnoreReturnValueSuggester")
        public OutputAssert withErrorMessages(List<String> messages) {
            messages.forEach(this::withErrorMessage);
            return this;
        }

        @SuppressWarnings("CanIgnoreReturnValueSuggester")
        public OutputAssert withFileTreeDiff(Diff diff) {
            Diff actual = execution.fileSystemEntriesDiff();
            assertThat(actual.newPaths())
                    // FIXME .describedAs("Should create the following file system entries")
                    .containsExactlyInAnyOrderElementsOf(diff.newPaths());
            assertThat(actual.removedPaths())
                    // FIXME .describedAs("Should remove the following file system entries")
                    .containsExactlyInAnyOrderElementsOf(diff.removedPaths());
            return this;
        }

        public ExecutionDocReport executionReport() {
            return new ExecutionDocReport(execution);
        }
    }
}
