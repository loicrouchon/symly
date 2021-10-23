package org.symly.links;

import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

@RequiredArgsConstructor
public class CreateLinkAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;
    private final FileSystemReader fsReader;

    @Override
    public Result<Path, Code> apply(FileSystemWriter fsWriter) {
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
