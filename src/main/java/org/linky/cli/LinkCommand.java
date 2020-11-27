package org.linky.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesMutatorServiceImpl;
import org.linky.files.FilesReaderService;
import org.linky.files.NoOpFilesMutatorService;
import org.linky.links.Link;
import org.linky.links.Links;
import org.linky.links.SourceReader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "link",
        description = "link"
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
        console.printf(
                "Creating links from %s to %s%n",
                sources.stream()
                        .map(Path::toAbsolutePath)
                        .map(Path::normalize)
                        .collect(Collectors.toList()),
                destination.toAbsolutePath().normalize());
        Links links = computeLinks();
        FilesReaderService reader = new FilesReaderService();
        FilesMutatorService mutator = getFilesMutatorService();
        createLinks(console, links, reader, mutator);
    }

    private Links computeLinks() {
        Links links = new Links(destination);
        for (Path source : sources) {
            final SourceReader reader = new SourceReader(source);
            reader.read().forEach(path -> links.add(path, source));
        }
        return links;
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
            Link.LinkingStatus status = link.create(filesReaderService, filesMutatorService);
            printStatus(console, link, status);
            if (!status.getStatus().isSuccessful()) {
                throw new LinkyExecutionException(String.format("Unable to create link %s", link));
            }
        }
    }

    private void printStatus(CliConsole console, Link link, Link.LinkingStatus status) {
        console.printf("[%s] %s", status.getStatus(), link);
        switch (status.getStatus()) {
            case UPDATED:
                console.printf(" - updated from %s%n", status.getDetails());
                break;
            case ERROR:
                console.printf(" - %s%n", status.getDetails());
                break;
            case INVALID_DESTINATION:
                console.printf(" - destination does not exist%n");
                break;
            case CREATED:
            case UP_TO_DATE:
            case CONFLICT:
            default:
                console.printf("%n");
                break;
        }
    }
}
