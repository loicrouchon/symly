package org.symly.links;

import java.nio.file.Path;

public class MainDirectory extends Directory {

    private MainDirectory(Path path) {
        super(path);
    }

    public static MainDirectory of(Path path) {
        return new MainDirectory(path);
    }
}
