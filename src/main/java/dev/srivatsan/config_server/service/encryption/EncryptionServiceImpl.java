package dev.srivatsan.config_server.service.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final String ENCRYPTION_PREFIX = "{cipher}";
    private static final String SALT = "deadbeef"; // Fixed salt for deterministic encryption
    
    private final TextEncryptor textEncryptor;

    public EncryptionServiceImpl(@Value("${encrypt.key}") String encryptionKey) {
        // Create a hash of the key to ensure consistent length
        String hashedKey = hashKey(encryptionKey);
        this.textEncryptor = Encryptors.text(hashedKey, SALT);
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        if (!isEncrypted(encryptedText)) {
            return encryptedText; // Not encrypted, return as is
        }
        
        String cipherText = encryptedText.substring(ENCRYPTION_PREFIX.length());
        return textEncryptor.decrypt(cipherText);
    }

    @Override
    public boolean isEncrypted(String text) {
        return text != null && text.startsWith(ENCRYPTION_PREFIX);
    }
    
    @Override
    public String encryptContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        List<String> lines = Arrays.asList(content.split("\n", -1));
        List<String> encryptedLines = lines.stream().map(this::encryptLine).collect(Collectors.toList());
        return String.join("\n", encryptedLines);
    }
    
    @Override
    public String decryptContent(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isEmpty()) {
            return encryptedContent;
        }
        
        List<String> lines = Arrays.asList(encryptedContent.split("\n", -1));
        List<String> decryptedLines = lines.stream().map(this::decryptLine).collect(Collectors.toList());
        return String.join("\n", decryptedLines);
    }
    
    private String encryptLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return line; // Don't encrypt empty lines
        }
        
        if (isEncrypted(line)) {
            return line; // Already encrypted
        }
        
        String encrypted = textEncryptor.encrypt(line);
        return ENCRYPTION_PREFIX + encrypted;
    }
    
    private String decryptLine(String encryptedLine) {
        if (encryptedLine == null || encryptedLine.trim().isEmpty()) {
            return encryptedLine; // Don't decrypt empty lines
        }
        
        if (!isEncrypted(encryptedLine)) {
            return encryptedLine; // Not encrypted
        }
        
        String cipherText = encryptedLine.substring(ENCRYPTION_PREFIX.length());
        return textEncryptor.decrypt(cipherText);
    }
    
    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}