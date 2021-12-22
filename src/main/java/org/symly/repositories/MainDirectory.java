package org.symly.repositories;

import java.nio.file.Path;
import org.symly.links.Directory;

/**
 * The directory in which links will be created to files present in {@link Repositories}.
 */
public class MainDirectory extends Directory {

    private MainDirectory(Path path) {
        super(path);
    }

    public static MainDirectory of(Path path) {
        return new MainDirectory(path);
    }
}
