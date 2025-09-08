package dev.srivatsan.config_server.service.encryption;

/**
 * Service interface for encryption and decryption operations within the config server.
 * Provides namespace-aware encryption capabilities to secure configuration data
 * with isolated encryption keys per namespace.
 */
public interface EncryptionService {

    /**
     * Encrypts the provided plain text using the encryption key associated with the specified namespace.
     *
     * @param plainText the plain text string to encrypt
     * @param namespace the namespace identifier used to determine the encryption key
     * @return the encrypted text as a string
     */
    String encrypt(String plainText, String namespace);

    /**
     * Decrypts the provided encrypted text using the encryption key associated with the specified namespace.
     *
     * @param encryptedText the encrypted text string to decrypt
     * @param namespace     the namespace identifier used to determine the decryption key
     * @return the decrypted plain text as a string
     */
    String decrypt(String encryptedText, String namespace);

    /**
     * Initializes or creates an encryption key for the specified namespace.
     * This method should be called before performing any encryption/decryption
     * operations for a new namespace.
     *
     * @param namespace the namespace identifier for which to initialize the encryption key
     */
    void initializeNamespaceKey(String namespace);

    /**
     * Checks whether the provided value is in encrypted format.
     * This method can be used to determine if a configuration value
     * needs to be decrypted before use.
     *
     * @param value the string value to check
     * @return true if the value is encrypted, false otherwise
     */
    boolean isEncrypted(String value);
}