package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.NonNull;
import org.symly.cli.SymlyExecutionException;
import org.symly.files.FileSystemReader;
import org.symly.repositories.MainDirectory;

/**
 * Conceptual representation of a linkage status.
 *
 * @param mainDirectory the {@link MainDirectory} in the source should belong to.
 * @param source the absolute {@link Path} to the source.
 * @param currentState the current state for the source.
 * @param desiredTarget the absolute path to the desired target for the source to point to.
 *
 */
public record LinkState(
        @NonNull MainDirectory mainDirectory, @NonNull Path source, @NonNull Entry currentState, Path desiredTarget) {

    public LinkState {
        if (!source.isAbsolute()) {
            throw new IllegalStateException("Source %s must be an absolute path".formatted(source));
        }
        if (desiredTarget != null && !desiredTarget.isAbsolute()) {
            throw new IllegalStateException("Desired target %s must be an absolute path".formatted(desiredTarget));
        }
        if (!mainDirectory.containsPath(source)) {
            throw new IllegalStateException("Source %s must be a sub-path of %s".formatted(source, mainDirectory));
        }
        checkConsistency(source, currentState, desiredTarget);
    }

    private static void checkConsistency(Path source, LinkState.Entry currentState, Path desiredTarget) {
        type(source, currentState, desiredTarget);
    }

    /**
     * The name of the link. It is the relative path of the {@link #source} to the {@link #mainDirectory}.
     * @return the name of the link.
     */
    public Path name() {
        return mainDirectory.relativize(source);
    }

    public LinkState.Type type() {
        return type(source, currentState, desiredTarget);
    }

    private static LinkState.Type type(Path source, LinkState.Entry currentState, Path desiredTarget) {
        if (desiredTarget == null && !(currentState instanceof LinkState.Entry.LinkEntry)) {
            throw new IllegalStateException(
                    "Such LinkStatus makes no sense, they don't exist and should not be created either: %s [%s]"
                            .formatted(source, currentState));
        }
        if (currentState instanceof LinkState.Entry.MissingEntry) {
            return LinkState.Type.MISSING;
        } else if (currentState instanceof LinkState.Entry.FileEntry) {
            return LinkState.Type.FILE_CONFLICT;
        } else if (currentState instanceof LinkState.Entry.DirectoryEntry) {
            return LinkState.Type.FILE_CONFLICT;
        } else if (currentState instanceof LinkState.Entry.LinkEntry le) {
            if (desiredTarget == null) {
                return LinkState.Type.ORPHAN;
            }
            if (!Objects.equals(le.target, desiredTarget)) {
                return LinkState.Type.LINK_CONFLICT;
            }
            return LinkState.Type.UP_TO_DATE;
        }
        throw new IllegalStateException(
                "Such LinkStatus makes no sense: %s [%s] -> %s".formatted(source, currentState, desiredTarget));
    }

    public Link desired() {
        return new Link(source(), desiredTarget);
    }

    public List<Action> toActions(FileSystemReader fsReader, boolean force) {
        return switch (type()) {
            case UP_TO_DATE -> List.of(Action.upToDate(desired()));
            case LINK_CONFLICT -> List.of(
                    Action.deleteLink(new Link(source, ((Entry.LinkEntry) currentState).target())),
                    Action.create(desired()));
            case FILE_CONFLICT -> resolveConflict(fsReader, force);
            case MISSING -> List.of(Action.create(desired()));
            case ORPHAN -> List.of(Action.deleteLink(new Link(source, ((Entry.LinkEntry) currentState).target())));
        };
    }

    private List<Action> resolveConflict(FileSystemReader fsReader, boolean force) {
        if (force) {
            List<Action> actions = new ArrayList<>();
            try (Stream<Path> files = fsReader.walk(source())) {
                files.sorted(Comparator.comparing(Path::toString).reversed())
                        .map(Action::delete)
                        .forEach(actions::add);
            } catch (IOException e) {
                throw new SymlyExecutionException(
                        "Unable to list files to be deleted for conflicting link %s".formatted(source), e);
            }
            actions.add(Action.create(desired()));
            return actions;
        }
        return List.of(Action.conflict(desired()));
    }

    public sealed interface Entry
            permits LinkState.Entry.MissingEntry,
                    LinkState.Entry.FileEntry,
                    LinkState.Entry.DirectoryEntry,
                    LinkState.Entry.LinkEntry {

        static LinkState.Entry missingEntry() {
            return LinkState.Entry.MissingEntry.INSTANCE;
        }

        static LinkState.Entry fileEntry() {
            return LinkState.Entry.FileEntry.INSTANCE;
        }

        static LinkState.Entry directoryEntry() {
            return LinkState.Entry.DirectoryEntry.INSTANCE;
        }

        static LinkState.Entry linkEntry(Path target) {
            return new LinkState.Entry.LinkEntry(target);
        }

        record MissingEntry() implements LinkState.Entry {
            private static final MissingEntry INSTANCE = new MissingEntry();
        }

        record FileEntry() implements LinkState.Entry {
            private static final FileEntry INSTANCE = new FileEntry();
        }

        record DirectoryEntry() implements LinkState.Entry {
            private static final DirectoryEntry INSTANCE = new DirectoryEntry();
        }

        record LinkEntry(@NonNull Path target) implements LinkState.Entry {}
    }

    public enum Type {
        UP_TO_DATE,
        LINK_CONFLICT,
        FILE_CONFLICT,
        MISSING,
        ORPHAN
    }
}
