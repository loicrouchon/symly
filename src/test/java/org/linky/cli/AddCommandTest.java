package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linky.files.FileSystemWriter;
import org.linky.files.IoMock;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddCommandTest {

    private final IoMock ioMock = new IoMock();

    @Mock
    private FileSystemWriter fsWriter;

    private TestCommand<AddCommand> app;

    @BeforeEach
    public void before() {
        app = new TestCommand<>(console -> new AddCommand(console, ioMock.fsReader, fsWriter));
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
        //given/when
        Execution<AddCommand> execution = app.run("add");
        //then
        assertThat(execution.command.from).isEqualTo(Path.of("/home/user"));
        assertThat(execution.command.to).isNull();
        assertThat(execution.command.file).isNull();
    }

    @Test
    void addCommand_shouldParseArguments_whenArgumentsArePassed() {
        //given/when
        Execution<AddCommand> execution = app.run("add", "-f", "from/dir", "-t", "to/dir", "some/file");
        //then
        assertThat(execution.command.from).isEqualTo(Path.of("from/dir"));
        assertThat(execution.command.to).isEqualTo(Path.of("to/dir"));
        assertThat(execution.command.file).isEqualTo(Path.of("some/file"));
    }

    @Test
    void addCommand_shouldFail_whenDirectoryDoesNotExist() {
        //given
        ioMock.fileDoesNotExist(Path.of("/home/user"));
        //when
        Execution<AddCommand> execution = app.run("add", "-t", "to/dir", "/home/user/some/file");
        //then
        execution.assertThatValidationFailedWithMessage(
                "Argument <from> (/home/user): must be an existing directory");
    }
}