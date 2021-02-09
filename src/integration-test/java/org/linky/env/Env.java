package org.linky.env;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.linky.files.FileTestUtils;
import org.linky.files.FileTree;
import org.linky.files.RIOException;

public class Env {

    private static final String USER_HOME = "user.home";

    private final Path root;
    private Path home;
    private Path workingDirectory;
    private final Map<String, String> properties = new HashMap<>();
    private final long timeout = 5L;

    public Env(Path root) throws IOException {
        this.root = root.toRealPath();
        withHome("home");
        withWorkingDir(".");
    }

    public Path root() {
        return root;
    }

    public Path home() {
        return home;
    }

    public Env withHome(String path) {
        home = path(path);
        properties.put(USER_HOME, home.toString());
        return this;
    }

    public Env withWorkingDir(String path) {
        workingDirectory = path(path);
        return this;
    }

    public Path path(String path) {
        return root().resolve(path);
    }

    public Env withFiles(String... paths) {
        for (String path : paths) {
            FileTestUtils.createFile(path(path));
        }
        return this;
    }

    public Env withDirectories(String... paths) {
        for (String path : paths) {
            FileTestUtils.createDirectory(path(path));
        }
        return this;
    }

    public Env createHomeDirectory() {
        return withDirectories(home().toString());
    }

    public Env withSymbolicLink(String sourcePath, String targetPath) {
        FileTestUtils.createSymbolicLink(path(sourcePath), path(targetPath));
        return this;
    }

    public Env create(FileTree tree) {
        tree.create(root);
        return this;
    }

    public FileTree getRootFileTree() {
        return getFileTree(".");
    }

    public FileTree getFileTree(String path) {
        return FileTree.fromPath(path(path));
    }

    public Execution run(String... args) {
        return new Command(workingDirectory, properties).run(args, timeout);
    }

    public void delete() {
        try (Stream<Path> walker = Files.walk(root)) {
            boolean allDeleted = walker
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .allMatch(File::delete);
            if (!allDeleted) {
                throw new RIOException(String.format("Unable to delete all files from %s", root));
            }
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }

    public static Env of() {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        try {
            Path root = Files.createTempDirectory(tmpDir, "linky-");
            return new Env(root).createHomeDirectory();
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }
}
