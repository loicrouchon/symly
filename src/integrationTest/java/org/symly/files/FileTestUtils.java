package org.symly.files;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileTestUtils {

    private FileTestUtils() {}

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

    public static void createOrUpdateFile(Path path, String content) {
        try {
            if (!Files.exists(path)) {
                FileTestUtils.createDirectory(path.getParent());
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
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

    public static boolean deleteIfExists(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }
}
