package org.symly.repositories;

import java.util.Collection;
import lombok.NonNull;
import org.symly.links.Link;

/**
 * The {@code Context} combines the information of where from and where to the links should be created.
 *
 * @param mainDirectory The main directory in which the links should be created.
 * @param repositories The repositories containing the files to be linked in {@link #mainDirectory}.
 * @param orphanMaxDepth the maximum depth for orphan-links lookup.
 */
public record Context(@NonNull MainDirectory mainDirectory, @NonNull Repositories repositories, int orphanMaxDepth) {

    public Collection<Link> links() {
        return repositories.links(mainDirectory);
    }
}
