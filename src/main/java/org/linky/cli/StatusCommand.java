package org.linky.cli;

import static picocli.CommandLine.Help;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.linky.cli.validation.Constraint;
import org.linky.files.FileSystemReader;
import org.linky.links.Link;
import org.linky.links.Links;
import org.linky.links.Status;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "status",
        aliases = {"st"},
        description = "Displays the current synchronization status"
)
class StatusCommand extends ValidatedCommand {

    @Option(
            names = {"-d", "--destination"},
            description = "Destination directory in which links will be created",
            required = true,
            showDefaultValue = Help.Visibility.ALWAYS
    )
    Path destination;

    @Option(
            names = {"-s", "--sources"},
            description = "Source directories containing files to link in destination",
            required = true,
            arity = "1..*"
    )
    List<Path> sources;

    private final FileSystemReader fsReader;

    StatusCommand() {
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
            Status status = link.status(fsReader);
            printStatus(console, status);
        }
    }

    private void printStatus(CliConsole console, Status status) {
        Link link = status.getLink();
        console.printf("[%-" + Status.Type.MAX_LENGTH + "s] %s%n", status.getType(), link);
        if (status.getType() == Status.Type.LINK_CONFLICT) {
            Path realPath = fsReader.readSymbolicLink(link.getFrom());
            console.printf("> Symbolic link conflict. Current target us %s%n", realPath);
        }
    }
}