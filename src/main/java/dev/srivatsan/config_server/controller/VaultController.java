package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.api.VaultAPI;
import dev.srivatsan.config_server.service.vault.GitVaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vault/{namespace}")
public class VaultController implements VaultAPI {

    private static final Logger log = LoggerFactory.getLogger(VaultController.class);
    
    private final GitVaultService gitVaultService;

    public VaultController(GitVaultService gitVaultService) {
        this.gitVaultService = gitVaultService;
    }

    @Override
    public ResponseEntity<Map<String, Object>> getVault(@PathVariable String namespace) {
        Map<String, String> secrets = gitVaultService.getVault(namespace);
        return ResponseEntity.ok(Map.of(
            "namespace", namespace,
            "secrets", secrets,
            "count", secrets.size()
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateVault(@PathVariable String namespace, @RequestBody Map<String, String> request) {
        
        // Extract secrets, email, and commitMessage from request
        Map<String, String> secrets = new HashMap<>(request);
        String email = secrets.remove("email");
        String commitMessage = secrets.remove("commitMessage");
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Commit message is required");
        }
        
        log.info("Updating vault with {} secrets in namespace '{}'", secrets.size(), namespace);
        
        gitVaultService.updateVault(namespace, secrets, email, commitMessage);
        
        return ResponseEntity.ok(Map.of(
            "message", "Vault updated successfully",
            "namespace", namespace,
            "count", secrets.size()
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getVaultHistory(@PathVariable String namespace) {
        log.info("Getting vault history for namespace '{}'", namespace);
        
        Map<String, Object> history = gitVaultService.getVaultHistory(namespace);
        
        return ResponseEntity.ok(history);
    }
}