package org.linky.files;

import java.io.IOException;
import java.nio.file.Path;

public interface FilesMutatorService {

    void deleteIfExists(Path path) throws IOException;

    void createDirectories(Path path) throws IOException;

    void createSymbolicLink(Path from, Path to) throws IOException;
}
