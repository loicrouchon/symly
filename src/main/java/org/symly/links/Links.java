package org.symly.links;

import java.nio.file.Path;
import java.util.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Links {

    /**
     * The directory in which the links must be created.
     */
    private final MainDirectory sourceDirectory;
    /**
     * A {@link Map} which keys are the links names as a relative {@link Path} and the values the links targets {@link
     * Path}.
     */
    private final Map<Path, LinkTarget> nameToTargetLinkMap = new HashMap<>();

    public Collection<Link> list() {
        return nameToTargetLinkMap.values()
                .stream()
                .map(this::toLink)
                .sorted(Comparator.comparing(Link::source))
                .toList();
    }

    private Link toLink(LinkTarget linkTarget) {
        return linkTarget.toLink(sourceDirectory);
    }

    private void add(LinkTarget linkTarget) {
        nameToTargetLinkMap.putIfAbsent(linkTarget.name(), linkTarget);
    }

    public static Links from(MainDirectory mainDirectory, List<Repository> repositories) {
        Links links = new Links(mainDirectory);
        for (Repository repository : repositories) {
            repository.links().forEach(links::add);
        }
        return links;
    }
}
