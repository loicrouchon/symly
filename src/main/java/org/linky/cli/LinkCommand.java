package org.linky.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesMutatorServiceImpl;
import org.linky.files.FilesReaderService;
import org.linky.files.NoOpFilesMutatorService;
import org.linky.links.Action;
import org.linky.links.Link;
import org.linky.links.Links;
import org.linky.links.Status;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "link",
        description = "Synchronize the links status"
)
@RequiredArgsConstructor
public class LinkCommand implements Runnable {

    @Option(
            names = {"-d", "--destination"},
            description = "Destination directory in which links will be created",
            required = true,
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    Path destination;

    @Option(
            names = {"-s", "--source"},
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

    @Override
    public void run() {
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
        Links links = Links.from(destination, sources);
        FilesReaderService reader = new FilesReaderService();
        FilesMutatorService mutator = getFilesMutatorService();
        createLinks(console, links, reader, mutator);
    }

    private FilesMutatorService getFilesMutatorService() {
        if (dryRun) {
            return new NoOpFilesMutatorService();
        }
        return new FilesMutatorServiceImpl();
    }

    private void createLinks(CliConsole console,
            Links links,
            FilesReaderService filesReaderService,
            FilesMutatorService filesMutatorService) {
        for (Link link : links.list()) {
            Status status = link.status(filesReaderService);
            Action action = status.toAction();
            Result<Path, Action.Code> result = action.apply(filesMutatorService);
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
                        "Expecting a previous link to be found for " + link.getFrom());
            }
        }
    }

    private void printError(CliConsole console, Action action, Action.Code error) {
        printAction(console, action, error.getPreviousPath());
        String details;
        Link link = action.getLink();
        switch (error.getState()) {
            case INVALID_DESTINATION:
                details = String.format("Destination %s does not exist", link.getTo());
                break;
            case CONFLICT:
                details = String.format(
                        "Regular file %s already exist. To overwrite it, use the --replace-file option.",
                        link.getFrom());
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
