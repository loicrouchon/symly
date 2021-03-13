package org.symly.cli;

public class SymlyExecutionException extends RuntimeException {

    public SymlyExecutionException(String message) {
        super(message);
    }

    public SymlyExecutionException(Exception cause) {
        super(cause);
    }

    public SymlyExecutionException(String message, Exception cause) {
        super(message, cause);
    }
}
