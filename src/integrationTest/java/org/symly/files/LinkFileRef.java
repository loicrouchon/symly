package org.symly.files;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import lombok.NonNull;

@SuppressWarnings({
    "java:S5960", // Assertions should not be used in production code (This is test code)
})
record LinkFileRef(
    @NonNull Path name,
    @NonNull Path target
) implements FileRef {

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
