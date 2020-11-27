package org.linky.cli;

public class LinkyExecutionException extends RuntimeException {

    public LinkyExecutionException(String message) {
        super(message);
    }

    public LinkyExecutionException(Exception cause) {
        super(cause);
    }

    public LinkyExecutionException(String message, Exception cause) {
        super(message, cause);
    }
}
