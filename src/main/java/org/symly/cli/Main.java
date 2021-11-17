package org.symly.cli;

import org.symly.cli.converters.MainDirectoryTypeConverter;
import org.symly.cli.converters.RepositoryTypeConverter;
import org.symly.links.MainDirectory;
import org.symly.links.Repository;
import picocli.CommandLine;

public class Main {

    public static void main(String... args) {
        int exitCode = runCommand(args);
        System.exit(exitCode);
    }

    private static int runCommand(String... args) {
        BeanFactory beanFactory = new BeanFactory();
        CliConsole console = beanFactory.create(CliConsole.class);
        CommandLine commandLine = initializeCommandLine(beanFactory, console);
        int exitCode = commandLine.execute(args);
        console.flush();
        return exitCode;
    }

    private static CommandLine initializeCommandLine(BeanFactory factory, CliConsole console) {
        CommandLine commandLine = new CommandLine(factory.create(MainCommand.class), factory);
        commandLine.setOut(console.writer());
        commandLine.setErr(console.ewriter());
        commandLine.setDefaultValueProvider(new EnvironmentVariableDefaultsProvider());
        commandLine.setExecutionExceptionHandler(factory.create(ExceptionHandler.class));
        commandLine.registerConverter(MainDirectory.class, new MainDirectoryTypeConverter());
        commandLine.registerConverter(Repository.class, new RepositoryTypeConverter());
        return commandLine;
    }
}
