package org.symly.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.symly.cli.SymlyExecutionException;
import org.symly.links.Directory;

public interface FileSystemReader {

    boolean exists(Path path);

    boolean isReadable(Path path);

    boolean isDirectory(Path path);

    default boolean isADirectory(Directory directory) {
        return isDirectory(directory.toPath());
    }

    boolean isSymbolicLink(Path path);

    Path readSymbolicLink(Path link);

    Stream<String> lines(Path path) throws IOException;

    Stream<Path> list(Path path) throws IOException;

    Stream<Path> walk(Path path) throws IOException;

    class RealFileSystemReader implements FileSystemReader {

        @Override
        public boolean exists(Path path) {
            return Files.exists(path);
        }

        @Override
        public boolean isReadable(Path path) {
            return Files.isReadable(path);
        }

        @Override
        public boolean isDirectory(Path path) {
            return Files.isDirectory(path);
        }

        @Override
        public boolean isSymbolicLink(Path path) {
            return Files.isSymbolicLink(path);
        }

        @Override
        public Path readSymbolicLink(Path link) {
            try {
                return Files.readSymbolicLink(link);
            } catch (IOException e) {
                throw new SymlyExecutionException(
                        "Unable to read link %s real path: %s".formatted(link, e.getMessage()), e);
            }
        }

        @Override
        public Stream<String> lines(Path path) throws IOException {
            return Files.lines(path);
        }

        @Override
        public Stream<Path> list(Path path) throws IOException {
            return Files.list(path);
        }

        @Override
        public Stream<Path> walk(Path path) throws IOException {
            return Files.walk(path);
        }
    }
}
