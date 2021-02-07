package org.linky.links;

import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriter;

@RequiredArgsConstructor
public class CreateLinkAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;
    private final FileSystemReader fsReader;

    @Override
    public Result<Path, Code> apply(FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.getTo())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, null));
        }
        try {
            if (!fsReader.exists(link.getFrom().getParent())) {
                fsWriter.createDirectories(link.getFrom().getParent());
            }
            fsWriter.createSymbolicLink(link.getFrom(), link.getTo());
            return Result.success(null);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error(new Code(Code.State.ERROR, "Unable to create link " + e.getMessage(), null));
        }
    }
}
