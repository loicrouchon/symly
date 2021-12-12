package org.symly.env;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import org.symly.files.FileTestUtils;
import org.symly.files.FileTree;
import org.symly.files.RIOException;

public class Env {

    private final Path root;
    private Path home;
    private Path workingDirectory;

    public Env(Path root) throws IOException {
        this.root = root.toRealPath();
        withHome("home/user");
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
        return getFileTree(root);
    }

    public FileTree getFileTree(String path) {
        return getFileTree(path(path));
    }

    private FileTree getFileTree(Path path) {
        return FileTree.fromPath(path);
    }

    public Execution run(String... args) {
        if (Objects.equals(System.getProperty("symly.testing.opaque-testing"), "true")) {
            return new JvmCommand(root, workingDirectory, home).run(args);
        } else {
            return new MainCommand(root, workingDirectory, home).run(args);
        }
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
            Path root = Files.createTempDirectory(tmpDir, "symly-");
            return new Env(root).createHomeDirectory();
        } catch (IOException e) {
            throw new RIOException(e);
        }
    }
}
