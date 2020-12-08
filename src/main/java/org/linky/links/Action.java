package org.linky.links;

import java.nio.file.Path;
import lombok.Value;
import org.linky.Result;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesReaderService;

public interface Action {

    Name getName();

    Link getLink();

    Result<Path, Code> apply(FilesMutatorService filesMutatorService);

    static Action upToDate(Link link, FilesReaderService filesReaderService) {
        return new NoOpAction(Name.UP_TO_DATE, link, filesReaderService);
    }

    static Action updateLink(Link link, FilesReaderService filesReaderService) {
        return new UpdateLinkAction(Name.UPDATE_LINK, link, filesReaderService);
    }

    static Action replaceFile(Link link, FilesReaderService filesReaderService) {
        return new ReplaceFileAction(Name.REPLACE_FILE, link, filesReaderService);
    }

    static Action createLink(Link link, FilesReaderService filesReaderService) {
        return new CreateLinkAction(Name.CREATE, link, filesReaderService);
    }

    enum Name {
        UP_TO_DATE,
        UPDATE_LINK,
        REPLACE_FILE,
        CREATE;

        public static final int MAX_LENGTH = 12;
    }

    @Value
    class Code {

        State state;
        String details;
        Path previousPath;

        public enum State {
            CONFLICT,
            INVALID_DESTINATION,
            ERROR;
        }
    }
}
