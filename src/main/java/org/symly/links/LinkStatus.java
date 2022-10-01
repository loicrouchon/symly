package org.symly.links;

import java.nio.file.Path;

/**
 * Conceptual representation of a symbolic link.
 *
 * @param source the {@link Path} of the link.
 * @param currentTarget the current {@link Path} pointed by the link.
 * @param desiredTarget the desired {@link Path} that the link should point to.
 */
public record LinkStatus(Path source, Path currentTarget, Path desiredTarget) {

    public Link current() {
        return new Link(source, currentTarget);
    }

    public Link desired() {
        return new Link(source, desiredTarget);
    }
}
