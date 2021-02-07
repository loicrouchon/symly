package org.linky.files;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor
class LinkFileRef implements FileRef {

    private static final String LINK_SEPARATOR = " -> ";

    @Getter
    @NonNull
    private final Path name;
    @NonNull
    private final Path target;

    @Override
    public void create(Path root) {
        Path path = root.resolve(name);
        FileTestUtils.createSymbolicLink(path, target);
    }

    @Override
    public String toString() {
        return name.toString() + LINK_SEPARATOR + target.toString();
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
