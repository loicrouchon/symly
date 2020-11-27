package org.linky.cli;

import picocli.CommandLine;

public class Main {

    public static void main(String... args) {
        CommandLine commandLine = new CommandLine(new MainCommand());
        commandLine.setDefaultValueProvider(new EnvironmentVariableDefaultsProvider());
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
