package org.symly.files;

import static org.symly.testing.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public sealed interface FileRef permits FileRef.LinkFileRef, FileRef.SimpleFileRef, FileRef.DirectoryRef {

    Path name();

    void create(Path root);

    static FileRef of(Path root, Path path) {
        Path currentPath = root.relativize(path);
        if (Files.isSymbolicLink(path)) {
            Path target = FileTestUtils.readSymbolicLink(path);
            Path targetPath;
            if (target.isAbsolute()) {
                targetPath = root.relativize(target);
            } else {
                targetPath = target;
            }
            return LinkFileRef.of(currentPath, targetPath);
        } else if (Files.isDirectory(path)) {
            return DirectoryRef.of(currentPath);
        }
        return SimpleFileRef.of(currentPath);
    }

    static FileRef parse(String ref) {
        if (LinkFileRef.isLink(ref)) {
            return LinkFileRef.parse(ref);
        }
        return SimpleFileRef.parse(ref);
    }

    record DirectoryRef(Path name) implements FileRef {
        public DirectoryRef {
            Objects.requireNonNull(name);
        }

        @Override
        public void create(Path root) {
            Path path = root.resolve(name);
            FileTestUtils.createDirectory(path);
        }

        @Override
        public String toString() {
            return "D " + name;
        }

        static FileRef of(Path name) {
            return new DirectoryRef(name);
        }
    }

    record SimpleFileRef(Path name) implements FileRef {
        public SimpleFileRef {
            Objects.requireNonNull(name);
        }

        @Override
        public void create(Path root) {
            Path path = root.resolve(name);
            FileTestUtils.createFile(path);
        }

        @Override
        public String toString() {
            return "F " + name;
        }

        static FileRef of(Path name) {
            return new SimpleFileRef(name);
        }

        static FileRef parse(String name) {
            return of(Path.of(name));
        }
    }

    @SuppressWarnings({
        "java:S5960", // Assertions should not be used in production code (This is test code)
    })
    record LinkFileRef(Path name, Path target) implements FileRef {
        public LinkFileRef {
            Objects.requireNonNull(name);
            Objects.requireNonNull(target);
        }

        private static final String LINK_SEPARATOR = " -> ";

        @Override
        public void create(Path root) {
            Path path = root.resolve(name);
            FileTestUtils.createSymbolicLink(path, target);
        }

        @Override
        public String toString() {
            return "L " + name + LINK_SEPARATOR + target;
        }

        static boolean isLink(String ref) {
            return ref.contains(LinkFileRef.LINK_SEPARATOR);
        }

        static LinkFileRef of(Path name, Path target) {
            return new LinkFileRef(name, target);
        }

        static LinkFileRef parse(String ref) {
            String[] parts = ref.split(LinkFileRef.LINK_SEPARATOR);
            assertThat(parts).hasSize(2);
            return LinkFileRef.of(Path.of(parts[0]), Path.of(parts[1]));
        }
    }
}
