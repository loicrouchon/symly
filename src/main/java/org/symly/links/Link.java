package org.symly.links;

import java.nio.file.Path;
import org.symly.repositories.MainDirectory;

/**
 * Conceptual representation of a symbolic link.
 *
 * @param source the {@link Path} of the link.
 * @param target the {@link Path} pointed by the link.
 */
public record Link(Path source, Path target) {

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
