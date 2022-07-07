package org.symly.links;

import java.nio.file.Path;
import java.util.Objects;
import org.symly.files.FileSystemReader;
import org.symly.repositories.MainDirectory;

/**
 * Conceptual representation of a symbolic link.
 *
 * @param source the {@link Path} of the link.
 * @param target the {@link Path} pointed by the link.
 */
public record Link(Path source, Path target) {

    public Status status(FileSystemReader fsReader) {
        if (fsReader.isSymbolicLink(source)) {
            Path fromRealDestination = fsReader.readSymbolicLink(source);
            if (Objects.equals(fromRealDestination, target)) {
                return new Status(Status.Type.UP_TO_DATE, this);
            }
            return new Status(Status.Type.LINK_CONFLICT, this);
        }
        if (fsReader.exists(source)) {
            return new Status(Status.Type.FILE_CONFLICT, this);
        }
        return new Status(Status.Type.MISSING, this);
    }

    @Override
    public String toString() {
        if (target == null) {
            return source.toString();
        }
        return source + " -> " + target;
    }

    public String toString(MainDirectory mainDirectory) {
        if (target == null) {
            return mainDirectory.relativize(source).toString();
        }
        return mainDirectory.relativize(source) + " -> " + target;
    }

    public static Link of(Path source, Path target) {
        return new Link(source, target);
    }
}
