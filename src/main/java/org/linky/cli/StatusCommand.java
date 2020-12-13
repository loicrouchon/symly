package org.linky.cli;

import static picocli.CommandLine.Help;
import static picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.linky.files.FilesReaderService;
import org.linky.links.Link;
import org.linky.links.Links;
import org.linky.links.Status;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

@Command(
        name = "status",
        aliases = {"st"},
        description = "Displays the current synchronization status"
)
public class StatusCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Option(
            names = {"-d", "--destination"},
            description = "Destination directory in which links will be created",
            required = true,
            showDefaultValue = Help.Visibility.ALWAYS
    )
    Path destination;

    @Option(
            names = {"-s", "--source"},
            description = "Source directories containing files to link in destination",
            required = true,
            arity = "1..*"
    )
    List<Path> sources;

    private final FilesReaderService filesReaderService;
    private final Validators validators;

    public StatusCommand() {
        filesReaderService = new FilesReaderService();
        validators = new Validators(filesReaderService);
    }

    @Override
    public void run() {
        Arg.of(spec, "--destination").validate(destination, validators::directoryExists);
        Arg.of(spec, "--sources").validate(sources, validators::directoryExists);
        CliConsole console = CliConsole.console();
        console.printf(
                "Checking links status from %s to %s%n",
                sources.stream()
                        .map(Path::toAbsolutePath)
                        .map(Path::normalize)
                        .collect(Collectors.toList()),
                destination.toAbsolutePath().normalize());
        Links links = Links.from(destination, sources);
        checkStatus(console, links);
    }

    private void checkStatus(CliConsole console, Links links) {
        for (Link link : links.list()) {
            Status status = link.status(filesReaderService);
            printStatus(console, status);
        }
    }

    private void printStatus(CliConsole console, Status status) {
        Link link = status.getLink();
        console.printf("[%-" + Status.Type.MAX_LENGTH + "s] %s%n", status.getType(), link);
        if (status.getType() == Status.Type.LINK_CONFLICT) {
            Path realPath = filesReaderService.toRealPath(link.getFrom());
            console.printf("> Symbolic link conflict. Current target us %s%n", realPath);
        }
    }
}
