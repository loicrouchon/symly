package org.linky.files;

import java.io.IOException;

public class RIOException extends RuntimeException {

    public RIOException(IOException e) {
        super(e);
    }
}
