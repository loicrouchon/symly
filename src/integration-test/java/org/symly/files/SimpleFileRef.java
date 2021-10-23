package org.symly.files;

import java.nio.file.Path;
import lombok.NonNull;

record SimpleFileRef(
        @NonNull Path name
) implements FileRef {

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
