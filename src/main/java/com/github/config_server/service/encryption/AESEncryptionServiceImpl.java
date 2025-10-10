package com.github.config_server.service.encryption;


import com.github.config_server.config.ApplicationConfig;
import com.github.config_server.exception.VaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AESEncryptionServiceImpl implements EncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AESEncryptionServiceImpl.class);

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final String ENCRYPTED_PREFIX = "VAULT:";

    private final ApplicationConfig applicationConfig;
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
            SecretKey key = getMasterKey();

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
            log.error("Failed to encrypt data: {}", e.getMessage());
            throw VaultException.encryptionFailed("Encryption failed: " + e.getMessage());
        }
    }

    @Override
    public String decrypt(String encryptedText, String namespace) {
        if (!isEncrypted(encryptedText)) {
            return encryptedText; // Return as-is if not encrypted
        }

        try {
            SecretKey key = getMasterKey();

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
            log.error("Failed to decrypt data: {}", e.getMessage());
            throw VaultException.decryptionFailed("Decryption failed: " + e.getMessage());
        }
    }

    @Override
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }

    private SecretKey getMasterKey() {
        try {
            String masterKeyBase64 = applicationConfig.getVaultMasterKey();

            if (masterKeyBase64 == null || masterKeyBase64.trim().isEmpty()) {
                log.error("No vault master key configured! Check VAULT_MASTER_KEY environment variable or application.yml default.");
                throw VaultException.keyLoadFailed("No vault master key configured");
            }

            String envKey = System.getenv("VAULT_MASTER_KEY");
            if (envKey == null || envKey.trim().isEmpty()) {
                log.warn("⚠️  SECURITY WARNING: Using default vault master key from application.yml");
                log.warn("⚠️  This is NOT secure for production! Set VAULT_MASTER_KEY environment variable.");
                log.warn("⚠️  Generate a new key with: openssl rand -base64 32");
            }

            // Decode the base64 key
            byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64.trim());
            if (keyBytes.length != 32) { // Validate key length (256 bits = 32 bytes)
                throw VaultException.keyLoadFailed("Invalid master key length. Expected 32 bytes (256 bits), got: " + keyBytes.length);
            }
            return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);

        } catch (IllegalArgumentException e) {
            log.error("Failed to decode master encryption key from environment variable: {}", e.getMessage());
            throw VaultException.keyLoadFailed("Invalid base64 encoding in VAULT_MASTER_KEY: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to load master encryption key: {}", e.getMessage());
            throw VaultException.keyLoadFailed("Failed to load master encryption key: " + e.getMessage());
        }
    }


}