package org.linky.env;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Execution {

    private final int exitCode;
    private final List<String> stdOut;
    private final List<String> stdErr;

    public int exitCode() {
        return this.exitCode;
    }

    public List<String> stdOut() {
        return this.stdOut;
    }

    public List<String> stdErr() {
        return this.stdErr;
    }

    public static Execution of(Process process) {
        return new Execution(
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


    public ExitCodeAssert assertThat() {
        return new ExitCodeAssert(this);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ExitCodeAssert {
        private final Execution execution;

        public OutputAssert succeeds() {
            Assertions.assertThat(execution.exitCode()).isZero();
            return new OutputAssert(execution);
        }

        public OutputAssert fails() {
            Assertions.assertThat(execution.exitCode()).isEqualTo(2);
            return new OutputAssert(execution);
        }

    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OutputAssert {
        private final Execution execution;


        public OutputAssert withMessage(String message) {
            Assertions.assertThat(execution.stdOut()).contains(message);
            return this;
        }

        public OutputAssert withMessage(String message, Object... objects) {
            return withMessage(String.format(message, objects));
        }

        public OutputAssert withErrorMessage(String message) {
            Assertions.assertThat(execution.stdErr()).contains(message);
            return this;
        }

        public OutputAssert withErrorMessage(String message, Object... objects) {
            return withErrorMessage(String.format(message, objects));
        }

        public ListAssert<String> andLayout(Env env) {
            return Assertions.assertThat(env.getRootFileTree().getLayout());
        }
    }
}
