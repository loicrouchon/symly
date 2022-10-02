package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record CreateLinkAction(Link link) implements Action {
    @Override
    public Path path() {
        return link.source();
    }

    @Override
    public Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.target())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null));
        }
        try {
            if (!fsReader.exists(link.source().getParent())) {
                fsWriter.createDirectories(link.source().getParent());
            }
            fsWriter.createSymbolicLink(link.source(), link.target());
            return Result.success();
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to create link " + e.getMessage()));
        }
    }
}
