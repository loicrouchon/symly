package org.linky.links;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.files.FileSystemReader;

@RequiredArgsConstructor
public class Status {

    @Getter
    private final Type type;

    @Getter
    private final Link link;

    private final FileSystemReader fsReader;

    public Action toAction() {
        switch (type) {
            case UP_TO_DATE:
                return Action.upToDate(link, fsReader);
            case LINK_CONFLICT:
                return Action.replace(link, fsReader);
            case FILE_CONFLICT:
                return Action.conflict(link, fsReader);
            case MISSING:
                return Action.create(link, fsReader);
            default:
                throw new UnsupportedOperationException("Unknown Status type " + type);
        }
    }

    public enum Type {
        UP_TO_DATE,
        LINK_CONFLICT,
        FILE_CONFLICT,
        MISSING;

        public static final int MAX_LENGTH = 13;
    }
}
