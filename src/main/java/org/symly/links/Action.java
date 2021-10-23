package org.symly.links;

import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

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

    static Action delete(Link link, FileSystemReader fsReader) {
        return new DeleteLinkAction(Type.DELETE, link, fsReader);
    }

    enum Type {
        UP_TO_DATE,
        UPDATE,
        CONFLICT,
        CREATE,
        DELETE;

        public static final int MAX_LENGTH = 10;
    }

    record Code(
            State state,
            String details,
            Path previousPath) {

        public enum State {
            CONFLICT,
            INVALID_SOURCE,
            INVALID_DESTINATION,
            ERROR
        }
    }
}
