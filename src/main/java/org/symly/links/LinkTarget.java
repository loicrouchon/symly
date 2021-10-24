package org.symly.links;

import java.nio.file.Path;

/**
 * <p>A {@link LinkTarget} represents a pair of {@link #name}/{@link #target}.
 * The {@link #name} being relative to the root of the {@link Repository}.</p>
 *
 * @param name The name of the link as a relative {@link Path} from the root of the {@link Repository}.
 * @param target The absolute {@link Path} pointed by the link.
 */
record LinkTarget(
        Path name,
        Path target
) {

    public Link toLink(MainDirectory sourceDirectory) {
        Path source = sourceDirectory.toPath().resolve(name).normalize();
        return Link.of(source, target);
    }
}
