package com.plainquery.exception;

public final class InsufficientMemoryException extends Exception {

    public InsufficientMemoryException(String message) {
        super(message);
    }

    public InsufficientMemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
