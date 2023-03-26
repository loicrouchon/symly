package releaser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class Publisher {

    private final IO io;
    private final Application app;

    public Publisher(IO io, Application app) {
        this.io = io;
        this.app = app;
    }

    public static void main(String[] args) {
        IO io = new IO();
        try {
            Application app = Application.create();
            Publisher publisher = new Publisher(io, app);
            publisher.publish(
                    "https://github.com/loicrouchon/fedora-copr-symly.git",
                    Map.of("build/distributions/symly.spec", "symly.spec"),
                    Set.of());
            publisher.publish(
                    "https://github.com/loicrouchon/symly-debian.git",
                    Map.of("src/packaging/debian/debian", "debian"),
                    Set.of("v%s".formatted(app.version())));
        } catch (ReleaseException e) {
            io.eprintln(e.getMessage());
        } catch (Exception e) {
            io.eprintln(e.getMessage(), e);
        }
    }

    private void publish(String remote, Map<String, String> files, Set<String> tags) throws IOException {
        Repo repo = new Repo(remote);
        Git.ReadWriteGit git = new Git.ReadWriteGit(Files.createTempDirectory(repo.name()));
        git.clone(remote);
        io.printf("Repository %s clone in %s%n", remote, git.dir());
        copyFiles(io, files, git.dir());
        if (!git.status().stdout().isEmpty()) {
            git.commitAll("Symly %s".formatted(app.version()));
            git.push(repo.authenticatedHttpsRemote());
            io.printf("Changes successfully pushed to %s%n", remote);
        } else {
            io.printf("Changes already published to %s%n", remote);
        }
        for (String tag : tags) {
            io.printf("Pushing tag %s to remote %s%n", tag, remote);
            git.pushTag("origin", tag);
        }
    }

    private void copyFiles(IO io, Map<String, String> map, Path destDir) throws IOException {
        for (Map.Entry<String, String> file : map.entrySet()) {
            Path source = Path.of(file.getKey());
            Path dest = destDir.resolve(file.getValue());
            if (!Files.exists(source)) {
                throw new ReleaseException("File %s does not exist".formatted(source));
            }
            io.printf(
                    "Copying %s to %s%n",
                    source.toAbsolutePath().normalize(), dest.toAbsolutePath().normalize());
            copy(source, dest);
        }
    }

    private void copy(Path source, Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(path -> copyFile(path, dest.resolve(source.relativize(path))));
        }
    }

    private void copyFile(Path source, Path dest) {
        try {
            if (!(Files.isDirectory(source) && Files.exists(dest))) {
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new ReleaseException("Failed to copy files from %s to %s: %s %s%n"
                    .formatted(source, dest, e.getClass(), e.getMessage()));
        }
    }

    record Repo(String remote) {

        public String name() {
            return remote.substring(remote.lastIndexOf("/") + 1, remote.lastIndexOf(".git"));
        }

        public String authenticatedHttpsRemote() {
            String username = env(usernameEnvVarName());
            String password = env(passwordEnvVarName());
            return remote.replaceFirst("^(?:git@|https?://)", "https://%s:%s@".formatted(username, password));
        }

        private String env(String name) {
            return Optional.ofNullable(System.getenv(name))
                    .orElseThrow(() -> new ReleaseException("Missing environment variable %s".formatted(name)));
        }

        public String usernameEnvVarName() {
            return "REPO_%s_USERNAME".formatted(envName());
        }

        public String passwordEnvVarName() {
            return "REPO_%s_PASSWORD".formatted(envName());
        }

        private String envName() {
            return name().toUpperCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "_");
        }
    }
}
