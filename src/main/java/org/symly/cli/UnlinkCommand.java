package org.symly.cli;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.stream.Stream;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.NoOpFileSystemWriter;
import org.symly.links.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
        name = "unlink",
        description =
                """
    Remove links in the 'directory' pointing to the 'repositories'.

    Note this operation does not restore files in the destination. \
    If that is the desired behavior, use the 'restore' command instead.
    """)
class UnlinkCommand implements Runnable {

    @Mixin
    ContextInput contextInput;

    @Option(
            names = {"--dry-run"},
            description = "Do not actually remove links but only displays which ones would be removed")
    boolean dryRun = false;

    private final CliConsole console;

    private final FileSystemReader fsReader;

    private final FileSystemWriter fileSystemWriter;

    private Context context;

    UnlinkCommand(CliConsole console, FileSystemReader fsReader, FileSystemWriter fileSystemWriter) {
        this.console = Objects.requireNonNull(console);
        this.fsReader = Objects.requireNonNull(fsReader);
        this.fileSystemWriter = Objects.requireNonNull(fileSystemWriter);
    }

    @Override
    public void run() {
        context = contextInput.context();
        console.printf(Level.DEBUG, "Removing links ");
        if (dryRun) {
            console.printf(Level.DEBUG, "(dry-run mode) ");
        }
        console.printf(
                Level.DEBUG,
                "in %s to %s%n",
                context.mainDirectory(),
                context.repositories().repositories());
        FileSystemWriter mutator = getFilesMutatorService();
        unlink(mutator);
    }

    private FileSystemWriter getFilesMutatorService() {
        if (dryRun) {
            return new NoOpFileSystemWriter();
        }
        return fileSystemWriter;
    }

    private void unlink(FileSystemWriter mutator) {
        try (Stream<LinkState> linkStates = context.status(fsReader)) {
            linkStates
                    .filter(ls -> ls.currentState() instanceof LinkState.Entry.LinkEntry le
                            && context.repositories().containsPath(le.target()))
                    .forEach(ls -> unlink(ls, mutator));
        }
    }

    private void unlink(LinkState linkState, FileSystemWriter mutator) {
        Action action = Action.deleteLink(
                new Link(linkState.source(), ((LinkState.Entry.LinkEntry) linkState.currentState()).target()));
        Result<Void, Action.Code> result = action.apply(fsReader, mutator);
        printStatus(linkState, action, result);
    }

    private void printStatus(LinkState linkState, Action action, Result<Void, Action.Code> result) {
        result.accept(previousLink -> printAction(linkState, action), error -> printError(linkState, action, error));
    }

    private void printAction(LinkState linkState, Action action) {
        if (!(action instanceof DeleteLinkAction dla)) {
            throw new SymlyExecutionException(
                    "Unable to unlink %s%n> Invalid action type %s%n".formatted(linkState, action.getClass()));
        }
        console.printf("%-12s %s%n", "unlink" + ":", dla.link().toString(context.mainDirectory()));
    }

    private void printError(LinkState linkState, Action action, Action.Code error) {
        printAction(linkState, action);
        String details = "An error occurred while deleting link: %s%n> - %s: %s"
                .formatted(linkState.source(), error.state(), error.details());
        if (dryRun) {
            console.eprintf("> %s%n", details);
        } else {
            throw new SymlyExecutionException("Unable to unlink %s%n> %s%n".formatted(linkState.source(), details));
        }
    }
}
