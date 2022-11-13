package org.symly.files;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"java:S5960" // Assertions should not be used in production code (this is test code)
})
public class FileTree {

    private final SortedSet<FileRef> layout;

    public FileTree(SortedSet<FileRef> layout) {
        this.layout = Objects.requireNonNull(layout);
    }

    public void create(Path root) {
        for (FileRef fileRef : layout) {
            fileRef.create(root);
        }
    }

    public Stream<FileRef> layout() {
        return layout.stream();
    }

    public Stream<String> getFilesLayout() {
        return layout.stream()
                .filter(fileRef -> !(fileRef instanceof FileRef.DirectoryRef))
                .map(FileRef::toString);
    }

    public FileTree subtree(Predicate<? super FileRef> predicate) {
        return of(layout.stream().filter(predicate));
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
        try (Stream<Path> files = Files.walk(root)) {
            return of(files.filter(path -> !Objects.equals(root, path)).map(path -> FileRef.of(root, path)));
        } catch (IOException e) {
            fail("Unable to initialize FileTree for path %s".formatted(root), e);
            throw new IllegalStateException("unreachable");
        }
    }

    public Diff diff(FileTree other) {
        Set<String> currentLayout = getFilesLayout().collect(Collectors.toSet());
        Set<String> otherLayout = other.getFilesLayout().collect(Collectors.toSet());
        Set<String> created = diff(currentLayout, otherLayout);
        Set<String> deleted = diff(otherLayout, currentLayout);
        return new Diff(created, deleted);
    }

    public Set<String> diff(Set<String> a, Set<String> b) {
        Set<String> set = new HashSet<>(b);
        set.removeAll(a);
        return set;
    }

    public record Diff(Set<String> newPaths, Set<String> removedPaths) {

        private static final Diff EMPTY = new Diff(Set.of(), Set.of());

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
