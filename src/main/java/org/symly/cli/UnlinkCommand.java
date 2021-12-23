package org.symly.cli;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.cli.validation.Constraint;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;
import org.symly.files.NoOpFileSystemWriter;
import org.symly.links.Action;
import org.symly.links.Link;
import org.symly.repositories.LinksFinder;
import org.symly.repositories.MainDirectory;
import org.symly.repositories.Repositories;
import org.symly.repositories.Repository;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "unlink",
    description = "Remove links in 'directory' pointing to the 'to' repositories"
)
@RequiredArgsConstructor
class UnlinkCommand extends ValidatedCommand {

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
    List<Repository> allRepositories;

    @Option(
        names = {"--dry-run"},
        description = "Do not actually remove links but only displays which ones would be removed"
    )
    boolean dryRun = false;

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
            Constraint.ofArg("repositories", allRepositories, "must be an existing directory",
                fsReader::isADirectory),
            Constraint.ofArg("max-depth", maxDepth, "must be a positive integer",
                depth -> depth >= 0)
        );
    }

    @Override
    public void execute() {
        console.printf("Removing links ");
        if (dryRun) {
            console.printf("(dry-run mode) ");
        }
        console.printf("in %s to %s%n", mainDirectory, allRepositories);
        Repositories repositories = Repositories.of(allRepositories);
        FileSystemWriter mutator = getFilesMutatorService();
        deleteOrphans(mainDirectory, repositories, mutator);
    }

    private FileSystemWriter getFilesMutatorService() {
        if (dryRun) {
            return new NoOpFileSystemWriter();
        }
        return fileSystemWriter;
    }

    private void deleteOrphans(MainDirectory mainDirectory, Repositories repositories, FileSystemWriter mutator) {
        LinksFinder finder = new LinksFinder(fsReader);
        finder
            .findLinks(mainDirectory.toPath(), maxDepth, repositories)
            .forEach(orphan -> deleteOrphan(orphan, mutator));
    }

    private void deleteOrphan(Link orphan, FileSystemWriter mutator) {
        Action action = Action.delete(orphan);
        Result<Path, Action.Code> status = action.apply(fsReader, mutator);
        printStatus(action, status);
    }

    private void printStatus(Action action, Result<Path, Action.Code> result) {
        result.accept(
            previousLink -> printAction(action, previousLink),
            error -> printError(action, error)
        );
    }

    private void printAction(Action action, Path previousLink) {
        Link link = action.link();
        console.printf("%-" + Action.Type.MAX_LENGTH + "s %s%n", action.type(), link);
        if (!Objects.equals(link.target(), previousLink)) {
            console.printf("> Previous link target was %s%n", previousLink);
        }
    }

    private void printError(Action action, Action.Code error) {
        printAction(action, error.previousPath());
        Link link = action.link();
        String details = switch (error.state()) {
            case INVALID_SOURCE -> String.format("Source %s does not exist", link.source());
            case INVALID_DESTINATION -> String.format("Destination %s does not exist", link.target());
            case CONFLICT -> String.format(
                "Regular file %s already exist. To overwrite it, use the --replace-file option.",
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
