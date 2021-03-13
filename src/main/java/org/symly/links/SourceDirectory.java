package org.symly.links;

import java.nio.file.Path;

public class SourceDirectory extends Directory {

    private SourceDirectory(Path path) {
        super(path);
    }

    public static SourceDirectory of(Path path) {
        return new SourceDirectory(path);
    }
}
