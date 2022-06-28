package org.symly.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.symly.links.Link;

class RepositoriesTest {

    private static final MainDirectory MAIN_DIR = MainDirectory.of(Path.of("/symly/main"));

    @Test
    void containsPath_shouldBeTrue_whenPathIsContainedInRepository() {
        // given
        Repositories repositories = Repositories.of(List.of(
                repo("/symly/repo1", List.of("dir", "dir/nested"), List.of("file", "dir/file")),
                repo(
                        "/symly/repo2",
                        List.of("dir", "other-dir/nested"),
                        List.of("otherfile", "dir/otherfile", "other-dir/nested"))));
        // when/then
        // existing files in repos
        assertThat(repositories.containsPath(Path.of("/symly/repo1", "file"))).isTrue();
        assertThat(repositories.containsPath(Path.of("/symly/repo2", "dir/otherfile")))
                .isTrue();
        // non-existing files in repos
        assertThat(repositories.containsPath(Path.of("/symly/repo1", "some/things")))
                .isTrue();
        assertThat(repositories.containsPath(Path.of("/symly/repo2", "some/other/things")))
                .isTrue();
        // files outside the repos
        assertThat(repositories.containsPath(Path.of("/symly/repo3", "no/luck")))
                .isFalse();
    }

    @Test
    void allDirectoriesNames_shouldReturn_allDirectoriesNames_inAllRepositories() {
        // given
        Repositories repositories = Repositories.of(List.of(
                repo("/symly/repo1", List.of("dir", "dir/nested"), List.of("file", "dir/file")),
                repo(
                        "/symly/repo2",
                        List.of("dir", "other-dir/nested"),
                        List.of("otherfile", "dir/otherfile", "other-dir/nested"))));
        // when/then
        assertThat(repositories.allDirectoriesNames())
                .containsExactly(Path.of("dir"), Path.of("dir/nested"), Path.of("other-dir/nested"));
    }

    @Test
    void emptyRepositories_shouldBeEmpty() {
        // given
        Repositories repositories = Repositories.of(List.of(repo("/symly/repo1", List.of(), List.of())));
        // when/then
        assertThat(repositories.links(MAIN_DIR)).isEmpty();
        assertThat(repositories.allDirectoriesNames()).isEmpty();
    }

    @Test
    void links_shouldReturn_fileLinks() {
        // given
        Repositories repositories = Repositories.of(List.of(
                repo("/symly/repo1", List.of("dir", "dir/nested"), List.of("file", "dir/file")),
                repo(
                        "/symly/repo2",
                        List.of("dir", "other-dir/nested"),
                        List.of("other-file", "dir/file", "other-dir/nested-file"))));
        // when/then
        assertThat(repositories.links(MAIN_DIR))
                .containsExactly(
                        link("/symly/main/dir/file", "/symly/repo1/dir/file"),
                        link("/symly/main/file", "/symly/repo1/file"),
                        link("/symly/main/other-dir/nested-file", "/symly/repo2/other-dir/nested-file"),
                        link("/symly/main/other-file", "/symly/repo2/other-file"));
    }

    @Test
    void links_shouldReturn_directoryLinks() {
        // given
        Repositories repositories = Repositories.of(List.of(
                repo(
                        "/symly/repo1",
                        List.of("dir", "dir/nested", "dir/nested2"),
                        List.of("foo", "dir/file", "dir/nested/.symlink", "dir/nested/foo", "dir/nested2/foo")),
                repo("/symly/repo2", List.of("dir/nested"), List.of("bar", "dir/nested/foo", "dir/nested/bar"))));
        // when/then
        assertThat(repositories.links(MAIN_DIR))
                .contains(
                        link("/symly/main/bar", "/symly/repo2/bar"),
                        link("/symly/main/foo", "/symly/repo1/foo"),
                        link("/symly/main/dir/file", "/symly/repo1/dir/file"),
                        link("/symly/main/dir/nested", "/symly/repo1/dir/nested"),
                        link("/symly/main/dir/nested2/foo", "/symly/repo1/dir/nested2/foo"));
    }

    private Link link(String source, String target) {
        return Link.of(Path.of(source), Path.of(target));
    }

    private Repository repo(String path, Collection<String> dirs, Collection<String> files) {
        return new TestRepository(path, dirs, files) {};
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
                    entries(directories, RepositoryEntry.Type.DIRECTORY), entries(files, RepositoryEntry.Type.FILE));
        }

        private Stream<RepositoryEntry> entries(Collection<String> paths, RepositoryEntry.Type type) {
            return paths.stream().map(Path::of).map(path -> RepositoryEntry.of(path, resolve(path), type));
        }
    }
}
