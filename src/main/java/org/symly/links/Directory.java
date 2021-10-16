package org.symly.links;

import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
public abstract class Directory {

    /**
     * The {@link Path} of this directory.
     */
    private final Path path;

    protected Directory(@NonNull Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public Path toPath() {
        return path;
    }

    public Path relativize(Path otherPath) {
        return path.relativize(otherPath);
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
