package org.linky.links;

import java.nio.file.Path;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.linky.files.FileSystemReader;

@Value
@RequiredArgsConstructor(staticName = "of")
public class Link {

    Path from;
    Path to;

    public Status status(FileSystemReader fsReader) {
        if (fsReader.isSymbolicLink(from)) {
            Path fromRealDestination = fsReader.readSymbolicLink(from);
            if (Objects.equals(fromRealDestination, to)) {
                return new Status(Status.Type.UP_TO_DATE, this, fsReader);
            }
            return new Status(Status.Type.LINK_CONFLICT, this, fsReader);
        }
        if (fsReader.exists(from)) {
            return new Status(Status.Type.FILE_CONFLICT, this, fsReader);
        }
        return new Status(Status.Type.MISSING, this, fsReader);
    }

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
