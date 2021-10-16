package org.symly.links;

import java.nio.file.Path;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.symly.files.FileSystemReader;

/**
 * Conceptual representation of a symbolic link where:
 * <ul>
 *     <li>{@link #source}: is the {@link Path} of the link.</li>
 *     <li>{@link #target}: is the {@link Path} pointed by the link.</li>
 * </ul>
 */
@Value
@RequiredArgsConstructor(staticName = "of")
public class Link {

    /**
     * The {@link Path} of the link.
     */
    Path source;
    /**
     * The {@link Path} pointed by the link.
     */
    Path target;

    public Status status(FileSystemReader fsReader) {
        if (fsReader.isSymbolicLink(source)) {
            Path fromRealDestination = fsReader.readSymbolicLink(source);
            if (Objects.equals(fromRealDestination, target)) {
                return new Status(Status.Type.UP_TO_DATE, this, fsReader);
            }
            return new Status(Status.Type.LINK_CONFLICT, this, fsReader);
        }
        if (fsReader.exists(source)) {
            return new Status(Status.Type.FILE_CONFLICT, this, fsReader);
        }
        return new Status(Status.Type.MISSING, this, fsReader);
    }

    @Override
    public String toString() {
        if (target == null) {
            return source.toString();
        }
        return source + " -> " + target;
    }
}
