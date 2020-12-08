package org.linky.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.linky.cli.LinkyExecutionException;

public class FilesReaderService {

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public boolean isSymbolicLink(Path path) {
        return Files.isSymbolicLink(path);
    }

    public Path toRealPath(Path link) {
        try {
            return link.toRealPath();
        } catch (IOException e) {
            throw new LinkyExecutionException(String.format("Unable to read link %s real path", link), e);
        }
    }
}
