package org.symly.files;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.*;

@SuppressWarnings({"java:S5960" // Assertions should not be used in production code (this is test code)
})
@RequiredArgsConstructor
public class FileTree {

    @NonNull
    private final SortedSet<FileRef> layout;

    public void create(Path root) {
        for (FileRef fileRef : layout) {
            fileRef.create(root);
        }
    }

    public Stream<String> getLayout() {
        return layout.stream().map(FileRef::toString);
    }

    @Override
    public String toString() {
        return layout.stream().map(FileRef::toString).sorted().collect(Collectors.joining("\n"));
    }

    public static FileTree of(Collection<String> files) {
        return of(files.stream().map(FileRef::parse));
    }

    public static FileTree of(Stream<FileRef> files) {
        return new FileTree(
                files.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(FileRef::name)))));
    }

    public static FileTree fromPath(Path root) {
        try {
            try (Stream<Path> files = Files.walk(root)) {
                return of(files.filter(p -> Files.isRegularFile(p) || Files.isSymbolicLink(p))
                        .map(path -> FileRef.of(root, path)));
            }
        } catch (IOException e) {
            fail("Unable to initialize FileTree for path %s".formatted(root), e);
            throw new IllegalStateException("unreachable");
        }
    }

    public Diff diff(FileTree other) {
        Set<String> currentLayout = getLayout().collect(Collectors.toSet());
        Set<String> otherLayout = other.getLayout().collect(Collectors.toSet());
        Set<String> created = diff(currentLayout, otherLayout);
        Set<String> deleted = diff(otherLayout, currentLayout);
        return new Diff(created, deleted);
    }

    public Set<String> diff(Set<String> a, Set<String> b) {
        Set<String> set = new HashSet<>(b);
        set.removeAll(a);
        return set;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Diff {

        private static final Diff EMPTY = new Diff(Set.of(), Set.of());

        Set<String> newPaths;
        Set<String> removedPaths;

        public static Diff ofChanges(String layout) {
            Set<String> newPaths = new HashSet<>();
            Set<String> removedPaths = new HashSet<>();
            layout.lines().forEach(line -> parse(line, newPaths, removedPaths));
            return new Diff(Collections.unmodifiableSet(newPaths), Collections.unmodifiableSet(removedPaths));
        }

        private static void parse(String line, Set<String> newPaths, Set<String> removedPaths) {
            if (line.length() <= 1) {
                throw new IllegalArgumentException("Invalid layout operation. Must start by '+'/'-': " + line);
            }
            if (line.startsWith("+")) {
                newPaths.add(line.substring(1));
            } else if (line.startsWith("-")) {
                removedPaths.add(line.substring(1));
            } else {
                throw new IllegalArgumentException("Invalid layout operation. Must start by '+'/'-': " + line);
            }
        }

        public static Diff empty() {
            return EMPTY;
        }
    }
}
