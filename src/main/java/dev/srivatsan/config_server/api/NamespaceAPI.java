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

/**
 * Namespace Management API Interface
 * 
 * <p>Provides REST API endpoints for managing configuration namespaces and directory operations.
 * Namespaces provide complete isolation between different environments, applications, or teams.</p>
 * 
 * <h2>Core Features</h2>
 * <ul>
 *   <li><strong>Namespace Isolation</strong> - Complete isolation between different environments</li>
 *   <li><strong>Git Repository Management</strong> - Each namespace is a separate Git repository</li>
 *   <li><strong>Directory Operations</strong> - Browse and manage directory structures within namespaces</li>
 *   <li><strong>Secure Deletion</strong> - Safe namespace deletion with all associated data</li>
 *   <li><strong>Automatic Initialization</strong> - Automated setup of Git repositories and encryption keys</li>
 * </ul>
 * 
 * <h2>Namespace Lifecycle</h2>
 * <ol>
 *   <li><strong>Creation</strong> - Initialize Git repository and encryption keys</li>
 *   <li><strong>Usage</strong> - Store configuration files and vault secrets</li>
 *   <li><strong>Management</strong> - Browse directories and manage content</li>
 *   <li><strong>Deletion</strong> - Clean removal of all namespace data</li>
 * </ol>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>Each namespace has its own encryption key for vault secrets</li>
 *   <li>Directory permissions are set securely during namespace creation</li>
 *   <li>Deletion operations are irreversible - use with caution</li>
 *   <li>File browsing is restricted to .yml files and directories</li>
 * </ul>
 * 
 * @author Config Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see dev.srivatsan.config_server.controller.NamespaceController
 * @see dev.srivatsan.config_server.service.repository.GitRepositoryService
 */
@Tag(name = "Namespace Management", description = "APIs for managing configuration namespaces and directory operations")
public interface NamespaceAPI {

    @Operation(
            summary = "Create new namespace",
            description = "Creates a new namespace directory with Git initialization for configuration isolation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Namespace created successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Namespace has been created successfully and is ready for configuration files\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid namespace name"),
            @ApiResponse(responseCode = "409", description = "Namespace already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create")
    ResponseEntity<Map<String, Object>> createNamespace(
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
                                      "user-service",
                                      "payment-service"
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
                                                "namespace": "test",
                                                "path": "/"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Map<String, String> request);

    @Operation(
            summary = "Delete namespace",
            description = "Deletes an existing namespace directory and all its contents permanently"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Namespace deleted successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"message\": \"Namespace has been deleted successfully\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid namespace name"),
            @ApiResponse(responseCode = "404", description = "Namespace not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteNamespace(
            @Parameter(description = "Namespace deletion request", required = true)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Request to delete a namespace",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Delete Namespace Example",
                                    value = """
                                            {
                                                "namespace": "test"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Map<String, String> request);
}