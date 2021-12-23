package org.symly.links;

import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public interface Action {

    Type type();

    Link link();

    Result<Path, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter);

    static Action upToDate(Link link) {
        return new NoOpAction(Type.UP_TO_DATE, link);
    }

    static Action replace(Link link) {
        return new UpdateLinkAction(Type.UPDATE, link);
    }

    static Action conflict(Link link) {
        return new ConflictAction(Type.CONFLICT, link);
    }

    static Action create(Link link) {
        return new CreateLinkAction(Type.CREATE, link);
    }

    static Action delete(Link link) {
        return new DeleteLinkAction(Type.DELETE, link);
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
