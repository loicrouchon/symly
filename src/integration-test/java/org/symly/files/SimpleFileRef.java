package org.symly.files;

import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor
class SimpleFileRef implements FileRef {

    @Getter
    @NonNull
    private final Path name;

    @Override
    public void create(Path root) {
        Path path = root.resolve(name);
        FileTestUtils.createFile(path);
    }

    @Override
    public String toString() {
        return name.toString();
    }

    static FileRef of(Path name) {
        return new SimpleFileRef(name);
    }

    static FileRef parse(String name) {
        return of(Path.of(name));
    }
}
