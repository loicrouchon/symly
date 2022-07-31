package org.symly.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.files.FileSystemReader;

class ContextConfigTest {

    private final Path path = Path.of("src/test/resources/org/symly/repositories");
    private final FileSystemReader fsReader = new FileSystemReader.RealFileSystemReader();

    @Test
    void syncConfig_shouldDefault_whenNoConfigFileIsFound() {
        // given/when
        ContextConfig config = ContextConfig.read(fsReader, path.resolve("non-existing.config"));
        // then
        assertThat(config).isNotNull();
        assertThat(config.directory()).isEmpty();
        assertThat(config.repositories()).isEmpty();
    }

    @Test
    void syncConfig_shouldRead_directory() {
        // given/when
        ContextConfig config = ContextConfig.read(fsReader, path.resolve("symly-simple-repo.config"));
        // then
        assertThat(config).isNotNull();
        assertThat(config.directory()).isPresent().hasValue(Path.of(System.getProperty("user.home")));
    }

    @Test
    void syncConfig_shouldRead_singleRepository() {
        // given/when
        ContextConfig config = ContextConfig.read(fsReader, path.resolve("symly-simple-repo.config"));
        // then
        assertThat(config).isNotNull();
        assertThat(config.repositories()).isEqualTo(List.of(path.resolve("some/dir")));
    }

    @Test
    void syncConfig_shouldRead_multipleRepositories() {
        // given/when
        ContextConfig config = ContextConfig.read(fsReader, path.resolve("symly-multi-repos.config"));
        // then
        assertThat(config).isNotNull();
        assertThat(config.repositories())
                .isEqualTo(List.of(path.resolve("first"), Path.of("/second"), PathAdapter.convert("~/third")));
    }
}
