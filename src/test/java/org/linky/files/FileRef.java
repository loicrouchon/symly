package org.linky.files;

import java.nio.file.Files;
import java.nio.file.Path;

interface FileRef {

    Path getName();

    void create(Path root);

    static FileRef of(Path root, Path path) {
        Path currentPath = root.relativize(path);
        if (Files.isSymbolicLink(path)) {
            Path destinationPath = root.relativize(FileTestUtils.readSymbolicLink(path));
            return LinkFileRef.of(currentPath, destinationPath);
        }
        return SimpleFileRef.of(currentPath);
    }

    static FileRef parse(String ref) {
        if (LinkFileRef.isLink(ref)) {
            return LinkFileRef.parse(ref);
        }
        return SimpleFileRef.parse(ref);
    }
}
