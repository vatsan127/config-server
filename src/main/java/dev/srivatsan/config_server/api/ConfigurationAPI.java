package dev.srivatsan.config_server.api;

import dev.srivatsan.config_server.model.ChangeEntry;
import dev.srivatsan.config_server.model.DirectoryEntry;
import dev.srivatsan.config_server.model.Payload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "Configuration Management", description = "APIs for managing application configuration files with Git version control")
public interface ConfigurationAPI {

    @Operation(
            summary = "Create a new configuration file",
            description = "Creates a new configuration file for an application with default YAML template and commits it to Git"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Configuration file created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Namespace not found"),
            @ApiResponse(responseCode = "409", description = "Configuration file already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create")
    ResponseEntity<String> createConfig(
            @Parameter(description = "Configuration creation request", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to create a new configuration file",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Create Config Example",
                                    value = """
                                            {
                                                "action": "create",
                                                "appName": "sample",
                                                "namespace": "test",
                                                "path": "/",
                                                "email": "test@gmail.com"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody Payload request);

    @Operation(
            summary = "Fetch configuration file content",
            description = "Retrieves the current content of a configuration file"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration file retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Configuration file or namespace not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/fetch")
    ResponseEntity<Payload> fetchConfig(
            @Parameter(description = "Configuration fetch request", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to fetch configuration file content",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Fetch Config Example",
                                    value = """
                                            {
                                                "action": "fetch",
                                                "appName": "sample",
                                                "namespace": "test",
                                                "path": "/",
                                                "email": "test@gmail.com"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody Payload payload) throws IOException;

    @Operation(
            summary = "Update configuration file content",
            description = "Updates an existing configuration file with new content and commits changes to Git"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration updated successfully",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "success"))),
            @ApiResponse(responseCode = "400", description = "Invalid request or YAML content"),
            @ApiResponse(responseCode = "404", description = "Configuration file not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/update")
    ResponseEntity<String> updateConfig(
            @Parameter(description = "Configuration update request with content and commit message", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to update configuration file with new content",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Update Config Example",
                                    value = """
                                            {
                                                "action": "update",
                                                "appName": "sample",
                                                "namespace": "test",
                                                "path": "/",
                                                "content": "server:\\n  port: 8081\\n\\nspring:\\n  application:\\n    name: abc",
                                                "message": "commit for updating app config",
                                                "email": "test@gmail.com"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody Payload payload);

    @Operation(
            summary = "Get cached configuration changes (Fast)",
            description = "Retrieves the last 20 configuration changes from in-memory cache for optimal performance"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cached changes retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "commitId": "abc123def456",
                                        "message": "Update user service port",
                                        "author": "developer",
                                        "email": "developer@company.com",
                                        "modifiedTime": "2024-01-15 10:30:00",
                                        "fileName": "user-service.yml",
                                        "changes": "--- a/user-service.yml\\n+++ b/user-service.yml\\n@@ -1,4 +1,4 @@\\n server:\\n-  port: 8080\\n+  port: 8081"
                                      }
                                    ]
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid namespace"),
            @ApiResponse(responseCode = "404", description = "Namespace not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/changes/cached")
    ResponseEntity<List<ChangeEntry>> getCachedChanges(
            @Parameter(description = "Namespace to get cached changes for", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to get cached changes for a namespace",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Get Cached Changes Example",
                                    value = """
                                            {
                                                "namespace": "test"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Map<String, String> request);

    @Operation(
            summary = "Get configuration file commit history",
            description = "Retrieves the commit history for a specific configuration file"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "filePath": "production/config/user-service.yml",
                                      "commits": [
                                        {
                                          "commitId": "abc123def456",
                                          "author": "developer",
                                          "email": "developer@company.com",
                                          "date": "2024-01-15 10:30:00",
                                          "message": "Update user service port"
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Configuration file not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/history")
    ResponseEntity<Map<String, Object>> getCommitHistory(
            @Parameter(description = "Configuration history request", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to get commit history for a configuration file",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Get History Example",
                                    value = """
                                            {
                                                "action": "history",
                                                "appName": "sample",
                                                "namespace": "test",
                                                "path": "/",
                                                "email": "test@gmail.com"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody Payload payload) throws Exception;

    @Operation(
            summary = "Get specific commit changes",
            description = "Retrieves detailed changes for a specific commit ID including diff information"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Commit changes retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "commitId": "abc123def456",
                                      "message": "Update user service port configuration",
                                      "author": "developer",
                                      "commitTime": "2024-01-15T10:30:00Z",
                                      "changes": "--- a/config/user-service.yml\\n+++ b/config/user-service.yml\\n@@ -1,4 +1,4 @@\\n server:\\n-  port: 8080\\n+  port: 8081"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid commit ID"),
            @ApiResponse(responseCode = "404", description = "Commit not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/changes")
    ResponseEntity<Map<String, Object>> getCommitDetails(
            @Parameter(description = "Request with commit ID to get changes for", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to get detailed changes for a specific commit",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Get Changes Example",
                                    value = """
                                            {
                                                "action": "changes",
                                                "appName": "sample",
                                                "namespace": "test",
                                                "path": "/",
                                                "email": "test@gmail.com",
                                                "commitId": "de2c57c02c091da9e61546db416142fe81f84dd3"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody Payload payload) throws IOException;

    @Operation(
            summary = "Create new namespace",
            description = "Creates a new namespace directory with Git initialization for configuration isolation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Namespace created successfully",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Namespace created successfully"))),
            @ApiResponse(responseCode = "400", description = "Invalid namespace name"),
            @ApiResponse(responseCode = "409", description = "Namespace already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/namespace/create")
    ResponseEntity<String> createNamespace(
            @Parameter(description = "Namespace creation request", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to create a new namespace",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Create Namespace Example",
                                    value = """
                                            {
                                                "namespace": "test"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Map<String, String> request) throws Exception;

    @Operation(
            summary = "List all namespaces",
            description = "Retrieves a list of all available namespaces in the configuration server"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Namespaces retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    [
                                      "production",
                                      "staging", 
                                      "development",
                                      "test"
                                    ]
                                    """))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/namespaces")
    ResponseEntity<List<String>> listNamespaces();

    @Operation(
            summary = "List directory contents",
            description = "Retrieves the list of .yml files and subdirectories within a specified directory path in a namespace"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Directory contents retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    [
                                      "services",
                                      "user-service.yml",
                                      "database.yml"
                                    ]
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Namespace or directory not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/files")
    ResponseEntity<List<String>> listDirectoryContents(
            @Parameter(description = "Directory listing request", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to list .yml files and directories in a namespace",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "List Files Example",
                                    value = """
                                            {
                                                "namespace": "production",
                                                "path": "config"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Map<String, String> request);
}