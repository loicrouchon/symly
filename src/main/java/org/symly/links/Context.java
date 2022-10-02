package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import org.symly.files.FileSystemReader;
import org.symly.repositories.MainDirectory;
import org.symly.repositories.Repositories;

/**
 * The {@code Context} combines the information of where from and where to the links should be created.
 *
 * @param mainDirectory The main directory in which the links should be created.
 * @param repositories The repositories containing the files to be linked in {@link #mainDirectory}.
 * @param orphanMaxDepth the maximum depth for orphan-links lookup.
 */
public record Context(@NonNull MainDirectory mainDirectory, @NonNull Repositories repositories, int orphanMaxDepth) {

    public Collection<Link> links() {
        return repositories.links(mainDirectory);
    }

    public Stream<LinkState> status(FileSystemReader fsReader) {
        LinkStateIterator it = new LinkStateIterator(this, fsReader);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false);
    }
}

class LinkStateIterator implements Iterator<LinkState> {

    private final PriorityQueue<Entry> remainingEntries = new PriorityQueue<>(Comparator.comparing(Entry::path));
    private final MainDirectory mainDirectory;
    private final Repositories repositories;
    private final Set<Path> existingLinks;
    private final Set<Path> allDirectoriesNames;
    private final FileSystemReader fsReader;

    LinkStateIterator(Context context, FileSystemReader fsReader) {
        mainDirectory = context.mainDirectory();
        this.fsReader = fsReader;
        this.repositories = context.repositories();
        int orphanMaxDepth = context.orphanMaxDepth();
        List<Link> links = repositories.links(mainDirectory);
        existingLinks = links.stream().map(Link::source).collect(Collectors.toSet());
        links.forEach(this::addLink);
        allDirectoriesNames = repositories.allDirectoriesNames().stream()
                .map(p -> mainDirectory.resolve(p).toAbsolutePath().normalize())
                .collect(Collectors.toSet());
        allDirectoriesNames.forEach(p -> addDir(p, orphanMaxDepth));
    }

    public void addLink(Link link) {
        remainingEntries.add(new RepositoryLink(link));
    }

    private void addDir(Path path, int orphanMaxDepth) {
        remainingEntries.add(new DirectoryToAnalyze(path, orphanMaxDepth));
    }

    @Override
    public boolean hasNext() {
        while (!remainingEntries.isEmpty()) {
            Entry firstElement = remainingEntries.element();
            if (firstElement instanceof DirectoryToAnalyze dir) {
                remainingEntries.remove();
                processDirectory(dir);
            } else {
                return true;
            }
        }
        return false;
    }

    private void processDirectory(DirectoryToAnalyze dir) {
        Path currentDirPath = dir.path();
        if (!isDirectoryReadable(currentDirPath)) {
            return;
        }
        int remainingOrphanDepthLookup = dir.remainingOrphanDepthLookup() - 1;
        try (Stream<Path> paths = fsReader.list(currentDirPath)) {
            paths.forEach(path -> processDirectoryEntry(remainingOrphanDepthLookup, path));
        } catch (IOException e) {
            // TODO replace with a proper exception
            throw new RuntimeException("Unable to read directory %s".formatted(currentDirPath), e);
        }
    }

    private boolean isDirectoryReadable(Path currentDirPath) {
        return fsReader.isDirectory(currentDirPath) && fsReader.isReadable(currentDirPath);
    }

    private void processDirectoryEntry(int remainingOrphanDepthLookup, Path path) {
        if (remainingOrphanDepthLookup > 0 && isDirectoryReadable(path) && !allDirectoriesNames.contains(path)) {
            addDir(path, remainingOrphanDepthLookup);
        } else if (fsReader.isSymbolicLink(path)) {
            Path target = fsReader.readSymbolicLink(path);
            if (repositories.containsPath(target) && !existingLinks.contains(path)) {
                remainingEntries.add(new OrphanLink(new Link(path, target)));
            }
        }
    }

    @Override
    public LinkState next() {
        Entry entry = remainingEntries.poll();
        if (entry == null) {
            return null;
        }
        return entry.toLinkState(fsReader, mainDirectory);
    }
}

sealed interface Entry permits RepositoryLink, OrphanLink, DirectoryToAnalyze {
    Path path();

    LinkState toLinkState(FileSystemReader fsReader, MainDirectory mainDirectory);
}

record RepositoryLink(Link link) implements Entry {

    @Override
    public Path path() {
        return link.source();
    }

    @Override
    public LinkState toLinkState(FileSystemReader fsReader, MainDirectory mainDirectory) {
        Path source = link.source();
        Path newTarget = link.target();
        if (!fsReader.isSymbolicLink(source)) {
            if (!fsReader.exists(source)) {
                return new LinkState(mainDirectory, source, LinkState.Entry.missingEntry(), newTarget);
            }
            if (fsReader.isDirectory(source)) {
                return new LinkState(mainDirectory, source, LinkState.Entry.directoryEntry(), newTarget);
            }
            return new LinkState(mainDirectory, source, LinkState.Entry.fileEntry(), newTarget);
        }
        Path currentTarget = fsReader.readSymbolicLink(source);
        boolean exists = fsReader.exists(source);
        return new LinkState(mainDirectory, source, LinkState.Entry.linkEntry(currentTarget, exists), newTarget);
    }
}

record OrphanLink(Link link) implements Entry {

    @Override
    public Path path() {
        return link.source();
    }

    @Override
    public LinkState toLinkState(FileSystemReader fsReader, MainDirectory mainDirectory) {
        return new LinkState(mainDirectory, link.source(), LinkState.Entry.linkEntry(link.target(), false), null);
    }
}

record DirectoryToAnalyze(Path path, int remainingOrphanDepthLookup) implements Entry {

    @Override
    public LinkState toLinkState(FileSystemReader fsReader, MainDirectory mainDirectory) {
        throw new UnsupportedOperationException("");
    }
}
