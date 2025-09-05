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
@RequestMapping("/api/vault")
public class VaultController implements VaultAPI {

    private static final Logger log = LoggerFactory.getLogger(VaultController.class);
    
    private final GitVaultService gitVaultService;

    public VaultController(GitVaultService gitVaultService) {
        this.gitVaultService = gitVaultService;
    }

    @Override
    public ResponseEntity<Map<String, Object>> getVault(@RequestBody Map<String, String> request) {
        String namespace = request.get("namespace");
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace is required");
        }
        
        Map<String, String> secrets = gitVaultService.getVault(namespace);
        return ResponseEntity.ok(Map.of(
            "namespace", namespace,
            "secrets", secrets,
            "count", secrets.size()
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateVault(@RequestBody Map<String, String> request) {
        Map<String, String> requestData = new HashMap<>(request);
        String namespace = requestData.remove("namespace");
        String email = requestData.remove("email");
        String commitMessage = requestData.remove("commitMessage");
        
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace is required");
        }
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Commit message is required");
        }

        // Remaining entries are the secrets
        gitVaultService.updateVault(namespace, requestData, email, commitMessage);
        return ResponseEntity.ok(Map.of(
            "message", "Vault updated successfully",
            "namespace", namespace,
            "count", requestData.size()
        ));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getVaultHistory(@RequestBody Map<String, String> request) {
        String namespace = request.get("namespace");
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace is required");
        }
        
        Map<String, Object> history = gitVaultService.getVaultHistory(namespace);
        return ResponseEntity.ok(history);
    }
}