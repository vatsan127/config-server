package dev.srivatsan.config_server.api;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
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
            @Valid @RequestBody Payload payload);

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
            @Parameter(description = "Namespace creation request", required = true, 
                      example = "{\"namespace\": \"production\"}")
            @RequestBody Map<String, String> request) throws Exception;
}