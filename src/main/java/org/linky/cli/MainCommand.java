package org.linky.cli;

import static picocli.CommandLine.Command;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "linky",
        description = "linky create links",
        version = "0.1",
        subcommands = {
                LinkCommand.class,
                StatusCommand.class,
                AddCommand.class,
        }
)
class MainCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Be verbose.")
    private boolean verbose = false;

    @Option(names = {"-h", "--help"}, usageHelp = true,
            description = "Prints this help message and exits")
    private boolean helpRequested;

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        CliConsole console = CliConsole.console();
        spec.commandLine().usage(console.writer());
    }
}
