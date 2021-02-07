package org.linky.files;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FileTreeTest extends TemporaryFolderTest {

    @Test
    void fromPath_shouldContainFileEntry_whenFileExist() {
        //given
        createFiles("hello", "world", "my/name/is");
        //when
        FileTree tree = rootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello",
                "my/name/is",
                "world"
        );
    }

    @Test
    void fromPath_shouldContainLinkEntry_whenTargetPathIsAnExistingFile() {
        //given
        createSymbolicLink("hello", "world");
        createFiles("world");
        //when
        FileTree tree = rootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> world",
                "world"
        );
    }

    @Test
    void fromPath_shouldContainLinkEntry_whenTargetPathIsAnExistingDirectory() {
        //given
        createSymbolicLink("hello", "some/dir");
        createDirectories("some/dir");
        //when
        FileTree tree = rootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> some/dir"
        );
    }

    @Test
    void fromPath_shouldContainLinkEntry_whenTargetPathDoesNotExist() {
        //given
        createSymbolicLink("hello", "anyone");
        //when
        FileTree tree = rootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> anyone"
        );
    }

    @Test
    void fromPath_shouldNotContainDirectoryEntries() {
        //given
        createSymbolicLink("hello1", "world");
        createSymbolicLink("hello2", "some/dir");
        createSymbolicLink("hello3", "some/dir/other/dir");
        createFiles("world");
        createDirectories("some/dir/other/dir", "another/dir");
        //when
        FileTree tree = rootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello1 -> world",
                "hello2 -> some/dir",
                "hello3 -> some/dir/other/dir",
                "world"
        );
    }

    @Test
    void fromPath_shouldReferenceLinksOutsideItself() {
        //given
        createFiles("real-hello", "world", "my/name/is", "tree/toto");
        createSymbolicLink("tree/hello", "real-hello");
        //when
        FileTree tree = fileTree("tree");
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> ../real-hello",
                "toto"
        );
    }

    @Test
    void create_shouldCreate_filesInTheTree() {
        //given
        FileTree initial = FileTree.of(List.of(
                "hello/world",
                "how/are/you -> doing/today"
        ));
        //when
        initial.create(workingDir);
        //then
        assertThat(rootFileTree().getLayout()).containsExactly(
                "hello/world",
                "how/are/you -> doing/today"
        );
    }
}
