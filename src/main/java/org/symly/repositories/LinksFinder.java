package org.symly.repositories;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.symly.cli.SymlyExecutionException;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;

/**
 * Finds links to repository.
 */
@RequiredArgsConstructor
public class LinksFinder {

    private final FileSystemReader fileSystemReader;

    /**
     * <p>Find links in {@code rootDir} that points to the {@link Repositories} and which destination does not
     * exist.</p>
     * <p>Note: the search will be performed in {@code rootDir} for every single directory of {@link Repositories}. See
     * {@link Repositories#allDirectoriesNames()} for information on which directories in the repositories will be
     * considered. Within each of those, the search will be done using the specified {@code maxDepth} to limit the
     * search.</p>
     *
     * @param rootDir the root directory in which the search should be made.
     * @param maxDepth the maximum depth of the search per directory listed in the {@link Repositories}.
     * @param repositories the repositories targeted.
     * @return orphan links in {@code rootDir} pointing to the {@link Repositories}.
     */
    public Stream<Link> findOrphans(Path rootDir, int maxDepth, Repositories repositories) {
        return findLinks(rootDir, maxDepth, repositories)
            .filter(link -> !fileSystemReader.exists(link.target()));
    }

    /**
     * <p>Find links in {@code rootDir} that points to the {@link Repositories}.</p>
     * <p>Note: the search will be performed in {@code rootDir} for every single directory of {@link Repositories}. See
     * {@link Repositories#allDirectoriesNames()} for information on which directories in the repositories will be
     * considered. Within each of those, the search will be done using the specified {@code maxDepth} to limit the
     * search.</p>
     *
     * @param rootDir the root directory in which the search should be made.
     * @param maxDepth the maximum depth of the search per directory listed in the {@link Repositories}.
     * @param repositories the repositories targeted.
     * @return links in {@code rootDir} pointing to the {@link Repositories}.
     */
    public Stream<Link> findLinks(Path rootDir, int maxDepth, Repositories repositories) {
        return findFiles(rootDir, maxDepth, repositories, repositories::containsPath);
    }

    private Stream<Link> findFiles(Path rootDir, int maxDepth, Repositories repositories, Predicate<Path> filter) {
        List<Path> dirs = repositories.allDirectoriesNames();
        return dirs
            .stream()
            .map(path -> rootDir.resolve(path).toAbsolutePath().normalize())
            .flatMap(dir -> findLinksInDirectory(dir, dirs, maxDepth, filter));
    }

    private Stream<Link> findLinksInDirectory(Path dir, List<Path> exclusions, int maxDepth,
        Predicate<Path> filter) {
        Set<Path> excludedDirs = directSubDirectories(dir, exclusions);
        LinkVisitor visitor = new LinkVisitor(fileSystemReader, filter, excludedDirs);
        try {
            Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), maxDepth, visitor);
        } catch (IOException e) {
            throw new SymlyExecutionException(
                String.format("Failed to find orphan links in %s", dir), e);
        }
        return visitor.links.stream();
    }

    private Set<Path> directSubDirectories(Path currentDirectory, List<Path> directories) {
        return directories.stream()
            .filter(directory -> Objects.equals(directory.getParent(), currentDirectory))
            .collect(Collectors.toSet());
    }

    @RequiredArgsConstructor
    private static class LinkVisitor extends SimpleFileVisitor<Path> {

        private final FileSystemReader fsReader;
        private final Predicate<Path> targetFilter;
        private final Set<Path> excluded;

        final Collection<Link> links = new ArrayList<>();
        long files = 0L;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (excluded.contains(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (fsReader.isSymbolicLink(dir)) {
                handleSymbolicLink(dir);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            files++;
            if (fsReader.isSymbolicLink(file)) {
                handleSymbolicLink(file);
                return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        private void handleSymbolicLink(Path source) {
            Path target = fsReader.readSymbolicLink(source);
            if (targetFilter.test(target)) {
                links.add(Link.of(source, target));
            }
        }
    }
}
