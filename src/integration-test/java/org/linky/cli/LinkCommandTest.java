package org.linky.cli;

import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.Execution;
import org.linky.env.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class LinkCommandTest extends IntegrationTest {

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        Env env = env();
        //when
        Execution execution = env.run("link");
        //then
        execution.assertThatFailedWith("Missing required option: '--sources=<sources>'");
        assertThat(env.getRootFileTree().getLayout()).isEmpty();
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        Env env = env()
                .withHome("home/user");
        //when
        Execution execution = env.run("link", "-s", "to/dir", "/home/user/some/file");
        //then
        execution.assertThatFailedWith("Argument <destination> (%s): must be an existing directory", env.home());
        assertThat(env.getRootFileTree().getLayout()).isEmpty();
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        Env env = env();
        //when
        Execution execution = env.run("link", "-s", "to/dir", "/home/user/some/file");
        //then
        execution.assertThatFailedWith("Argument <sources> (%s): must be an existing directory", "to/dir");
        assertThat(env.getRootFileTree().getLayout()).isEmpty();
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        Env env = env()
                .withDirectories("from/dir");
        //when
        Execution execution = env.run("link", "-s", "from/dir");
        //then
        execution.assertThatSucceededWith("Creating links from [%s] to %s", env.path("from/dir"), env.home());
        assertThat(env.getRootFileTree().getLayout()).isEmpty();
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        Env env = env()
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when
        Execution execution = env.run("link", "-s", "from/dir", "from/other-dir", "-d", "to/dir");
        //then
        execution.assertThatSucceededWith("Creating links from [%s, %s] to %s", env.path("from/dir"), env.path("from/other-dir"), env.path("to/dir"));
        assertThat(env.getRootFileTree().getLayout()).isEmpty();
    }
}