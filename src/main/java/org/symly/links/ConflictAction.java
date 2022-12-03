package org.symly.links;

import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record ConflictAction(Link link) implements Action {

    @Override
    public Path path() {
        return link.source();
    }

    @Override
    public Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.target())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null));
        }
        return Result.error(new Code(Code.State.CONFLICT, null));
    }
}
