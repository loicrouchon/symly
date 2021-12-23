package org.symly.repositories;

import static org.symly.links.Configuration.symlinkMarker;
import static org.symly.repositories.RepositoryEntry.Type.DIRECTORY;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
        try (Stream<RepositoryEntry> elements = entries()) {
            return elements
                .map(element -> toLink(element, mainDirectory))
                .toList();
        }
    }

    private Stream<RepositoryEntry> entries() {
        Set<Path> addedNames = new HashSet<>();
        List<Path> addedDirs = new ArrayList<>();
        List<RepositoryEntry> allEntries = allEntries().toList();
        Set<Path> allEntriesFullPaths = allEntries.stream()
            .map(RepositoryEntry::fullPath)
            .collect(Collectors.toSet());
        return allEntries.stream()
            .<RepositoryEntry>mapMulti((entry, stream) -> {
                if (skipEntry(entry, addedNames, addedDirs, allEntriesFullPaths)) {
                    return;
                }
                if (entry.type() == DIRECTORY) {
                    addedDirs.add(entry.name());
                }
                addedNames.add(entry.name());
                stream.accept(entry);
            })
            .sorted(Comparator.comparing(RepositoryEntry::name));
    }

    private boolean skipEntry(RepositoryEntry entry,
        Set<Path> addedNames, List<Path> addedDirs, Set<Path> allEntriesFullPaths) {
        if (addedNames.contains(entry.name())) {
            return true;
        }
        if (addedDirs.stream().anyMatch(dir -> entry.name().startsWith(dir))) {
            return true;
        }
        return entry.type() == DIRECTORY && !allEntriesFullPaths.contains(symlinkMarker(entry.fullPath()));
    }

    private Link toLink(RepositoryEntry repositoryEntry, MainDirectory directory) {
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
                .filter(e -> e.type() == DIRECTORY)
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
