package org.symly.links;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

public record RepositoryEntry(
    @NonNull Path name,
    @NonNull Path fullPath,
    @NonNull Type type
) {

    public enum Type {
        FILE,
        DIRECTORY
    }

    public static RepositoryEntry of(Path name, Path fullPath) {
        Type type;
        if (Files.isDirectory(fullPath)) {
            type = Type.DIRECTORY;
        } else {
            type = Type.FILE;
        }
        Path normalizedFullPath = fullPath.toAbsolutePath().normalize();
        Path normalizedName = name.normalize();
        return new RepositoryEntry(normalizedName, normalizedFullPath, type);
    }
}
