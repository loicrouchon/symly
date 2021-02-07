package org.linky.links;

import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriter;

@RequiredArgsConstructor
public class UpdateLinkAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;
    private final FileSystemReader fsReader;

    @Override
    public Result<Path, Code> apply(FileSystemWriter fsWriter) {
        Path previousLink = fsReader.readSymbolicLink(link.getFrom());
        if (!fsReader.exists(link.getTo())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, previousLink));
        }
        try {
            fsWriter.deleteIfExists(link.getFrom());
            fsWriter.createSymbolicLink(link.getFrom(), link.getTo());
            return Result.success(previousLink);
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to update link " + e.getMessage(),
                    previousLink));
        }
    }
}
