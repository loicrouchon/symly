package org.symly.links;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

public record RepositoryEntry(
    /**
     * The name of the repository entry, i.e. the relative {@link Path} of the entry from the {@link Repository}'s root.
     */
    @NonNull Path name,
    /**
     * The full path of the entry, i.e. the {@link Repository}'s root Path + the {@link #name()}.
     */
    @NonNull Path fullPath,
    /**
     * The type of the filesystem entry pointed by the {@link #fullPath()}
     */
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
