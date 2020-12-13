package org.linky.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemWriterImpl implements FileSystemWriter {

    @Override
    public void deleteIfExists(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    @Override
    public void move(Path from, Path to) throws IOException {
        Files.move(from, to);
    }

    @Override
    public void createDirectories(Path path) throws IOException {
        Files.createDirectories(path);
    }

    @Override
    public void createSymbolicLink(Path from, Path to) throws IOException {
        Files.createSymbolicLink(from, to);
    }

    @Override
    public void createEmptyFile(Path path) throws IOException {
        Files.createFile(path);
    }
}
