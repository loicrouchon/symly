package org.symly.files;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.symly.links.Directory;

public class IoMock {

    private final Set<Path> exists = new HashSet<>();
    private final Map<Path, Path> links = new HashMap<>();

    public void file(Path path) {
        exists.add(path);
    }

    public void symlink(Path source, Path target) {
        file(source);
        links.put(source, target);
    }

    public FileSystemReader buildFileSystemReader() {
        return new FileSystemReaderStub(Set.copyOf(exists), Map.copyOf(links));
    }

    @RequiredArgsConstructor
    private static class FileSystemReaderStub extends FileSystemReader {

        private final Set<Path> exists;
        private final Map<Path, Path> links;

        @Override
        public boolean exists(Path path) {
            return exists.contains(path);
        }

        @Override
        public boolean isDirectory(Path path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isADirectory(Directory directory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSymbolicLink(Path path) {
            return links.containsKey(path);
        }

        @Override
        public Path readSymbolicLink(Path link) {
            return links.get(link);
        }
    }
}
