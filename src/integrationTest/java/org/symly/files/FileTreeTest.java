package org.symly.files;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;

class FileTreeTest extends IntegrationTest {

    @Test
    void fromPath_shouldContainFileEntry_whenFileExist() {
        //given/when
        FileTree tree = given(env)
                .withFiles("hello", "world", "my/name/is")
                .getRootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello",
                "my/name/is",
                "world"
        );
    }

    @Test
    void fromPath_shouldContainLinkEntry_whenTargetPathIsAnExistingFile() {
        //given/when
        FileTree tree = given(env)
                .withSymbolicLink("hello", "world")
                .withFiles("world")
                .getRootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> world",
                "world"
        );
    }

    @Test
    void fromPath_shouldContainLinkEntry_whenTargetPathIsAnExistingDirectory() {
        //given/when
        FileTree tree = given(env)
                .withSymbolicLink("hello", "some/dir")
                .withDirectories("some/dir")
                .getRootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> some/dir"
        );
    }

    @Test
    void fromPath_shouldContainLinkEntry_whenTargetPathDoesNotExist() {
        //given/when
        FileTree tree = given(env)
                .withSymbolicLink("hello", "anyone")
                .getRootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> anyone"
        );
    }

    @Test
    void fromPath_shouldNotContainDirectoryEntries() {
        //given/when
        FileTree tree = given(env)
                .withSymbolicLink("hello1", "world")
                .withSymbolicLink("hello2", "some/dir")
                .withSymbolicLink("hello3", "some/dir/other/dir")
                .withFiles("world")
                .withDirectories("some/dir/other/dir", "another/dir")
                .getRootFileTree();
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
        //given/when
        FileTree tree = given(env)
                .withFiles("real-hello", "world", "my/name/is", "tree/toto")
                .withSymbolicLink("tree/hello", "real-hello")
                .getFileTree("tree");
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
        FileTree tree = given(env)
                .create(initial)
                .getRootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello/world",
                "how/are/you -> doing/today"
        );
    }
}
