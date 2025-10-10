package com.github.config_server.exception;

public class NamespaceException extends ConfigServerException {

    public static final String NAMESPACE_NOT_FOUND = "NAMESPACE_NOT_FOUND";
    public static final String NAMESPACE_CREATION_FAILED = "NAMESPACE_CREATION_FAILED";
    public static final String NAMESPACE_ALREADY_EXISTS = "NAMESPACE_ALREADY_EXISTS";

    public NamespaceException(String errorCode, String message) {
        super(errorCode, message);
    }

    public NamespaceException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static NamespaceException notFound(String namespace) {
        return new NamespaceException(NAMESPACE_NOT_FOUND, "Namespace '" + namespace + "' does not exist. Please create it first using /namespace/create endpoint.");
    }

    public static NamespaceException creationFailed(String namespace, Throwable cause) {
        return new NamespaceException(NAMESPACE_CREATION_FAILED, "Failed to create namespace '" + namespace + "'", cause);
    }

    public static NamespaceException alreadyExists(String namespace) {
        return new NamespaceException(NAMESPACE_ALREADY_EXISTS, "Namespace '" + namespace + "' already exists");
    }
}