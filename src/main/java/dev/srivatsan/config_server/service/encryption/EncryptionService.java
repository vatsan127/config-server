package dev.srivatsan.config_server.service.encryption;

/**
 * Encryption Service Interface
 * 
 * <p>Provides cryptographic operations for securing secrets in namespace-isolated vaults.
 * This service implements AES-256-GCM encryption with namespace-specific keys for maximum security.</p>
 * 
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>AES-256-GCM Encryption</strong> - Military-grade symmetric encryption</li>
 *   <li><strong>Namespace Isolation</strong> - Each namespace has its own encryption key</li>
 *   <li><strong>Random IV Generation</strong> - Each encryption uses a unique initialization vector</li>
 *   <li><strong>Authenticated Encryption</strong> - GCM mode provides built-in integrity verification</li>
 *   <li><strong>Key Management</strong> - Automatic key generation and caching</li>
 * </ul>
 * 
 * <h2>Key Management</h2>
 * <p>Encryption keys are stored per-namespace in secure directories with restricted permissions:</p>
 * <ul>
 *   <li>Key location: {@code /{namespace}/.vault-keys/{namespace}.key}</li>
 *   <li>Directory permissions: 700 (owner-only access)</li>
 *   <li>File permissions: 600 (owner read/write only)</li>
 *   <li>In-memory caching for performance</li>
 *   <li>Automatic cleanup of unused keys</li>
 * </ul>
 * 
 * <h2>Encryption Format</h2>
 * <p>Encrypted values are stored with the following format:</p>
 * <pre>
 * VAULT:&lt;base64-encoded(IV + ciphertext + auth_tag)&gt;
 * </pre>
 * 
 * @author Config Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see dev.srivatsan.config_server.service.encryption.AESEncryptionServiceImpl
 */
public interface EncryptionService {
    
    /**
     * Encrypts plain text using AES-256-GCM with namespace-specific key.
     * 
     * <p>The encryption process:</p>
     * <ol>
     *   <li>Loads or generates namespace-specific encryption key</li>
     *   <li>Generates random 12-byte initialization vector (IV)</li>
     *   <li>Encrypts plaintext using AES-256-GCM</li>
     *   <li>Prepends "VAULT:" prefix to indicate encrypted content</li>
     *   <li>Returns base64-encoded result containing IV + ciphertext + auth_tag</li>
     * </ol>
     * 
     * @param plainText The plain text to encrypt (must not be null or empty)
     * @param namespace The namespace for which to encrypt (determines encryption key)
     * @return The encrypted text with "VAULT:" prefix and base64 encoding
     * @throws dev.srivatsan.config_server.exception.VaultException if encryption fails
     * @since 1.0.0
     */
    String encrypt(String plainText, String namespace);
    
    /**
     * Decrypts encrypted text using AES-256-GCM with namespace-specific key.
     * 
     * <p>The decryption process:</p>
     * <ol>
     *   <li>Validates "VAULT:" prefix (returns as-is if not encrypted)</li>
     *   <li>Base64-decodes the encrypted content</li>
     *   <li>Extracts IV, ciphertext, and authentication tag</li>
     *   <li>Loads namespace-specific decryption key</li>
     *   <li>Decrypts and verifies authenticity using AES-256-GCM</li>
     * </ol>
     * 
     * @param encryptedText The encrypted text to decrypt (with "VAULT:" prefix)
     * @param namespace The namespace for which to decrypt (determines decryption key)
     * @return The decrypted plain text, or original text if not encrypted
     * @throws dev.srivatsan.config_server.exception.VaultException if decryption fails
     * @since 1.0.0
     */
    String decrypt(String encryptedText, String namespace);
    
    /**
     * Initializes a new encryption key for the specified namespace.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Generates a new 256-bit AES key using secure random number generation</li>
     *   <li>Creates the namespace's .vault-keys directory with secure permissions (700)</li>
     *   <li>Stores the key in {@code /{namespace}/.vault-keys/{namespace}.key}</li>
     *   <li>Sets secure file permissions (600) on the key file</li>
     *   <li>Caches the key in memory for performance</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> This method is idempotent - if a key already exists, 
     * it will not be overwritten.</p>
     * 
     * @param namespace The namespace for which to initialize the encryption key
     * @throws dev.srivatsan.config_server.exception.VaultException if key initialization fails
     * @since 1.0.0
     */
    void initializeNamespaceKey(String namespace);
    
    /**
     * Checks if a given value is encrypted by this service.
     * 
     * <p>Determines if a string value was encrypted by checking for the "VAULT:" prefix.
     * This allows the service to handle both encrypted and plain text values gracefully.</p>
     * 
     * @param value The value to check for encryption status
     * @return {@code true} if the value starts with "VAULT:" prefix, {@code false} otherwise
     * @since 1.0.0
     */
    boolean isEncrypted(String value);
}