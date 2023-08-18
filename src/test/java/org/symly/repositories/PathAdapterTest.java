package org.symly.repositories;

import static org.symly.testing.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PathAdapterTest {

    @Test
    void userHome_shouldBeAdapted() {
        assertThat(adapt("~", "/current/working/dir")).isEqualTo(Path.of(System.getProperty("user.home")));
    }

    @Test
    void userHomeSubDir_shouldBeAdapted() {
        assertThat(adapt("~/dir", "/current/working/dir"))
                .isEqualTo(Path.of(System.getProperty("user.home")).resolve("dir"));
    }

    @Test
    void absolutePath_shouldNotBeAdapted_whenNoCwdIsDefined() {
        assertThat(adapt("/some/path", null)).isEqualTo(Path.of("/some/path"));
    }

    @Test
    void absolutePath_shouldNotBeAdapted_whenCwdIsDefined() {
        assertThat(adapt("/some/path", "/current/working/dir")).isEqualTo(Path.of("/some/path"));
    }

    @Test
    void relativePath_shouldNotBeAdapted_whenNoCwdIsDefined() {
        assertThat(adapt("some/path", null)).isEqualTo(Path.of("some/path"));
    }

    @Test
    void relativePath_shouldBeAdapted_whenCwdIsDefined() {
        assertThat(adapt("some/path", "/current/working/dir")).isEqualTo(Path.of("/current/working/dir/some/path"));
    }

    private Path adapt(String path, String cwd) {
        if (cwd == null) {
            System.clearProperty(PathAdapter.SYMLY_CWD_PROPERTY);
        } else {
            System.setProperty(PathAdapter.SYMLY_CWD_PROPERTY, cwd);
        }
        return PathAdapter.convert(path);
    }
}
