package org.linky.cli;

import org.junit.jupiter.api.Test;
import org.linky.env.IntegrationTest;

@SuppressWarnings("java:S2699")
class StatusCommandTest extends IntegrationTest {

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        givenCleanEnv();
        //when/then
        whenRunningCommand("status")
                .thenItShould()
                .fail()
                .withErrorMessage("Missing required option: '--sources=<sources>'")
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        givenCleanEnv()
                .withHome("home/doesnotexist");
        //when/then
        whenRunningCommand("status", "-s", "to/dir", "/home/user/some/file")
                .thenItShould()
                .fail()
                .withErrorMessage(
                        "Argument <destination> (%s): must be an existing directory",
                        home())
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        givenCleanEnv();
        //when/then
        whenRunningCommand("status", "-s", "to/dir", "/home/user/some/file")
                .thenItShould()
                .fail()
                .withErrorMessage(
                        "Argument <sources> (%s): must be an existing directory",
                        "to/dir")
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        givenCleanEnv()
                .withDirectories("from/dir");
        //when/then
        whenRunningCommand("status", "-s", "from/dir")
                .thenItShould()
                .succeed()
                .withMessage(
                        "Checking links status from [%s] to %s",
                        path("from/dir"),
                        home())
                .andLayout()
                .isEmpty();
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        givenCleanEnv()
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
                .andLayout()
                .isEmpty();
    }
}