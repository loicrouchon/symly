package org.linky.cli;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.cli.validation.Constraint;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriter;
import org.linky.files.NoOpFileSystemWriter;
import org.linky.links.*;
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
            names = {"-s", "--source-directory"},
            paramLabel = "<source-directory>",
            description = "Source directory in which links will be created",
            required = true,
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    SourceDirectory source;

    @Option(
            names = {"-r", "--repositories"},
            paramLabel = "<repositories>",
            description = "Target directories containing files to link in destination",
            required = true,
            arity = "1..*"

    )
    List<Repository> repositories;

    @Option(
            names = {"--dry-run"},
            description = "Do not actually create links but only displays which ones would be created"
    )
    boolean dryRun;

    @NonNull
    private final CliConsole console;
    @NonNull
    private final FileSystemReader fsReader;
    @NonNull
    private final FileSystemWriter fileSystemWriter;

    @Override
    protected Collection<Constraint> constraints() {
        return List.of(
                Constraint.ofArg("source-directory", source, "must be an existing directory",
                        fsReader::isADirectory),
                Constraint.ofArg("repositories", repositories, "must be an existing directory",
                        fsReader::isADirectory)
        );
    }

    @Override
    public void execute() {
        console.printf("Creating links ");
        if (dryRun) {
            console.printf("(dry-run mode) ");
        }
        console.printf("from %s to %s%n", repositories, source);
        Links links = Links.from(source, repositories);
        FileSystemWriter mutator = getFilesMutatorService();
        createLinks(links, mutator);
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
            Action action = status.toAction();
            Result<Path, Action.Code> result = action.apply(fsWriter);
            printStatus(action, result);
        }
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
                        "Expecting a previous link to be found for " + link.getSource());
            }
        }
    }

    private void printError(Action action, Action.Code error) {
        printAction(action, error.getPreviousPath());
        String details;
        Link link = action.getLink();
        switch (error.getState()) {
            case INVALID_DESTINATION:
                details = String.format("Destination %s does not exist", link.getTarget());
                break;
            case CONFLICT:
                details = String.format(
                        "Regular file %s already exist. To overwrite it, use the --replace-file option.",
                        link.getSource());
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
            throw new LinkyExecutionException(String.format("Unable to create link %s%n> %s%n", link, details));
        }
    }
}
