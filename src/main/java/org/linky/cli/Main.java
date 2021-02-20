package org.linky.cli;

import org.linky.cli.converters.SourceDirectoryTypeConverter;
import org.linky.cli.converters.TargetDirectoryTypeConverter;
import org.linky.links.SourceDirectory;
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
        CommandLine commandLine = new CommandLine(new MainCommand(), factory)
                .setOut(console.writer())
                .setErr(console.ewriter())
                .setDefaultValueProvider(new EnvironmentVariableDefaultsProvider())
                .setExecutionExceptionHandler(new ExceptionHandler(console))
                .registerConverter(SourceDirectory.class, new SourceDirectoryTypeConverter())
                .registerConverter(TargetDirectory.class, new TargetDirectoryTypeConverter());
        return commandLine.execute(args);
    }
}
