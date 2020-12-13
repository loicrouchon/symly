package org.linky.links;

import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FileSystemReader;
import org.linky.files.FileSystemWriter;

@RequiredArgsConstructor
public class ConflictAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;
    private final FileSystemReader fsReader;

    @Override
    public Result<Path, Code> apply(FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.getTo())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, link.getFrom()));
        }
        return Result.error(new Code(Code.State.CONFLICT, null, link.getFrom()));
    }
}
