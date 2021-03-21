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
    private final FileSystemReader fsReader;

    @Override
    public Result<Path, Code> apply(FileSystemWriter fsWriter) {
        if (!fsReader.exists(link.getTarget())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, link.getSource()));
        }
        return Result.error(new Code(Code.State.CONFLICT, null, link.getSource()));
    }
}
