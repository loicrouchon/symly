package org.symly.links;

import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record ConflictAction(Type type, LinkStatus link) implements Action {

    @Override
    public Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.desiredTarget())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null));
        }
        return Result.error(new Code(Code.State.CONFLICT, null));
    }
}
