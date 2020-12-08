package org.linky.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.linky.files.FilesReaderService;
import org.linky.links.Link;
import org.linky.links.Links;
import org.linky.links.Status;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "status",
        description = "Displays the current synchronization status"
)
@RequiredArgsConstructor
public class StatusCommand implements Runnable {

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

    @Override
    public void run() {
        CliConsole console = CliConsole.console();
        console.printf(
                "Checking links status from %s to %s%n",
                sources.stream()
                        .map(Path::toAbsolutePath)
                        .map(Path::normalize)
                        .collect(Collectors.toList()),
                destination.toAbsolutePath().normalize());
        Links links = Links.from(destination, sources);
        FilesReaderService reader = new FilesReaderService();
        checkStatus(console, links, reader);
    }

    private void checkStatus(CliConsole console, Links links, FilesReaderService filesReaderService) {
        for (Link link : links.list()) {
            Status status = link.status(filesReaderService);
            printStatus(console, status, filesReaderService);
        }
    }

    private void printStatus(CliConsole console, Status status, FilesReaderService filesReaderService) {
        Link link = status.getLink();
        console.printf("[%-" + Status.Type.MAX_LENGTH + "s] %s%n", status.getType(), link);
        if (status.getType() == Status.Type.LINK_CONFLICT) {
            Path realPath = filesReaderService.toRealPath(link.getFrom());
            console.printf("> Symbolic link conflict. Current target us %s%n", realPath);
        }
    }
}
