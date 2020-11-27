package org.linky.links;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import lombok.Value;
import org.linky.files.FilesMutatorService;
import org.linky.files.FilesReaderService;

@Value
public class Link {

    Path from;
    Path to;

    public LinkingStatus create(FilesReaderService filesReaderService, FilesMutatorService filesMutatorService) {
        if (!filesReaderService.exists(to)) {
            return new LinkingStatus(Status.INVALID_DESTINATION, null);
        }
        try {
            if (filesReaderService.exists(from)) {
                if (filesReaderService.isSymbolicLink(from)) {
                    Path fromRealDestination = from.toRealPath();
                    if (Objects.equals(fromRealDestination, to.toRealPath())) {
                        return new LinkingStatus(Status.UP_TO_DATE, null);
                    } else {
                        filesMutatorService.deleteIfExists(from);
                        filesMutatorService.createSymbolicLink(from, to);
                        return new LinkingStatus(Status.UPDATED, fromRealDestination.toString());
                    }
                } else {
                    return new LinkingStatus(Status.CONFLICT, null);
                }
            } else {
                if (!filesReaderService.exists(from.getParent())) {
                    filesMutatorService.createDirectories(from.getParent());
                }
                filesMutatorService.createSymbolicLink(from, to);
                return new LinkingStatus(Status.CREATED, null);
            }
        } catch (IOException e) {
            return new LinkingStatus(Status.ERROR, "Unable to create link " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return from + " ->" + to;
    }

    @Value
    public static class LinkingStatus {
        Status status;
        String details;
    }

    public enum Status {
        CREATED(true),
        UPDATED(true),
        UP_TO_DATE(true),
        CONFLICT(false),
        INVALID_DESTINATION(false),
        ERROR(false);

        private final boolean successful;

        Status(boolean successful) {
            this.successful = successful;
        }

        public boolean isSuccessful() {
            return successful;
        }
    }
}
