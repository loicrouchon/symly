package org.symly.repositories;

import java.nio.file.Path;

public final class PathAdapter {

    public static final String SYMLY_CWD_PROPERTY = "symly.cwd";
    private static final String USER_HOME_PROPERTY = "user.home";

    private PathAdapter() {}

    public static Path convert(String value) {
        return convert(value, getCurrentWorkingDirectory());
    }

    private static Path getCurrentWorkingDirectory() {
        String cwd = System.getProperty(SYMLY_CWD_PROPERTY);
        if (cwd == null) {
            return null;
        }
        return Path.of(cwd);
    }

    public static Path convert(String value, Path baseDir) {
        if (referencesUserHomeDirectory(value)) {
            return expandUserHome(value);
        }
        return patchCurrentWorkingDirectory(value, baseDir);
    }

    private static boolean referencesUserHomeDirectory(String value) {
        return value.startsWith("~/") || value.equals("~");
    }

    private static Path expandUserHome(String value) {
        String home = System.getProperty(USER_HOME_PROPERTY);
        return Path.of(value.replaceFirst("^~", home));
    }

    private static Path patchCurrentWorkingDirectory(String value, Path baseDir) {
        Path path = Path.of(value);
        if (baseDir != null && !path.isAbsolute()) {
            return baseDir.resolve(path);
        }
        return path;
    }
}
