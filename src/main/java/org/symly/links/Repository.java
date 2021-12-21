package org.symly.links;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import lombok.NonNull;
import org.symly.cli.SymlyExecutionException;

/**
 * A directory which content will have to be linked into the {@link MainDirectory}.
 */
public class Repository extends Directory {

    Repository(@NonNull Path path) {
        super(path);
    }

    Stream<RepositoryEntry> entries() {
        Path path = toPath();
        try {
            return Files.walk(path)
                .map(filePath -> RepositoryEntry.of(relativize(filePath), filePath));
        } catch (IOException e) {
            throw new SymlyExecutionException(
                String.format("Unable to analyze repository structure %s", path), e);
        }
    }

    public static Repository of(Path path) {
        return new Repository(path);
    }
}
