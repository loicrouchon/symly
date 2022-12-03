package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record DeleteLinkAction(Link link) implements Action {

    @Override
    public Path path() {
        return link.source();
    }

    @Override
    public Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        try {
            fsWriter.deleteIfExists(link.source());
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to delete link " + e.getMessage()));
        }
    }
}
