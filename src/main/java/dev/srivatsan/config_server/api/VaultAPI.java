package dev.srivatsan.config_server.api;

import dev.srivatsan.config_server.model.BulkSecretsRequest;
import dev.srivatsan.config_server.model.SecretRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Vault Management API Interface
 * 
 * <p>Provides REST API endpoints for managing encrypted secrets in Git-based vaults with complete namespace isolation.
 * This interface defines the contract for all vault operations including secret storage, retrieval, updates, and 
 * audit trail management.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Namespace Isolation</strong> - Each namespace has its own encrypted vault with separate encryption keys</li>
 *   <li><strong>AES-256-GCM Encryption</strong> - Military-grade encryption for all secrets at rest</li>
 *   <li><strong>Git Versioning</strong> - Complete audit trail with commit history for compliance</li>
 *   <li><strong>Bulk Operations</strong> - Efficient batch processing for multiple secrets</li>
 *   <li><strong>Key Rotation</strong> - Support for encryption key rotation without data loss</li>
 *   <li><strong>Access Control</strong> - Namespace-based access control for multi-tenant environments</li>
 * </ul>
 * 
 * <h2>Security Model</h2>
 * <p>Each namespace maintains its own encrypted vault file (.vault-secrets.json) with the following security features:</p>
 * <ul>
 *   <li><strong>Encryption Keys</strong> - Unique AES-256-GCM keys per namespace stored in .vault-keys/</li>
 *   <li><strong>Initialization Vectors</strong> - Random IV for each secret to prevent pattern analysis</li>
 *   <li><strong>Authentication Tags</strong> - GCM authentication tags ensure data integrity</li>
 *   <li><strong>Git Integration</strong> - All changes are committed to Git with full audit trail</li>
 * </ul>
 * 
 * <h2>Vault Structure</h2>
 * <pre>
 * /namespace/
 * ├── .vault-secrets.json    # Encrypted secrets storage
 * ├── .vault-keys/           # Encryption keys (never committed to Git)
 * │   └── master.key         # Primary encryption key
 * └── config/                # Regular configuration files
 *     ├── app1.yml
 *     └── app2.yml
 * </pre>
 * 
 * @author Config Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see dev.srivatsan.config_server.controller.VaultController
 * @see dev.srivatsan.config_server.service.vault.VaultService
 */
public interface VaultAPI {

    @PostMapping
    ResponseEntity<Map<String, Object>> storeSecret(
            @PathVariable String namespace,
            @Valid @RequestBody SecretRequest request) throws Exception;

    @PostMapping("/bulk")
    ResponseEntity<Map<String, Object>> storeBulkSecrets(
            @PathVariable String namespace,
            @Valid @RequestBody BulkSecretsRequest request) throws Exception;

    @PostMapping("/get/{key}")
    ResponseEntity<Map<String, Object>> getSecret(
            @PathVariable String namespace,
            @PathVariable String key,
            @RequestBody Map<String, String> request) throws Exception;

    @PostMapping("/list")
    ResponseEntity<Map<String, Object>> getAllSecrets(
            @PathVariable String namespace,
            @RequestBody Map<String, String> request) throws Exception;

    @PutMapping("/{key}")
    ResponseEntity<Map<String, Object>> updateSecret(
            @PathVariable String namespace,
            @PathVariable String key,
            @Valid @RequestBody SecretRequest request) throws Exception;

    @DeleteMapping("/{key}")
    ResponseEntity<Map<String, Object>> deleteSecret(
            @PathVariable String namespace,
            @PathVariable String key,
            @Valid @RequestBody SecretRequest request) throws Exception;

    @PostMapping("/exists/{key}")
    ResponseEntity<Map<String, Object>> secretExists(
            @PathVariable String namespace,
            @PathVariable String key,
            @RequestBody Map<String, String> request) throws Exception;

    @PostMapping("/history")
    ResponseEntity<Map<String, Object>> getVaultHistory(
            @PathVariable String namespace,
            @RequestBody Map<String, String> request) throws Exception;
}