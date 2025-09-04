package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.api.VaultAPI;
import dev.srivatsan.config_server.model.SecretRequest;
import dev.srivatsan.config_server.model.BulkSecretsRequest;
import dev.srivatsan.config_server.service.vault.GitVaultService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vault")
public class VaultController implements VaultAPI {

    private static final Logger log = LoggerFactory.getLogger(VaultController.class);
    
    private final GitVaultService gitVaultService;

    public VaultController(GitVaultService gitVaultService) {
        this.gitVaultService = gitVaultService;
    }

    @Override
    public ResponseEntity<Map<String, Object>> storeSecret(
            @PathVariable String namespace,
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

    @Override
    public ResponseEntity<Map<String, Object>> getSecret(
            @PathVariable String namespace,
            @PathVariable String key) {
        
        log.info("Getting secret '{}' from namespace '{}'", key, namespace);
        
        String value = gitVaultService.getSecret(namespace, key);
        
        return ResponseEntity.ok(Map.of(
            "namespace", namespace,
            "key", key,
            "value", value
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateSecret(
            @PathVariable String namespace,
            @PathVariable String key,
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

    @Override
    public ResponseEntity<Map<String, Object>> deleteSecret(
            @PathVariable String namespace,
            @PathVariable String key,
            @Valid @RequestBody SecretRequest request) {
        gitVaultService.deleteSecret(namespace, key, request.getEmail(), request.getCommitMessage());
        return ResponseEntity.ok(Map.of(
            "message", "Secret deleted successfully",
            "namespace", namespace,
            "key", key
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getAllSecrets(
            @PathVariable String namespace) {
        Map<String, String> secrets = gitVaultService.getAllSecrets(namespace);
        return ResponseEntity.ok(Map.of(
            "namespace", namespace,
            "secrets", secrets,
            "count", secrets.size()
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> storeBulkSecrets(
            @PathVariable String namespace,
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

    @Override
    public ResponseEntity<Map<String, Object>> secretExists(
            @PathVariable String namespace,
            @PathVariable String key) {
        
        boolean exists = gitVaultService.secretExists(namespace, key);
        
        return ResponseEntity.ok(Map.of(
            "namespace", namespace,
            "key", key,
            "exists", exists
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getVaultHistory(
            @PathVariable String namespace) {
        
        log.info("Getting vault history for namespace '{}'", namespace);
        
        Map<String, Object> history = gitVaultService.getVaultHistory(namespace);
        
        return ResponseEntity.ok(history);
    }
}