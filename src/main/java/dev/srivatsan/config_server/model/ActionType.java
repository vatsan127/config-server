package dev.srivatsan.config_server.model;

/**
 * Enumeration defining the supported configuration management operations.
 * Each action type corresponds to a specific API endpoint and operation.
 */
public enum ActionType {
    
    /**
     * Creates a new configuration file for an application.
     * Initializes the file with a default YAML template and commits it to Git.
     * Used by: POST /config/create
     */
    create,
    
    /**
     * Retrieves the current content of an existing configuration file.
     * Returns the file content along with success status.
     * Used by: POST /config/fetch
     */
    fetch,
    
    /**
     * Updates an existing configuration file with new content.
     * Validates YAML format and commits changes to Git with a custom message.
     * Used by: POST /config/update
     */
    update,
    
    /**
     * Retrieves the commit history for a specific configuration file.
     * Returns a list of commits with author, date, and commit message information.
     * Used by: POST /config/history
     */
    history,
    
    /**
     * Retrieves detailed changes for a specific Git commit.
     * Shows the diff information and commit metadata for a given commit ID.
     * Used by: POST /config/changes
     */
    changes
}
