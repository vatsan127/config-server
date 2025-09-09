package dev.srivatsan.config_server.constants;

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
    changes,

    /**
     * Deletes an existing configuration file.
     * Removes the file from the filesystem and commits the change to Git.
     * Used by: POST /config/delete
     */
    delete,

    /**
     * Retrieves the event history (git log) for an entire namespace.
     * Returns commit history for all files within the namespace directory.
     * Used by: POST /namespace/events
     */
    events,

    /**
     * Retrieves API call status notifications for the last commit-history-size operations.
     * Returns status information including trigger time, app name, success/failure counts, and status
     * (success, in-progress, failed) for each namespace.
     * Used by: POST /namespace/notify
     */
    notify,

    /**
     * Triggers refresh notification API calls for a specific namespace and commit.
     * Manually initiates notification calls that can be used when automatic notifications fail.
     * The app name is automatically extracted from the commit details.
     * Used by: POST /namespace/trigger-notify
     */
    triggerNotify
}
