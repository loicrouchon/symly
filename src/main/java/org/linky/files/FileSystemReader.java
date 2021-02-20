package org.linky.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.linky.cli.LinkyExecutionException;
import org.linky.links.Directory;

public class FileSystemReader {

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public boolean isDirectory(Path path) {
        return Files.isDirectory(path);
    }

    public boolean isATargetDirectory(Directory directory) {
        return Files.isDirectory(directory.toPath());
    }

    public boolean isSymbolicLink(Path path) {
        return Files.isSymbolicLink(path);
    }

    public Path readSymbolicLink(Path link) {
        try {
            return Files.readSymbolicLink(link);
        } catch (IOException e) {
            throw new LinkyExecutionException(String.format(
                    "Unable to read link %s real path: %s", link, e.getMessage()), e);
        }
    }
}
