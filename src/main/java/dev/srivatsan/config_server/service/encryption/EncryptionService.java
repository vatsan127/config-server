package dev.srivatsan.config_server.service.encryption;

public interface EncryptionService {
    
    String decrypt(String encryptedText);
    
    boolean isEncrypted(String text);

    String encryptContent(String content);
    
    String decryptContent(String encryptedContent);
}