package org.linky.links;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.Result;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesReaderService;

@RequiredArgsConstructor
public class CreateLinkAction implements Action {

    @Getter
    private final Name name;
    @Getter
    private final Link link;
    private final FilesReaderService filesReaderService;

    @Override
    public Result<Path, Code> apply(FilesMutatorService filesMutatorService) {
        if (!filesReaderService.exists(link.getTo())) {
            return Result.error(new Code(Code.State.INVALID_DESTINATION, null, null));
        }
        try {
            if (!filesReaderService.exists(link.getFrom().getParent())) {
                filesMutatorService.createDirectories(link.getFrom().getParent());
            }
            filesMutatorService.createSymbolicLink(link.getFrom(), link.getTo());
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(new Code(Code.State.ERROR, "Unable to create link " + e.getMessage(), null));
        }
    }
}
