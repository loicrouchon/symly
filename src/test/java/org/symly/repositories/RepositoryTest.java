package org.symly.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.symly.files.IoMock;

class RepositoryTest {

    private final IoMock ioMock = new IoMock();

    @Test
    void entries_shouldReturnNoEntries_whenRepositoryIsEmpty() {
        // given
        Path repo = Path.of("repo");
        ioMock.directory(repo);
        // when
        Stream<RepositoryEntry> entries = Repository.of(repo).entries(ioMock.buildFileSystemReader());
        // then
        assertThat(entries).hasSize(1).containsExactlyInAnyOrder(dir(repo, ""));
    }

    @Test
    void entries_shouldReturnDirectoryEntries() {
        // given
        Path repo = Path.of("repo");
        ioMock.directory(repo);
        ioMock.directory(repo.resolve("some/dir"));
        // when
        Stream<RepositoryEntry> entries = Repository.of(repo).entries(ioMock.buildFileSystemReader());
        // then
        assertThat(entries)
                .hasSize(3)
                .containsExactlyInAnyOrder(dir(repo, ""), dir(repo, "some"), dir(repo, "some/dir"));
    }

    @Test
    void entries_shouldReturnFileEntries() {
        // given
        Path repo = Path.of("repo");
        ioMock.directory(repo);
        ioMock.file(repo.resolve("some/file"));
        // when
        Stream<RepositoryEntry> entries = Repository.of(repo).entries(ioMock.buildFileSystemReader());
        // then
        assertThat(entries)
                .hasSize(3)
                .containsExactlyInAnyOrder(dir(repo, ""), dir(repo, "some"), file(repo, "some/file"));
    }

    @Test
    void entries_shouldIgnoreFiles_whenDefinedInRootLevelIgnoreList() {
        // given
        Path repo = Path.of("repo");
        ioMock.directory(repo);
        ioMock.file(repo.resolve(".symlyignore"), "file");
        ioMock.file(repo.resolve("some/file"));
        // when
        Stream<RepositoryEntry> entries = Repository.of(repo).entries(ioMock.buildFileSystemReader());
        // then
        assertThat(entries).hasSize(2).containsExactlyInAnyOrder(dir(repo, ""), dir(repo, "some"));
    }

    @Test
    void entries_shouldIgnoreFiles_whenDefinedInSubDirIgnoreList() {
        // given
        Path repo = Path.of("repo");
        ioMock.directory(repo);
        ioMock.file(repo.resolve("some/.symlyignore"), "file");
        ioMock.file(repo.resolve("some/file"));
        // when
        Stream<RepositoryEntry> entries = Repository.of(repo).entries(ioMock.buildFileSystemReader());
        // then
        assertThat(entries).hasSize(2).containsExactlyInAnyOrder(dir(repo, ""), dir(repo, "some"));
    }

    @Test
    void entries_shouldNotIgnoreFiles_whenDefinedInOtherSubDirIgnoreList() {
        // given
        Path repo = Path.of("repo");
        ioMock.directory(repo);
        ioMock.file(repo.resolve("other/.symlyignore"), "file");
        ioMock.file(repo.resolve("some/file"));
        // when
        Stream<RepositoryEntry> entries = Repository.of(repo).entries(ioMock.buildFileSystemReader());
        // then
        assertThat(entries)
                .hasSize(4)
                .containsExactlyInAnyOrder(
                        dir(repo, ""), dir(repo, "some"), dir(repo, "other"), file(repo, "some/file"));
    }

    @Test
    void entries_shouldIgnoreFiles_whenDefinedWithExtensionInIgnoreList() {
        // given
        Path repo = Path.of("repo");
        ioMock.directory(repo);
        ioMock.file(repo.resolve("some/.symlyignore"), "file.ext1");
        ioMock.file(repo.resolve("some/file.ext1"));
        ioMock.file(repo.resolve("some/file.ext2"));
        // when
        Stream<RepositoryEntry> entries = Repository.of(repo).entries(ioMock.buildFileSystemReader());
        // then
        assertThat(entries)
                .hasSize(3)
                .containsExactlyInAnyOrder(dir(repo, ""), dir(repo, "some"), file(repo, "some/file.ext2"));
    }

    @Test
    void entries_shouldIgnoreFiles_whenDefinedWithWildcardInIgnoreList() {
        // given
        Path repo = Path.of("repo");
        ioMock.directory(repo);
        ioMock.file(repo.resolve("some/.symlyignore"), "*.ext1");
        ioMock.file(repo.resolve("some/file1.ext1"));
        ioMock.file(repo.resolve("some/file2.ext1"));
        ioMock.file(repo.resolve("some/file3.ext2"));
        // when
        Stream<RepositoryEntry> entries = Repository.of(repo).entries(ioMock.buildFileSystemReader());
        // then
        assertThat(entries)
                .hasSize(3)
                .containsExactlyInAnyOrder(dir(repo, ""), dir(repo, "some"), file(repo, "some/file3.ext2"));
    }

    private RepositoryEntry file(Path repo, String name) {
        return entry(RepositoryEntry.Type.FILE, repo, name);
    }

    private RepositoryEntry dir(Path repo, String name) {
        return entry(RepositoryEntry.Type.DIRECTORY, repo, name);
    }

    private RepositoryEntry entry(RepositoryEntry.Type type, Path repo, String name) {
        return RepositoryEntry.of(Path.of(name), repo.resolve(name).toAbsolutePath(), type);
    }
}
