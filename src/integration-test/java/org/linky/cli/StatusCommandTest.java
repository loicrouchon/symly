package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.Execution;
import org.linky.env.IntegrationTest;

class StatusCommandTest extends IntegrationTest {

    @Test
    void addCommand_shouldFail_whenRequiredArgsAreMissing() {
        //given/when
        Execution execution = getEnv().run("status");
        //then
        assertThat(execution.getStdErr()).contains("Missing required option: '--sources=<sources>'");
        assertThat(execution.getExitCode()).isEqualTo(2);
    }

    @Test
    void addCommand_shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        Env env = getEnv()
                .withHome("home/user");
        //when
        Execution execution = env.run("status", "-s", "to/dir", "/home/user/some/file");
        //then
        assertThat(execution.getStdErr()).contains(String.format(
                "Argument <destination> (%s): must be an existing directory", env.home()));
        assertThat(execution.getExitCode()).isEqualTo(2);
    }

    @Test
    void addCommand_shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        Env env = getEnv();
        //when
        Execution execution = env.run("status", "-s", "to/dir", "/home/user/some/file");
        //then
        assertThat(execution.getStdErr()).contains(String.format(
                "Argument <sources> (%s): must be an existing directory", "to/dir"));
        assertThat(execution.getExitCode()).isEqualTo(2);
    }

    @Test
    void addCommand_shouldProvideCorrectDefaults() {
        //given
        Env env = getEnv()
                .withDirectories("from/dir");
        //when
        Execution execution = env
                .run("status", "-s", "from/dir");
        //then
        assertThat(execution.getStdErr()).isEmpty();
        assertThat(execution.getStdOut()).contains(String.format("Checking links status from [%s] to %s",
                env.path("from/dir"), env.home()));
        assertThat(execution.getExitCode()).isZero();
    }

    @Test
    void addCommand_shouldParseArguments_whenArgumentsArePassed() {
        //given
        Env env = getEnv()
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when
        Execution execution = env.run("status", "-s", "from/dir", "from/other-dir", "-d", "to/dir");
        //then
        assertThat(execution.getStdErr()).isEmpty();
        assertThat(execution.getStdOut()).contains(String.format("Checking links status from [%s, %s] to %s",
                env.path("from/dir"), env.path("from/other-dir"), env.path("to/dir")));
        assertThat(execution.getExitCode()).isZero();
    }
}