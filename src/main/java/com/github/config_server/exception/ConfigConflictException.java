package com.github.config_server.exception;

public class ConfigConflictException extends ConfigServerException {

    public static final String CONFIG_CONFLICT = "CONFIG_CONFLICT";

    public ConfigConflictException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ConfigConflictException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static ConfigConflictException conflictDetected(String fileName) {
        return new ConfigConflictException(CONFIG_CONFLICT,
                String.format("Configuration file '%s' was modified!. Please refresh the page and try again.", fileName));
    }
}