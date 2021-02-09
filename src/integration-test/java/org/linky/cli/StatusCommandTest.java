package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.Execution;
import org.linky.env.IntegrationTest;

class StatusCommandTest extends IntegrationTest {

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given/when
        Execution execution = env().run("status");
        //then
        assertThat(execution.stdErr()).contains("Missing required option: '--sources=<sources>'");
        assertThat(execution.exitCode()).isEqualTo(2);
    }

    @Test
    void shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        Env env = env()
                .withHome("home/user");
        //when
        Execution execution = env.run("status", "-s", "to/dir", "/home/user/some/file");
        //then
        assertThat(execution.stdErr()).contains(String.format(
                "Argument <destination> (%s): must be an existing directory", env.home()));
        assertThat(execution.exitCode()).isEqualTo(2);
    }

    @Test
    void shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        Env env = env();
        //when
        Execution execution = env.run("status", "-s", "to/dir", "/home/user/some/file");
        //then
        assertThat(execution.stdErr()).contains(String.format(
                "Argument <sources> (%s): must be an existing directory", "to/dir"));
        assertThat(execution.exitCode()).isEqualTo(2);
    }

    @Test
    void shouldProvideCorrectDefaults() {
        //given
        Env env = env()
                .withDirectories("from/dir");
        //when
        Execution execution = env
                .run("status", "-s", "from/dir");
        //then
        assertThat(execution.stdErr()).isEmpty();
        assertThat(execution.stdOut()).contains(String.format("Checking links status from [%s] to %s",
                env.path("from/dir"), env.home()));
        assertThat(execution.exitCode()).isZero();
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        Env env = env()
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when
        Execution execution = env.run("status", "-s", "from/dir", "from/other-dir", "-d", "to/dir");
        //then
        assertThat(execution.stdErr()).isEmpty();
        assertThat(execution.stdOut()).contains(String.format("Checking links status from [%s, %s] to %s",
                env.path("from/dir"), env.path("from/other-dir"), env.path("to/dir")));
        assertThat(execution.exitCode()).isZero();
    }
}