package org.symly.cli;

import org.symly.cli.converters.RepositoryTypeConverter;
import org.symly.cli.converters.SourceDirectoryTypeConverter;
import org.symly.links.Repository;
import org.symly.links.MainDirectory;
import picocli.CommandLine;

public class Main {

    /**
     * Done as part of the Main class static initializer to benefit from GraalVM native-image static initialization
     * optimization.
     */
    private static final CommandLine COMMAND_LINE = initializeCommandLine();

    public static void main(String... args) {
        int exitCode = runCommand(args);
        System.exit(exitCode);
    }

    private static int runCommand(String... args) {
        return COMMAND_LINE.execute(args);
    }

    private static CommandLine initializeCommandLine() {
        BeanFactory factory = new BeanFactory();
        // Instantiate at compile time thanks to GraalVM native-image optimization
        // Doing so also avoids to have to include resources in the native-image.
        factory.preInit();
        CliConsole console = factory.create(CliConsole.class);
        CommandLine commandLine = new CommandLine(factory.create(MainCommand.class), factory);
        commandLine.setOut(console.writer());
        commandLine.setErr(console.ewriter());
        commandLine.setDefaultValueProvider(new EnvironmentVariableDefaultsProvider());
        commandLine.setExecutionExceptionHandler(new ExceptionHandler(console));
        commandLine.registerConverter(MainDirectory.class, new SourceDirectoryTypeConverter());
        commandLine.registerConverter(Repository.class, new RepositoryTypeConverter());
        return commandLine;
    }
}
