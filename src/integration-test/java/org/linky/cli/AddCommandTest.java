package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.linky.files.Env;
import org.linky.files.IntegrationTest;

class AddCommandTest extends IntegrationTest {

    @Test
    void addCommand_shouldFail_whenRequiredArgsAreMissing() {
        //given
        Env env = getEnv();
        //when
        Execution execution = env.run("add");
        //then
        assertThat(execution.getStdErr()).contains(
                "Missing required options and parameters: '--to=<to>', '<file>'");
        assertThat(execution.getExitCode()).isEqualTo(2);
    }

    @Test
    @Disabled
    void addCommand_shouldProvideCorrectDefaults() {
        //given
        Env env = getEnv()
                .withDirectories("to/dir")
                .withFiles("some/file");
        //when
        Execution execution = env.run("add", "-t", "to/dir", "some/file");
        //then
        assertThat(execution.getStdErr()).isEmpty();
        assertThat(execution.getExitCode()).isZero();
        assertThat(execution.getStdOut()).contains(
                "Missing required options and parameters: '--to=<to>', '<file>'");
    }

    @Test
    void addCommand_shouldParseArguments_whenArgumentsArePassed() {
        //given
        Env env = getEnv()
                .withDirectories("from/dir", "to/dir")
                .withFiles("from/dir/some/file");
        //when
        Execution execution = env.run("add", "-f", "from/dir", "-t", "to/dir", "from/dir/some/file");
        //then
        assertThat(execution.getStdErr()).isEmpty();
        assertThat(execution.getExitCode()).isZero();
        assertThat(execution.getStdOut()).contains(
                String.format(
                        "Moving some/file from %s to %s and creating link",
                        env.path("from/dir"), env.path("to/dir")),
                String.format(
                        "[MOVED] from/dir/some/file -> %s",
                        env.path("to/dir/some/file")));
    }
    // TODO add tests where some/file does not exist
    // TODO add tests where some/file is not a subfile of from/dir

    @Test
    void addCommand_shouldFail_whenDirectoryDoesNotExist() {
        //given
        Env env = getEnv()
                .withHome("home/user");
        //when
        Execution execution = env.run("add", "-t", "to/dir", "/home/user/some/file");
        //then
        assertThat(execution.getStdErr()).contains(String.format(
                "Argument <from> (%s): must be an existing directory", env.home()));
        assertThat(execution.getExitCode()).isEqualTo(2);
    }
}