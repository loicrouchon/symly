package org.linky.cli;

import picocli.CommandLine;

public class Main {

    public static void main(String... args) {
        int exitCode = runCommand(args);
        System.exit(exitCode);
    }

    static int runCommand(String... args) {
        BeanFactory factory = new BeanFactory();
        CliConsole console = factory.create(CliConsole.class);
        CommandLine commandLine = new CommandLine(factory.create(MainCommand.class), factory);
        commandLine.setOut(console.writer());
        commandLine.setErr(console.ewriter());
        commandLine.setDefaultValueProvider(new EnvironmentVariableDefaultsProvider());
        commandLine.setExecutionExceptionHandler(new ExceptionHandler(console));
        return commandLine.execute(args);
    }
}
