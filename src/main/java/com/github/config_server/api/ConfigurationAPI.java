package com.github.config_server.api;

import com.github.config_server.model.Payload;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.Map;

/**
 * REST API interface for configuration file management operations.
 */
@RequestMapping("/config")
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
     */
    @PostMapping("/create")
    ResponseEntity<Map<String, Object>> createConfig(@Valid @RequestBody Payload request);

    /**
     * Retrieves an existing configuration file.
     */
    @PostMapping("/fetch")
    ResponseEntity<Payload> fetchConfig(@Valid @RequestBody Payload payload) throws IOException;

    /**
     * Updates an existing configuration file with new content.
     */
    @PostMapping("/update")
    ResponseEntity<Map<String, Object>> updateConfig(@Valid @RequestBody Payload payload);

    /**
     * Retrieves the complete commit history for a specific configuration file.
     */
    @PostMapping("/history")
    ResponseEntity<Map<String, Object>> getCommitHistory(@Valid @RequestBody Payload payload) throws Exception;

    /**
     * Retrieves detailed information about a specific commit.
     */
    @PostMapping("/changes")
    ResponseEntity<Map<String, Object>> getCommitDetails(@Valid @RequestBody Payload payload) throws IOException;

    /**
     * Deletes an existing configuration file from the namespace.
     */
    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteConfig(@Valid @RequestBody Payload payload);

}