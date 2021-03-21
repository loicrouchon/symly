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
    private final FileSystemReader fsReader;

    @Override
    public Result<Path, Code> apply(FileSystemWriter fsWriter) {
        Path currentLink = fsReader.readSymbolicLink(link.getSource());
        return Result.success(currentLink);
    }
}
