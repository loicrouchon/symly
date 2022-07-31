package org.symly.cli;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;
import org.symly.links.Status;
import org.symly.repositories.ContextConfig;
import org.symly.repositories.ContextConfig.Context;
import org.symly.repositories.ContextConfig.InputContext;
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
            description = "Main directory in which links will be created")
    Path mainDirectory;

    @Option(
            names = {"-r", "--repositories"},
            paramLabel = "<repositories>",
            description =
                    """
            Repositories containing files to link in the main directory. \
            Repositories are to be listed by decreasing priority as the first ones will \
            override the content of the later ones.""",
            arity = "0..*")
    List<Path> repositoriesList;

    @NonNull
    private final CliConsole console;

    @NonNull
    private final FileSystemReader fsReader;

    private Context context;

    @Override
    public void run() {
        InputContext inputContext = new InputContext(mainDirectory, repositoriesList);
        context = Context.from(fsReader, ContextConfig.read(fsReader), inputContext);
        validate(context.constraints(fsReader));
        console.printf(
                Level.DEBUG,
                "Checking links status from %s to %s%n",
                context.mainDirectory(),
                context.repositories().repositories());
        checkStatus(console);
    }

    private void checkStatus(CliConsole console) {
        int updates = 0;
        for (Link link : context.links()) {
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
                    case LINK_CONFLICT, FILE_CONFLICT -> "!conflict";
                };
        Level level;
        if (status.type().equals(Status.Type.UP_TO_DATE)) {
            level = Level.DEBUG;
        } else {
            level = Level.INFO;
        }
        console.printf(level, "%-12s%s%n", statusType, link.toString(context.mainDirectory()));
        if (status.type() == Status.Type.LINK_CONFLICT) {
            Path realPath = fsReader.readSymbolicLink(link.source());
            console.printf("> Symbolic link conflict. Current target is %s%n", realPath);
        }
    }
}
