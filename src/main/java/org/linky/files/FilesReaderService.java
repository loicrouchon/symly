package org.linky.files;

import java.nio.file.Files;
import java.nio.file.Path;

public class FilesReaderService {

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public boolean isSymbolicLink(Path path) {
        return Files.isSymbolicLink(path);
    }
}
