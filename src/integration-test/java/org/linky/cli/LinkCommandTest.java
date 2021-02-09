package org.linky.cli;

import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.Execution;
import org.linky.env.IntegrationTest;

class LinkCommandTest extends IntegrationTest {

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        Env env = env();
        //when
        Execution execution = env.run("link");
        //then
        execution.assertThat()
                .fails()
                .withErrorMessage("Missing required option: '--sources=<sources>'")
                .andLayout(env)
                .isEmpty();
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        Env env = env()
                .withHome("home/user");
        //when
        Execution execution = env.run("link", "-s", "to/dir", "/home/user/some/file");
        //then
        execution.assertThat()
                .fails()
                .withErrorMessage("Argument <destination> (%s): must be an existing directory", env.home())
                .andLayout(env)
                .isEmpty();
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        Env env = env();
        //when
        Execution execution = env.run("link", "-s", "to/dir", "/home/user/some/file");
        //then
        execution.assertThat()
                .fails()
                .withErrorMessage("Argument <sources> (%s): must be an existing directory", "to/dir")
                .andLayout(env)
                .isEmpty();
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        Env env = env()
                .withDirectories("from/dir");
        //when
        Execution execution = env.run("link", "-s", "from/dir");
        //then
        execution.assertThat()
                .succeeds()
                .withMessage("Creating links from [%s] to %s", env.path("from/dir"), env.home())
                .andLayout(env)
                .isEmpty();
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        Env env = env()
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when
        Execution execution = env.run("link", "-s", "from/dir", "from/other-dir", "-d", "to/dir");
        //then
        execution.assertThat()
                .succeeds()
                .withMessage("Creating links from [%s, %s] to %s", env.path("from/dir"), env.path("from/other-dir"), env.path("to/dir"))
                .andLayout(env)
                .isEmpty();
    }
}