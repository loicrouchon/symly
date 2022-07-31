package org.symly.repositories;

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.symly.cli.validation.Constraint;
import org.symly.files.FileSystemReader;
import org.symly.links.Link;

public class ContextConfig {

    public record InputContext(Path mainDirectory, List<Path> repositories, Integer orphanMaxDepth) {

        public InputContext(Path mainDirectory, List<Path> repositoriesList) {
            this(mainDirectory, repositoriesList, null);
        }
    }

    public record Context(MainDirectory mainDirectory, Repositories repositories, int orphanMaxDepth) {

        public Collection<Constraint> constraints(FileSystemReader fsReader) {
            Collection<Constraint> constraints = new ArrayList<>();
            constraints.add(Constraint.of("Main directory is not defined", () -> mainDirectory != null));
            constraints.add(Constraint.of(
                    String.format("Main directory (%s) is not an existing directory", mainDirectory),
                    () -> fsReader.isDirectory(mainDirectory.toPath())));
            constraints.add(Constraint.of(
                    "Repositories are not defined",
                    () -> repositories != null && !repositories.repositories().isEmpty()));
            repositories.repositories().forEach(repository -> {
                constraints.add(Constraint.of(
                        String.format("Repository (%s) is not an existing directory", repository.toPath()),
                        () -> fsReader.isDirectory(repository.toPath())));
            });
            constraints.add(Constraint.of(
                    String.format("Orphan lookup max-depth (%s) must be a positive integer", orphanMaxDepth),
                    () -> orphanMaxDepth >= 0));
            return constraints;
        }

        public static Context from(FileSystemReader fsReader, ContextConfig contextConfig, InputContext inputContext) {
            MainDirectory directory = Optional.ofNullable(inputContext.mainDirectory)
                    .or(contextConfig::directory)
                    .map(MainDirectory::of)
                    .orElse(null);
            Repositories repositories = Repositories.of(
                    fsReader,
                    Optional.ofNullable(inputContext.repositories)
                            .filter(not(Collection::isEmpty))
                            .orElseGet(contextConfig::repositories)
                            .stream()
                            .map(Repository::of)
                            .toList());
            int orphanMaxDepth =
                    Optional.ofNullable(inputContext.orphanMaxDepth()).orElseGet(contextConfig::orphanMaxDepth);
            return new Context(directory, repositories, orphanMaxDepth);
        }

        public Collection<Link> links() {
            return repositories.links(mainDirectory);
        }
    }

    private static final String SYMLY_CONFIG = "symly.config";
    private static final String DIRECTORY_PROPERTY = "directory";
    private static final String REPOSITORIES_PROPERTY = "repositories";
    private static final String ORPHANS_MAX_DEPTH_PROPERTY = "orphans.max-depth.search";
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

    Optional<Path> directory() {
        return Optional.ofNullable(properties.get(DIRECTORY_PROPERTY)).map(path -> PathAdapter.convert(path, baseDir));
    }

    List<Path> repositories() {
        String repositories = properties.get(REPOSITORIES_PROPERTY);
        if (repositories == null) {
            return List.of();
        }
        return Arrays.stream(repositories.split(","))
                .map(String::trim)
                .map(path -> PathAdapter.convert(path, baseDir))
                .toList();
    }

    int orphanMaxDepth() {
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
                throw new RuntimeException(e);
            }
        }
        return new ContextConfig(config.getParent(), props);
    }

    private static Map<String, String> defaults() {
        HashMap<String, String> defaults = new HashMap<>();
        defaults.put(DIRECTORY_PROPERTY, null);
        defaults.put(REPOSITORIES_PROPERTY, null);
        defaults.put(ORPHANS_MAX_DEPTH_PROPERTY, "2");
        return defaults;
    }
}
