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
        description =
                """
            Symly creates, updates and removes links allowing for \
            centralized management of sparse file-trees.""",
        versionProvider = VersionProvider.class,
        subcommands = {
            LinkCommand.class,
            StatusCommand.class,
            UnlinkCommand.class,
        })
@RequiredArgsConstructor
class MainCommand implements Runnable {

    @SuppressWarnings("unused") // used by picocli
    @Option(
            names = {"-h", "--help"},
            usageHelp = true,
            description = "Prints this help message and exits",
            scope = CommandLine.ScopeType.INHERIT)
    boolean helpRequested;

    @SuppressWarnings("unused") // used by picocli
    @Option(
            names = {"-v", "--verbose"},
            description = "Be verbose.",
            scope = CommandLine.ScopeType.INHERIT)
    public void setVerbose(boolean verbose) {
        config.verbose(verbose);
        if (verbose) {
            console.enableVerboseMode();
        }
    }

    @SuppressWarnings("unused") // used by picocli
    @Option(
            names = {"-V", "--version"},
            description = "Prints version information.",
            versionHelp = true)
    boolean version = false;

    @Spec
    CommandSpec spec;

    @NonNull
    private final Config config;

    @NonNull
    private final CliConsole console;

    @Override
    public void run() {
        CommandLine commandLine = spec.commandLine();
        if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(console.writer());
        } else {
            commandLine.usage(console.writer());
        }
    }
}
