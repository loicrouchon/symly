package org.symly.cli;

import java.lang.System.Logger.Level;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.NoOpFileSystemWriter;
import org.symly.links.Action;
import org.symly.links.Link;
import org.symly.links.LinkStatus;
import org.symly.repositories.Context;
import org.symly.repositories.LinksFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(
        name = "link",
        aliases = {"ln"},
        description =
                """
            Create/update/delete links from 'directory' to the 'repositories'.

            Repositories should be specified with base layers first and overriding layers next. \
            In case two repositories contain a file with the same path, the file in the latest \
            repository will be used as the target for the link for the given path""")
@RequiredArgsConstructor
class LinkCommand implements Runnable {

    @Mixin
    ContextInput contextInput;

    @Option(
            names = {"--dry-run"},
            description = "Do not create links but only displays which ones would be created")
    boolean dryRun = false;

    @Option(
            names = {"-f", "--force"},
            description = "Force existing files and directories to be overwritten instead of failing in case of "
                    + "conflicts")
    boolean force = false;

    @NonNull
    private final CliConsole console;

    @NonNull
    private final FileSystemReader fsReader;

    @NonNull
    private final FileSystemWriter fileSystemWriter;

    @NonNull
    private final LinksFinder linksFinder;

    private int updates;
    private Context context;

    @Override
    public void run() {
        context = contextInput.context();
        updates = 0;
        console.printf(Level.DEBUG, "Creating links ");
        if (dryRun) {
            console.printf(Level.DEBUG, "(dry-run mode) ");
        }
        console.printf(
                Level.DEBUG,
                "in %s to %s%n",
                context.mainDirectory(),
                context.repositories().repositories());
        FileSystemWriter mutator = getFilesMutatorService();
        createLinks(mutator);
        if (updates == 0) {
            console.printf("Everything is already up to date%n");
        }
    }

    private FileSystemWriter getFilesMutatorService() {
        if (dryRun) {
            return new NoOpFileSystemWriter();
        }
        return fileSystemWriter;
    }

    private void createLinks(FileSystemWriter fsWriter) {
        try (var statuses = context.status(fsReader)) {
            statuses.map(status -> status.toActions(fsReader, force)).forEach(actions -> {
                for (Action action : actions) {
                    Result<Void, Action.Code> result = action.apply(fsReader, fsWriter);
                    if (!action.type().equals(Action.Type.UP_TO_DATE)) {
                        updates++;
                    }
                    printStatus(action, result);
                }
            });
        }
    }

    private void printStatus(Action action, Result<Void, Action.Code> result) {
        result.accept(success -> printAction(action), error -> printError(action, error));
    }

    private void printAction(Action action) {
        LinkStatus link = action.link();
        switch (action.type()) {
            case UP_TO_DATE -> printAction(Level.DEBUG, "up-to-date", link.desired());
            case CREATE -> printAction(Level.INFO, "added", link.desired());
            case MODIFY -> {
                if (link.currentTarget() == null) {
                    throw new IllegalStateException("Expecting a previous link to be found for " + link.source());
                }
                printAction(Level.INFO, "deleted", link.current());
                printAction(Level.INFO, "added", link.desired());
            }
            case DELETE -> printAction(Level.INFO, "deleted", link.current());
            case CONFLICT -> printAction(Level.INFO, "!conflict", link.desired());
        }
    }

    private void printAction(Level level, String actionType, Link link) {
        console.printf(level, "%-12s %s%n", actionType + ":", link.toString(context.mainDirectory()));
    }

    private void printError(Action action, Action.Code error) {
        LinkStatus link = action.link();
        printAction(action);
        String details =
                switch (error.state()) {
                    case INVALID_SOURCE -> "Source %s does not exist".formatted(link.source());
                    case INVALID_DESTINATION -> "Destination %s does not exist".formatted(link.desiredTarget());
                    case CONFLICT -> "Regular file %s already exist. To overwrite it, use the -f (--force) option."
                            .formatted(link.source());
                    case ERROR -> "An error occurred during linkage: - %s".formatted(error.details());
                };
        if (dryRun) {
            console.eprintf("> %s%n", details);
        } else {
            throw new SymlyExecutionException("Unable to create link %s%n> %s%n".formatted(link.desired(), details));
        }
    }
}
