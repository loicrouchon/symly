package org.linky.links;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.linky.files.FilesReaderService;

@RequiredArgsConstructor
public class Status {

    @Getter
    private final Type type;

    @Getter
    private final Link link;

    private final FilesReaderService filesReaderService;

    public Action toAction() {
        switch (type) {
            case UP_TO_DATE:
                return Action.upToDate(link, filesReaderService);
            case LINK_CONFLICT:
                return Action.replace(link, filesReaderService);
            case FILE_CONFLICT:
                return Action.conflict(link, filesReaderService);
            case MISSING:
                return Action.create(link, filesReaderService);
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
