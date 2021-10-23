package org.symly.cli;

import static picocli.CommandLine.Command;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "symly",
        description = "symly create links",
        versionProvider = VersionProvider.class,
        subcommands = {
                LinkCommand.class,
                StatusCommand.class
        }
)
@RequiredArgsConstructor
class MainCommand implements Runnable {

    @Option(names = {"-v", "--verbose"},
            description = "Be verbose.")
    boolean verbose = false;

    @Option(names = {"-V", "--version"},
            description = "Prints version information.")
    boolean version = false;

    @Option(names = {"-h", "--help"},
            usageHelp = true,
            description = "Prints this help message and exits")
    boolean helpRequested;

    @Spec
    private CommandSpec spec;

    @NonNull
    private final CliConsole console;

    @Override
    public void run() {
        CommandLine commandLine = spec.commandLine();
        if (helpRequested || !version) {
            commandLine.usage(console.writer());
        }
        if (version) {
            commandLine.printVersionHelp(console.writer());
        }
    }
}
