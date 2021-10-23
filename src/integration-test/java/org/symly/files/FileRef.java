package org.symly.files;

import java.nio.file.Files;
import java.nio.file.Path;

interface FileRef {

    Path name();

    void create(Path root);

    static FileRef of(Path root, Path path) {
        Path currentPath = root.relativize(path);
        if (Files.isSymbolicLink(path)) {
            Path target = FileTestUtils.readSymbolicLink(path);
            Path targetPath;
            if (target.isAbsolute()) {
                targetPath = root.relativize(target);
            } else {
                targetPath = target;
            }
            return LinkFileRef.of(currentPath, targetPath);
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
