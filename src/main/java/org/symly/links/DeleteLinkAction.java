package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

@RequiredArgsConstructor
public class DeleteLinkAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;
    private final FileSystemReader fsReader;

    @Override
    public Result<Path, Code> apply(FileSystemWriter fsWriter) {
        Path source = link.getSource();
        try {
            fsWriter.deleteIfExists(source);
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(
                    new Code(Code.State.ERROR, "Unable to delete link " + e.getMessage(), link.getTarget()));
        }
    }
}
