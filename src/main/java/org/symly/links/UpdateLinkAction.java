package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record UpdateLinkAction(
    Type type,
    Link link
) implements Action {

    @Override
    public Result<Path, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        Path previousLink = fsReader.readSymbolicLink(link.source());
        if (!fsReader.exists(link.target())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, previousLink));
        }
        try {
            fsWriter.deleteIfExists(link.source());
            fsWriter.createSymbolicLink(link.source(), link.target());
            return Result.success(previousLink);
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to update link " + e.getMessage(),
                previousLink));
        }
    }
}
