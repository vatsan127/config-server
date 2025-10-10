package com.github.config_server.exception;

public class ValidationException extends ConfigServerException {

    public static final String INVALID_ACTION_TYPE = "INVALID_ACTION_TYPE";
    public static final String INVALID_PATH = "INVALID_PATH";
    public static final String INVALID_NAMESPACE = "INVALID_NAMESPACE";
    public static final String INVALID_APP_NAME = "INVALID_APP_NAME";
    public static final String INVALID_EMAIL = "INVALID_EMAIL";
    public static final String INVALID_COMMIT_ID = "INVALID_COMMIT_ID";
    public static final String INVALID_CONTENT = "INVALID_CONTENT";
    public static final String INVALID_YAML = "INVALID_YAML";
    public static final String INVALID_COMMIT_MESSAGE = "INVALID_COMMIT_MESSAGE";
    public static final String MISSING_COMMIT_ID = "MISSING_COMMIT_ID";

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static ValidationException invalidActionType(String expected, String actual) {
        return new ValidationException(INVALID_ACTION_TYPE,
                "Invalid action type. Expected: " + expected + ", but got: " + actual);
    }

    public static ValidationException invalidPath(String path, String reason) {
        return new ValidationException(INVALID_PATH,
                "Invalid path '" + path + "': " + reason);
    }

    public static ValidationException invalidNamespace(String namespace, String reason) {
        return new ValidationException(INVALID_NAMESPACE,
                "Invalid namespace '" + namespace + "': " + reason);
    }

    public static ValidationException invalidAppName(String appName, String reason) {
        return new ValidationException(INVALID_APP_NAME,
                "Invalid application name '" + appName + "': " + reason);
    }

    public static ValidationException invalidEmail(String email) {
        return new ValidationException(INVALID_EMAIL,
                "Invalid email format: " + email);
    }

    public static ValidationException invalidCommitId(String commitId) {
        return new ValidationException(INVALID_COMMIT_ID,
                "Invalid or missing commit ID: " + commitId);
    }

    public static ValidationException invalidContent(String message) {
        return new ValidationException(INVALID_CONTENT, message);
    }

    public static ValidationException invalidYaml(String message) {
        return new ValidationException(INVALID_YAML, message);
    }

    public static ValidationException invalidCommitMessage(String message) {
        return new ValidationException(INVALID_COMMIT_MESSAGE, message);
    }

    public static ValidationException missingCommitId(String message) {
        return new ValidationException(MISSING_COMMIT_ID, message);
    }
}