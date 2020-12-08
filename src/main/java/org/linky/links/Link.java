package org.linky.links;

import java.nio.file.Path;
import java.util.Objects;
import lombok.Value;
import org.linky.files.FilesReaderService;

@Value
public class Link {

    Path from;
    Path to;

    public Action synchronizeAction(FilesReaderService filesReaderService) {
        if (filesReaderService.exists(from)) {
            if (filesReaderService.isSymbolicLink(from)) {
                Path fromRealDestination = filesReaderService.toRealPath(from);
                if (Objects.equals(fromRealDestination, filesReaderService.toRealPath(to))) {
                    return Action.upToDate(this, filesReaderService);
                }
                return Action.updateLink(this, filesReaderService);
            }
            return Action.replaceFile(this, filesReaderService);
        }
        return Action.createLink(this, filesReaderService);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
