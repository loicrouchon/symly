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
import org.symly.links.Action;
import org.symly.links.Link;
import org.symly.repositories.LinksFinder;
import org.symly.repositories.MainDirectory;
import org.symly.repositories.Repositories;
import org.symly.repositories.Repository;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "unlink", description = "Remove links in 'directory' pointing to the 'to' repositories")
@RequiredArgsConstructor
class UnlinkCommand extends ValidatedCommand {

    @Option(
            names = {"-d", "--dir", "--directory"},
            paramLabel = "<main-directory>",
            description = "Main directory in which links will be created",
            required = true,
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    MainDirectory mainDirectory;

    @Option(
            names = {"-r", "--repositories"},
            paramLabel = "<repositories>",
            description =
                    """
                Repositories containing files to link in the main directory. \
                Repositories are to be listed by decreasing priority as the first ones will \
                override the content of the later ones.""",
            required = true,
            arity = "1..*")
    List<Repository> allRepositories;

    @Option(
            names = {"--dry-run"},
            description = "Do not actually remove links but only displays which ones would be removed")
    boolean dryRun = false;

    @Option(
            names = {"--max-depth"},
            paramLabel = "<max-depth>",
            description = "Depth of the lookup for orphans deletion")
    int maxDepth = 2;

    @NonNull
    private final CliConsole console;

    @NonNull
    private final FileSystemReader fsReader;

    @NonNull
    private final FileSystemWriter fileSystemWriter;

    @NonNull
    private final LinksFinder linksFinder;

    @Override
    protected Collection<Constraint> constraints() {
        return List.of(
                Constraint.ofArg(
                        "main-directory", mainDirectory, "must be an existing directory", fsReader::isADirectory),
                Constraint.ofArg(
                        "repositories", allRepositories, "must be an existing directory", fsReader::isADirectory),
                Constraint.ofArg("max-depth", maxDepth, "must be a positive integer", depth -> depth >= 0));
    }

    @Override
    public void execute() {
        console.printf("Removing links ");
        if (dryRun) {
            console.printf("(dry-run mode) ");
        }
        console.printf("in %s to %s%n", mainDirectory, allRepositories);
        Repositories repositories = Repositories.of(fsReader, allRepositories);
        FileSystemWriter mutator = getFilesMutatorService();
        unlink(mainDirectory, repositories, mutator);
    }

    private FileSystemWriter getFilesMutatorService() {
        if (dryRun) {
            return new NoOpFileSystemWriter();
        }
        return fileSystemWriter;
    }

    private void unlink(MainDirectory mainDirectory, Repositories repositories, FileSystemWriter mutator) {
        linksFinder
                .findLinks(mainDirectory.toPath(), maxDepth, repositories)
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
        console.printf("%-12s %s%n", "unlink" + ":", link);
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
