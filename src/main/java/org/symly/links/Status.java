package org.symly.links;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.symly.cli.SymlyExecutionException;
import org.symly.files.FileSystemReader;

@ToString
@RequiredArgsConstructor
public class Status {

    @Getter
    private final Type type;

    @Getter
    private final Link link;

    private final FileSystemReader fsReader;

    public List<Action> toActions(boolean force) {
        return switch (type) {
            case UP_TO_DATE -> List.of(Action.upToDate(link, fsReader));
            case LINK_CONFLICT -> List.of(Action.replace(link, fsReader));
            case FILE_CONFLICT -> resolveConflict(force);
            case MISSING -> List.of(Action.create(link, fsReader));
        };
    }

    private List<Action> resolveConflict(boolean force) {
        if (force) {
            List<Action> actions = new ArrayList<>();
            try (Stream<Path> files = Files.walk(link.source())) {
                files
                        .sorted(Comparator.comparing(Path::toString).reversed())
                        .map(path -> Action.delete(Link.of(path, null), fsReader))
                        .forEach(actions::add);
            } catch (IOException e) {
                throw new SymlyExecutionException(String.format(
                        "Unable to list files to be deleted for conflicting link %s", link), e);
            }
            actions.add(Action.create(link, fsReader));
            actions.forEach(System.out::println);
            return actions;
        }
        return List.of(Action.conflict(link, fsReader));
    }

    public enum Type {
        UP_TO_DATE,
        LINK_CONFLICT,
        FILE_CONFLICT,
        MISSING;

        public static final int MAX_LENGTH = 13;
    }
}
