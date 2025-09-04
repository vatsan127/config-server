package dev.srivatsan.config_server.api;

import dev.srivatsan.config_server.model.BulkSecretsRequest;
import dev.srivatsan.config_server.model.SecretRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Vault Management", description = "APIs for managing encrypted secrets in Git-based vaults with namespace isolation")
public interface VaultAPI {

    @Operation(
            summary = "Store a secret",
            description = "Store a new encrypted secret in the specified namespace vault. The secret value is encrypted using AES-256-GCM and stored with Git version control."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Secret stored successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "message": "Secret stored successfully",
                                        "namespace": "production",
                                        "key": "db_password"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters or validation failure"),
            @ApiResponse(responseCode = "404", description = "Namespace not found"),
            @ApiResponse(responseCode = "409", description = "Secret already exists",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Secret already exists: db_password. Use update instead."
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{namespace}/secrets")
    ResponseEntity<Map<String, Object>> storeSecret(
            @Parameter(description = "Namespace identifier containing the vault", required = true)
            @PathVariable String namespace,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Secret creation request with key, value, and commit information",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Store Secret Example",
                                    value = """
                                            {
                                                "key": "db_password",
                                                "value": "super_secure_password_123",
                                                "email": "admin@company.com",
                                                "commitMessage": "Add database password for production environment"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody SecretRequest request);

    @Operation(
            summary = "Get a secret",
            description = "Retrieve a decrypted secret value from the vault. The secret is decrypted server-side and returned in plain text."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Secret retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "namespace": "production",
                                        "key": "db_password",
                                        "value": "super_secure_password_123"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Secret not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Secret not found: unknown_key"
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{namespace}/secrets/{key}")
    ResponseEntity<Map<String, Object>> getSecret(
            @Parameter(description = "Namespace identifier containing the vault", required = true)
            @PathVariable String namespace,
            @Parameter(description = "Unique identifier of the secret to retrieve", required = true)
            @PathVariable String key);

    @Operation(
            summary = "Update a secret",
            description = "Update an existing encrypted secret in the vault. The secret must already exist - use store operation to create new secrets."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Secret updated successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "message": "Secret updated successfully",
                                        "namespace": "production",
                                        "key": "db_password"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Secret not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{namespace}/secrets/{key}")
    ResponseEntity<Map<String, Object>> updateSecret(
            @Parameter(description = "Namespace identifier containing the vault", required = true)
            @PathVariable String namespace,
            @Parameter(description = "Unique identifier of the secret to update", required = true)
            @PathVariable String key,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Secret update request with new value and commit information",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Update Secret Example",
                                    value = """
                                            {
                                                "key": "db_password",
                                                "value": "new_super_secure_password_456",
                                                "email": "admin@company.com",
                                                "commitMessage": "Update database password for security rotation"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody SecretRequest request);

    @Operation(
            summary = "Delete a secret",
            description = "Delete a secret from the vault permanently. This operation creates a Git commit removing the secret from the latest version."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Secret deleted successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "message": "Secret deleted successfully",
                                        "namespace": "production",
                                        "key": "old_api_key"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Secret not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{namespace}/secrets/{key}")
    ResponseEntity<Map<String, Object>> deleteSecret(
            @Parameter(description = "Namespace identifier containing the vault", required = true)
            @PathVariable String namespace,
            @Parameter(description = "Unique identifier of the secret to delete", required = true)
            @PathVariable String key,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Delete request with commit information",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Delete Secret Example",
                                    value = """
                                            {
                                                "email": "admin@company.com",
                                                "commitMessage": "Remove deprecated API key"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody SecretRequest request);

    @Operation(
            summary = "Get all secrets",
            description = "Retrieve all decrypted secrets from the namespace vault. Use with caution as this returns all secrets in plain text."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All secrets retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "namespace": "production",
                                        "secrets": {
                                            "db_password": "super_secure_password_123",
                                            "api_key": "sk-1234567890abcdef",
                                            "jwt_secret": "my-jwt-signing-secret"
                                        },
                                        "count": 3
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Namespace not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{namespace}/secrets")
    ResponseEntity<Map<String, Object>> getAllSecrets(
            @Parameter(description = "Namespace identifier containing the vault", required = true)
            @PathVariable String namespace);

    @Operation(
            summary = "Store bulk secrets",
            description = "Store multiple secrets in a single atomic Git commit operation. All secrets are processed together - either all succeed or all fail."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bulk secrets stored successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "message": "Bulk secrets stored successfully",
                                        "namespace": "production",
                                        "count": 4
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request or exceeds batch size limits",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "error": "Cannot store more than 100 secrets in a single operation. Provided: 150"
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Namespace not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{namespace}/secrets/bulk")
    ResponseEntity<Map<String, Object>> storeBulkSecrets(
            @Parameter(description = "Namespace identifier containing the vault", required = true)
            @PathVariable String namespace,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Bulk secrets creation request with multiple key-value pairs",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Store Bulk Secrets Example",
                                    value = """
                                            {
                                                "secrets": {
                                                    "db_host": "prod-db.company.com",
                                                    "db_port": "5432",
                                                    "db_username": "app_user",
                                                    "db_password": "secure_password_123"
                                                },
                                                "email": "devops@company.com",
                                                "commitMessage": "Initial database configuration setup"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody BulkSecretsRequest request);

    @Operation(
            summary = "Check if secret exists",
            description = "Check if a specific secret exists in the vault without retrieving its value. Useful for conditional logic and validation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Secret existence check completed",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "Secret exists", value = """
                                            {
                                                "namespace": "production",
                                                "key": "db_password",
                                                "exists": true
                                            }
                                            """),
                                    @ExampleObject(name = "Secret does not exist", value = """
                                            {
                                                "namespace": "production",
                                                "key": "missing_key",
                                                "exists": false
                                            }
                                            """)
                            })),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{namespace}/secrets/{key}/exists")
    ResponseEntity<Map<String, Object>> secretExists(
            @Parameter(description = "Namespace identifier containing the vault", required = true)
            @PathVariable String namespace,
            @Parameter(description = "Unique identifier of the secret to check", required = true)
            @PathVariable String key);

    @Operation(
            summary = "Get vault history",
            description = "Retrieve the Git commit history of vault changes for audit and compliance purposes. Returns commit metadata and messages."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vault history retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                        "namespace": "production",
                                        "vaultFile": ".vault-secrets.json",
                                        "commits": [
                                            {
                                                "commitId": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0",
                                                "author": "admin",
                                                "email": "admin@company.com",
                                                "date": "2024-01-15 10:30:15",
                                                "commitMessage": "Update database password for security rotation"
                                            },
                                            {
                                                "commitId": "b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1",
                                                "author": "devops",
                                                "email": "devops@company.com",
                                                "date": "2024-01-14 09:15:30",
                                                "commitMessage": "Initial database configuration setup"
                                            }
                                        ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Namespace not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{namespace}/history")
    ResponseEntity<Map<String, Object>> getVaultHistory(
            @Parameter(description = "Namespace identifier containing the vault", required = true)
            @PathVariable String namespace);
}