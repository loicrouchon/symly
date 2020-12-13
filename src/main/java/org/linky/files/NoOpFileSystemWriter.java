package org.linky.files;

import java.nio.file.Path;

public class NoOpFileSystemWriter implements FileSystemWriter {

    @Override
    public void deleteIfExists(Path path) {
        // NO-OP
    }

    @Override
    public void move(Path from, Path to) {
        // NO-OP
    }

    @Override
    public void createDirectories(Path path) {
        // NO-OP
    }

    @Override
    public void createSymbolicLink(Path from, Path to) {
        // NO-OP
    }

    @Override
    public void createEmptyFile(Path path) {
        // NO-OP
    }
}
