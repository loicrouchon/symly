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
    private final Map<Path, Path> linksNameToTargetMap = new HashMap<>();

    public Collection<Link> list() {
        return linksNameToTargetMap.entrySet()
                .stream()
                .map(this::toLink)
                .sorted(Comparator.comparing(Link::getSource))
                .collect(Collectors.toList());
    }

    private Link toLink(Map.Entry<Path, Path> entry) {
        Path source = destination.resolve(entry.getKey()).normalize();
        Path target = entry.getValue();
        return Link.of(source, target);
    }

    private void add(Path path, Path target) {
        Path linkName = target.relativize(path);
        linksNameToTargetMap.computeIfAbsent(linkName, key -> path.toAbsolutePath().normalize());
    }

    public static Links from(Path destination, List<Path> targets) {
        Links links = new Links(destination);
        for (Path target : targets) {
            SourceReader reader = new SourceReader(target);
            reader.read().forEach(path -> links.add(path, target));
        }
        return links;
    }
}
