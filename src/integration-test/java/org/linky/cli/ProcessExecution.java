package org.linky.cli;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessExecution {

    private final List<String> stdOut;
    private final List<String> stdErr;
    private final int exitCode;

    public ProcessExecution(Process process) {
        stdOut = lines(process.getInputStream());
        stdErr = lines(process.getErrorStream());
        exitCode = process.exitValue();
    }

    private static List<String> lines(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .collect(Collectors.toList());
    }

    public List<String> getStdOut() {
        return this.stdOut;
    }

    public List<String> getStdErr() {
        return this.stdErr;
    }

    public int getExitCode() {
        return this.exitCode;
    }
}
