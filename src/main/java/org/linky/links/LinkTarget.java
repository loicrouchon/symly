package org.linky.links;

import java.nio.file.Path;
import lombok.Value;

@Value
class LinkTarget {

    /**
     * The name of the link as a relative {@link Path} from the root of the {@link TargetDirectory}.
     */
    Path name;
    /**
     * The absolute {@link Path} pointed by the link.
     */
    Path target;

    public Link toLink(Path destination) {
        Path source = destination.resolve(name).normalize();
        return Link.of(source, target);
    }
}
