package org.symly.repositories;

import java.nio.file.Path;
import java.util.Objects;

public abstract class Directory {

    /**
     * The {@link Path} of this directory.
     */
    private final Path path;

    protected Directory(Path path) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
    }

    public Path toPath() {
        return path;
    }

    public Path relativize(Path otherPath) {
        return path.relativize(otherPath);
    }

    public boolean containsPath(Path path) {
        return path.startsWith(toPath());
    }

    public Path resolve(Path subPath) {
        return path.resolve(subPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Directory directory && path.equals(directory.path));
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
