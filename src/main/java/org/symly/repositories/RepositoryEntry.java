package org.symly.repositories;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * A file system entry of a {@link Repository}.
 *
 * @param name The name of the repository entry, i.e. the relative {@link Path} of the entry from the {@link
 * Repository}'s root.
 * @param fullPath The full path of the entry, i.e. the {@link Repository}'s root Path + the {@link #name}.
 * @param type The type of the filesystem entry pointed by the {@link #fullPath}
 */
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
        return of(name, fullPath, type);
    }

    static RepositoryEntry of(Path name, Path fullPath, Type type) {
        Path normalizedFullPath = fullPath.toAbsolutePath().normalize();
        Path normalizedName = name.normalize();
        return new RepositoryEntry(normalizedName, normalizedFullPath, type);
    }
}
