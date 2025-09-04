package dev.srivatsan.config_server.service.encryption;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.VaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AESEncryptionServiceImpl implements EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AESEncryptionServiceImpl.class);
    
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final String ENCRYPTED_PREFIX = "VAULT:";
    
    private final ApplicationConfig applicationConfig;
    private final ConcurrentHashMap<String, SecretKey> namespaceKeys = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public AESEncryptionServiceImpl(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    @Override
    public String encrypt(String plainText, String namespace) {
        if (plainText == null || plainText.trim().isEmpty()) {
            throw VaultException.encryptionFailed("Cannot encrypt null or empty text");
        }
        
        try {
            SecretKey key = getOrCreateNamespaceKey(namespace);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            
            byte[] encryptedData = cipher.doFinal(plainText.getBytes());
            
            // Combine IV + encrypted data
            byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);
            
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            log.error("Failed to encrypt data for namespace: {}", namespace, e);
            throw VaultException.encryptionFailed("Encryption failed: " + e.getMessage());
        }
    }

    @Override
    public String decrypt(String encryptedText, String namespace) {
        if (!isEncrypted(encryptedText)) {
            return encryptedText; // Return as-is if not encrypted
        }
        
        try {
            SecretKey key = getOrCreateNamespaceKey(namespace);
            
            // Remove prefix and decode
            String base64Data = encryptedText.substring(ENCRYPTED_PREFIX.length());
            byte[] encryptedWithIv = Base64.getDecoder().decode(base64Data);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
            System.arraycopy(encryptedWithIv, iv.length, encryptedData, 0, encryptedData.length);
            
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData);
            
        } catch (Exception e) {
            log.error("Failed to decrypt data for namespace: {}", namespace, e);
            throw VaultException.decryptionFailed("Decryption failed: " + e.getMessage());
        }
    }

    @Override
    public void initializeNamespaceKey(String namespace) { // TODO: lets use this one when the namespace is create and reuse the same
        try {
            Path keyPath = getNamespaceKeyPath(namespace);
            
            if (Files.exists(keyPath)) {
                log.debug("Key already exists for namespace: {}", namespace);
                return;
            }
            
            // Generate new key
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey secretKey = keyGenerator.generateKey();
            
            // Create directory if it doesn't exist
            Files.createDirectories(keyPath.getParent());
            
            // Save key to file
            Files.write(keyPath, secretKey.getEncoded());
            
            // Cache the key
            namespaceKeys.put(namespace, secretKey);
            
            log.info("Initialized encryption key for namespace: {}", namespace);
            
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to initialize key for namespace: {}", namespace, e);
            throw VaultException.keyInitializationFailed("Failed to initialize encryption key: " + e.getMessage());
        }
    }

    @Override
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }
    
    private SecretKey getOrCreateNamespaceKey(String namespace) {
        return namespaceKeys.computeIfAbsent(namespace, ns -> {
            try {
                Path keyPath = getNamespaceKeyPath(ns);
                
                if (!Files.exists(keyPath)) {
                    initializeNamespaceKey(ns);
                }
                
                byte[] keyBytes = Files.readAllBytes(keyPath);
                return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
                
            } catch (IOException e) {
                log.error("Failed to load key for namespace: {}", ns, e);
                throw VaultException.keyLoadFailed("Failed to load encryption key: " + e.getMessage());
            }
        });
    }
    
    private Path getNamespaceKeyPath(String namespace) {
        return new File(applicationConfig.getBasePath())
                .toPath()
                .resolve(".vault-keys")
                .resolve(namespace + ".key");
    }
}