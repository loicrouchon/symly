package org.linky.files;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileTreeTest extends TemporaryFolderTest {

    @Test
    void testLoad() throws IOException {
        FileTree initial = FileTree.of(workingDir, List.of(
                "hello/world",
                "how/are/you/doing/today"
        ));
        initial.create(workingDir);
        Files.createFile(workingDir.resolve("toto"));
        System.err.println(initial);
        FileTree read = FileTree.fromPath(workingDir);
        initial.assertLayoutIsIdenticalTo(read);
    }
}
