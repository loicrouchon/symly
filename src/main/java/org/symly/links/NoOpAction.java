package org.symly.links;

import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record NoOpAction(Type type, LinkStatus link) implements Action {

    @Override
    public Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        return Result.success();
    }
}
