package org.symly.repositories;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;
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

    public Stream<Status> linksStatuses(LinksFinder linksFinder, FileSystemReader fsReader) {
        List<Path> dirs = repositories.allDirectoriesNames().stream()
                .map(p -> mainDirectory.resolve(p).toAbsolutePath().normalize())
                .toList();
        Collection<Link> links = links();
        Set<Path> existingLinks = links.stream().map(Link::source).collect(Collectors.toSet());
        return Stream.concat(
                        links.stream().map(RepositoryLink::new), dirs.stream().map(RepositoryDirectory::new))
                .sorted(Comparator.comparing(RepositoryEntry::path))
                .flatMap(p -> expand(p, linksFinder, dirs, existingLinks, fsReader));
    }

    private Stream<Status> expand(
            RepositoryEntry repositoryEntry,
            LinksFinder linksFinder,
            List<Path> dirs,
            Set<Path> existingLinksSources,
            FileSystemReader fsReader) {
        if (repositoryEntry instanceof RepositoryLink lp) {
            return Stream.of(toStatus(lp.link(), fsReader));
        }
        return linksFinder
                .findLinksInDirectory(repositoryEntry.path(), dirs, orphanMaxDepth, repositories::containsPath)
                // only keeping orphans
                .filter(link -> !fsReader.exists(link.target()))
                // filtering existing links (A link might be orphan but still existing in another repository)
                .filter(link -> !existingLinksSources.contains(link.source()))
                .map(link -> new Status(Status.Type.ORPHAN, link));
    }

    private Status toStatus(Link link, FileSystemReader fsReader) {
        Path source = link.source();
        if (!fsReader.isSymbolicLink(source)) {
            if (!fsReader.exists(source)) {
                return new Status(Status.Type.MISSING, link);
            }
            return new Status(Status.Type.FILE_CONFLICT, link);
        }
        Path currentTarget = fsReader.readSymbolicLink(source);
        Path newTarget = link.target();
        if (Objects.equals(currentTarget, newTarget)) {
            return new Status(Status.Type.UP_TO_DATE, link);
        }
        return new Status(Status.Type.LINK_CONFLICT, link);
    }

    private sealed interface RepositoryEntry permits RepositoryLink, RepositoryDirectory {
        Path path();
    }

    private record RepositoryLink(Link link) implements RepositoryEntry {

        @Override
        public Path path() {
            return link.source();
        }
    }

    private record RepositoryDirectory(Path path) implements RepositoryEntry {}
}
