package dev.srivatsan.config_server.api;

import dev.srivatsan.config_server.model.Payload;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.Map;

/**
 * REST API interface for configuration file management operations.
 * Provides comprehensive CRUD operations for configuration files with
 * version control, secret processing, and history tracking capabilities.
 */
public interface ConfigurationAPI {

    /**
     * Success message returned when a configuration file is created
     */
    String CONFIG_CREATED_MESSAGE = "Configuration file has been created successfully";

    /**
     * Success message returned when a configuration file is updated
     */
    String CONFIG_UPDATED_MESSAGE = "Configuration file has been updated successfully";

    /**
     * Success message returned when a configuration file is deleted
     */
    String CONFIG_DELETED_MESSAGE = "Configuration file has been deleted successfully";

    /**
     * Creates a new configuration file within the specified namespace.
     * Validates the payload, processes any encrypted secrets, and commits
     * the new configuration to version control.
     *
     * @param request validated payload containing namespace, file path, and configuration content
     * @return ResponseEntity containing creation result and metadata
     */
    @PostMapping("/create")
    ResponseEntity<Map<String, Object>> createConfig(@Valid @RequestBody Payload request);

    /**
     * Retrieves an existing configuration file with processed secret values.
     * Decrypts encrypted secrets and returns the complete configuration
     * ready for client consumption.
     *
     * @param payload validated payload containing namespace and file path
     * @return ResponseEntity containing the processed configuration payload
     * @throws IOException if file reading or processing fails
     */
    @PostMapping("/fetch")
    ResponseEntity<Payload> fetchConfig(@Valid @RequestBody Payload payload) throws IOException;

    /**
     * Updates an existing configuration file with new content.
     * Processes encrypted secrets, validates changes, and creates
     * a new commit in version control with the updates.
     *
     * @param payload validated payload containing updated configuration content
     * @return ResponseEntity containing update result and metadata
     */
    @PostMapping("/update")
    ResponseEntity<Map<String, Object>> updateConfig(@Valid @RequestBody Payload payload);

    /**
     * Retrieves the complete commit history for a specific configuration file.
     * Returns Git history with timestamps, authors, and commit messages
     * for audit and tracking purposes.
     *
     * @param payload validated payload containing namespace and file path
     * @return ResponseEntity containing commit history details
     * @throws Exception if history retrieval fails or file is not found
     */
    @PostMapping("/history")
    ResponseEntity<Map<String, Object>> getCommitHistory(@Valid @RequestBody Payload payload) throws Exception;

    /**
     * Retrieves detailed information about a specific commit.
     * Shows the exact changes made in a particular commit including
     * file differences and metadata for audit purposes.
     *
     * @param payload validated payload containing commit ID and file information
     * @return ResponseEntity containing detailed commit information
     * @throws IOException if commit details cannot be retrieved
     */
    @PostMapping("/changes")
    ResponseEntity<Map<String, Object>> getCommitDetails(@Valid @RequestBody Payload payload) throws IOException;

    /**
     * Deletes an existing configuration file from the namespace.
     * Removes the file from the filesystem and creates a deletion
     * commit in version control for tracking purposes.
     *
     * @param payload validated payload containing namespace and file path to delete
     * @return ResponseEntity containing deletion result and metadata
     */
    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteConfig(@Valid @RequestBody Payload payload);

}