package com.github.config_server.exception;

public abstract class ConfigServerException extends RuntimeException {

    private final String errorCode;

    protected ConfigServerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ConfigServerException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}