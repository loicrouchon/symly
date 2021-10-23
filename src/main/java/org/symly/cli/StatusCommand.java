package org.symly.cli;

import static picocli.CommandLine.Help;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.cli.validation.Constraint;
import org.symly.files.FileSystemReader;
import org.symly.links.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "status",
        aliases = {"st"},
        description = "Displays the current synchronization status"
)
@RequiredArgsConstructor
class StatusCommand extends ValidatedCommand {

    @Option(
            names = {"-d", "--dir", "--directory"},
            paramLabel = "<main-directory>",
            description = "Main directory in which links will be created",
            required = true,
            showDefaultValue = Help.Visibility.ALWAYS
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

    @NonNull
    private final CliConsole console;
    @NonNull
    private final FileSystemReader fsReader;

    @Override
    protected Collection<Constraint> constraints() {
        return List.of(
                Constraint.ofArg("main-directory", mainDirectory, "must be an existing directory",
                        fsReader::isADirectory),
                Constraint.ofArg("repositories", repositories, "must be an existing directory",
                        fsReader::isADirectory)
        );
    }

    @Override
    public void execute() {
        console.printf("Checking links status from %s to %s%n", mainDirectory, repositories);
        Links links = Links.from(mainDirectory, repositories);
        checkStatus(console, links);
    }

    private void checkStatus(CliConsole console, Links links) {
        for (Link link : links.list()) {
            Status status = link.status(fsReader);
            printStatus(console, status);
        }
    }

    private void printStatus(CliConsole console, Status status) {
        Link link = status.link();
        console.printf("[%-" + Status.Type.MAX_LENGTH + "s] %s%n", status.type(), link);
        if (status.type() == Status.Type.LINK_CONFLICT) {
            Path realPath = fsReader.readSymbolicLink(link.source());
            console.printf("> Symbolic link conflict. Current target us %s%n", realPath);
        }
    }
}
