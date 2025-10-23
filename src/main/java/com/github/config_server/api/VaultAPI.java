package com.github.config_server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * REST API interface for vault operations providing secure storage and management of encrypted secrets.
 */
@RequestMapping("/vault")
public interface VaultAPI {

    /**
     * Retrieves all secrets from the vault for a specified namespace.
     */
    @PostMapping("/get")
    ResponseEntity<Map<String, Object>> getVault(@RequestBody Map<String, String> request) throws Exception;

    /**
     * Updates or creates secrets in the vault for a specified namespace.
     */
    @PostMapping("/update")
    ResponseEntity<Map<String, Object>> updateVault(@RequestBody Map<String, String> request) throws Exception;

}