package org.symly.links;

import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public interface Action {

    Type type();

    LinkStatus link();

    Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter);

    static Action upToDate(LinkStatus link) {
        return new NoOpAction(Type.UP_TO_DATE, link);
    }

    static Action replace(LinkStatus link) {
        return new UpdateLinkAction(Type.MODIFY, link);
    }

    static Action conflict(LinkStatus link) {
        return new ConflictAction(Type.CONFLICT, link);
    }

    static Action create(LinkStatus link) {
        return new CreateLinkAction(Type.CREATE, link);
    }

    static Action delete(LinkStatus link) {
        return new DeleteLinkAction(Type.DELETE, link);
    }

    enum Type {
        UP_TO_DATE,
        CONFLICT,
        MODIFY,
        CREATE,
        DELETE
    }

    record Code(State state, String details) {

        public enum State {
            CONFLICT,
            INVALID_SOURCE,
            INVALID_DESTINATION,
            ERROR
        }
    }
}
