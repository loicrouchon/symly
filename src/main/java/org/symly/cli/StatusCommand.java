package org.symly.cli;

import static picocli.CommandLine.Help;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.cli.validation.Constraint;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;
import org.symly.links.Status;
import org.symly.repositories.MainDirectory;
import org.symly.repositories.Repositories;
import org.symly.repositories.Repository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "status",
        aliases = {"st"},
        description = "Displays the current synchronization status")
@RequiredArgsConstructor
class StatusCommand extends ValidatedCommand {

    @Option(
            names = {"-d", "--dir", "--directory"},
            paramLabel = "<main-directory>",
            description = "Main directory in which links will be created",
            required = true,
            showDefaultValue = Help.Visibility.ALWAYS)
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
    List<Repository> repositoriesList;

    @NonNull
    private final CliConsole console;

    @NonNull
    private final FileSystemReader fsReader;

    @Override
    protected Collection<Constraint> constraints() {
        return List.of(
                Constraint.ofArg(
                        "main-directory", mainDirectory, "must be an existing directory", fsReader::isADirectory),
                Constraint.ofArg(
                        "repositories", repositoriesList, "must be an existing directory", fsReader::isADirectory));
    }

    @Override
    public void execute() {
        console.printf("Checking links status from %s to %s%n", mainDirectory, repositoriesList);
        Repositories repositories = Repositories.of(fsReader, repositoriesList);
        checkStatus(console, repositories);
    }

    private void checkStatus(CliConsole console, Repositories repositories) {
        int updates = 0;
        for (Link link : repositories.links(mainDirectory)) {
            Status status = link.status(fsReader);
            if (!status.type().equals(Status.Type.UP_TO_DATE)) {
                updates++;
            }
            printStatus(console, status);
        }
        if (updates == 0) {
            console.printf("Everything is already up to date%n");
        }
    }

    private void printStatus(CliConsole console, Status status) {
        Link link = status.link();
        String statusType =
                switch (status.type()) {
                    case UP_TO_DATE -> "up-to-date";
                    case MISSING -> "missing";
                    case LINK_CONFLICT, FILE_CONFLICT -> "conflict";
                };
        Level level;
        if (status.type().equals(Status.Type.UP_TO_DATE)) {
            level = Level.DEBUG;
        } else {
            level = Level.INFO;
        }
        console.printf(level, "%-12s%s%n", statusType, link.toString(mainDirectory));
        if (status.type() == Status.Type.LINK_CONFLICT) {
            Path realPath = fsReader.readSymbolicLink(link.source());
            console.printf("> Symbolic link conflict. Current target is %s%n", realPath);
        }
    }
}
