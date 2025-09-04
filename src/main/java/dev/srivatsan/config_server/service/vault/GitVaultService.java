package dev.srivatsan.config_server.service.vault;

import java.util.Map;

/**
 * Git-Based Vault Service Interface
 * 
 * <p>Service interface for managing encrypted secrets stored in a Git-based vault system with complete
 * namespace isolation and enterprise-grade security features.</p>
 * 
 * <h2>Core Features</h2>
 * <ul>
 *   <li><strong>Namespace Isolation</strong> - Each namespace operates as an independent vault</li>
 *   <li><strong>AES-256-GCM Encryption</strong> - Military-grade encryption for all secrets</li>
 *   <li><strong>Git Version Control</strong> - Complete audit trail with commit history</li>
 *   <li><strong>Atomic Operations</strong> - All operations are atomic and consistent</li>
 *   <li><strong>Caching Layer</strong> - Intelligent caching for improved performance</li>
 *   <li><strong>Bulk Operations</strong> - Efficient batch processing capabilities</li>
 * </ul>
 * 
 * <h2>Security Architecture</h2>
 * <ul>
 *   <li>Each namespace has its own encryption key stored securely</li>
 *   <li>Keys are stored in {@code /{namespace}/.vault-keys/} with 700 permissions</li>
 *   <li>Encrypted secrets stored in {@code /{namespace}/.vault-secrets.json}</li>
 *   <li>All operations require authentication and authorization</li>
 *   <li>Git commits provide complete audit trail with author attribution</li>
 * </ul>
 * 
 * <h2>Data Flow</h2>
 * <pre>
 * 1. Client Request → Validation → Encryption → Git Storage → Response
 * 2. Git Storage ← Decryption ← Retrieval ← Client Request
 * </pre>
 * 
 * <h2>Caching Strategy</h2>
 * <ul>
 *   <li><strong>Individual Secrets</strong> - Cached per namespace+key combination</li>
 *   <li><strong>Secret Existence</strong> - Cached to avoid unnecessary Git operations</li>
 *   <li><strong>Vault History</strong> - Cached commit history for audit purposes</li>
 *   <li><strong>Cache Eviction</strong> - Automatic eviction on modifications</li>
 * </ul>
 * 
 * @author Config Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see dev.srivatsan.config_server.service.vault.GitVaultServiceImpl
 * @see dev.srivatsan.config_server.service.encryption.EncryptionService
 */
public interface GitVaultService {
    
    /**
     * Stores a new encrypted secret in the specified namespace vault.
     * 
     * @param namespace the namespace (vault) to store the secret in
     * @param key the unique identifier for the secret within the namespace
     * @param value the plain text value to be encrypted and stored
     * @param email the email address of the user performing the operation (for Git commits)
     * @param commitMessage the commit message for the Git operation
     * @throws VaultException if the secret already exists or encryption fails
     */
    void storeSecret(String namespace, String key, String value, String email, String commitMessage);
    
    /**
     * Retrieves and decrypts a secret from the specified namespace vault.
     * 
     * @param namespace the namespace (vault) to retrieve the secret from
     * @param key the unique identifier of the secret to retrieve
     * @return the decrypted plain text value of the secret
     * @throws VaultException if the secret is not found or decryption fails
     */
    String getSecret(String namespace, String key);
    
    /**
     * Updates an existing encrypted secret in the specified namespace vault.
     * 
     * @param namespace the namespace (vault) containing the secret to update
     * @param key the unique identifier of the secret to update
     * @param value the new plain text value to be encrypted and stored
     * @param email the email address of the user performing the operation (for Git commits)
     * @param commitMessage the commit message for the Git operation
     * @throws VaultException if the secret does not exist or encryption fails
     */
    void updateSecret(String namespace, String key, String value, String email, String commitMessage);
    
    /**
     * Deletes a secret from the specified namespace vault.
     * 
     * @param namespace the namespace (vault) containing the secret to delete
     * @param key the unique identifier of the secret to delete
     * @param email the email address of the user performing the operation (for Git commits)
     * @param commitMessage the commit message for the Git operation
     * @throws VaultException if the secret does not exist
     */
    void deleteSecret(String namespace, String key, String email, String commitMessage);
    
    /**
     * Retrieves and decrypts all secrets from the specified namespace vault.
     * 
     * @param namespace the namespace (vault) to retrieve all secrets from
     * @return a map containing all secret key-value pairs (decrypted values)
     * @throws VaultException if the vault does not exist or decryption fails
     */
    Map<String, String> getAllSecrets(String namespace);
    
    /**
     * Stores multiple encrypted secrets in the specified namespace vault in a single operation.
     * 
     * @param namespace the namespace (vault) to store the secrets in
     * @param secrets a map of key-value pairs (plain text) to be encrypted and stored
     * @param email the email address of the user performing the operation (for Git commits)
     * @param commitMessage the commit message for the Git operation
     * @throws VaultException if any secret already exists or encryption fails
     */
    void storeBulkSecrets(String namespace, Map<String, String> secrets, String email, String commitMessage);
    
    /**
     * Checks whether a secret exists in the specified namespace vault.
     * 
     * @param namespace the namespace (vault) to check for the secret
     * @param key the unique identifier of the secret to check
     * @return true if the secret exists, false otherwise
     */
    boolean secretExists(String namespace, String key);
    
    /**
     * Retrieves the Git commit history for the specified namespace vault.
     * 
     * @param namespace the namespace (vault) to get the history for
     * @return a map containing the commit history with timestamps, authors, and messages
     * @throws VaultException if the vault does not exist or Git operation fails
     */
    Map<String, Object> getVaultHistory(String namespace);
}