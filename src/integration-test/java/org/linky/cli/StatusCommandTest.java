package org.linky.cli;

import org.junit.jupiter.api.Test;
import org.linky.env.IntegrationTest;
import org.linky.files.FileTree;

@SuppressWarnings("java:S2699")
class StatusCommandTest extends IntegrationTest {

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        given(env);
        //when/then
        whenRunningCommand("status")
                .thenItShould()
                .fail()
                .withErrorMessage("Missing required option: '--sources=<sources>'")
                .withFileTreeDiff(FileTree.Diff.unchanged());
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        given(env)
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("status", "-s", "to/dir", "/home/user/some/file")
                .thenItShould()
                .fail()
                .withErrorMessage(
                        "Argument <destination> (%s): must be an existing directory",
                        home())
                .withFileTreeDiff(FileTree.Diff.unchanged());
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        given(env);
        //when/then
        whenRunningCommand("status", "-s", "to/dir", "/home/user/some/file")
                .thenItShould()
                .fail()
                .withErrorMessage(
                        "Argument <sources> (%s): must be an existing directory",
                        "to/dir")
                .withFileTreeDiff(FileTree.Diff.unchanged());
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        given(env)
                .withDirectories("from/dir");
        //when/then
        whenRunningCommand("status", "-s", "from/dir")
                .thenItShould()
                .succeed()
                .withMessage(
                        "Checking links status from [%s] to %s",
                        path("from/dir"),
                        home())
                .withFileTreeDiff(FileTree.Diff.unchanged());
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        given(env)
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when/then
        whenRunningCommand("status", "-s", "from/dir", "from/other-dir", "-d", "to/dir")
                .thenItShould()
                .succeed()
                .withMessage(
                        "Checking links status from [%s, %s] to %s",
                        path("from/dir"),
                        path("from/other-dir"),
                        path("to/dir"))
                .withFileTreeDiff(FileTree.Diff.unchanged());
    }
}