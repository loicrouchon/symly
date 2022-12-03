package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

public record DeleteAction(Path path) implements Action {

    @Override
    public Result<Void, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        try {
            fsWriter.deleteIfExists(path);
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to delete link " + e.getMessage()));
        }
    }
}
