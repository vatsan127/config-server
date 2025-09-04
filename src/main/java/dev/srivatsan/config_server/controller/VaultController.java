package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.model.SecretRequest;
import dev.srivatsan.config_server.model.BulkSecretsRequest;
import dev.srivatsan.config_server.service.vault.GitVaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vault")
@Tag(name = "Vault Management", description = "APIs for managing encrypted secrets in the vault")
public class VaultController {

    private static final Logger log = LoggerFactory.getLogger(VaultController.class);
    
    private final GitVaultService gitVaultService;

    public VaultController(GitVaultService gitVaultService) {
        this.gitVaultService = gitVaultService;
    }

    @PostMapping("/{namespace}/secrets")
    @Operation(summary = "Store a secret", description = "Store a new secret in the specified namespace vault")
    public ResponseEntity<Map<String, Object>> storeSecret(
            @Parameter(description = "Namespace identifier") @PathVariable String namespace,
            @Valid @RequestBody SecretRequest request) {
        
        log.info("Storing secret '{}' in namespace '{}'", request.getKey(), namespace);
        
        gitVaultService.storeSecret(namespace, request.getKey(), request.getValue(), 
                                   request.getEmail(), request.getCommitMessage());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "Secret stored successfully",
                    "namespace", namespace,
                    "key", request.getKey()
                ));
    }

    @GetMapping("/{namespace}/secrets/{key}")
    @Operation(summary = "Get a secret", description = "Retrieve a decrypted secret value from the vault")
    public ResponseEntity<Map<String, Object>> getSecret(
            @Parameter(description = "Namespace identifier") @PathVariable String namespace,
            @Parameter(description = "Secret key") @PathVariable String key) {
        
        log.info("Getting secret '{}' from namespace '{}'", key, namespace);
        
        String value = gitVaultService.getSecret(namespace, key);
        
        return ResponseEntity.ok(Map.of(
            "namespace", namespace,
            "key", key,
            "value", value
        ));
    }

    @PutMapping("/{namespace}/secrets/{key}")
    @Operation(summary = "Update a secret", description = "Update an existing secret in the vault")
    public ResponseEntity<Map<String, Object>> updateSecret(
            @Parameter(description = "Namespace identifier") @PathVariable String namespace,
            @Parameter(description = "Secret key") @PathVariable String key,
            @Valid @RequestBody SecretRequest request) {
        
        log.info("Updating secret '{}' in namespace '{}'", key, namespace);
        gitVaultService.updateSecret(namespace, key, request.getValue(), 
                                    request.getEmail(), request.getCommitMessage());
        
        return ResponseEntity.ok(Map.of(
            "message", "Secret updated successfully",
            "namespace", namespace,
            "key", key
        ));
    }

    @DeleteMapping("/{namespace}/secrets/{key}")
    @Operation(summary = "Delete a secret", description = "Delete a secret from the vault")
    public ResponseEntity<Map<String, Object>> deleteSecret(
            @Parameter(description = "Namespace identifier") @PathVariable String namespace,
            @Parameter(description = "Secret key") @PathVariable String key,
            @Valid @RequestBody SecretRequest request) {
        gitVaultService.deleteSecret(namespace, key, request.getEmail(), request.getCommitMessage());
        return ResponseEntity.ok(Map.of(
            "message", "Secret deleted successfully",
            "namespace", namespace,
            "key", key
        ));
    }

    @GetMapping("/{namespace}/secrets")
    @Operation(summary = "Get all secrets", description = "Retrieve all decrypted secrets from the namespace vault")
    public ResponseEntity<Map<String, Object>> getAllSecrets(
            @Parameter(description = "Namespace identifier") @PathVariable String namespace) {
        Map<String, String> secrets = gitVaultService.getAllSecrets(namespace);
        return ResponseEntity.ok(Map.of(
            "namespace", namespace,
            "secrets", secrets,
            "count", secrets.size()
        ));
    }

    @PostMapping("/{namespace}/secrets/bulk")
    @Operation(summary = "Store bulk secrets", description = "Store multiple secrets in a single operation")
    public ResponseEntity<Map<String, Object>> storeBulkSecrets(
            @Parameter(description = "Namespace identifier") @PathVariable String namespace,
            @Valid @RequestBody BulkSecretsRequest request) {
        
        log.info("Storing {} secrets in namespace '{}'", request.getSecrets().size(), namespace);
        
        gitVaultService.storeBulkSecrets(namespace, request.getSecrets(), 
                                        request.getEmail(), request.getCommitMessage());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "Bulk secrets stored successfully",
                    "namespace", namespace,
                    "count", request.getSecrets().size()
                ));
    }

    @GetMapping("/{namespace}/secrets/{key}/exists")
    @Operation(summary = "Check if secret exists", description = "Check if a secret exists in the vault")
    public ResponseEntity<Map<String, Object>> secretExists(
            @Parameter(description = "Namespace identifier") @PathVariable String namespace,
            @Parameter(description = "Secret key") @PathVariable String key) {
        
        boolean exists = gitVaultService.secretExists(namespace, key);
        
        return ResponseEntity.ok(Map.of(
            "namespace", namespace,
            "key", key,
            "exists", exists
        ));
    }

    @GetMapping("/{namespace}/history")
    @Operation(summary = "Get vault history", description = "Get the commit history of vault changes")
    public ResponseEntity<Map<String, Object>> getVaultHistory(
            @Parameter(description = "Namespace identifier") @PathVariable String namespace) {
        
        log.info("Getting vault history for namespace '{}'", namespace);
        
        Map<String, Object> history = gitVaultService.getVaultHistory(namespace);
        
        return ResponseEntity.ok(history);
    }
}