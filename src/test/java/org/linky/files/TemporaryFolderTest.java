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
        workingDir = Files.createTempDirectory(tmpDir, "linky-");
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
}
