package org.linky.links;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.linky.files.IoMock;

class LinkTest {

    private final Path from = Path.of("from");
    private final Path to = Path.of("to");
    private final Path toRealPath = Path.of("realPath");

    private final IoMock ioMock = new IoMock();

    @Test
    void toString_shouldDisplayLink() {
        assertThat(Link.of(from, to)).hasToString("from -> to");
    }

    @Test
    void synchronizeAction_shouldReturnCreateLinkAction_whenItDoesNotExist() {
        //given
        Link link = Link.of(from, to);
        //and
        ioMock.fileDoesNotExist(from);
        ioMock.symlinkTargets(to, toRealPath);
        //when
        Status status = link.status(ioMock.fsReader);
        Action action = status.toAction();
        //then
        assertThat(status.getType()).isEqualTo(Status.Type.MISSING);
        assertThat(action).isInstanceOf(CreateLinkAction.class);
        assertThat(action.getType()).isEqualTo(Action.Type.CREATE);
    }

    @Test
    void synchronizeAction_shouldReturnReplaceFileAction_whenARegularFileAlreadyExist() {
        //given
        Link link = Link.of(from, to);
        //and
        ioMock.fileExists(from);
        ioMock.symlinkTargets(to, toRealPath);
        //when
        Status status = link.status(ioMock.fsReader);
        Action action = status.toAction();
        //then
        assertThat(status.getType()).isEqualTo(Status.Type.FILE_CONFLICT);
        assertThat(action).isInstanceOf(ConflictAction.class);
        assertThat(action.getType()).isEqualTo(Action.Type.CONFLICT);
    }

    @Test
    void synchronizeAction_shouldReturnUpdateLinkAction_whenSymlinkIsNotUpToDate() {
        //given
        Link link = Link.of(from, to);
        //and
        ioMock.symlinkExists(from, Path.of("fromRealPath"));
        ioMock.symlinkTargets(to, toRealPath);
        //when
        Status status = link.status(ioMock.fsReader);
        Action action = status.toAction();
        //then
        assertThat(status.getType()).isEqualTo(Status.Type.LINK_CONFLICT);
        assertThat(action).isInstanceOf(UpdateLinkAction.class);
        assertThat(action.getType()).isEqualTo(Action.Type.UPDATE);
    }

    @Test
    void synchronizeAction_shouldReturnNoOpAction_whenSymlinkIsUpToDate() {
        //given
        Link link = Link.of(from, to);
        //and
        ioMock.symlinkExists(from, toRealPath);
        ioMock.symlinkTargets(to, toRealPath);
        //when
        Status status = link.status(ioMock.fsReader);
        Action action = status.toAction();
        //then
        assertThat(status.getType()).isEqualTo(Status.Type.UP_TO_DATE);
        assertThat(action).isInstanceOf(NoOpAction.class);
        assertThat(action.getType()).isEqualTo(Action.Type.UP_TO_DATE);
    }
}