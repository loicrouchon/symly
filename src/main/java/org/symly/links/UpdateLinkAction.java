package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

@RequiredArgsConstructor
public class UpdateLinkAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;
    private final FileSystemReader fsReader;

    @Override
    public Result<Path, Code> apply(FileSystemWriter fsWriter) {
        Path previousLink = fsReader.readSymbolicLink(link.getSource());
        if (!fsReader.exists(link.getTarget())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, previousLink));
        }
        try {
            fsWriter.deleteIfExists(link.getSource());
            fsWriter.createSymbolicLink(link.getSource(), link.getTarget());
            return Result.success(previousLink);
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to update link " + e.getMessage(),
                    previousLink));
        }
    }
}
