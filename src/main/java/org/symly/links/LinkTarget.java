package org.symly.links;

import java.nio.file.Path;

record LinkTarget(
        /**
         * The name of the link as a relative {@link Path} from the root of the {@link Repository}.
         */
        Path name,
        /**
         * The absolute {@link Path} pointed by the link.
         */
        Path target
) {

    public Link toLink(MainDirectory sourceDirectory) {
        Path source = sourceDirectory.toPath().resolve(name).normalize();
        return Link.of(source, target);
    }
}
