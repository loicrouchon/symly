package org.linky.links;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.cli.LinkyExecutionException;

@RequiredArgsConstructor
class SourceReader {

    private final Path source;

    public Stream<Path> read() {
        try {
            LinkerVisitor visitor = new LinkerVisitor();
            Files.walkFileTree(source, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
            return visitor.getPaths().stream();
        } catch (IOException e) {
            throw new LinkyExecutionException(String.format("Unable to analyze source directory %s", source), e);
        }
    }

    private static class LinkerVisitor extends SimpleFileVisitor<Path> {

        @Getter
        private final Collection<Path> paths = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (Files.exists(dir.resolve(".symlink"))) {
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
