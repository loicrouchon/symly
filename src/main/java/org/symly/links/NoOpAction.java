package org.symly.links;

import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record NoOpAction(Link link) implements Action {

    @Override
    public Path path() {
        return link.source();
    }

    @Override
    public Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        return Result.success();
    }
}
