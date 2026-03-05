package com.plainquery.exception;

public final class AiConnectorException extends Exception {

    public AiConnectorException(String message) {
        super(message);
    }

    public AiConnectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
