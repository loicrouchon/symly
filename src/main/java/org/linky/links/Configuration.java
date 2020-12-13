package org.linky.links;

import java.nio.file.Path;

public class Configuration {

    private Configuration() {
    }

    public static Path symlinkMarker(Path directory) {
        return directory.resolve(".symlink");
    }
}
