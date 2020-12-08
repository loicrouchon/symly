package org.linky.links;

import java.io.IOException;
import java.nio.file.Path;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesReaderService;

@RequiredArgsConstructor
public class UpdateLinkAction implements Action {

    @Getter
    private final Name name;
    @Getter
    private final Link link;
    private final FilesReaderService filesReaderService;

    @Override
    public Result<Path, Code> apply(FilesMutatorService filesMutatorService) {
        Path previousLink = filesReaderService.toRealPath(link.getFrom());
        if (!filesReaderService.exists(link.getTo())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, previousLink));
        }
        try {
            filesMutatorService.deleteIfExists(link.getFrom());
            filesMutatorService.createSymbolicLink(link.getFrom(), link.getTo());
            return Result.success(previousLink);
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to update link " + e.getMessage(),
                    previousLink));
        }
    }
}
