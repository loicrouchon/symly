package org.linky.cli;

import org.linky.links.TargetDirectory;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

public class Main {

    public static void main(String... args) {
        CliConsole console = CliConsole.console();
        int exitCode = runCommand(CommandLine.defaultFactory(), console, args);
        System.exit(exitCode);
    }

    static int runCommand(IFactory factory, CliConsole console, String... args) {
        CommandLine commandLine = new CommandLine(new MainCommand(), factory);
        commandLine.setOut(console.writer());
        commandLine.setErr(console.ewriter());
        commandLine.setDefaultValueProvider(new EnvironmentVariableDefaultsProvider());
        commandLine.setExecutionExceptionHandler(new ExceptionHandler(console));
        commandLine.registerConverter(TargetDirectory.class, new TargetDirectoryTypeConverter());
        return commandLine.execute(args);
    }
}
