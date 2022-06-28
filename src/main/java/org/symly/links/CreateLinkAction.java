package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record CreateLinkAction(Type type, Link link) implements Action {

    @Override
    public Result<Path, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.target())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, null));
        }
        try {
            if (!fsReader.exists(link.source().getParent())) {
                fsWriter.createDirectories(link.source().getParent());
            }
            fsWriter.createSymbolicLink(link.source(), link.target());
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to create link " + e.getMessage(), null));
        }
    }
}
