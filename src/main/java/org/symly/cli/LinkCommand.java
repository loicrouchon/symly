package org.symly.cli;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.cli.validation.Constraint;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.NoOpFileSystemWriter;
import org.symly.links.*;
import org.symly.orphans.OrphanFinder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "link",
        aliases = {"ln"},
        description = "Synchronizes the links from the target directories to the destination"
)
@RequiredArgsConstructor
class LinkCommand extends ValidatedCommand {

    @Option(
            names = {"-d", "--dir", "--directory"},
            paramLabel = "<main-directory>",
            description = "Main directory in which links will be created",
            required = true,
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    MainDirectory mainDirectory;

    @Option(
            names = {"-t", "--to"},
            paramLabel = "<repositories>",
            description = "Target directories (a.k.a. repositories) containing files to link in the main directory",
            required = true,
            arity = "1..*"
    )
    List<Repository> repositories;

    @Option(
            names = {"--dry-run"},
            description = "Do not actually create links but only displays which ones would be created"
    )
    boolean dryRun = false;

    @Option(
            names = {"-f", "--force"},
            description = "Force existing files and directories to be overwritten instead of failing in case of "
                    + "conflicts"
    )
    boolean force = false;

    @Option(
            names = {"--max-depth"},
            paramLabel = "<max-depth>",
            description = "Depth of the lookup for orphans deletion"
    )
    int maxDepth = 2;

    @NonNull
    private final CliConsole console;
    @NonNull
    private final FileSystemReader fsReader;
    @NonNull
    private final FileSystemWriter fileSystemWriter;

    @Override
    protected Collection<Constraint> constraints() {
        return List.of(
                Constraint.ofArg("main-directory", mainDirectory, "must be an existing directory",
                        fsReader::isADirectory),
                Constraint.ofArg("repositories", repositories, "must be an existing directory",
                        fsReader::isADirectory),
                Constraint.ofArg("max-depth", maxDepth, "must be a positive integer",
                        depth -> depth >= 0)
        );
    }

    @Override
    public void execute() {
        console.printf("Creating links ");
        if (dryRun) {
            console.printf("(dry-run mode) ");
        }
        console.printf("in %s to %s%n", mainDirectory, repositories);
        Links links = Links.from(mainDirectory, repositories);
        FileSystemWriter mutator = getFilesMutatorService();
        createLinks(links, mutator);
        deleteOrphans(mainDirectory, repositories, mutator);
    }

    private FileSystemWriter getFilesMutatorService() {
        if (dryRun) {
            return new NoOpFileSystemWriter();
        }
        return fileSystemWriter;
    }

    private void createLinks(Links links, FileSystemWriter fsWriter) {
        for (Link link : links.list()) {
            Status status = link.status(fsReader);
            List<Action> actions = status.toActions(force);
            for (Action action : actions) {
                Result<Path, Action.Code> result = action.apply(fsWriter);
                printStatus(action, result);
            }
        }
    }

    private void deleteOrphans(MainDirectory mainDirectory, List<Repository> repositories, FileSystemWriter mutator) {
        OrphanFinder orphanFinder = new OrphanFinder(fsReader);
        Collection<Link> orphans = orphanFinder.findOrphans(mainDirectory.toPath(), maxDepth, repositories);
        orphans.forEach(orphan -> deleteOrphan(orphan, mutator));
    }

    private void deleteOrphan(Link orphan, FileSystemWriter mutator) {
        Action action = Action.delete(orphan, fsReader);
        Result<Path, Action.Code> status = action.apply(mutator);
        printStatus(action, status);
    }

    private void printStatus(Action action, Result<Path, Action.Code> result) {
        result.accept(
                previousLink -> printAction(action, previousLink),
                error -> printError(action, error)
        );
    }

    private void printAction(Action action, Path previousLink) {
        Link link = action.getLink();
        console.printf("[%-" + Action.Type.MAX_LENGTH + "s] %s%n", action.getType(), link);
        if (action.getType().equals(Action.Type.UPDATE)) {
            if (previousLink != null) {
                console.printf("> Previous link target was %s%n", previousLink);
            } else {
                throw new IllegalStateException(
                        "Expecting a previous link to be found for " + link.source());
            }
        }
    }

    private void printError(Action action, Action.Code error) {
        printAction(action, error.getPreviousPath());
        String details;
        Link link = action.getLink();
        switch (error.getState()) {
            case INVALID_SOURCE:
                details = String.format("Source %s does not exist", link.source());
                break;
            case INVALID_DESTINATION:
                details = String.format("Destination %s does not exist", link.target());
                break;
            case CONFLICT:
                details = String.format(
                        "Regular file %s already exist. To overwrite it, use the --replace-file option.",
                        link.source());
                break;
            case ERROR:
                details = String.format("An error occurred during linkage: - %s", error.getDetails());
                break;
            default:
                throw new UnsupportedOperationException("Unknown error " + error.getState());
        }
        if (dryRun) {
            console.eprintf("> %s%n", details);
        } else {
            throw new SymlyExecutionException(String.format("Unable to create link %s%n> %s%n", link, details));
        }
    }
}
