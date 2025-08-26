package dev.srivatsan.config_server.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@Tag(name = "Namespace Management", description = "APIs for managing configuration namespaces and directory operations")
public interface NamespaceAPI {

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
    @PostMapping("/create")
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
            description = "Retrieves a list of all available namespaces in the configuration server (POST request for consistency)"
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
    @PostMapping("/list")
    ResponseEntity<List<String>> listNamespaces(
            @Parameter(description = "Empty request to list all namespaces", required = false)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Optional empty request body (can be empty {})",
                    required = false,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "List Namespaces Example",
                                    value = "{}"
                            )
                    )
            )
            @RequestBody(required = false) Map<String, String> request);

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