package org.linky.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class TemporaryFolderTest {

    protected Path workingDir;

    @BeforeEach
    public void before() throws IOException {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        workingDir = Files.createTempDirectory(tmpDir, "linky-").toRealPath();
    }

    @AfterEach
    public void after() throws IOException {
        if (workingDir != null) {
            Files.walk(workingDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    protected void createFiles(String... paths) {
        for (String path : paths) {
            FileTestUtils.createFile(workingDir.resolve(path));
        }
    }

    protected void createDirectories(String... paths) {
        for (String path : paths) {
            FileTestUtils.createDirectory(workingDir.resolve(path));
        }
    }

    protected void createSymbolicLink(String sourcePath, String targetPath) {
        FileTestUtils.createSymbolicLink(workingDir.resolve(sourcePath), workingDir.resolve(targetPath));
    }

    protected FileTree rootFileTree() {
        return fileTree(".");
    }

    protected FileTree fileTree(String path) {
        return FileTree.fromPath(workingDir.resolve(path));
    }
}
