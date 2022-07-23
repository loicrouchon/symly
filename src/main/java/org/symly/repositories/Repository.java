package org.symly.repositories;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.NonNull;
import org.symly.cli.SymlyExecutionException;
import org.symly.files.FileSystemReader;
import org.symly.links.Directory;

/**
 * A directory which content will have to be linked into the {@link MainDirectory}.
 */
public class Repository extends Directory {

    Repository(@NonNull Path path) {
        super(path);
    }

    Stream<RepositoryEntry> entries(FileSystemReader fsReader) {
        Path path = toPath();
        try {
            return fsReader.walk(path)
                    .map(filePath -> RepositoryEntry.of(relativize(filePath), filePath, type(fsReader, filePath)));
        } catch (IOException e) {
            throw new SymlyExecutionException(String.format("Unable to analyze repository structure %s", path), e);
        }
    }

    private RepositoryEntry.Type type(FileSystemReader fsReader, Path path) {
        if (fsReader.isDirectory(path)) {
            return RepositoryEntry.Type.DIRECTORY;
        }
        return RepositoryEntry.Type.FILE;
    }

    public static Repository of(Path path) {
        return new Repository(path);
    }
}
