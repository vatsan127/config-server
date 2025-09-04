package dev.srivatsan.config_server.service.encryption;

import java.util.List;

public interface EncryptionService {
    
    String encrypt(String plainText);
    
    String decrypt(String encryptedText);
    
    boolean isEncrypted(String text);
    
    // Line-by-line encryption methods
    String encryptContent(String content);
    
    String decryptContent(String encryptedContent);
    
    List<String> encryptLines(List<String> lines);
    
    List<String> decryptLines(List<String> encryptedLines);
}