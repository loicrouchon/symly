package org.linky.cli;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.linky.Result;
import org.linky.cli.validation.Constraint;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriter;
import org.linky.files.FileSystemWriterImpl;
import org.linky.files.NoOpFileSystemWriter;
import org.linky.links.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "link",
        aliases = {"ln"},
        description = "Synchronizes the links from the sources to the destination"
)
class LinkCommand extends ValidatedCommand {

    @Option(
            names = {"-d", "--destination"},
            description = "Destination directory in which links will be created",
            required = true,
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    Path destination;

    @Option(
            names = {"-s", "--sources"},
            description = "Source directories containing files to link in destination",
            required = true,
            arity = "1..*"
    )
    List<Path> sources;

    @Option(
            names = {"--dry-run"},
            description = "Do not actually create links but only displays which ones would be created"
    )
    boolean dryRun;

    private final FileSystemReader fsReader;

    LinkCommand() {
        fsReader = new FileSystemReader();
    }

    @Override
    protected Collection<Constraint> constraints() {
        return List.of(
                Constraint.ofArg("destination", destination, "must be an existing directory",
                        fsReader::isDirectory),
                Constraint.ofArg("sources", sources, "must be an existing directory", fsReader::isDirectory)
        );
    }

    @Override
    public void execute() {
        CliConsole console = CliConsole.console();
        console.printf("Creating links ");
        if (dryRun) {
            console.printf("(dry-run mode) ");
        }
        console.printf(
                "from %s to %s%n",
                sources.stream()
                        .map(Path::toAbsolutePath)
                        .map(Path::normalize)
                        .collect(Collectors.toList()),
                destination.toAbsolutePath().normalize());
        Links links = Links.from(destination, sources.stream().map(TargetDirectory::of).collect(Collectors.toList()));
        FileSystemWriter mutator = getFilesMutatorService();
        createLinks(console, links, mutator);
    }

    private FileSystemWriter getFilesMutatorService() {
        if (dryRun) {
            return new NoOpFileSystemWriter();
        }
        return new FileSystemWriterImpl();
    }

    private void createLinks(CliConsole console,
            Links links,
            FileSystemWriter fsWriter) {
        for (Link link : links.list()) {
            Status status = link.status(fsReader);
            Action action = status.toAction();
            Result<Path, Action.Code> result = action.apply(fsWriter);
            printStatus(console, action, result);
        }
    }

    private void printStatus(CliConsole console, Action action, Result<Path, Action.Code> result) {
        result.accept(
                previousLink -> printAction(console, action, previousLink),
                error -> printError(console, action, error)
        );
    }

    private void printAction(CliConsole console, Action action, Path previousLink) {
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

    private void printError(CliConsole console, Action action, Action.Code error) {
        printAction(console, action, error.getPreviousPath());
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
