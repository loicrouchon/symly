package org.symly.links;

import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record ConflictAction(
        Type type,
        Link link
) implements Action {

    @Override
    public Result<Path, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.target())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, link.source()));
        }
        return Result.error(new Code(Code.State.CONFLICT, null, link.source()));
    }
}
