package org.linky.links;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Links {

    /**
     * The directory in which the links must be created.
     */
    private final SourceDirectory sourceDirectory;
    /**
     * A {@link Map} which keys are the links names as a relative {@link Path} and the values the links targets {@link
     * Path}.
     */
    private final Map<Path, LinkTarget> nameToTargetLinkMap = new HashMap<>();

    public Collection<Link> list() {
        return nameToTargetLinkMap.values()
                .stream()
                .map(this::toLink)
                .sorted(Comparator.comparing(Link::getSource))
                .collect(Collectors.toList());
    }

    private Link toLink(LinkTarget linkTarget) {
        return linkTarget.toLink(sourceDirectory);
    }

    private void add(LinkTarget linkTarget) {
        nameToTargetLinkMap.putIfAbsent(linkTarget.getName(), linkTarget);
    }

    public static Links from(SourceDirectory sourceDirectory, List<Repository> repositories) {
        Links links = new Links(sourceDirectory);
        for (Repository repository : repositories) {
            repository.links().forEach(links::add);
        }
        return links;
    }
}
