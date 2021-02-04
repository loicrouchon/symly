package org.linky.files;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileTree {

    private final SortedSet<FileRef> layout;

    public void create(Path root) {
        try {
            for (FileRef fileRef : layout) {
                fileRef.create(root);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void assertLayoutIsIdenticalTo(FileTree expected) {
        assertThat(layout).containsExactlyElementsOf(expected.layout);
    }

    @Override
    public String toString() {
        return layout.stream()
                .map(FileRef::toString)
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    public static FileTree of(Path root, Collection<String> files) {
        return of(root, files.stream().map(FileRef::parse));
    }

    public static FileTree of(Path root, Stream<FileRef> files) {
        return new FileTree(
                files.collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(FileRef::getName))))
        );
    }

    public static FileTree fromPath(String root) {
        return fromPath(Path.of(root));
    }

    public static FileTree fromPath(Path root) {
        try {
            return of(root, Files.walk(root)
                    .filter(path -> !Files.isDirectory(path))
                    .map(path -> FileRef.of(root, path)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    interface FileRef {

        String getName();

        void create(Path root) throws IOException;

        static FileRef ofFile(String name) {
            return new SimpleFileRef(name);
        }

        static FileRef ofLink(String name, String target) {
            return new LinkFileRef(name, target);
        }

        static FileRef of(Path root, Path path) {
            Path currentPath = root.relativize(path);
            if (Files.isSymbolicLink(path)) {
                try {
                    Path destinationPath = root.relativize(path.toRealPath());
                    return new LinkFileRef(currentPath.toString(), destinationPath.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return new SimpleFileRef(currentPath.toString());
        }

        static FileRef parse(String ref) {
            if (ref.contains(" -> ")) {
                String[] parts = ref.split(" -> ");
                assertThat(parts).hasSize(2);
                return ofLink(parts[0], parts[1]);
            }
            return ofFile(ref);
        }
    }

    @EqualsAndHashCode
    @RequiredArgsConstructor
    private static class SimpleFileRef implements FileRef {

        @NonNull
        @Getter
        private final String name;

        @Override
        public void create(Path root) throws IOException {
            Path path = root.resolve(Path.of(name));
            Utils.createDirectory(path.getParent());
            Files.createFile(path);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @EqualsAndHashCode
    @RequiredArgsConstructor
    private static class LinkFileRef implements FileRef {

        @NonNull
        @Getter
        private final String name;
        @NonNull
        private final String target;

        @Override
        public void create(Path root) throws IOException {
            Path path = root.resolve(Path.of(name));
            Utils.createDirectory(path.getParent());
            Files.createLink(path, Path.of(target));
        }

        @Override
        public String toString() {
            return name + " -> " + target;
        }
    }

    private static class Utils {

        static void createDirectory(Path path) throws IOException {
            if (!Files.exists(path)) {
                createDirectory(path.getParent());
                Files.createDirectory(path);
            }
        }
    }
}