package dev.srivatsan.config_server.api;

import dev.srivatsan.config_server.model.ChangeEntry;
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

@Tag(name = "Change Log Management", description = "APIs for retrieving cached configuration change logs and history")
public interface ChangeLogAPI {

    @Operation(
            summary = "Get cached configuration changes (Fast)",
            description = "Retrieves the last configuration changes from in-memory cache for optimal performance. This endpoint provides faster access to recent changes compared to Git history queries."
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
    @PostMapping("")
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
}