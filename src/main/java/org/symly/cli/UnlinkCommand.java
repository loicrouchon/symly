package org.symly.cli;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.Comparator;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.NoOpFileSystemWriter;
import org.symly.links.Action;
import org.symly.links.Link;
import org.symly.repositories.Context;
import org.symly.repositories.LinksFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "unlink", description = "Remove links in 'directory' pointing to the 'repositories'")
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
        Action action = Action.delete(orphan);
        Result<Path, Action.Code> status = action.apply(fsReader, mutator);
        printStatus(action, status);
    }

    private void printStatus(Action action, Result<Path, Action.Code> result) {
        result.accept(previousLink -> printAction(action), error -> printError(action, error));
    }

    private void printAction(Action action) {
        Link link = action.link();
        if (!action.type().equals(Action.Type.DELETE)) {
            throw new SymlyExecutionException(
                    String.format("Unable to unlink %s%n> Invalid action type %s%n", link, action.type()));
        }
        console.printf("%-12s %s%n", "unlink" + ":", link.toString(context.mainDirectory()));
    }

    private void printError(Action action, Action.Code error) {
        printAction(action);
        Link link = action.link();
        String details = String.format(
                "An error occurred while deleting link: %s%n> - %s: %s", link, error.state(), error.details());
        if (dryRun) {
            console.eprintf("> %s%n", details);
        } else {
            throw new SymlyExecutionException(String.format("Unable to unlink %s%n> %s%n", link, details));
        }
    }
}
