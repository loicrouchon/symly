package org.symly.links;

import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record NoOpAction(
    Type type,
    Link link
) implements Action {

    @Override
    public Result<Path, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        Path currentLink = fsReader.readSymbolicLink(link.source());
        return Result.success(currentLink);
    }
}
