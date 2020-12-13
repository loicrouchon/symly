package org.linky.files;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IoMock {

    public final FileSystemReader fsReader;

    public IoMock() {
        fsReader = mock(FileSystemReader.class);
    }

    public void fileDoesNotExist(Path path) {
        given(fsReader.exists(path)).willReturn(false);
    }

    public void fileExists(Path path) {
        given(fsReader.exists(path)).willReturn(true);
        given(fsReader.isSymbolicLink(path)).willReturn(false);
    }

    public void symlinkExists(Path path, Path target) {
        given(fsReader.exists(path)).willReturn(true);
        given(fsReader.isSymbolicLink(path)).willReturn(true);
        symlinkTargets(path, target);
    }

    public void symlinkTargets(Path path, Path target) {
        lenient().when(fsReader.toRealPath(path)).thenReturn(target);
    }

}
