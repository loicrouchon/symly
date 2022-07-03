package org.symly.cli;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree.Diff;

@SuppressWarnings({
    "java:S100", // Method names should comply with a naming convention (test method names)
    "java:S1192", // String literals should not be duplicated
})
class LinkCommandTest extends IntegrationTest {

    private final LinkCommandMessageFactory msg = new LinkCommandMessageFactory(env);

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        // given
        given(env);
        // when/then
        whenRunningCommand("link")
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
        whenRunningCommand("link", "-v", "--repositories", "to/dir", "/home/user/some/file")
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
        whenRunningCommand("link", "-v", "--repositories", "to/dir", "/home/user/some/file")
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
        whenRunningCommand("link", "-v", "--repositories", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.creatingLinks(env.home().toString(), List.of("to/dir")))
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
        whenRunningCommand("link", "-v", "--dir", "main/dir", "--repositories", "to/dir", "to/other-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.creatingLinks("main/dir", List.of("to/dir", "to/other-dir")))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldLinkFile_whenTargetFileDoesNotExist() {
        // given
        given(env)
                .withLayout(
                        """
            F home/user/to/dir/file
            F home/user/to/dir/nested/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionCreate("home/user/nested/file", "home/user/to/dir/nested/file"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                +L home/user/file -> home/user/to/dir/file
                +L home/user/nested/file -> home/user/to/dir/nested/file
                """));
    }

    @Test
    void shouldLinkLink_whenTargetLinkDoesNotExist() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/to/dir/link -> opt/file
            L home/user/to/dir/nested/link -> opt/file
            F opt/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/link", "home/user/to/dir/link"))
                .withMessage(msg.linkActionCreate("home/user/nested/link", "home/user/to/dir/nested/link"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                +L home/user/link -> home/user/to/dir/link
                +L home/user/nested/link -> home/user/to/dir/nested/link
                """));
    }

    @Test
    void shouldNotLinkDirectory_whenDirectorySymlinkDoesNotExist() {
        // given
        given(env).withLayout("D home/user/to/dir/sub/dir");
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldLinkDirectory_whenDirectorySymlinkExists() {
        // given
        given(env).withLayout("F home/user/to/dir/sub/dir/.symlink");
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/sub/dir", "home/user/to/dir/sub/dir"))
                .withFileTreeDiff(Diff.ofChanges("+L home/user/sub/dir -> home/user/to/dir/sub/dir"));
    }

    @Test
    void shouldNotLinkFile_whenTargetIsAnExistingFile() {
        // given
        given(env).withLayout("""
            F home/user/file
            F home/user/to/dir/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "home/user/to/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLinkError("home/user/file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionConflict("home/user/file", "home/user/to/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldNotLinkFile_whenTargetIsAnExistingDirectory() {
        // given
        given(env).withLayout("""
            D home/user/file
            F home/user/to/dir/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "home/user/to/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLinkError("home/user/file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionConflict("home/user/file", "home/user/to/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldUpdateLink_whenTargetIsAnExistingLink() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/file -> home/user/other-file
            F home/user/to/dir/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(msg.linkActionUpdate("home/user/file", "home/user/to/dir/file", "home/user/other-file"))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                +L home/user/file -> home/user/to/dir/file
                -L home/user/file -> home/user/other-file
                """));
    }

    @Test
    void shouldNotUpdateLinkFile_whenLinkIsAlreadyUpToDate() {
        // given
        given(env)
                .withLayout(
                        """
            L home/user/file -> home/user/to/dir/file
            F home/user/to/dir/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionUpToDate("home/user/file", "home/user/to/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldReplaceFile_whenTarget_isAnExistingFile_andForceOption_isPassed() {
        // given
        given(env).withLayout("""
            F home/user/file
            F home/user/to/dir/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--force", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(List.of(
                        msg.linkActionDelete("home/user/file"),
                        msg.linkActionCreate("home/user/file", "home/user/to/dir/file")))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                -F home/user/file
                +L home/user/file -> home/user/to/dir/file
                """));
    }

    @Test
    void shouldReplaceFile_whenTarget_isAnExistingDirectory_andForceOption_isPassed() {
        // given
        given(env)
                .withLayout(
                        """
            D home/user/file
            F home/user/file/dir/this-is-a-file
            F home/user/file/parent-is-a-dir
            F home/user/to/dir/file
            """);
        // when/then
        whenRunningCommand("link", "-v", "--force", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(List.of(
                        msg.linkActionDelete("home/user/file/parent-is-a-dir"),
                        msg.linkActionDelete("home/user/file/dir/this-is-a-file"),
                        msg.linkActionDelete("home/user/file"),
                        msg.linkActionCreate("home/user/file", "home/user/to/dir/file")))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                -F home/user/file/dir/this-is-a-file
                -F home/user/file/parent-is-a-dir
                +L home/user/file -> home/user/to/dir/file
                """));
    }
}
