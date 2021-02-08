package org.linky.env;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public class Execution {

    private final List<String> stdOut;
    private final List<String> stdErr;
    private final int exitCode;

    public Execution(Process process) {
        stdOut = lines(process.getInputStream());
        stdErr = lines(process.getErrorStream());
        exitCode = process.exitValue();
    }

    private static List<String> lines(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .collect(Collectors.toList());
    }
}
