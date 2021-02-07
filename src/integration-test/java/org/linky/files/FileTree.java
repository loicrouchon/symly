package org.linky.files;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
}