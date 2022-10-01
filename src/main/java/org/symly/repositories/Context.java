package org.symly.repositories;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;
import org.symly.links.LinkStatus;
import org.symly.links.Status;

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

    public Stream<Status> status(FileSystemReader fsReader) {
        DirLinkStructureIterator it = new DirLinkStructureIterator(this, fsReader);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false);
    }

    private static class DirLinkStructureIterator implements Iterator<Status> {

        private final PriorityQueue<Entry> repositoryEntries = new PriorityQueue<>(Comparator.comparing(Entry::path));
        private final FileSystemReader fsReader;
        private final Repositories repositories;
        private final Set<Path> existingLinks;
        private final Set<Path> dirsToLookup = new HashSet<>();

        private DirLinkStructureIterator(Context context, FileSystemReader fsReader) {
            MainDirectory mainDirectory = context.mainDirectory();
            this.fsReader = fsReader;
            this.repositories = context.repositories();
            int orphanMaxDepth = context.orphanMaxDepth();
            List<Link> links = repositories.links(mainDirectory);
            existingLinks = links.stream().map(Link::source).collect(Collectors.toSet());
            links.forEach(this::addLink);
            repositories.allDirectoriesNames().stream()
                    .map(p -> mainDirectory.resolve(p).toAbsolutePath().normalize())
                    .forEach(p -> addDir(p, orphanMaxDepth));
        }

        public void addLink(Link link) {
            repositoryEntries.add(new RepositoryLink(link));
        }

        private void addDir(Path path, int orphanMaxDepth) {
            if (!dirsToLookup.contains(path)) {
                dirsToLookup.add(path);
                repositoryEntries.add(new DirectoryToAnalyze(path, orphanMaxDepth));
            }
        }

        @Override
        public boolean hasNext() {
            while (!repositoryEntries.isEmpty()) {
                Entry firstElement = repositoryEntries.element();
                if (firstElement instanceof DirectoryToAnalyze rd) {
                    repositoryEntries.remove();
                    walkIt(rd);
                } else {
                    return true;
                }
            }
            return false;
        }

        private void walkIt(DirectoryToAnalyze dir) {
            Path currentDirPath = dir.path();
            if (!fsReader.isDirectory(currentDirPath)) {
                return;
            }
            int remainingOrphanDepthLookup = dir.remainingOrphanDepthLookup() - 1;
            try (Stream<Path> paths = fsReader.list(currentDirPath)) {
                paths.forEach(path -> {
                    if (remainingOrphanDepthLookup > 0 && fsReader.isDirectory(path)) {
                        addDir(path, remainingOrphanDepthLookup);
                    } else if (fsReader.isSymbolicLink(path)) {
                        Path target = fsReader.readSymbolicLink(path);
                        if (repositories.containsPath(target) && !existingLinks.contains(path)) {
                            repositoryEntries.add(new OrphanLink(new LinkStatus(path, target, null)));
                        }
                    }
                });
            } catch (IOException e) {
                System.out.println(e); // TODO
            }
            dirsToLookup.remove(currentDirPath);
        }

        @Override
        public Status next() {
            Entry entry = repositoryEntries.poll();
            if (entry instanceof OrphanLink ol) {
                return new Status(Status.Type.ORPHAN, ol.link());
            }
            if (entry instanceof RepositoryLink rl) {
                return toStatus(rl.link());
            }
            throw new IllegalStateException("Not a link"); // TODO
        }

        private Status toStatus(Link link) {
            Path source = link.source();
            Path newTarget = link.target();
            if (!fsReader.isSymbolicLink(source)) {
                LinkStatus linkStatus = new LinkStatus(link.source(), null, newTarget);
                if (!fsReader.exists(source)) {
                    return new Status(Status.Type.MISSING, linkStatus);
                }
                return new Status(Status.Type.FILE_CONFLICT, linkStatus);
            }
            Path currentTarget = fsReader.readSymbolicLink(source);
            LinkStatus linkStatus = new LinkStatus(link.source(), currentTarget, newTarget);
            if (Objects.equals(currentTarget, newTarget)) {
                return new Status(Status.Type.UP_TO_DATE, linkStatus);
            }
            return new Status(Status.Type.LINK_CONFLICT, linkStatus);
        }
    }

    private sealed interface Entry permits RepositoryLink, OrphanLink, DirectoryToAnalyze {
        Path path();
    }

    private record RepositoryLink(Link link) implements Entry {

        @Override
        public Path path() {
            return link.source();
        }
    }

    private record OrphanLink(LinkStatus link) implements Entry {

        @Override
        public Path path() {
            return link.source();
        }
    }

    private record DirectoryToAnalyze(Path path, int remainingOrphanDepthLookup) implements Entry {}
}
