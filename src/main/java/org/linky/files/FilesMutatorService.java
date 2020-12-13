package org.linky.files;

import java.io.IOException;
import java.nio.file.Path;

public interface FilesMutatorService {

    void deleteIfExists(Path path) throws IOException;

    void move(Path from, Path to) throws IOException;

    void createDirectories(Path path) throws IOException;

    void createSymbolicLink(Path from, Path to) throws IOException;

    void createEmptyFile(Path path) throws IOException;
}
