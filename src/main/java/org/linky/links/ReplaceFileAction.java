package org.linky.links;

import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesReaderService;

@RequiredArgsConstructor
public class ReplaceFileAction implements Action {

    @Getter
    private final Name name;
    @Getter
    private final Link link;
    private final FilesReaderService filesReaderService;

    @Override
    public Result<Path, Code> apply(FilesMutatorService filesMutatorService) {
        if (!filesReaderService.exists(link.getTo())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, link.getFrom()));
        }
        return Result.error(new Code(Code.State.CONFLICT, null, link.getFrom()));
    }
}
