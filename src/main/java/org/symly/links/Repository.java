package org.symly.links;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import lombok.Getter;
import org.symly.cli.SymlyExecutionException;

public class Repository extends Directory {

    private Repository(Path path) {
        super(path);
    }

    /**
     * @return a {@link Stream} of {@link Path} of the files present in the target.
     */
    public Stream<LinkTarget> links() {
        Path path = toPath();
        try {
            LinksVisitor visitor = new LinksVisitor();
            Files.walkFileTree(path, visitor);
            return visitor.getPaths()
                    .stream()
                    .map(filePath -> toLinkTarget(path, filePath));
        } catch (IOException e) {
            throw new SymlyExecutionException(
                    String.format("Unable to analyze target directory %s", path), e);
        }
    }

    private LinkTarget toLinkTarget(Path targetDirectoryPath, Path linkTarget) {
        Path name = targetDirectoryPath.relativize(linkTarget);
        Path target = linkTarget.toAbsolutePath().normalize();
        return new LinkTarget(name, target);
    }

    public static Repository of(Path path) {
        return new Repository(path);
    }

    private static class LinksVisitor extends SimpleFileVisitor<Path> {

        @Getter
        private final Collection<Path> paths = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (Files.exists(Configuration.symlinkMarker(dir))) {
                paths.add(dir);
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            paths.add(file);
            return FileVisitResult.CONTINUE;
        }
    }
}
