package org.symly.cli;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.env.IntegrationTest;
import org.symly.files.FileTree.Diff;

@SuppressWarnings("java:S2699")
class LinkCommandTest extends IntegrationTest {

    private final LinkCommandMessageFactory msg = new LinkCommandMessageFactory(env);

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        given(env);
        //when/then
        whenRunningCommand("link")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.missingTargetDirectories())
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenMainDirectoryDoesNotExist() {
        //given
        given(env)
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("link", "--to", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.mainDirectoryDoesNotExist(env.home().toString()))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldFail_whenTargetDirectoryDoesNotExist() {
        //given
        given(env);
        //when/then
        whenRunningCommand("link", "--to", "to/dir", "/home/user/some/file")
                .thenItShould()
                .failWithConfigurationError()
                .withErrorMessage(msg.targetDirectoryDoesNotExist("to/dir"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        given(env)
                .withDirectories("to/dir");
        //when/then
        whenRunningCommand("link", "--to", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.creatingLinks(env.home().toString(), List.of("to/dir")))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        given(env)
                .withDirectories("main/dir", "to/other-dir", "to/dir");
        //when/then
        whenRunningCommand("link", "--dir", "main/dir", "--to", "to/dir", "to/other-dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.creatingLinks("main/dir", List.of("to/dir", "to/other-dir")))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldLinkFile_whenTargetFileDoesNotExist() {
        //given
        given(env)
                .withFiles(
                        "home/user/to/dir/file",
                        "home/user/to/dir/nested/file"
                );
        //when/then
        whenRunningCommand("link", "--to", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionCreate("home/user/nested/file", "home/user/to/dir/nested/file"))
                .withFileTreeDiff(Diff.empty().withNewPaths(
                        "home/user/file -> home/user/to/dir/file",
                        "home/user/nested/file -> home/user/to/dir/nested/file"
                ));
    }

    @Test
    void shouldLinkLink_whenTargetLinkDoesNotExist() {
        //given
        given(env)
                .withFiles("opt/file")
                .withSymbolicLink("home/user/to/dir/link", "opt/file")
                .withSymbolicLink("home/user/to/dir/nested/link", "opt/file");
        //when/then
        whenRunningCommand("link", "--to", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/link", "home/user/to/dir/link"))
                .withMessage(msg.linkActionCreate("home/user/nested/link", "home/user/to/dir/nested/link"))
                .withFileTreeDiff(Diff.empty().withNewPaths(
                        "home/user/link -> home/user/to/dir/link",
                        "home/user/nested/link -> home/user/to/dir/nested/link"
                ));
    }

    @Test
    void shouldNotLinkDirectory_whenDirectorySymlinkDoesNotExist() {
        //given
        given(env)
                .withDirectories("home/user/to/dir/sub/dir");
        //when/then
        whenRunningCommand("link", "--to", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldLinkDirectory_whenDirectorySymlinkExists() {
        //given
        given(env)
                .withFiles("home/user/to/dir/sub/dir/.symlink");
        //when/then
        whenRunningCommand("link", "--to", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionCreate("home/user/sub/dir", "home/user/to/dir/sub/dir"))
                .withFileTreeDiff(Diff.empty().withNewPaths(
                        "home/user/sub/dir -> home/user/to/dir/sub/dir"
                ));
    }

    @Test
    void shouldNotLinkFile_whenTargetIsAnExistingFile() {
        //given
        given(env)
                .withFiles(
                        "home/user/file",
                        "home/user/to/dir/file"
                );
        //when/then
        whenRunningCommand("link", "--to", "home/user/to/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLinkError("home/user/file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionConflict("home/user/file", "home/user/to/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldNotLinkFile_whenTargetIsAnExistingDirectory() {
        //given
        given(env)
                .withDirectories("home/user/file")
                .withFiles("home/user/to/dir/file");
        //when/then
        whenRunningCommand("link", "--to", "home/user/to/dir")
                .thenItShould()
                .failWithError()
                .withErrorMessages(msg.cannotCreateLinkError("home/user/file", "home/user/to/dir/file"))
                .withMessage(msg.linkActionConflict("home/user/file", "home/user/to/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldUpdateLink_whenTargetIsAnExistingLink() {
        //given
        given(env)
                .withSymbolicLink("home/user/file", "home/user/other-file")
                .withFiles("home/user/to/dir/file");
        //when/then
        whenRunningCommand("link", "--to", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(msg.linkActionUpdate("home/user/file", "home/user/to/dir/file",
                        "home/user/other-file"))
                .withFileTreeDiff(Diff.empty()
                        .withNewPaths("home/user/file -> home/user/to/dir/file")
                        .withRemovedPaths("home/user/file -> home/user/other-file")
                );
    }

    @Test
    void shouldNotUpdateLinkFile_whenLinkIsAlreadyUpToDate() {
        //given
        given(env)
                .withSymbolicLink("home/user/file", "home/user/to/dir/file")
                .withFiles("home/user/to/dir/file");
        //when/then
        whenRunningCommand("link", "--to", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessage(msg.linkActionUpToDate("home/user/file", "home/user/to/dir/file"))
                .withFileTreeDiff(Diff.empty());
    }

    @Test
    void shouldReplaceFile_whenTarget_isAnExistingFile_andForceOption_isPassed() {
        //given
        given(env)
                .withFiles(
                        "home/user/file",
                        "home/user/to/dir/file"
                );
        //when/then
        whenRunningCommand("link", "--force", "--to", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(List.of(
                        msg.linkActionDelete("home/user/file"),
                        msg.linkActionCreate("home/user/file", "home/user/to/dir/file")
                ))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths("home/user/file")
                        .withNewPaths("home/user/file -> home/user/to/dir/file")
                );
    }

    @Test
    void shouldReplaceFile_whenTarget_isAnExistingDirectory_andForceOption_isPassed() {
        //given
        given(env)
                .withDirectories(
                        "home/user/file",
                        "home/user/file/dir"
                )
                .withFiles(
                        "home/user/file/parent-is-a-dir",
                        "home/user/file/dir/this-is-a-file",
                        "home/user/to/dir/file"
                );
        //when/then
        whenRunningCommand("link", "--force", "--to", "home/user/to/dir")
                .thenItShould()
                .succeed()
                .withMessages(List.of(
                        msg.linkActionDelete("home/user/file/parent-is-a-dir"),
                        msg.linkActionDelete("home/user/file/dir/this-is-a-file"),
                        msg.linkActionDelete("home/user/file"),
                        msg.linkActionCreate("home/user/file", "home/user/to/dir/file")
                ))
                .withFileTreeDiff(Diff.empty()
                        .withRemovedPaths(
                                "home/user/file/parent-is-a-dir",
                                "home/user/file/dir/this-is-a-file"
                        )
                        .withNewPaths("home/user/file -> home/user/to/dir/file")
                );
    }
}