package org.linky.cli;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.linky.env.IntegrationTest;

@SuppressWarnings("java:S2699")
class AddCommandTest extends IntegrationTest {

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        givenCleanEnv();
        //when/then
        whenRunningCommand("add")
                .thenItShould()
                .fail()
                .withErrorMessage("Missing required options and parameters: '--to=<to>', '<file>'")
                .andLayout()
                .isEmpty();
    }

    @Test
    @Disabled
    void shouldProvideCorrectDefaults() {
        //given
        givenCleanEnv()
                .withDirectories("to/dir")
                .withFiles("some/file");
        //when/then
        whenRunningCommand("add", "-t", "to/dir", "some/file")
                .thenItShould()
                .fail()
                .withErrorMessage("Missing required options and parameters: '--to=<to>', '<file>'");
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        givenCleanEnv()
                .withDirectories("from/dir", "to/dir")
                .withFiles("from/dir/some/file");
        //when/then
        whenRunningCommand("add", "-f", "from/dir", "-t", "to/dir", "from/dir/some/file")
                .thenItShould()
                .succeed()
                .withMessage(
                        "Moving some/file from %s to %s and creating link",
                        path("from/dir"),
                        path("to/dir"))
                .withMessage(
                        "[MOVED] from/dir/some/file -> %s",
                        path("to/dir/some/file"));
    }
    // TODO add tests where some/file does not exist
    // TODO add tests where some/file is not a subfile of from/dir

    @Test
    void shouldFail_whenDirectoryDoesNotExist() {
        //given
        givenCleanEnv()
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("add", "-t", "to/dir", "/home/user/some/file")
                .thenItShould()
                .fail()
                .withErrorMessage(
                        "Argument <from> (%s): must be an existing directory",
                        home())
                .andLayout()
                .isEmpty();
    }
}