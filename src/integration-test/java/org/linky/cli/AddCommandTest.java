package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.linky.env.Env;
import org.linky.env.Execution;
import org.linky.env.IntegrationTest;

class AddCommandTest extends IntegrationTest {

    @Test
    void shouldFail_whenRequiredArgsAreMissing() {
        //given
        Env env = env();
        //when
        Execution execution = env.run("add");
        //then
        assertThat(execution.stdErr()).contains(
                "Missing required options and parameters: '--to=<to>', '<file>'");
        assertThat(execution.exitCode()).isEqualTo(2);
    }

    @Test
    @Disabled
    void shouldProvideCorrectDefaults() {
        //given
        Env env = env()
                .withDirectories("to/dir")
                .withFiles("some/file");
        //when
        Execution execution = env.run("add", "-t", "to/dir", "some/file");
        //then
        assertThat(execution.stdErr()).isEmpty();
        assertThat(execution.exitCode()).isZero();
        assertThat(execution.stdOut()).contains(
                "Missing required options and parameters: '--to=<to>', '<file>'");
    }

    @Test
    void shouldParseArguments_whenArgumentsArePassed() {
        //given
        Env env = env()
                .withDirectories("from/dir", "to/dir")
                .withFiles("from/dir/some/file");
        //when
        Execution execution = env.run("add", "-f", "from/dir", "-t", "to/dir", "from/dir/some/file");
        //then
        assertThat(execution.stdErr()).isEmpty();
        assertThat(execution.exitCode()).isZero();
        assertThat(execution.stdOut()).contains(
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
    void shouldFail_whenDirectoryDoesNotExist() {
        //given
        Env env = env()
                .withHome("home/user");
        //when
        Execution execution = env.run("add", "-t", "to/dir", "/home/user/some/file");
        //then
        assertThat(execution.stdErr()).contains(String.format(
                "Argument <from> (%s): must be an existing directory", env.home()));
        assertThat(execution.exitCode()).isEqualTo(2);
    }
}