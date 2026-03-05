package com.plainquery.exception;

public final class SqlValidationException extends Exception {

    public SqlValidationException(String message) {
        super(message);
    }

    public SqlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
