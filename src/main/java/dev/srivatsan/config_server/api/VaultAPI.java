package dev.srivatsan.config_server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * REST API interface for vault operations providing secure storage and management
 * of encrypted secrets. All endpoints use POST requests with JSON payloads for
 * enhanced security and to avoid exposing sensitive data in URL parameters.
 */
public interface VaultAPI {

    /**
     * Retrieves all secrets from the vault for a specified namespace.
     * Returns decrypted secret values for authorized access.
     *
     * @param request a map containing the request parameters including namespace identifier
     * @return ResponseEntity containing a map with vault secrets and metadata
     * @throws Exception if vault access fails or namespace is not found
     */
    @PostMapping("/get")
    ResponseEntity<Map<String, Object>> getVault(@RequestBody Map<String, String> request) throws Exception;

    /**
     * Updates or creates secrets in the vault for a specified namespace.
     * Encrypts and stores the provided secret values with version control tracking.
     *
     * @param request a map containing namespace, secrets to update, email, and commit message
     * @return ResponseEntity containing operation result and metadata
     * @throws Exception if vault update fails or validation errors occur
     */
    @PostMapping("/update")
    ResponseEntity<Map<String, Object>> updateVault(@RequestBody Map<String, String> request) throws Exception;

    /**
     * Retrieves the complete history of changes made to the vault for a namespace.
     * Returns Git commit history with timestamps, authors, and change descriptions.
     *
     * @param request a map containing the namespace identifier
     * @return ResponseEntity containing vault history with commit details
     * @throws Exception if history retrieval fails or namespace access is denied
     */
    @PostMapping("/history")
    ResponseEntity<Map<String, Object>> getVaultHistory(@RequestBody Map<String, String> request) throws Exception;

    /**
     * Retrieves specific changes made in a particular vault commit.
     * Shows the differences introduced by a specific commit for audit purposes.
     *
     * @param request a map containing namespace and commit ID parameters
     * @return ResponseEntity containing the specific changes made in the commit
     * @throws Exception if commit is not found or access is denied
     */
    @PostMapping("/changes")
    ResponseEntity<Map<String, Object>> getVaultChanges(@RequestBody Map<String, String> request) throws Exception;
}