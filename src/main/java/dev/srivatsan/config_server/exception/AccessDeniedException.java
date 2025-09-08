package dev.srivatsan.config_server.exception;

public class AccessDeniedException extends RuntimeException {

    private final String username;
    private final String resource;
    private final String action;

    public AccessDeniedException(String username, String resource, String action) {
        super(String.format("User '%s' does not have permission to %s '%s'", username, action, resource));
        this.username = username;
        this.resource = resource;
        this.action = action;
    }

    public AccessDeniedException(String message) {
        super(message);
        this.username = null;
        this.resource = null;
        this.action = null;
    }

    public String getUsername() {
        return username;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }
}