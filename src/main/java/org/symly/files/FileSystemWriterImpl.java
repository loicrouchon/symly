package org.symly.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSystemWriterImpl implements FileSystemWriter {

    @Override
    public void deleteIfExists(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    @Override
    public void createDirectories(Path path) throws IOException {
        Files.createDirectories(path);
    }

    @Override
    public void createSymbolicLink(Path from, Path to) throws IOException {
        Files.createSymbolicLink(from, to);
    }
}
