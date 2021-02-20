package org.linky.links;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Links {

    /**
     * The directory in which the links must be created to the targets.
     */
    private final Path destination;
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
        return linkTarget.toLink(destination);
    }

    private void add(LinkTarget linkTarget) {
        nameToTargetLinkMap.putIfAbsent(linkTarget.getName(), linkTarget);
    }

    public static Links from(Path destination, List<TargetDirectory> targetDirectories) {
        Links links = new Links(destination);
        for (TargetDirectory targetDirectory : targetDirectories) {
            targetDirectory.links().forEach(links::add);
        }
        return links;
    }
}
