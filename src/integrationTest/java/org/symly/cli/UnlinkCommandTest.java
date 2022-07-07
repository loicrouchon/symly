package org.symly.cli;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree.Diff;

@SuppressWarnings({
    "java:S100", // Method names should comply with a naming convention (test method names)
    "java:S1192", // String literals should not be duplicated
})
class UnlinkCommandTest extends IntegrationTest {

    private final UnlinkCommandMessageFactory msg = new UnlinkCommandMessageFactory(env);

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        // given
        given(env);
        // when/then
        whenRunningCommand("unlink")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.missingTargetDirectories())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenMainDirectoryDoesNotExist() {
        // given
        given(env).withHome("home/doesnotexist");
        // when/then
        whenRunningCommand("unlink", "--repositories", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.mainDirectoryDoesNotExist(env.home().toString()))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenTargetDirectoryDoesNotExist() {
        // given
        given(env);
        // when/then
        whenRunningCommand("unlink", "--repositories", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.targetDirectoryDoesNotExist("to/dir"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        // given
        given(env).withLayout("D to/dir");
        // when/then
        whenRunningCommand("unlink", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.unlink(env.home().toString(), List.of("to/dir")))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        // given
        given(env)
                .withLayout("""
            D main/dir
            D to/dir
            D to/other-dir
            """);
        // when/then
        whenRunningCommand("unlink", "--dir", "main/dir", "--repositories", "to/dir", "to/other-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.unlink("main/dir", List.of("to/dir", "to/other-dir")))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldIgnoreSimpleFiles() {
        // given
        given(env)
                .withLayout(
                        """
            F home/user/file
            F home/user/other-file
            F home/user/nested/file
            F to-dir/file
            F to-dir/nested/file
            """);
        // when/then
        whenRunningCommand("unlink", "--repositories", "to-dir")
                .thenItShould()
                .succeed()
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldIgnoreLinks_whenLinkTargetNotInRepository() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/file -> non-repo/file
            L home/user/nested/file -> non-repo/nested/file
            F non-repo/file
            F non-repo/nested/file
            D to-dir
            """);
        // when/then
        whenRunningCommand("unlink", "--repositories", "to-dir")
                .thenItShould()
                .succeed()
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldUnlink_whenLinkTarget_pointsToRepository_andIsAnExistingFile() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/file -> to-dir/file
            L home/user/nested/file -> to-dir/nested/file
            F to-dir/file
            F to-dir/nested/file
            """);
        // when/then
        whenRunningCommand("unlink", "--repositories", "to-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.actionUnlink("file", "to-dir/file"))
                .withMessage(msg.actionUnlink("nested/file", "to-dir/nested/file"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                -L home/user/file -> to-dir/file
                -L home/user/nested/file -> to-dir/nested/file
                """));
    }

    @Test
    void shouldUnlink_whenLinkTarget_pointsToRepository_andIsAnExistingDirectory() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/some-dir -> to-dir/some-dir
            L home/user/nested/some-dir -> to-dir/nested/some-dir
            D to-dir/some-dir
            D to-dir/nested/some-dir
            """);
        // when/then
        whenRunningCommand("unlink", "--repositories", "to-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.actionUnlink("some-dir", "to-dir/some-dir"))
                .withMessage(msg.actionUnlink("nested/some-dir", "to-dir/nested/some-dir"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                -L home/user/some-dir -> to-dir/some-dir
                -L home/user/nested/some-dir -> to-dir/nested/some-dir
                """));
    }

    @Test
    void shouldUnlink_whenLinkTarget_pointsToRepository_evenIfTargetDoesNotExist() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/some/file -> to-dir/some/file
            D to-dir
            """);
        // when/then
        whenRunningCommand("unlink", "--repositories", "to-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.actionUnlink("some/file", "to-dir/some/file"))
                .withFileTreeDiff(Diff.ofChanges(
                        """
                -L home/user/some/file -> to-dir/some/file
                """));
    }

    @Test
    void shouldNotUnlink_whenLinkTarget_pointsToRepository_butNestingLevelIsHigherThanMaxDepthLookup() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/some/nested/file -> to-dir/some/nested/file
            D to-dir
            """);
        // when/then
        whenRunningCommand("unlink", "--repositories", "to-dir")
                .thenItShould()
                .succeed()
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldUnlink_whenLinkTarget_pointsToRepository_withExtendedMaxDepthLookup() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/some/nested/file -> to-dir/some/nested/file
            D to-dir
            """);
        // when/then
        whenRunningCommand("unlink", "--repositories", "to-dir", "--max-depth", "3")
                .thenItShould()
                .succeed()
                .withMessage(msg.actionUnlink("some/nested/file", "to-dir/some/nested/file"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                -L home/user/some/nested/file -> to-dir/some/nested/file
                """));
    }
}
