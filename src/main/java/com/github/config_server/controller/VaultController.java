package com.github.config_server.controller;


import com.github.config_server.api.VaultAPI;
import com.github.config_server.service.vault.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/vault")
public class VaultController implements VaultAPI {

    private static final Logger log = LoggerFactory.getLogger(VaultController.class);

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Override
    public ResponseEntity<Map<String, Object>> getVault(@RequestBody Map<String, String> request) {
        String namespace = request.get("namespace");
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace is required");
        }

        Map<String, String> secrets = vaultService.getVault(namespace);
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
        vaultService.updateVault(namespace, requestData, email, commitMessage);
        return ResponseEntity.ok(Map.of(
                "message", "Vault updated successfully",
                "namespace", namespace,
                "count", requestData.size()
        ));
    }


}