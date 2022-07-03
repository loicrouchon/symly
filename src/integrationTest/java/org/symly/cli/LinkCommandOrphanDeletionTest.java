package org.symly.cli;

import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree.Diff;

@SuppressWarnings({
    "java:S100", // Method names should comply with a naming convention (test method names)
    "java:S1192", // String literals should not be duplicated
})
class LinkCommandOrphanDeletionTest extends IntegrationTest {

    private final LinkCommandMessageFactory msg = new LinkCommandMessageFactory(env);

    @Test
    void shouldFail_whenOrphanLookupMaxDepth_isInvalid() {
        // given
        given(env).withLayout("D to/dir");
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir", "--max-depth", "-1")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.maxDepthMustBePositive(-1))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldNotDelete_linksNotBeingOrphans() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/file -> to/dir/file
            D to/dir
            F to/dir/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withoutMessage(msg.linkActionDelete("home/user/file", "to/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldIgnoreOrphans_notBelonging_toARepository() {
        // given
        given(env)
                .withLayout("""
            L home/user/file -> to/otherdir/file
            D to/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withoutMessage(msg.linkActionDelete("home/user/file", "to/otherdir/file"))
                .withMessage(msg.everythingUpToDate())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundAtRoot_level0() {
        // given
        given(env).withLayout("""
            L home/user/file -> to/dir/file
            D to/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/file", "to/dir/file"))
                .withFileTreeDiff(Diff.ofChanges("-L home/user/file -> to/dir/file"));
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundAtRoot_level1() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/level1/file -> to/dir/level1/file
            D to/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/level1/file", "to/dir/level1/file"))
                .withFileTreeDiff(Diff.ofChanges("-L home/user/level1/file -> to/dir/level1/file"));
    }

    @Test
    void shouldNotDeleteOrphan_whenOrphanFile_isFoundAtRoot_level2() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/level1/level2/file -> to/dir/level1/level2/file
            D to/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withoutMessage(msg.linkActionDelete("home/user/level1/level2/file", "to/dir/level1/level2/file"))
                .withMessage(msg.everythingUpToDate())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundAtRoot_level2_withIncreased_lookupMaxDepth() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/level1/level2/file -> to/dir/level1/level2/file
            D to/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir", "--max-depth", "3")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/level1/level2/file", "to/dir/level1/level2/file"))
                .withFileTreeDiff(Diff.ofChanges("-L home/user/level1/level2/file -> to/dir/level1/level2/file"));
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundInSubDirectory_level0() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/sub/dir/file -> to/dir/sub/dir/file
            D to/dir/sub/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/sub/dir/file", "to/dir/sub/dir/file"))
                .withFileTreeDiff(Diff.ofChanges("-L home/user/sub/dir/file -> to/dir/sub/dir/file"));
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundInSubDirectory_level1() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/sub/dir/level1/file -> to/dir/sub/dir/level1/file
            D to/dir/sub/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete("home/user/sub/dir/level1/file", "to/dir/sub/dir/level1/file"))
                .withFileTreeDiff(Diff.ofChanges("-L home/user/sub/dir/level1/file -> to/dir/sub/dir/level1/file"));
    }

    @Test
    void shouldNotDeleteOrphan_whenOrphanFile_isFoundInSubDirectory_level2() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/sub/dir/level1/level2/file -> to/dir/sub/dir/level1/level2/file
            D to/dir/sub/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withoutMessage(msg.linkActionDelete(
                        "home/user/sub/dir/level1/level2/file", "to/dir/sub/dir/level1/level2/file"))
                .withMessage(msg.everythingUpToDate())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_isFoundInSubDirectory_level2_withIncreased_lookupMaxDepth() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/sub/dir/level1/level2/file -> to/dir/sub/dir/level1/level2/file
            D to/dir/sub/dir
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to/dir", "--max-depth", "3")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionDelete(
                        "home/user/sub/dir/level1/level2/file", "to/dir/sub/dir/level1/level2/file"))
                .withFileTreeDiff(
                        Diff.ofChanges("-L home/user/sub/dir/level1/level2/file -> to/dir/sub/dir/level1/level2/file"));
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_parentDirectoryIsASymlink() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/dir -> outside
            D outside
            L outside/existingfile -> to-dir/dir/existingfile
            L outside/nonexistingfile -> to-dir/dir/nonexistingfile
            D to-dir
            F to-dir/dir/existingfile
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to-dir", "--max-depth", "1")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionUpToDate("home/user/dir/existingfile", "to-dir/dir/existingfile"))
                .withMessage(msg.linkActionDelete("home/user/dir/nonexistingfile", "to-dir/dir/nonexistingfile"))
                .withFileTreeDiff(Diff.ofChanges("-L outside/nonexistingfile -> to-dir/dir/nonexistingfile"));
    }

    @Test
    void shouldDeleteOrphan_whenOrphanFile_oneParentDirectoryInHierarchyIsASymlink() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/sub -> outside
            D outside
            L outside/dir/existingfile    -> to-dir/sub/dir/existingfile
            L outside/dir/nonexistingfile -> to-dir/sub/dir/nonexistingfile
            D to-dir
            F to-dir/sub/dir/existingfile
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "to-dir", "--max-depth", "1")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionUpToDate("home/user/sub/dir/existingfile", "to-dir/sub/dir/existingfile"))
                .withMessage(
                        msg.linkActionDelete("home/user/sub/dir/nonexistingfile", "to-dir/sub/dir/nonexistingfile"))
                .withFileTreeDiff(Diff.ofChanges("-L outside/dir/nonexistingfile -> to-dir/sub/dir/nonexistingfile"));
    }
}
