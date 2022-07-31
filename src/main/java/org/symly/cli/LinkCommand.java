package org.symly.cli;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.NoOpFileSystemWriter;
import org.symly.links.Action;
import org.symly.links.Link;
import org.symly.links.Status;
import org.symly.repositories.ContextConfig;
import org.symly.repositories.ContextConfig.Context;
import org.symly.repositories.ContextConfig.InputContext;
import org.symly.repositories.LinksFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "link",
        aliases = {"ln"},
        description = "Create/update links from 'directory' to the 'to' repositories")
@RequiredArgsConstructor
class LinkCommand extends ValidatedCommand {

    @Option(
            names = {"-d", "--dir", "--directory"},
            paramLabel = "<main-directory>",
            description = "Main directory in which links will be created")
    Path mainDirectory;

    @Option(
            names = {"-r", "--repositories"},
            paramLabel = "<repositories>",
            description =
                    """
                Repositories containing files to link in the main directory. \
                Repositories are to be listed by decreasing priority as the first ones will \
                override the content of the later ones.""",
            arity = "0..*")
    List<Path> repositoriesList;

    @Option(
            names = {"--dry-run"},
            description = "Do not actually create links but only displays which ones would be created")
    boolean dryRun = false;

    @Option(
            names = {"-f", "--force"},
            description = "Force existing files and directories to be overwritten instead of failing in case of "
                    + "conflicts")
    boolean force = false;

    @Option(
            names = {"--max-depth"},
            paramLabel = "<max-depth>",
            description = "Depth of the lookup for orphans deletion")
    Integer maxDepth;

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
        InputContext inputContext = new InputContext(mainDirectory, repositoriesList, maxDepth);
        context = Context.from(fsReader, ContextConfig.read(fsReader), inputContext);
        validate(context.constraints(fsReader));
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
        deleteOrphans(mutator);
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
        for (Link link : context.links()) {
            Status status = link.status(fsReader);
            List<Action> actions = status.toActions(fsReader, force);
            for (Action action : actions) {
                Result<Path, Action.Code> result = action.apply(fsReader, fsWriter);
                if (!action.type().equals(Action.Type.UP_TO_DATE)) {
                    updates++;
                }
                printStatus(action, result);
            }
        }
    }

    private void deleteOrphans(FileSystemWriter mutator) {
        Stream<Link> orphans = linksFinder.findOrphans(
                context.mainDirectory().toPath(), context.orphanMaxDepth(), context.repositories());
        orphans.forEach(orphan -> {
            updates++;
            deleteOrphan(orphan, mutator);
        });
    }

    private void deleteOrphan(Link orphan, FileSystemWriter mutator) {
        Action action = Action.delete(orphan);
        Result<Path, Action.Code> status = action.apply(fsReader, mutator);
        printStatus(action, status);
    }

    private void printStatus(Action action, Result<Path, Action.Code> result) {
        result.accept(previousLink -> printAction(action, previousLink), error -> printError(action, error));
    }

    private void printAction(Action action, Path previousLink) {
        Link link = action.link();
        switch (action.type()) {
            case UP_TO_DATE -> printAction(Level.DEBUG, "up-to-date", link);
            case CREATE -> printAction(Level.INFO, "added", link);
            case MODIFY -> {
                if (previousLink == null) {
                    throw new IllegalStateException("Expecting a previous link to be found for " + link.source());
                }
                printAction(Level.INFO, "deleted", new Link(link.source(), previousLink));
                printAction(Level.INFO, "added", link);
            }
            case DELETE -> printAction(Level.INFO, "deleted", link);
            case CONFLICT -> printAction(Level.INFO, "!conflict", link);
        }
    }

    private void printAction(Level level, String actionType, Link link) {
        console.printf(level, "%-12s %s%n", actionType + ":", link.toString(context.mainDirectory()));
    }

    private void printError(Action action, Action.Code error) {
        printAction(action, error.previousPath());
        Link link = action.link();
        String details =
                switch (error.state()) {
                    case INVALID_SOURCE -> String.format("Source %s does not exist", link.source());
                    case INVALID_DESTINATION -> String.format("Destination %s does not exist", link.target());
                    case CONFLICT -> String.format(
                            "Regular file %s already exist. To overwrite it, use the -f (--force) option.",
                            link.source());
                    case ERROR -> String.format("An error occurred during linkage: - %s", error.details());
                };
        if (dryRun) {
            console.eprintf("> %s%n", details);
        } else {
            throw new SymlyExecutionException(String.format("Unable to create link %s%n> %s%n", link, details));
        }
    }
}
