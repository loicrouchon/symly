package org.symly.orphans;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;
import org.symly.links.Repository;

class OrphanTest {

    @Test
    void findOrphans() throws IOException {
        Path rootDir = Paths.get(System.getProperty("user.home"));
        final int maxDepth = 3;
        Set<Repository> repos = Stream.of(
                        "/Users/loicrouchon/private/workspace/env/mysugr",
                        "/Users/loicrouchon/private/workspace/env/macos",
                        "/Users/loicrouchon/private/workspace/env/defaults")
                .map(Paths::get)
                .map(Repository::of)
                .collect(Collectors.toSet());
        OrphanFinder orphanFinder = new OrphanFinder(new FileSystemReader());
        Collection<Link> orphans = orphanFinder.findOrphans(rootDir, maxDepth, repos);
        System.out.printf("%d orphans found%n", orphans.size());
        orphans.forEach(orphan -> System.out.printf(" - [ORPHAN] %s%n", orphan));
    }
}
