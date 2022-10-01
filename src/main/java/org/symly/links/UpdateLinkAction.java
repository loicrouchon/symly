package org.symly.links;

import java.io.IOException;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record UpdateLinkAction(Type type, LinkStatus link) implements Action {

    @Override
    public Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.desiredTarget())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null));
        }
        try {
            fsWriter.deleteIfExists(link.source());
            fsWriter.createSymbolicLink(link.source(), link.desiredTarget());
            return Result.success();
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to update link " + e.getMessage()));
        }
    }
}
