package com.github.config_server.service.encryption;

/**
 * Service interface for encryption and decryption operations within the config server.
 */
public interface EncryptionService {

    /**
     * Encrypts the provided plain text using the encryption key associated with the specified namespace.
     */
    String encrypt(String plainText, String namespace);

    /**
     * Decrypts the provided encrypted text using the encryption key associated with the specified namespace.
     */
    String decrypt(String encryptedText, String namespace);

    /**
     * Checks whether the provided value is in encrypted format.
     */
    boolean isEncrypted(String value);

}