package org.linky.links;

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
import lombok.RequiredArgsConstructor;
import org.linky.cli.LinkyExecutionException;

@RequiredArgsConstructor(staticName = "of")
public class TargetDirectory {

    /**
     * The {@link Path} of the target directory.
     */
    private final Path targetDirectoryPath;

    /**
     * @return a {@link Stream} of {@link Path} of the files present in the target.
     */
    public Stream<LinkTarget> links() {
        try {
            LinksVisitor visitor = new LinksVisitor();
            Files.walkFileTree(targetDirectoryPath, visitor);
            return visitor.getPaths()
                    .stream()
                    .map(path -> toLinkTarget(targetDirectoryPath, path));
        } catch (IOException e) {
            throw new LinkyExecutionException(
                    String.format("Unable to analyze target directory %s", targetDirectoryPath), e);
        }
    }

    private LinkTarget toLinkTarget(Path targetDirectoryPath, Path linkTarget) {
        Path name = targetDirectoryPath.relativize(linkTarget);
        Path target = linkTarget.toAbsolutePath().normalize();
        return new LinkTarget(name, target);
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
