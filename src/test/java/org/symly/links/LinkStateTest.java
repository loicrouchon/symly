package org.symly.links;

import static org.symly.testing.Assertions.assertThat;
import static org.symly.testing.Assertions.assertThatCode;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.symly.files.IoMock;
import org.symly.links.LinkState.Entry;
import org.symly.repositories.MainDirectory;

class LinkStateTest {

    private final MainDirectory mainDir = MainDirectory.of(Path.of("/main-dir"));
    private final Path source = mainDir.resolve(Path.of("dir/name"));
    private final Path desiredTarget = Path.of("/repo/dir/name");
    private final IoMock ioMock = new IoMock();

    @Test
    void linkState_cannotBeCreated_whenSourceDoesNotBelongToMainDir() {
        Path source = Path.of("/main-dir2/name");
        Entry missingEntry = Entry.missingEntry();
        assertThatCode(() -> new LinkState(mainDir, source, missingEntry, desiredTarget))
                .throwsThrowableOfType(IllegalStateException.class)
                .hasMessage("Source /main-dir2/name must be a sub-path of /main-dir");
    }

    @Test
    void linkState_cannotBeCreated_whenSourceIsNotAnAbsolutePath() {
        Path source = Path.of("name");
        Entry missingEntry = Entry.missingEntry();
        assertThatCode(() -> new LinkState(mainDir, source, missingEntry, desiredTarget))
                .throwsThrowableOfType(IllegalStateException.class)
                .hasMessage("Source name must be an absolute path");
    }

    @Test
    void linkState_cannotBeCreated_whenDesiredTargetIsNotAnAbsolutePath() {
        Path desiredTarget = Path.of("desired/target");
        Entry missingEntry = Entry.missingEntry();
        assertThatCode(() -> new LinkState(mainDir, source, missingEntry, desiredTarget))
                .throwsThrowableOfType(IllegalStateException.class)
                .hasMessage("Desired target desired/target must be an absolute path");
    }

    @Test
    void linkState_cannotBeCreated_whenItDoesNotCurrentlyExistAsALink_norShouldBeCreated() {
        linkStateShouldThrowWhenNotAMeaningfulEntry(Entry.missingEntry());
        linkStateShouldThrowWhenNotAMeaningfulEntry(Entry.fileEntry());
        linkStateShouldThrowWhenNotAMeaningfulEntry(Entry.directoryEntry());
    }

    private void linkStateShouldThrowWhenNotAMeaningfulEntry(Entry currentState) {
        assertThatCode(() -> new LinkState(mainDir, source, currentState, null))
                .throwsThrowableOfType(IllegalStateException.class)
                .hasMessageStartingWith(
                        "Such LinkStatus makes no sense, they don't exist and should not be created either: /main-dir/dir/name ["
                                + currentState.getClass().getSimpleName());
    }

    @Test
    void linkState_canBeCreated() {
        // given
        LinkState linkState = new LinkState(mainDir, source, Entry.missingEntry(), desiredTarget);
        // when/then
        assertThat(linkState.source()).isEqualTo(Path.of("/main-dir/dir/name"));
        assertThat(linkState.name()).isEqualTo(Path.of("dir/name"));
        assertThat(linkState.desiredTarget()).isEqualTo(desiredTarget);
    }

    @Test
    void linkState_forMissingEntry_shouldMapTo_createLinkAction() {
        // given
        LinkState linkState = new LinkState(mainDir, source, Entry.missingEntry(), desiredTarget);
        // when/then
        assertThat(linkState.currentState()).isEqualTo(Entry.missingEntry());
        assertThat(linkState.type()).isEqualTo(LinkState.Type.MISSING);
        assertThat(linkState.toActions(ioMock.buildFileSystemReader(), false))
                .containsExactly(Action.create(new Link(source, desiredTarget)));
    }

    @Test
    void linkState_forFileEntry_shouldMapTo_conflictAction() {
        // given
        LinkState linkState = new LinkState(mainDir, source, Entry.fileEntry(), desiredTarget);
        // and
        ioMock.file(source);
        // when/then
        assertThat(linkState.currentState()).isEqualTo(Entry.fileEntry());
        assertThat(linkState.type()).isEqualTo(LinkState.Type.FILE_CONFLICT);
        assertThat(linkState.toActions(ioMock.buildFileSystemReader(), false))
                .containsExactly(Action.conflict(new Link(source, desiredTarget)));
    }

