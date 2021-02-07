package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.linky.files.Env;
import org.linky.files.TemporaryFolderTest;

class StatusCommandTest extends TemporaryFolderTest {

    @Test
    void addCommand_shouldFail_whenRequiredArgsAreMissing() {
        //given/when
        ProcessExecution run = getEnv().run("status");
        //then
        assertThat(run.getStdErr()).contains("Missing required option: '--sources=<sources>'");
        assertThat(run.getExitCode()).isEqualTo(2);
    }

    @Test
    void addCommand_shouldFail_whenDestinationDirectoryDoesNotExist() {
        //given
        Env env = getEnv()
                .withHome("home/user");
        //when
        ProcessExecution run = env.run("status", "-s", "to/dir", "/home/user/some/file");
        //then
        assertThat(run.getStdErr()).contains(String.format(
                "Argument <destination> (%s): must be an existing directory", env.home()));
        assertThat(run.getExitCode()).isEqualTo(2);
    }

    @Test
    void addCommand_shouldFail_whenSourceDirectoryDoesNotExist() {
        //given
        Env env = getEnv();
        //when
        ProcessExecution run = env.run("status", "-s", "to/dir", "/home/user/some/file");
        //then
        assertThat(run.getStdErr()).contains(String.format(
                "Argument <sources> (%s): must be an existing directory", "to/dir"));
        assertThat(run.getExitCode()).isEqualTo(2);
    }

    @Test
    void addCommand_shouldProvideCorrectDefaults() {
        //given
        Env env = getEnv()
                .withDirectories("from/dir");
        //when
        ProcessExecution run = env
                .run("status", "-s", "from/dir");
        //then
        assertThat(run.getStdErr()).isEmpty();
        assertThat(run.getStdOut()).contains(String.format("Checking links status from [%s] to %s",
                env.path("from/dir"), env.home()));
        assertThat(run.getExitCode()).isZero();
    }

    @Test
    void addCommand_shouldParseArguments_whenArgumentsArePassed() {
        //given
        Env env = getEnv()
                .withDirectories("from/dir", "from/other-dir", "to/dir");
        //when
        ProcessExecution run = env.run("status", "-s", "from/dir", "from/other-dir", "-d", "to/dir");
        //then
        assertThat(run.getStdErr()).isEmpty();
        assertThat(run.getStdOut()).contains(String.format("Checking links status from [%s, %s] to %s",
                env.path("from/dir"), env.path("from/other-dir"), env.path("to/dir")));
        assertThat(run.getExitCode()).isZero();
    }
}