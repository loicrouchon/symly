package org.linky.links;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Links {

    private final Path destination;
    private final Map<Path, Path> linksMap = new HashMap<>();

    public void add(Path name, Path source) {
        Path normalizedName = destination.resolve(source.relativize(name)).normalize();
        if (!linksMap.containsKey(normalizedName)) {
            linksMap.put(normalizedName, name.toAbsolutePath().normalize());
        }
    }

    public Collection<Link> list() {
        return linksMap.entrySet()
                .stream()
                .map(e -> new Link(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(Link::getFrom))
                .collect(Collectors.toList());
    }

    public static Links from(Path destination, List<Path> sources) {
        Links links = new Links(destination);
        for (Path source : sources) {
            SourceReader reader = new SourceReader(source);
            reader.read().forEach(path -> links.add(path, source));
        }
        return links;
    }
}
