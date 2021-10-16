package org.symly.links;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.symly.files.IoMock;

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
        List<Action> actions = status.toActions(false);
        //then
        assertThat(status.getType()).isEqualTo(Status.Type.MISSING);
        assertThat(actions)
                .hasSize(1)
                .first()
                .isInstanceOf(CreateLinkAction.class)
                .satisfies(action -> assertThat(action.getType()).isEqualTo(Action.Type.CREATE));
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
        List<Action> actions = status.toActions(false);
        //then
        assertThat(status.getType()).isEqualTo(Status.Type.FILE_CONFLICT);
        assertThat(actions)
                .hasSize(1)
                .first()
                .isInstanceOf(ConflictAction.class)
                .satisfies(action -> assertThat(action.getType()).isEqualTo(Action.Type.CONFLICT));
    }

    @Test
    void synchronizeAction_shouldReturnUpdateLinkAction_whenSymlinkIsNotUpToDate() {
        //given
        Link link = Link.of(from, to);
        //and
        ioMock.symlinkExists(from, Path.of("something"));
        //when
        Status status = link.status(ioMock.fsReader);
        List<Action> actions = status.toActions(false);
        //then
        assertThat(status.getType()).isEqualTo(Status.Type.LINK_CONFLICT);
        assertThat(actions)
                .hasSize(1)
                .first()
                .isInstanceOf(UpdateLinkAction.class)
                .satisfies(action -> assertThat(action.getType()).isEqualTo(Action.Type.UPDATE));
    }

    @Test
    void synchronizeAction_shouldReturnNoOpAction_whenSymlinkIsUpToDate() {
        //given
        Link link = Link.of(from, to);
        //and
        ioMock.symlinkExists(from, to);
        //when
        Status status = link.status(ioMock.fsReader);
        List<Action> actions = status.toActions(false);
        //then
        assertThat(status.getType()).isEqualTo(Status.Type.UP_TO_DATE);
        assertThat(actions)
                .hasSize(1)
                .first()
                .isInstanceOf(NoOpAction.class)
                .satisfies(action -> assertThat(action.getType()).isEqualTo(Action.Type.UP_TO_DATE));
    }
}