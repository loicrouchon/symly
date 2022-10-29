package org.symly.env;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.symly.files.FileTestUtils;
import org.symly.files.FileTree;
import org.symly.files.RIOException;

public class Env {

    private static final String PATH_ELEMENT_PATTERN = "[a-zA-X0-9-\\.]+";
    private static final String PATH_PATTERN = "(%1$s(?:/%1$s)*)".formatted(PATH_ELEMENT_PATTERN);
    private static final Pattern LAYOUT_DIRECTORY_PATTERN = Pattern.compile("^D %s$".formatted(PATH_PATTERN));
    private static final Pattern LAYOUT_FILE_PATTERN = Pattern.compile("^F %s$".formatted(PATH_PATTERN));
    private static final Pattern LAYOUT_LINK_PATTERN =
            Pattern.compile("^L %1$s(?:\\s+)->(?:\\s+)%1$s$".formatted(PATH_PATTERN));

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

    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    public Env withHome(String path) {
        home = path(path);
        return this;
    }

    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    public Env withWorkingDir(String path) {
        workingDirectory = path(path);
        return this;
    }

    public Path path(String path) {
        return root().resolve(path);
    }

    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    public Env withLayout(String layout) {
        layout.lines().forEach(this::processLayoutLine);
        return this;
    }

    @SuppressWarnings("CanIgnoreReturnValueSuggester")
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
            boolean allDeleted =
                    walker.sorted(Comparator.reverseOrder()).map(Path::toFile).allMatch(File::delete);
            if (!allDeleted) {
                throw new RIOException("Unable to delete all files from %s".formatted(root));
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

    private void processLayoutLine(String layoutLine) {
        Matcher matcher = LAYOUT_DIRECTORY_PATTERN.matcher(layoutLine);
        if (matcher.matches()) {
            withDirectories(matcher.group(1));
            return;
        }
        matcher = LAYOUT_FILE_PATTERN.matcher(layoutLine);
        if (matcher.matches()) {
            withFiles(matcher.group(1));
            return;
        }
        matcher = LAYOUT_LINK_PATTERN.matcher(layoutLine);
        if (matcher.matches()) {
            withSymbolicLink(matcher.group(1), matcher.group(2));
            return;
        }
        throw new IllegalArgumentException("Invalid layout: " + layoutLine);
    }

    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    private Env withFiles(String... paths) {
        for (String path : paths) {
            FileTestUtils.createFile(path(path));
        }
        return this;
    }

    public void withFileContent(String path, String content) {
        FileTestUtils.createOrUpdateFile(path(path), content);
    }

    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    private Env withDirectories(String... paths) {
        for (String path : paths) {
            FileTestUtils.createDirectory(path(path));
        }
        return this;
    }

    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    private Env withSymbolicLink(String sourcePath, String targetPath) {
        FileTestUtils.createSymbolicLink(path(sourcePath), path(targetPath));
        return this;
    }

    public Env deleteFile(String path) {
        FileTestUtils.deleteIfExists(path(path));
        return this;
    }

    private Env createHomeDirectory() {
        return withDirectories(home().toString());
    }
}
