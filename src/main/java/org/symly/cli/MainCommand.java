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
            StatusCommand.class,
            UnlinkCommand.class,
        })
@RequiredArgsConstructor
class MainCommand implements Runnable {

    @Option(
            names = {"-h", "--help"},
            usageHelp = true,
            description = "Prints this help message and exits",
            scope = CommandLine.ScopeType.INHERIT)
    boolean helpRequested;

    @Option(
            names = {"-v", "--verbose"},
            description = "Be verbose.",
            scope = CommandLine.ScopeType.INHERIT)
    boolean verbose = false;

    @Option(
            names = {"-V", "--version"},
            description = "Prints version information.")
    boolean version = false;

    @Spec
    CommandSpec spec;

    @NonNull
    private final Config config;

    @NonNull
    private final CliConsole console;

    @Override
    public void run() {
        config.verbose(verbose);
        CommandLine commandLine = spec.commandLine();
        if (helpRequested || !version) {
            commandLine.usage(console.writer());
        }
        if (version) {
            commandLine.printVersionHelp(console.writer());
        }
    }
}
