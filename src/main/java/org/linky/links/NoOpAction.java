package org.linky.links;

import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesReaderService;

@RequiredArgsConstructor
public class NoOpAction implements Action {

    @Getter
    private final Type type;
    @Getter
    private final Link link;
    private final FilesReaderService filesReaderService;

    @Override
    public Result<Path, Code> apply(FilesMutatorService filesMutatorService) {
        Path currentLink = filesReaderService.toRealPath(link.getFrom());
        return Result.success(currentLink);
    }
}
