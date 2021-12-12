package org.symly.cli;

import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree.Diff;

@SuppressWarnings({
        "java:S100",  // Method names should comply with a naming convention (test method names)
        "java:S1192", // String literals should not be duplicated
})
class LinkCommandOrphanDeletionTest extends IntegrationTest {

    private final LinkCommandMessageFactory msg = new LinkCommandMessageFactory(env);

    @Test
    void shouldFail_whenOrphanLookupMaxDepth_isInvalid() {
        //given
        given(env)
                .withDirectories("to/dir");
        //when/then
        whenRunningCommand("link", "--to", "to/dir", "--max-depth", "-1")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.maxDepthMustBePositive(-1))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldNotDelete_linksNotBeingOrphans() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withFiles("to/dir/file")
                .withSymbolicLink("home/user/file", "to/dir/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withoutMessage(msg.linkActionDelete("home/user/file", "to/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldIgnoreOrphans_notBelonging_toARepository() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withSymbolicLink("home/user/file", "to/otherdir/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withoutMessage(msg.linkActionDelete("home/user/file", "to/otherdir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundAtRoot_level0() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withSymbolicLink("home/user/file", "to/dir/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/file", "to/dir/file"))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("home/user/file -> to/dir/file")
                );
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundAtRoot_level1() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withSymbolicLink("home/user/level1/file", "to/dir/level1/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/level1/file", "to/dir/level1/file"))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("home/user/level1/file -> to/dir/level1/file")
                );
    }

    @Test
    void shouldNotDeleteOrphan_whenOrphanFile_isFoundAtRoot_level2() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withSymbolicLink("home/user/level1/level2/file", "to/dir/level1/level2/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withoutMessage(msg.linkActionDelete("home/user/level1/level2/file", "to/dir/level1/level2/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundAtRoot_level2_withIncreased_lookupMaxDepth() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withSymbolicLink("home/user/level1/level2/file", "to/dir/level1/level2/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir", "--max-depth", "3")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/level1/level2/file", "to/dir/level1/level2/file"))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("home/user/level1/level2/file -> to/dir/level1/level2/file")
                );
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundInSubDirectory_level0() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withDirectories("to/dir/sub/dir")
                .withSymbolicLink("home/user/sub/dir/file", "to/dir/sub/dir/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/sub/dir/file", "to/dir/sub/dir/file"))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("home/user/sub/dir/file -> to/dir/sub/dir/file")
                );
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundInSubDirectory_level1() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withDirectories("to/dir/sub/dir")
                .withSymbolicLink("home/user/sub/dir/level1/file", "to/dir/sub/dir/level1/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/sub/dir/level1/file", "to/dir/sub/dir/level1/file"))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("home/user/sub/dir/level1/file -> to/dir/sub/dir/level1/file")
                );
    }

    @Test
    void shouldNotDeleteOrphan_whenOrphanFile_isFoundInSubDirectory_level2() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withDirectories("to/dir/sub/dir")
                .withSymbolicLink("home/user/sub/dir/level1/level2/file", "to/dir/sub/dir/level1/level2/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withoutMessage(
                        msg.linkActionDelete(
                                "home/user/sub/dir/level1/level2/file", "to/dir/sub/dir/level1/level2/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundInSubDirectory_level2_withIncreased_lookupMaxDepth() {
        //given
        given(env)
                .withDirectories("to/dir")
                .withDirectories("to/dir/sub/dir")
                .withSymbolicLink("home/user/sub/dir/level1/level2/file", "to/dir/sub/dir/level1/level2/file");
        //when/then
        whenRunningCommand("link", "--to", "to/dir", "--max-depth", "3")
                .thenItShould()
                .succeed()
                .withMessage(
                        msg.linkActionDelete(
                                "home/user/sub/dir/level1/level2/file", "to/dir/sub/dir/level1/level2/file"))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("home/user/sub/dir/level1/level2/file -> to/dir/sub/dir/level1/level2/file")
                );
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_parentDirectoryIsASymlink() {
        //given
        given(env)
                .withDirectories("to-dir")
                .withDirectories("outside")
                .withFiles("to-dir/dir/existingfile")
                .withSymbolicLink("home/user/dir", "outside")
                .withSymbolicLink("outside/existingfile", "to-dir/dir/existingfile")
                .withSymbolicLink("outside/nonexistingfile", "to-dir/dir/nonexistingfile");
        //when/then
        whenRunningCommand("link", "--to", "to-dir", "--max-depth", "1")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionUpToDate("home/user/dir/existingfile", "to-dir/dir/existingfile"))
                .withMessage(msg.linkActionDelete("home/user/dir/nonexistingfile", "to-dir/dir/nonexistingfile"))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("outside/nonexistingfile -> to-dir/dir/nonexistingfile")
                );
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_oneParentDirectoryInHierarchyIsASymlink() {
        //given
        given(env)
                .withDirectories("to-dir")
                .withDirectories("outside")
                .withFiles("to-dir/sub/dir/existingfile")
                .withSymbolicLink("home/user/sub", "outside")
                .withSymbolicLink("outside/dir/existingfile", "to-dir/sub/dir/existingfile")
                .withSymbolicLink("outside/dir/nonexistingfile", "to-dir/sub/dir/nonexistingfile");
        //when/then
        whenRunningCommand("link", "--to", "to-dir", "--max-depth", "1")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionUpToDate("home/user/sub/dir/existingfile", "to-dir/sub/dir/existingfile"))
                .withMessage(
                        msg.linkActionDelete("home/user/sub/dir/nonexistingfile", "to-dir/sub/dir/nonexistingfile"))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("outside/dir/nonexistingfile -> to-dir/sub/dir/nonexistingfile")
                );
    }
}