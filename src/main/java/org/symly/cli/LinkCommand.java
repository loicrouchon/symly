package org.symly.cli;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.NoOpFileSystemWriter;
import org.symly.links.*;
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

    private final CliConsole console;

    private final FileSystemReader fsReader;

    private final FileSystemWriter fileSystemWriter;

    private int updates;
    private Context context;

    public LinkCommand(CliConsole console, FileSystemReader fsReader, FileSystemWriter fileSystemWriter) {
        this.console = Objects.requireNonNull(console);
        this.fsReader = Objects.requireNonNull(fsReader);
        this.fileSystemWriter = Objects.requireNonNull(fileSystemWriter);
    }

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
        try (var linkStates = context.status(fsReader)) {
            linkStates.forEach(linkState -> {
                if (linkState.type() != LinkState.Type.UP_TO_DATE) {
                    updates++;
                }
                List<Action> actions = linkState.toActions(fsReader, force);
                for (Action action : actions) {
                    Result<Void, Action.Code> result = action.apply(fsReader, fsWriter);
                    printStatus(linkState, action, result);
                }
            });
        }
    }

    private void printStatus(LinkState linkState, Action action, Result<Void, Action.Code> result) {
        result.accept(success -> printAction(action), error -> printError(linkState, action, error));
    }

    private void printAction(Action action) {
        if (action instanceof NoOpAction a) {
            printAction(Level.DEBUG, "up-to-date", a.link());
        } else if (action instanceof CreateLinkAction a) {
            printAction(Level.INFO, "added", a.link());
        } else if (action instanceof DeleteLinkAction a) {
            printAction(Level.INFO, "deleted", a.link());
        } else if (action instanceof DeleteAction a) {
            printAction(Level.INFO, "deleted", new Link(a.path(), null));
        } else if (action instanceof ConflictAction a) {
            printAction(Level.INFO, "!conflict", a.link());
        } else {
            throw new IllegalStateException("Not reachable, normally " + action);
        }
    }

    private void printAction(Level level, String actionType, Link link) {
        console.printf(level, "%-12s %s%n", actionType + ":", link.toString(context.mainDirectory()));
    }

    private void printError(LinkState linkState, Action action, Action.Code error) {
        printAction(action);
        String details =
                switch (error.state()) {
                    case INVALID_SOURCE -> "Source %s does not exist".formatted(action.path());
                    case INVALID_DESTINATION -> "Destination %s does not exist".formatted(linkState.desiredTarget());
                    case CONFLICT -> "Regular file %s already exist. To overwrite it, use the -f (--force) option."
                            .formatted(action.path());
                    case ERROR -> "An error occurred during linkage: - %s".formatted(error.details());
                };
        if (dryRun) {
            console.eprintf("> %s%n", details);
        } else {
            throw new SymlyExecutionException(
                    "Unable to create link %s%n> %s%n".formatted(linkState.desired(), details));
        }
    }
}
