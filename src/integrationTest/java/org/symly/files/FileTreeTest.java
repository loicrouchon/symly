package org.symly.files;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;

@SuppressWarnings({
        "java:S100",  // Method names should comply with a naming convention (test method names)
        "java:S1192", // String literals should not be duplicated
        "java:S5960", // Assertions should not be used in production code (This is test code)
})
class FileTreeTest extends IntegrationTest {

    @Test
    void fromPath_shouldContainFileEntry_whenFileExist() {
        //given/when
        FileTree tree = given(env).withLayout("""
                        F hello
                        F my/name/is
                        F world
                        """)
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
        FileTree tree = given(env).withLayout("""
                        L hello -> world
                        F world
                        """)
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
        FileTree tree = given(env).withLayout("""
                        L hello -> some/dir
                        D some/dir
                        """)
                .getRootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> some/dir"
        );
    }

    @Test
    void fromPath_shouldContainLinkEntry_whenTargetPathDoesNotExist() {
        //given/when
        FileTree tree = given(env).withLayout("""
                        L hello -> anyone
                        """)
                .getRootFileTree();
        //then
        assertThat(tree.getLayout()).containsExactly(
                "hello -> anyone"
        );
    }

    @Test
    void fromPath_shouldNotContainDirectoryEntries() {
        //given/when
        FileTree tree = given(env).withLayout("""
                        D another/dir
                        L hello1 -> world
                        L hello2 -> some/dir
                        L hello3 -> some/dir/other/dir
                        D some/dir/other/dir
                        F world
                        """)
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
        FileTree tree = given(env).withLayout("""
                        F my/name/is
                        F real-hello
                        L tree/hello -> real-hello
                        F tree/toto
                        F world
                        """)
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
