package org.linky.cli;

import org.linky.cli.converters.SourceDirectoryTypeConverter;
import org.linky.cli.converters.TargetDirectoryTypeConverter;
import org.linky.links.SourceDirectory;
import org.linky.links.TargetDirectory;
import picocli.CommandLine;

public class Main {

    public static void main(String... args) {
        int exitCode = runCommand(args);
        System.exit(exitCode);
    }

    private static int runCommand(String... args) {
        BeanFactory factory = new BeanFactory();
        CliConsole console = factory.create(CliConsole.class);
        CommandLine commandLine = new CommandLine(factory.create(MainCommand.class), factory);
        commandLine.setOut(console.writer());
        commandLine.setErr(console.ewriter());
        commandLine.setDefaultValueProvider(new EnvironmentVariableDefaultsProvider());
        commandLine.setExecutionExceptionHandler(new ExceptionHandler(console));
        commandLine.registerConverter(SourceDirectory.class, new SourceDirectoryTypeConverter());
        commandLine.registerConverter(TargetDirectory.class, new TargetDirectoryTypeConverter());
        return commandLine.execute(args);
    }
}
