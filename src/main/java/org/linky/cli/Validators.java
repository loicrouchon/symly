package org.linky.cli;

import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.linky.files.FilesReaderService;

@RequiredArgsConstructor
class Validators {

    private final FilesReaderService filesReaderService;

    public Optional<String> exists(Path path) {
        if (!filesReaderService.exists(path)) {
            return Optional.of("Path does not exist " + path);
        }
        return Optional.empty();
    }

    public Optional<String> doesNotExists(Path path) {
        if (filesReaderService.exists(path)) {
            return Optional.of("Path already exists " + path);
        }
        return Optional.empty();
    }

    public Optional<String> directoryExists(Path path) {
        return exists(path).or(() -> isDirectory(path));
    }

    private Optional<String> isDirectory(Path path) {
        if (!filesReaderService.exists(path)) {
            return Optional.of("Path is not a directory " + path);
        }
        return Optional.empty();
    }

    public Optional<String> isSubPathOf(Path path, Path parent) {
        if (!path.startsWith(parent)) {
            return Optional.of(String.format("Is not a subpath of %s", parent));
        }
        return Optional.empty();
    }
}
