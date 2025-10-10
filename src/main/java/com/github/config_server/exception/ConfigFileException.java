package com.github.config_server.exception;

public class ConfigFileException extends ConfigServerException {

    public static final String CONFIG_FILE_NOT_FOUND = "CONFIG_FILE_NOT_FOUND";
    public static final String CONFIG_FILE_CREATION_FAILED = "CONFIG_FILE_CREATION_FAILED";
    public static final String CONFIG_FILE_UPDATE_FAILED = "CONFIG_FILE_UPDATE_FAILED";
    public static final String CONFIG_FILE_READ_FAILED = "CONFIG_FILE_READ_FAILED";
    public static final String CONFIG_FILE_ALREADY_EXISTS = "CONFIG_FILE_ALREADY_EXISTS";

    public ConfigFileException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ConfigFileException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static ConfigFileException notFound(String filePath) {
        return new ConfigFileException(CONFIG_FILE_NOT_FOUND, "Configuration file not found: " + filePath);
    }

    public static ConfigFileException creationFailed(String filePath, Throwable cause) {
        return new ConfigFileException(CONFIG_FILE_CREATION_FAILED, "Failed to create configuration file: " + filePath, cause);
    }

    public static ConfigFileException updateFailed(String filePath, Throwable cause) {
        return new ConfigFileException(CONFIG_FILE_UPDATE_FAILED, "Failed to update configuration file: " + filePath, cause);
    }

    public static ConfigFileException readFailed(String filePath, Throwable cause) {
        return new ConfigFileException(CONFIG_FILE_READ_FAILED, "Failed to read configuration file: " + filePath, cause);
    }

    public static ConfigFileException alreadyExists(String filePath) {
        return new ConfigFileException(CONFIG_FILE_ALREADY_EXISTS, "Configuration file already exists: " + filePath);
    }
}