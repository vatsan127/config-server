package dev.srivatsan.config_server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * REST API interface for namespace management operations.
 * Provides functionality to create, list, browse, and delete namespaces
 * which serve as isolated environments for configuration management.
 */
public interface NamespaceAPI {

    /**
     * Success message returned when a namespace is created successfully
     */
    String NAMESPACE_CREATED_MESSAGE = "Namespace has been created successfully and is ready for configuration files";

    /**
     * Success message returned when a namespace is deleted successfully
     */
    String NAMESPACE_DELETED_MESSAGE = "Namespace has been deleted successfully";

    /**
     * Creates a new namespace for organizing configuration files and secrets.
     * Initializes the necessary directory structure and encryption keys for the namespace.
     *
     * @param request a map containing namespace name and configuration parameters
     * @return ResponseEntity containing creation result and metadata
     * @throws Exception if namespace creation fails or name conflicts exist
     */
    @PostMapping("/create")
    ResponseEntity<Map<String, Object>> createNamespace(@RequestBody Map<String, String> request) throws Exception;

    /**
     * Retrieves a list of all available namespaces in the system.
     * Returns namespace names that can be used for configuration operations.
     *
     * @return ResponseEntity containing a list of namespace names
     */
    @PostMapping("/list")
    ResponseEntity<List<String>> listNamespaces();

    /**
     * Lists the contents of a specific directory within a namespace.
     * Useful for browsing configuration files and directory structure.
     *
     * @param request a map containing namespace and directory path parameters
     * @return ResponseEntity containing a list of files and directories
     */
    @PostMapping("/files")
    ResponseEntity<List<String>> listDirectoryContents(@RequestBody Map<String, String> request);

    /**
     * Deletes an existing namespace and all its associated data.
     * This operation removes configuration files, secrets, and encryption keys.
     * Use with caution as this action is irreversible.
     *
     * @param request a map containing the namespace name to delete
     * @return ResponseEntity containing deletion result and metadata
     */
    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteNamespace(@RequestBody Map<String, String> request);

    /**
     * Retrieves the complete event history (git log) for an entire namespace.
     * Returns Git history with timestamps, authors, and commit messages
     * for all files and activities within the namespace root directory.
     * The number of events returned is limited by the commit-history-size configuration.
     *
     * @param request a map containing the namespace name
     * @return ResponseEntity containing namespace event history details
     * @throws Exception if event history retrieval fails or namespace is not found
     */
    @PostMapping("/events")
    ResponseEntity<Map<String, Object>> getNamespaceEvents(@RequestBody Map<String, String> request) throws Exception;

    /**
     * Retrieves API call status notifications for the last commit-history-size operations.
     * Returns status information including trigger time, app name in payload, retry count,
     * and status (success, in-progress when retry is happening, failed) for each namespace operation.
     * The number of notifications returned is limited by the commit-history-size configuration.
     *
     * @param request a map containing the namespace name
     * @return ResponseEntity containing notification status details
     * @throws Exception if notification retrieval fails or namespace is not found
     */
    @PostMapping("/notify")
    ResponseEntity<Map<String, Object>> getNamespaceNotifications(@RequestBody Map<String, String> request) throws Exception;
}