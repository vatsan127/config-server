package com.github.config_server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * REST API interface for namespace management operations.
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
     */
    @PostMapping("/create")
    ResponseEntity<Map<String, Object>> createNamespace(@RequestBody Map<String, String> request) throws Exception;

    /**
     * Retrieves a list of all available namespaces in the system.
     */
    @PostMapping("/list")
    ResponseEntity<List<String>> listNamespaces();

    /**
     * Lists the contents of a specific directory within a namespace.
     */
    @PostMapping("/files")
    ResponseEntity<List<String>> listDirectoryContents(@RequestBody Map<String, String> request);

    /**
     * Deletes an existing namespace and all its associated data.
     */
    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteNamespace(@RequestBody Map<String, String> request);

    /**
     * Retrieves the complete event history (git log) for an entire namespace.
     */
    @PostMapping("/events")
    ResponseEntity<Map<String, Object>> getNamespaceEvents(@RequestBody Map<String, String> request) throws Exception;

    /**
     * Retrieves API call status notifications for the last commit-history-size operations.
     */
    @PostMapping("/notify")
    ResponseEntity<Map<String, Object>> getNamespaceNotifications(@RequestBody Map<String, String> request) throws Exception;

}