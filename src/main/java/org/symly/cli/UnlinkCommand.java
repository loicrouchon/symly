package org.symly.cli;

import java.lang.System.Logger.Level;
import java.util.Comparator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.NoOpFileSystemWriter;
import org.symly.links.Action;
import org.symly.links.Context;
import org.symly.links.DeleteLinkAction;
import org.symly.links.Link;
import org.symly.links.LinkState;
import org.symly.repositories.LinksFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "unlink", description = "Remove links in the 'directory' pointing to the 'repositories'")
@RequiredArgsConstructor
class UnlinkCommand implements Runnable {

    @Mixin
    ContextInput contextInput;

    @Option(
            names = {"--dry-run"},
            description = "Do not actually remove links but only displays which ones would be removed")
    boolean dryRun = false;

    @NonNull
    private final CliConsole console;

    @NonNull
    private final FileSystemReader fsReader;

    @NonNull
    private final FileSystemWriter fileSystemWriter;

    @NonNull
    private final LinksFinder linksFinder;

    private Context context;

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
        linksFinder
                .findLinks(context.mainDirectory().toPath(), context.orphanMaxDepth(), context.repositories())
                .sorted(Comparator.comparing(Link::source))
                .distinct()
                .forEach(orphan -> unlink(orphan, mutator));
    }

    private void unlink(Link orphan, FileSystemWriter mutator) {
        LinkState linkState = new LinkState(
                context.mainDirectory(), orphan.source(), LinkState.Entry.linkEntry(orphan.target(), false), null);
        for (Action action : linkState.toActions(fsReader, false)) {
            Result<Void, Action.Code> result = action.apply(fsReader, mutator);
            printStatus(linkState, action, result);
        }
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
