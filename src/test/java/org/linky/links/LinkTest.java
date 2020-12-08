package org.linky.links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.linky.files.FilesReaderService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkTest {

    private final Path from = Path.of("from");
    private final Path to = Path.of("to");
    private final Path toRealPath = Path.of("realPath");

    @Mock
    private FilesReaderService filesReaderService;

    @Test
    void toString_shouldDisplayLink() {
        assertThat(new Link(from, to)).hasToString("from -> to");
    }

    @Test
    void synchronizeAction_shouldReturnCreateLinkAction_whenItDoesNotExist() {
        //given
        Link link = new Link(from, to);
        //and
        fileDoesNotExist(from);
        symlinkTargets(to, toRealPath);
        //when
        Action action = link.synchronizeAction(filesReaderService);
        //then
        assertThat(action).isInstanceOf(CreateLinkAction.class);
        assertThat(action.getName()).isEqualTo(Action.Name.CREATE);
    }

    @Test
    void synchronizeAction_shouldReturnReplaceFileAction_whenARegularFileAlreadyExist() {
        //given
        Link link = new Link(from, to);
        //and
        fileExists(from);
        symlinkTargets(to, toRealPath);
        //when
        Action action = link.synchronizeAction(filesReaderService);
        //then
        assertThat(action).isInstanceOf(ReplaceFileAction.class);
        assertThat(action.getName()).isEqualTo(Action.Name.REPLACE_FILE);
    }

    @Test
    void synchronizeAction_shouldReturnUpdateLinkAction_whenSymlinkIsNotUpToDate() {
        //given
        Link link = new Link(from, to);
        //and
        symlinkExists(from, Path.of("fromRealPath"));
        symlinkTargets(to, toRealPath);
        //when
        Action action = link.synchronizeAction(filesReaderService);
        //then
        assertThat(action).isInstanceOf(UpdateLinkAction.class);
        assertThat(action.getName()).isEqualTo(Action.Name.UPDATE_LINK);
    }

    @Test
    void synchronizeAction_shouldReturnNoOpAction_whenSymlinkIsUpToDate() {
        //given
        Link link = new Link(from, to);
        //and
        symlinkExists(from, toRealPath);
        symlinkTargets(to, toRealPath);
        //when
        Action action = link.synchronizeAction(filesReaderService);
        //then
        assertThat(action).isInstanceOf(NoOpAction.class);
        assertThat(action.getName()).isEqualTo(Action.Name.UP_TO_DATE);
    }

    private void fileDoesNotExist(Path path) {
        given(filesReaderService.exists(path)).willReturn(false);
    }

    private void fileExists(Path path) {
        given(filesReaderService.exists(path)).willReturn(true);
        given(filesReaderService.isSymbolicLink(path)).willReturn(false);
    }

    private void symlinkExists(Path path, Path target) {
        given(filesReaderService.exists(path)).willReturn(true);
        given(filesReaderService.isSymbolicLink(path)).willReturn(true);
        symlinkTargets(path, target);
    }

    private void symlinkTargets(Path path, Path target) {
        lenient().when(filesReaderService.toRealPath(path)).thenReturn(target);
    }
}