package com.plainquery.exception;

public final class SqlExtractionException extends Exception {

    public SqlExtractionException(String message) {
        super(message);
    }

    public SqlExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
