package org.linky.links;

import java.nio.file.Path;
import java.util.Objects;
import lombok.Value;
import org.linky.files.FilesReaderService;

@Value
public class Link {

    Path from;
    Path to;

    public Status status(FilesReaderService filesReaderService) {
        if (filesReaderService.exists(from)) {
            if (filesReaderService.isSymbolicLink(from)) {
                Path fromRealDestination = filesReaderService.toRealPath(from);
                if (Objects.equals(fromRealDestination, filesReaderService.toRealPath(to))) {
                    return new Status(Status.Type.UP_TO_DATE, this, filesReaderService);
                }
                return new Status(Status.Type.LINK_CONFLICT, this, filesReaderService);
            }
            return new Status(Status.Type.FILE_CONFLICT, this, filesReaderService);
        }
        return new Status(Status.Type.MISSING, this, filesReaderService);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }

}