    @Test
    void linkState_forFileEntry_withForceOption_shouldMapTo_deleteThenCreateActions() {
        // given
        LinkState linkState = new LinkState(mainDir, source, Entry.fileEntry(), desiredTarget);
        // and
        ioMock.file(source);
        // when/then
        assertThat(linkState.currentState()).isEqualTo(Entry.fileEntry());
        assertThat(linkState.type()).isEqualTo(LinkState.Type.FILE_CONFLICT);
        assertThat(linkState.toActions(ioMock.buildFileSystemReader(), true))
                .containsExactly(Action.delete(source), Action.create(new Link(source, desiredTarget)));
    }

    @Test
    void linkState_forDirectoryEntry_shouldMapTo_conflictAction() {
        // given
        LinkState linkState = new LinkState(mainDir, source, Entry.directoryEntry(), desiredTarget);
        // and
        ioMock.directory(source);
        ioMock.directory(source.resolve("dir1"));
        ioMock.directory(source.resolve("dir1").resolve("dir2"));
        // when/then
        assertThat(linkState.currentState()).isEqualTo(Entry.directoryEntry());
        assertThat(linkState.type()).isEqualTo(LinkState.Type.FILE_CONFLICT);
        assertThat(linkState.toActions(ioMock.buildFileSystemReader(), false))
                .containsExactly(Action.conflict(new Link(source, desiredTarget)));
    }

    @Test
    void linkState_forDirectoryEntry_withForceOption_shouldMapTo_deleteThenCreateActions() {
        // given
        LinkState linkState = new LinkState(mainDir, source, Entry.directoryEntry(), desiredTarget);
        // and
        ioMock.directory(source);
        ioMock.directory(source.resolve("dir1"));
        ioMock.directory(source.resolve("dir1").resolve("dir2"));
        // when/then
        assertThat(linkState.currentState()).isEqualTo(Entry.directoryEntry());
        assertThat(linkState.type()).isEqualTo(LinkState.Type.FILE_CONFLICT);
        assertThat(linkState.toActions(ioMock.buildFileSystemReader(), true))
                .containsExactly(
                        Action.delete(source.resolve("dir1").resolve("dir2")),
                        Action.delete(source.resolve("dir1")),
                        Action.delete(source),
                        Action.create(new Link(source, desiredTarget)));
    }

    @Test
    void linkState_forOrphanLinkEntry_shouldMapTo_deleteLinkAction() {
        // given
        Path orphanLinkTarget = Path.of("/repo2/dir/name");
        LinkState linkState = new LinkState(mainDir, source, Entry.linkEntry(orphanLinkTarget), null);
        // when/then
        assertThat(linkState.currentState()).isEqualTo(Entry.linkEntry(orphanLinkTarget));
        assertThat(linkState.type()).isEqualTo(LinkState.Type.ORPHAN);
        assertThat(linkState.toActions(ioMock.buildFileSystemReader(), false))
                .containsExactly(Action.deleteLink(new Link(source, orphanLinkTarget)));
    }

    @Test
    void linkState_forOutdatedLinkEntry_shouldMapTo_createLinkAction() {
        // given
        Path formerLinkTarget = Path.of("/repo2/dir/name");
        LinkState linkState = new LinkState(mainDir, source, Entry.linkEntry(formerLinkTarget), desiredTarget);
        // when/then
        assertThat(linkState.currentState()).isEqualTo(Entry.linkEntry(formerLinkTarget));
        assertThat(linkState.type()).isEqualTo(LinkState.Type.LINK_CONFLICT);
        assertThat(linkState.toActions(ioMock.buildFileSystemReader(), false))
                .containsExactly(
                        Action.deleteLink(new Link(source, formerLinkTarget)),
                        Action.create(new Link(source, desiredTarget)));
    }

    @Test
    void linkState_forUpToDateLinkEntry_shouldMapTo_noOpAction() {
        // given
        LinkState linkState = new LinkState(mainDir, source, Entry.linkEntry(desiredTarget), desiredTarget);
        // when/then
        assertThat(linkState.currentState()).isEqualTo(Entry.linkEntry(desiredTarget));
        assertThat(linkState.type()).isEqualTo(LinkState.Type.UP_TO_DATE);
        assertThat(linkState.toActions(ioMock.buildFileSystemReader(), false))
                .containsExactly(Action.upToDate(new Link(source, desiredTarget)));
    }
}
