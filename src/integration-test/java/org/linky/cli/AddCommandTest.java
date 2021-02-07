package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.linky.files.Env;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriterImpl;
import org.linky.files.TemporaryFolderTest;

class AddCommandTest extends TemporaryFolderTest {

    private TestCommand<AddCommand> app;

    @BeforeEach
    public void before() {
        app = new TestCommand<>(console -> new AddCommand(console, new FileSystemReader(), new FileSystemWriterImpl()));
    }

    @Test
    void addCommand_shouldFail_whenRequiredArgsAreMissing() {
        //given/when
        Execution<AddCommand> execution = app.run("add");
        //then
        execution.assertThatValidationFailedWithMessage(
                "Missing required options and parameters: '--to=<to>', '<file>'");
    }

    @Test
    void addCommand_shouldProvideCorrectDefaults() {
        //given
        Env env = getEnv();
        //when
        Execution<AddCommand> execution = app.run("add");
        //then
        Assertions.assertThat(execution.command.from).isEqualTo(env.home());
        Assertions.assertThat(execution.command.to).isNull();
        Assertions.assertThat(execution.command.file).isNull();
    }

    @Test
    void addCommand_shouldParseArguments_whenArgumentsArePassed() {
        //given/when
        Execution<AddCommand> execution = app.run("add", "-f", "from/dir", "-t", "to/dir", "some/file");
        //then
        Assertions.assertThat(execution.command.from).isEqualTo(Path.of("from/dir"));
        Assertions.assertThat(execution.command.to).isEqualTo(Path.of("to/dir"));
        Assertions.assertThat(execution.command.file).isEqualTo(Path.of("some/file"));
    }

    @Test
    void addCommand_shouldFail_whenDirectoryDoesNotExist() {
        //given
        Env env = getEnv()
                .withHome("home/user");
        //when
        Execution<AddCommand> execution = app.run("add", "-t", "to/dir", "/home/user/some/file");
        //then
        execution.assertThatValidationFailedWithMessage(String.format(
                "Argument <from> (%s): must be an existing directory", env.home()));
    }
}