package org.linky.files;

import java.nio.file.Path;

public class NoOpFilesMutatorService implements FilesMutatorService {

    @Override
    public void deleteIfExists(Path path) {
    }

    @Override
    public void createDirectories(Path path) {
    }

    @Override
    public void createSymbolicLink(Path from, Path to) {
    }
}
