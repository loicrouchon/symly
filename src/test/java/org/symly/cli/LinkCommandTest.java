package org.symly.cli;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree.Diff;

@SuppressWarnings({
    // Method names should comply with a naming convention (test method names)
    "java:S100",
    // String literals should not be duplicated
    "java:S1192",
    // Tests should include assertions: assertions are actually performed by the `.thenItShould()....` chain
    "java:S2699"
})
public class LinkCommandTest extends IntegrationTest {

    private final LinkCommandMessageFactory msg = new LinkCommandMessageFactory(env);
    private final ContextInputMessageFactory ctxMsg = new ContextInputMessageFactory(env);

    @Test
    void shouldFail_whenMainDirectoryIsNotDefined() {
        // given
        given(env);
        // when/then
        whenRunningCommand("link")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(ctxMsg.mainDirectoryIsNotDefined())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenRepositoriesAreNotDefined() {
        // given
        given(env);
        // when/then
        whenRunningCommand("link", "--dir", "~")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(ctxMsg.repositoriesAreNotDefined())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenMainDirectoryDoesNotExist() {
        // given
        given(env).withHome("home/doesnotexist");
        // when/then
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(ctxMsg.mainDirectoryDoesNotExist(env.home().toString()))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenRepositoryDoesNotExist() {
        // given
        given(env);
        // when/then
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(ctxMsg.repositoryDoesNotExist("to/dir"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        // given
        given(env).withLayout("D to/dir");
        // when/then
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "to/dir")
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
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionCreate("nested/file", "home/user/to/dir/nested/file"))
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
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("link", "home/user/to/dir/link"))
                .withMessage(msg.linkActionCreate("nested/link", "home/user/to/dir/nested/link"))
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
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldLinkDirectory_whenDirectorySymlinkExists() {
        // given
        given(env).withLayout("F home/user/to/dir/sub/dir/.symlink");
        // when/then
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("sub/dir", "home/user/to/dir/sub/dir"))
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
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "home/user/to/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLinkError("file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionConflict("file", "home/user/to/dir/file"))
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
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "home/user/to/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLinkError("file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionConflict("file", "home/user/to/dir/file"))
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
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(msg.linkActionUpdate("file", "home/user/to/dir/file", "home/user/other-file"))
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
        whenRunningCommand("link", "-v", "--dir", "~", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionUpToDate("file", "home/user/to/dir/file"))
                .withMessage(msg.everythingUpToDate())
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
        whenRunningCommand("link", "-v", "--dir", "~", "--force", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(
                        List.of(msg.linkActionDelete("file"), msg.linkActionCreate("file", "home/user/to/dir/file")))
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
        whenRunningCommand("link", "-v", "--dir", "~", "--force", "--repositories", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(List.of(
                        msg.linkActionDelete("file/parent-is-a-dir"),
                        msg.linkActionDelete("file/dir/this-is-a-file"),
                        msg.linkActionDelete("file"),
                        msg.linkActionCreate("file", "home/user/to/dir/file")))
                .withFileTreeDiff(
                        Diff.ofChanges(
                                """
                -F home/user/file/dir/this-is-a-file
                -F home/user/file/parent-is-a-dir
                +L home/user/file -> home/user/to/dir/file
                """));
    }

    @Test
    void shouldPrintActions_butNotModifyFileSystem_whenDryRunisEnabled() {
        // given
        given(env).withLayout("""
    L home/user/file -> home/user/repo/file
    F home/user/repo/dir/file
    """);
        // when/then
        whenRunningCommand("link", "-v", "--dry-run", "--dir", "~", "--force", "--repositories", "home/user/repo")
                .thenItShould()
                .succeed()
                .withMessages(List.of(
                        msg.linkActionCreate("dir/file", "home/user/repo/dir/file"),
                        msg.linkActionDelete("file", "home/user/repo/file")))
                .withFileTreeDiff(Diff.empty());
    }
}
