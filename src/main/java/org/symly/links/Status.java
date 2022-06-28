package org.symly.links;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.symly.cli.SymlyExecutionException;

public record Status(Type type, Link link) {

    public List<Action> toActions(boolean force) {
        return switch (type) {
            case UP_TO_DATE -> List.of(Action.upToDate(link));
            case LINK_CONFLICT -> List.of(Action.replace(link));
            case FILE_CONFLICT -> resolveConflict(force);
            case MISSING -> List.of(Action.create(link));
        };
    }

    private List<Action> resolveConflict(boolean force) {
        if (force) {
            List<Action> actions = new ArrayList<>();
            try (Stream<Path> files = Files.walk(link.source())) {
                files.sorted(Comparator.comparing(Path::toString).reversed())
                        .map(path -> Action.delete(Link.of(path, null)))
                        .forEach(actions::add);
            } catch (IOException e) {
                throw new SymlyExecutionException(
                        String.format("Unable to list files to be deleted for conflicting link %s", link), e);
            }
            actions.add(Action.create(link));
            return actions;
        }
        return List.of(Action.conflict(link));
    }

    public enum Type {
        UP_TO_DATE,
        LINK_CONFLICT,
        FILE_CONFLICT,
        MISSING;

        public static final int MAX_LENGTH = 13;
    }
}
