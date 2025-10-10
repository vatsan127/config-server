package com.github.config_server.constants;

/**
 * Enumeration defining the supported configuration management operations.
 * Each action type corresponds to a specific API endpoint and operation.
 */
public enum ActionType {

    /**
     * Creates a new configuration file for an application.
     * Used by: POST /config/create
     */
    create,

    /**
     * Retrieves the current content of an existing configuration file.
     * Used by: POST /config/fetch
     */
    fetch,

    /**
     * Updates an existing configuration file with new content.
     * Used by: POST /config/update
     */
    update,

    /**
     * Retrieves the commit history for a specific configuration file.
     * Used by: POST /config/history
     */
    history,

    /**
     * Retrieves detailed changes for a specific Git commit.
     * Used by: POST /config/changes
     */
    changes,

    /**
     * Deletes an existing configuration file.
     * Used by: POST /config/delete
     */
    delete,

}
