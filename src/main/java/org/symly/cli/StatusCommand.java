package org.symly.cli;

import java.lang.System.Logger.Level;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.files.FileSystemReader;
import org.symly.links.Context;
import org.symly.links.LinkState;
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

    private int updates;

    @Override
    public void run() {
        context = contextInput.context();
        console.printf(
                Level.DEBUG,
                "Checking links status from %s to %s%n",
                context.mainDirectory(),
                context.repositories().repositories());
        updates = 0;
        checkStatus(console);
        if (updates == 0) {
            console.printf("Everything is already up to date%n");
        }
    }

    private void checkStatus(CliConsole console) {
        try (var linkStates = context.status(fsReader)) {
            linkStates.forEach(linkState -> {
                if (!linkState.type().equals(LinkState.Type.UP_TO_DATE)) {
                    updates++;
                }
                printStatus(console, linkState);
            });
        }
    }

    private void printStatus(CliConsole console, LinkState linkState) {
        String statusType =
                switch (linkState.type()) {
                    case UP_TO_DATE -> "up-to-date";
                    case MISSING -> "missing";
                    case ORPHAN -> "orphan";
                    case LINK_CONFLICT, FILE_CONFLICT -> "!conflict";
                };
        Level level;
        if (linkState.type().equals(LinkState.Type.UP_TO_DATE)) {
            level = Level.DEBUG;
        } else {
            level = Level.INFO;
        }
        console.printf(level, "%-12s%s%n", statusType, linkState.desired().toString(context.mainDirectory()));
        if (linkState.type() == LinkState.Type.LINK_CONFLICT) {
            Path realPath = fsReader.readSymbolicLink(linkState.source());
            console.printf("> Symbolic link conflict. Current target is %s%n", realPath);
        }
    }
}
