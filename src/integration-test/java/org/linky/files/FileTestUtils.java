package org.linky.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTestUtils {

    public static void createDirectory(Path path) {
        if (!Files.exists(path)) {
            createDirectory(path.getParent());
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RIOException(e);
            }
        }
    }

    public static void createFile(Path path) {
        try {
            FileTestUtils.createDirectory(path.getParent());
            Files.createFile(path);
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }

    public static void createSymbolicLink(Path path, Path target) {
        try {
            FileTestUtils.createDirectory(path.getParent());
            Files.createSymbolicLink(path, target);
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }

    public static Path readSymbolicLink(Path path) {
        try {
            return Files.readSymbolicLink(path);
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }
}
