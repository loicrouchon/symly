package org.linky.env;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

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
}
