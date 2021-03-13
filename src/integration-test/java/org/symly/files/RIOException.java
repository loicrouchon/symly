package org.symly.files;

import java.io.IOException;

public class RIOException extends RuntimeException {

    public RIOException(String message) {
        super(message);
    }

    public RIOException(IOException e) {
        super(e);
    }
}
