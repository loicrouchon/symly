package org.linky.cli;

import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.linky.files.FilesReaderService;

@RequiredArgsConstructor
class Validators {

    private final FilesReaderService filesReaderService;

    public Optional<String> directoryExists(Path path) {
        return exists(path).or(() -> isDirectory(path));
    }

    private Optional<String> exists(Path path) {
        if (!filesReaderService.exists(path)) {
            return Optional.of("Path does not exist");
        }
        return Optional.empty();
    }

    private Optional<String> isDirectory(Path path) {
        if (!filesReaderService.exists(path)) {
            return Optional.of("Path is not a directory");
        }
        return Optional.empty();
    }
}
