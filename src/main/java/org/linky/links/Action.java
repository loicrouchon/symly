package org.linky.links;

import java.nio.file.Path;
import lombok.Value;
import org.linky.Result;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesReaderService;

public interface Action {

    Type getType();

    Link getLink();

    Result<Path, Code> apply(FilesMutatorService filesMutatorService);

    static Action upToDate(Link link, FilesReaderService filesReaderService) {
        return new NoOpAction(Type.UP_TO_DATE, link, filesReaderService);
    }

    static Action replace(Link link, FilesReaderService filesReaderService) {
        return new UpdateLinkAction(Type.UPDATE, link, filesReaderService);
    }

    static Action conflict(Link link, FilesReaderService filesReaderService) {
        return new ConflictAction(Type.CONFLICT, link, filesReaderService);
    }

    static Action create(Link link, FilesReaderService filesReaderService) {
        return new CreateLinkAction(Type.CREATE, link, filesReaderService);
    }

    enum Type {
        UP_TO_DATE,
        UPDATE,
        CONFLICT,
        CREATE;

        public static final int MAX_LENGTH = 10;
    }

    @Value
    class Code {

        State state;
        String details;
        Path previousPath;

        public enum State {
            CONFLICT,
            INVALID_DESTINATION,
            ERROR
        }
    }
}
