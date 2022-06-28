package org.symly.cli.converters;

import java.nio.file.Path;

public final class PathAdapter {

    public static final String SYMLY_CWD_PROPERTY = "symly.cwd";

    private PathAdapter() {}

    public static Path convert(String value) {
        Path path = Path.of(value);
        String cwd = System.getProperty(SYMLY_CWD_PROPERTY);
        if (cwd != null && !path.isAbsolute()) {
            return Path.of(cwd).resolve(path);
        }
        return path;
    }
}
