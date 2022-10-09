package org.symly.repositories;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.symly.cli.SymlyExecutionException;
import org.symly.files.FileSystemReader;

/**
 * <p>Represents the context configuration read from the {@code symly.config} file in the current working directory.
 * If no {@code symly.config} file exists in the current directory, a default {@code ContextConfig} is returned.</p>
 * <p>The {@code symly.config} file is a properties file matching the Java properties files syntax. It is composed of the following entries:
 * </p>
 * <ul>
 *     <li>{@link #DIRECTORY_PROPERTY}</li>
 *     <li>{@link #REPOSITORIES_PROPERTY}</li>
 *     <li>{@link #ORPHANS_MAX_DEPTH_PROPERTY}</li>
 * </ul>
 * <p>Paths expressed in this file can be:</p>
 * <ul>
 *     <li>Absolute paths: {@code /an/absolute/path}</li>
 *     <li>User home's relative paths when starting with {@code ~}: {@code ~}, {@code ~/a/path}</li>
 *     <li>Current working directory's relative paths: {@code a/path}</li>
 * </ul>
 */
public class ContextConfig {

    private static final String SYMLY_CONFIG = "symly.config";

    /**
     * The main directory path.
     */
    private static final String DIRECTORY_PROPERTY = "directory";
    /**
     * The repositories paths as a comma separated list.
     * For example {@code first-path, ~/second-path, /third-path}
     */
    private static final String REPOSITORIES_PROPERTY = "repositories";

    /**
     * The maximum depth for orphan-links lookup. The default value if not defined is {@link #ORPHAN_MAX_DEPTH_DEFAULT_VALUE}.
     */
    private static final String ORPHANS_MAX_DEPTH_PROPERTY = "orphans.max-depth.search";

    public static final String ORPHAN_MAX_DEPTH_DEFAULT_VALUE = "2";

    /**
     * The {@link Path} to consider as the parent directory of relative paths found in Symly configuration.
     */
    private final Path baseDir;

    /**
     * The Symly configuration properties
     */
    private final Map<String, String> properties;

    private ContextConfig(Path baseDir, Map<String, String> properties) {
        this.baseDir = baseDir;
        this.properties = properties;
    }

    public Optional<Path> directory() {
        return Optional.ofNullable(properties.get(DIRECTORY_PROPERTY)).map(path -> PathAdapter.convert(path, baseDir));
    }

    public List<Path> repositories() {
        String repositories = properties.get(REPOSITORIES_PROPERTY);
        if (repositories == null) {
            return List.of();
        }
        return Arrays.stream(repositories.split(","))
                .map(String::trim)
                .map(path -> PathAdapter.convert(path, baseDir))
                .toList();
    }

    public int orphanMaxDepth() {
        return Integer.parseInt(properties.get(ORPHANS_MAX_DEPTH_PROPERTY));
    }

    public static ContextConfig read(FileSystemReader fsReader) {
        Path config = PathAdapter.convert(SYMLY_CONFIG);
        return read(fsReader, config);
    }

    static ContextConfig read(FileSystemReader fsReader, Path config) {
        Map<String, String> props = defaults();
        if (fsReader.exists(config)) {
            try (var br = Files.newBufferedReader(config)) {
                Properties properties = new Properties();
                properties.load(br);
                properties.forEach((key, value) -> props.put((String) key, (String) value));
            } catch (IOException e) {
                throw new SymlyExecutionException(
                        "Failed to read the symly configuration file %s: %s".formatted(config, e.getMessage()), e);
            }
        }
        return new ContextConfig(config.getParent(), props);
    }

    private static Map<String, String> defaults() {
        HashMap<String, String> defaults = new HashMap<>();
        defaults.put(DIRECTORY_PROPERTY, null);
        defaults.put(REPOSITORIES_PROPERTY, null);
        defaults.put(ORPHANS_MAX_DEPTH_PROPERTY, ORPHAN_MAX_DEPTH_DEFAULT_VALUE);
        return defaults;
    }
}
