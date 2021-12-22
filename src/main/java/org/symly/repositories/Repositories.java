package org.symly.repositories;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.symly.links.Configuration;
import org.symly.links.Link;

/**
 * An ordered collection of {@link Repository} which files will be linked into the {@link MainDirectory}.
 */
@RequiredArgsConstructor
public class Repositories {

    @NonNull
    private final List<Repository> repos;

    /**
     * Returns {@code true} if the absolute path given is contained in one of the repositories.
     *
     * @param path the path to check
     * @return {@code true} if the absolute path given is contained in one of the repositories
     */
    public boolean containsPath(Path path) {
        return repos.stream().anyMatch(r -> r.containsPath(path.toAbsolutePath()));
    }

    /**
     * Returns a list of {@link Link} to be created in {@code mainDirectory}. The links targets points to files
     * contained in the repositories.
     *
     * @return a list of {@link Link} to be created in {@code mainDirectory}.
     */
    public List<Link> links(MainDirectory mainDirectory) {
        Path mainDirectoryPath = mainDirectory.toPath();
        try (Stream<RepositoryEntry> elements = entries()) {
            return elements
                .map(element -> toLink(element, mainDirectoryPath))
                .toList();
        }
    }

    private Stream<RepositoryEntry> entries() {
        Set<Path> addedNames = new HashSet<>();
        List<Path> addedDirs = new ArrayList<>();
        return allEntries()
            .<RepositoryEntry>mapMulti((element, stream) -> {
                if (addedNames.contains(element.name())) {
                    return;
                }
                if (addedDirs.stream().anyMatch(dir -> element.fullPath().startsWith(dir))) {
                    return;
                }
                if (element.type() == RepositoryEntry.Type.DIRECTORY) {
                    if (!Files.exists(Configuration.symlinkMarker(element.fullPath()))) {
                        return;
                    }
                    addedDirs.add(element.fullPath());
                }
                addedNames.add(element.name());
                stream.accept(element);
            })
            .sorted(Comparator.comparing(RepositoryEntry::name));
    }

    private Link toLink(RepositoryEntry repositoryEntry, Path directory) {
        return Link.of(directory.resolve(repositoryEntry.name()), repositoryEntry.fullPath());
    }

    /**
     * Returns the list of directories names (i.e. relative path to the root of each repository) contained in any of the
     * repositories.
     *
     * @return the list of directories present in the repositories.
     */
    public List<Path> allDirectoriesNames() {
        try (Stream<RepositoryEntry> allElements = allEntries()) {
            return allElements
                .filter(e -> e.type() == RepositoryEntry.Type.DIRECTORY)
                .map(RepositoryEntry::name)
                .sorted()
                .distinct()
                .toList();
        }
    }

    private Stream<RepositoryEntry> allEntries() {
        return repos.stream().flatMap(Repository::entries);
    }

    public static Repositories of(List<Repository> repositories) {
        return new Repositories(repositories);
    }
}
