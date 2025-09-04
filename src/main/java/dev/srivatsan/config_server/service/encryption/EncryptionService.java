package dev.srivatsan.config_server.service.encryption;

public interface EncryptionService {
    
    String encrypt(String plainText, String namespace);
    
    String decrypt(String encryptedText, String namespace);
    
    void initializeNamespaceKey(String namespace);
    
    boolean isEncrypted(String value);
}