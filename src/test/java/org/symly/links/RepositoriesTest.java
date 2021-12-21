package org.symly.links;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RepositoriesTest {

    private static final MainDirectory MAIN_DIR = MainDirectory.of(Path.of("/symly/main"));

    @Test
    void emptyRepositories_shouldBeEmpty() {
        //given
        Repositories repositories = Repositories.of(List.of(
            repo("/symly/repo1",
                List.of(),
                List.of()
            )
        ));
        //when/then
        assertThat(repositories.links(MAIN_DIR)).isEmpty();
        assertThat(repositories.allDirectoriesNames()).isEmpty();
    }

    @Test
    void links_shouldBeReturn_fileLinks() {
        //given
        Repositories repositories = Repositories.of(List.of(
            repo("/symly/repo1",
                List.of("dir", "dir/nested"),
                List.of("file", "dir/file")
            ),
            repo("/symly/repo2",
                List.of("dir", "other-dir/nested"),
                List.of("other-file", "dir/file", "other-dir/nested-file")
            )
        ));
        //when/then
        assertThat(repositories.links(MAIN_DIR))
            .hasSize(4)
            .contains(
                Link.of(Path.of("/symly/main/file"), Path.of("/symly/repo1/file")),
                Link.of(Path.of("/symly/main/dir/file"), Path.of("/symly/repo1/dir/file")),
                Link.of(Path.of("/symly/main/other-file"), Path.of("/symly/repo2/other-file")),
                Link.of(Path.of("/symly/main/other-dir/nested-file"), Path.of("/symly/repo2/other-dir/nested-file"))
            );
    }

    @Test
    void containsPath_shouldBeTrue_whenPathIsContainedInRepository() {
        //given
        Repositories repositories = Repositories.of(List.of(
            repo("/symly/repo1",
                List.of("dir", "dir/nested"),
                List.of("file", "dir/file")
            ),
            repo("/symly/repo2",
                List.of("dir", "other-dir/nested"),
                List.of("otherfile", "dir/otherfile", "other-dir/nested")
            )
        ));
        //when/then
        // existing files in repos
        assertThat(repositories.containsPath(Path.of("/symly/repo1","file"))).isTrue();
        assertThat(repositories.containsPath(Path.of("/symly/repo2","dir/otherfile"))).isTrue();
        // non existing files in repos
        assertThat(repositories.containsPath(Path.of("/symly/repo1","some/things"))).isTrue();
        assertThat(repositories.containsPath(Path.of("/symly/repo2","some/other/things"))).isTrue();
        // files outside the repos
        assertThat(repositories.containsPath(Path.of("/symly/repo3","no/luck"))).isFalse();
    }

    private Repository repo(String path, Collection<String> dirs, Collection<String> files) {
        return new TestRepository(path, dirs, files) {

        };
    }

    private static class TestRepository extends Repository {

        private final Collection<String> directories;
        private final Collection<String> files;

        public TestRepository(String path, Collection<String> directories, Collection<String> files) {
            super(Path.of(path));
            this.directories = directories;
            this.files = files;
        }

        @Override
        Stream<RepositoryEntry> entries() {
            return Stream.concat(
                entries(directories, RepositoryEntry.Type.DIRECTORY),
                entries(files, RepositoryEntry.Type.FILE)
            );
        }

        private Stream<RepositoryEntry> entries(Collection<String> paths, RepositoryEntry.Type type) {
            return paths.stream()
                .map(Path::of)
                .map(path -> new RepositoryEntry(path, toPath().resolve(path), type));
        }
    }
}
