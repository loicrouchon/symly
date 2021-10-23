package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record DeleteLinkAction(
        Type type,
        Link link
) implements Action {

    @Override
    public Result<Path, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        Path source = link.source();
        try {
            fsWriter.deleteIfExists(source);
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(
                    new Code(Code.State.ERROR, "Unable to delete link " + e.getMessage(), link.target()));
        }
    }
}
