package org.symly.cli;

import java.nio.file.Path;
import picocli.CommandLine;

public class Main {

    public static void main(String... args) {
        BeanFactory beanFactory = new BeanFactory();
        int exitCode = runCommand(beanFactory, CommandLine.Help.Ansi.AUTO, args);
        System.exit(exitCode);
    }

    public static int runCommand(BeanFactory beanFactory, CommandLine.Help.Ansi ansi, String... args) {
        CliConsole console = beanFactory.create(CliConsole.class);
        CommandLine commandLine = initializeCommandLine(beanFactory, console);
        commandLine.setColorScheme(CommandLine.Help.defaultColorScheme(ansi));
        int exitCode = commandLine.execute(args);
        console.flush();
        return exitCode;
    }

    private static CommandLine initializeCommandLine(BeanFactory factory, CliConsole console) {
        CommandLine commandLine = new CommandLine(factory.create(MainCommand.class), factory);
        commandLine.setOut(console.writer());
        commandLine.setErr(console.ewriter());
        commandLine.setExecutionExceptionHandler(factory.create(ExceptionHandler.class));
        commandLine.registerConverter(Path.class, new PathTypeConverter());
        return commandLine;
    }
}
