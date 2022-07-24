package org.symly.repositories;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
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
        Map<Path, Collection<IgnoreRule>> ignoreRules = new HashMap<>();
        ignoreRules.put(path, IgnoreList.read(fsReader, path));
        try {
            return fsReader.walk(path)
                    .filter(filePath -> shouldProcessPath(fsReader, filePath, ignoreRules))
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

    private boolean shouldProcessPath(
            FileSystemReader fsReader, Path filePath, Map<Path, Collection<IgnoreRule>> ignoreRules) {
        Path pathName = relativize(filePath);
        if (filePath.endsWith(IgnoreList.SYMLY_IGNORE)) {
            return false;
        }
        Path currentPath = toPath();
        Collection<IgnoreRule> rules = new ArrayList<>(ignoreRules.get(currentPath));
        for (Path pathElement : pathName) {
            currentPath = currentPath.resolve(pathElement);
            rules.addAll(ignoreRules.computeIfAbsent(currentPath, key -> IgnoreList.read(fsReader, key)));
            if (ignorePath(pathElement.toString(), rules)) {
                return false;
            }
        }
        return true;
    }

    private boolean ignorePath(String name, Collection<IgnoreRule> rules) {
        return rules.stream().anyMatch(rule -> rule.match(name));
    }

    public static Repository of(Path path) {
        return new Repository(path);
    }
}
