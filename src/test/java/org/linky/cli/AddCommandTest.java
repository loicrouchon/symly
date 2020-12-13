package org.linky.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriter;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddCommandTest {

    private final Path from = Path.of("from");
    private final Path to = Path.of("to");
    private final Path toRealPath = Path.of("realPath");

    @Mock
    private FileSystemReader fsReader;
    @Mock
    private FileSystemWriter fsWriter;

    private TestCommand<AddCommand> app;

    @BeforeEach
    public void before() {
        app = new TestCommand<>(console -> new AddCommand(console, fsReader, fsWriter));
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
}