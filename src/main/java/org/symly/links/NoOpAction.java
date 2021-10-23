package org.symly.links;

import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.symly.Result;
import org.symly.files.FileSystemReader;
import org.symly.files.FileSystemWriter;

@RequiredArgsConstructor
public class NoOpAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;

    @Override
    public Result<Path, Code> apply(FileSystemReader fsReader, FileSystemWriter fsWriter) {
        Path currentLink = fsReader.readSymbolicLink(link.source());
        return Result.success(currentLink);
    }
}
