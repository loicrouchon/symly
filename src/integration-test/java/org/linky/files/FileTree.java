package org.linky.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.*;

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
        return layout.stream()
                .map(FileRef::toString)
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    public static FileTree of(Collection<String> files) {
        return of(files.stream().map(FileRef::parse));
    }

    public static FileTree of(Stream<FileRef> files) {
        return new FileTree(
                files.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(FileRef::getName))))
        );
    }

    public static FileTree fromPath(Path root) {
        try {
            return of(Files.walk(root)
                    .filter(p -> Files.isRegularFile(p) || Files.isSymbolicLink(p))
                    .map(path -> FileRef.of(root, path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
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

        public Diff withNewPaths(String... newPaths) {
            return new Diff(Set.of(newPaths), removedPaths);
        }

        public Diff withRemovedPaths(String... removedPaths) {
            return new Diff(newPaths, Set.of(removedPaths));
        }

        public static Diff empty() {
            return EMPTY;
        }
    }
}