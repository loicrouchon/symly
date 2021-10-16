package org.symly.orphans;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;
import org.symly.links.Repository;

@RequiredArgsConstructor
public class OrphanFinder {

    private final FileSystemReader fileSystemReader;

    public Collection<Link> findOrphans(Path rootDir, int maxDepth, Set<Repository> repositories) throws IOException {
        Predicate<Path> filter = orphanLinkTargetFilter(repositories);
        List<Path> dirs = dirs(rootDir, repositories);
        Collection<Link> orphans = new ArrayList<>();
        for (Path dir : dirs) {
            orphans.addAll(findOrphansInDirectory(dir, dirs, maxDepth, filter));
        }
        return orphans;
    }

    private List<Path> dirs(Path rootDir, Set<Repository> repositories) {
        List<Path> dirs = repositories.stream()
                .flatMap(repository -> repository.directories().map(repository::relativize))
                .map(p -> rootDir.resolve(p).toAbsolutePath().normalize())
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        dirs.add(rootDir);
        return dirs;
    }

    private Predicate<Path> orphanLinkTargetFilter(Set<Repository> repositories) {
        Set<Path> repositoriesPath = repositories.stream()
                .map(Repository::toPath)
                .collect(Collectors.toSet());
        return target -> repositoriesPath.stream().anyMatch(target::startsWith);
    }

    private Collection<Link> findOrphansInDirectory(Path dir, List<Path> exclusions, int maxDepth,
            Predicate<Path> targetsFilter) throws IOException {
        Set<Path> excludedDirs = directSubDirectories(dir, exclusions);
        if (!excludedDirs.isEmpty()) {
            System.out.printf("Walking %s excluding %s", dir, excludedDirs.stream()
                    .map(dir::relativize)
                    .collect(Collectors.toList()));
        } else {
            System.out.printf("Walking %s", dir);
        }

        OrphanLinkVisitor visitor = new OrphanLinkVisitor(fileSystemReader, targetsFilter, excludedDirs);
        Files.walkFileTree(dir, EnumSet.noneOf(FileVisitOption.class), maxDepth, visitor);
        Collection<Link> orphans = visitor.orphans;
        System.out.printf(" - %d orphans found, scanned %d%n", orphans.size(), visitor.files);
        return orphans;
    }

    private Set<Path> directSubDirectories(Path currentDirectory, List<Path> directories) {
        return directories.stream()
                .filter(directory -> Objects.equals(directory.getParent(), currentDirectory))
                .collect(Collectors.toSet());
    }

    @RequiredArgsConstructor
    private static class OrphanLinkVisitor extends SimpleFileVisitor<Path> {

        private final FileSystemReader fsReader;
        private final Predicate<Path> targetFilter;
        private final Set<Path> excluded;

        final Collection<Link> orphans = new ArrayList<>();
        long files = 0L;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (fsReader.isSymbolicLink(dir)) {
                handleSymbolicLink(dir);
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (excluded.contains(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
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
            if (!fsReader.exists(target) && targetFilter.test(target)) {
                orphans.add(Link.of(source, target));
            }
        }
    }
}
