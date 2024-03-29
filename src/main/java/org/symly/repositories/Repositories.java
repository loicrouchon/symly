package org.symly.repositories;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static org.symly.links.Configuration.symlinkMarker;
import static org.symly.repositories.RepositoryEntry.Type.DIRECTORY;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;

/**
 * An ordered collection of {@link Repository} which files will be linked into the {@link MainDirectory}.
 */
public class Repositories {

    private final FileSystemReader fsReader;

    private final Deque<Repository> layers;

    public Repositories(FileSystemReader fsReader, Deque<Repository> layers) {
        this.fsReader = Objects.requireNonNull(fsReader);
        this.layers = Objects.requireNonNull(layers);
    }

    public Collection<Repository> repositories() {
        return List.copyOf(layers);
    }

    /**
     * Returns {@code true} if the absolute path given is contained in one of the repositories.
     *
     * @param path the path to check
     * @return {@code true} if the absolute path given is contained in one of the repositories
     */
    public boolean containsPath(Path path) {
        return layers.stream().anyMatch(r -> r.containsPath(path.toAbsolutePath()));
    }

    /**
     * Returns a list of {@link Link} to be created in {@code mainDirectory}. The links targets points to files
     * contained in the repositories.
     *
     * @return a list of {@link Link} to be created in {@code mainDirectory}.
     */
    public List<Link> links(MainDirectory mainDirectory) {
        try (Stream<RepositoryEntry> elements = entries(fsReader)) {
            return elements.map(element -> toLink(element, mainDirectory)).toList();
        }
    }

    private Stream<RepositoryEntry> entries(FileSystemReader fs) {
        Set<Path> addedNames = new HashSet<>();
        List<Path> addedDirs = new ArrayList<>();
        List<RepositoryEntry> allEntries;
        try (Stream<RepositoryEntry> stream = allEntries(fs)) {
            allEntries = stream.toList();
        }
        Set<Path> allEntriesFullPaths =
                allEntries.stream().map(RepositoryEntry::fullPath).collect(Collectors.toSet());
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

    private boolean skipEntry(
            RepositoryEntry entry, Set<Path> addedNames, List<Path> addedDirs, Set<Path> allEntriesFullPaths) {
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
        try (Stream<RepositoryEntry> allElements = allEntries(fsReader)) {
            return allElements
                    .filter(e -> e.type() == DIRECTORY)
                    .map(RepositoryEntry::name)
                    .sorted()
                    .distinct()
                    .toList();
        }
    }

    private Stream<RepositoryEntry> allEntries(FileSystemReader fs) {
        return layersByPriority().flatMap(repo -> repo.entries(fs));
    }

    private Stream<Repository> layersByPriority() {
        return StreamSupport.stream(spliteratorUnknownSize(layers.descendingIterator(), ORDERED), false);
    }

    /**
     * Wraps an ordered list of {@link Repository} into a {@code Repositories} object.
     * @param fsReader the file system reader
     * @param repositories the ordered list of {@link Repository}. Ordered from the base layer first to the most specific one last
     * @return the wrapped repositories
     */
    public static Repositories of(FileSystemReader fsReader, List<Repository> repositories) {
        return new Repositories(fsReader, new ArrayDeque<>(repositories));
    }
}
