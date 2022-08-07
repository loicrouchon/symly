package org.symly.cli;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;
import org.symly.links.Status;
import org.symly.repositories.Context;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
        name = "status",
        aliases = {"st"},
        description = "Displays the current synchronization status")
@RequiredArgsConstructor
class StatusCommand implements Runnable {

    @Mixin
    ContextInput contextInput;

    @NonNull
    private final CliConsole console;

    @NonNull
    private final FileSystemReader fsReader;

    private Context context;

    @Override
    public void run() {
        context = contextInput.context();
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
