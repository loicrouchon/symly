package org.symly.links;

import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

@RequiredArgsConstructor
public class ConflictAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;

    @Override
    public Result<Path, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.target())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, link.source()));
        }
        return Result.error(new Code(Code.State.CONFLICT, null, link.source()));
    }
}
