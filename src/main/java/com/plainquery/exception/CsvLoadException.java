package com.plainquery.exception;

public final class CsvLoadException extends Exception {

    public CsvLoadException(String message) {
        super(message);
    }

    public CsvLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
