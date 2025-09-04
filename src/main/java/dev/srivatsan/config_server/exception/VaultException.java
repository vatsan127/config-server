package dev.srivatsan.config_server.exception;

public class VaultException extends RuntimeException {
    
    public static final String ENCRYPTION_FAILED = "ENCRYPTION_FAILED";
    public static final String DECRYPTION_FAILED = "DECRYPTION_FAILED";
    public static final String KEY_INITIALIZATION_FAILED = "KEY_INITIALIZATION_FAILED";
    public static final String KEY_LOAD_FAILED = "KEY_LOAD_FAILED";
    public static final String VAULT_FILE_NOT_FOUND = "VAULT_FILE_NOT_FOUND";
    public static final String VAULT_OPERATION_FAILED = "VAULT_OPERATION_FAILED";
    public static final String SECRET_NOT_FOUND = "SECRET_NOT_FOUND";
    
    private final String errorCode;
    
    public VaultException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public VaultException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public static VaultException encryptionFailed(String message) {
        return new VaultException(ENCRYPTION_FAILED, message);
    }
    
    public static VaultException decryptionFailed(String message) {
        return new VaultException(DECRYPTION_FAILED, message);
    }
    
    public static VaultException keyInitializationFailed(String message) {
        return new VaultException(KEY_INITIALIZATION_FAILED, message);
    }
    
    public static VaultException keyLoadFailed(String message) {
        return new VaultException(KEY_LOAD_FAILED, message);
    }
    
    public static VaultException vaultFileNotFound(String filePath) {
        return new VaultException(VAULT_FILE_NOT_FOUND, "Vault file not found: " + filePath);
    }
    
    public static VaultException vaultOperationFailed(String message) {
        return new VaultException(VAULT_OPERATION_FAILED, message);
    }
    
    public static VaultException secretNotFound(String key) {
        return new VaultException(SECRET_NOT_FOUND, "Secret not found: " + key);
    }
}