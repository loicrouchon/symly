package org.linky.links;

import java.nio.file.Path;
import lombok.Value;
import org.linky.Result;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriter;

public interface Action {

    Type getType();

    Link getLink();

    Result<Path, Code> apply(FileSystemWriter fsWriter);

    static Action upToDate(Link link, FileSystemReader fsReader) {
        return new NoOpAction(Type.UP_TO_DATE, link, fsReader);
    }

    static Action replace(Link link, FileSystemReader fsReader) {
        return new UpdateLinkAction(Type.UPDATE, link, fsReader);
    }

    static Action conflict(Link link, FileSystemReader fsReader) {
        return new ConflictAction(Type.CONFLICT, link, fsReader);
    }

    static Action create(Link link, FileSystemReader fsReader) {
        return new CreateLinkAction(Type.CREATE, link, fsReader);
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
