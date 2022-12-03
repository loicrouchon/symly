package org.symly.links;

import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public sealed interface Action permits NoOpAction, CreateLinkAction, DeleteLinkAction, ConflictAction, DeleteAction {

    Path path();

    Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter);

    static Action upToDate(Link link) {
        return new NoOpAction(link);
    }

    static Action conflict(Link link) {
        return new ConflictAction(link);
    }

    static Action create(Link link) {
        return new CreateLinkAction(link);
    }

    static Action deleteLink(Link link) {
        return new DeleteLinkAction(link);
    }

    static Action delete(Path path) {
        return new DeleteAction(path);
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
