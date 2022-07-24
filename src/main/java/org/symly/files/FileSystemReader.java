package org.symly.files;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;
import org.symly.cli.SymlyExecutionException;
import org.symly.links.Directory;

public class FileSystemReader {

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    public boolean isADirectory(Directory directory) {
        return Files.isDirectory(directory.toPath());
    }

    public boolean isSymbolicLink(Path path) {
        return Files.isSymbolicLink(path);
    }

    public Path readSymbolicLink(Path link) {
        try {
            return Files.readSymbolicLink(link);
        } catch (IOException e) {
            throw new SymlyExecutionException(
                    String.format("Unable to read link %s real path: %s", link, e.getMessage()), e);
        }
    }

    public Stream<Path> walk(Path path) throws IOException {
        return Files.walk(path);
    }

    public void walkFileTree(Path start, Set<FileVisitOption> options, int maxDepth, FileVisitor<? super Path> visitor)
            throws IOException {
        Files.walkFileTree(start, options, maxDepth, visitor);
    }

    public Stream<String> lines(Path path) throws IOException {
        return Files.lines(path);
    }
}
