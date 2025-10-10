package com.github.config_server.exception;

/**
 * Custom runtime exception for vault-related operations and errors.
 * <p>
 * This exception class provides specific error codes and factory methods
 * for different types of vault failures including encryption/decryption errors,
 * key management issues, file operations, and secret management problems.
 * <p>
 * Each exception instance includes both a human-readable message and a
 * machine-readable error code for better error handling and debugging.
 */
public class VaultException extends RuntimeException {

    /**
     * Error code for encryption operation failures
     */
    public static final String ENCRYPTION_FAILED = "ENCRYPTION_FAILED";

    /**
     * Error code for decryption operation failures
     */
    public static final String DECRYPTION_FAILED = "DECRYPTION_FAILED";

    /**
     * Error code for encryption key initialization failures
     */
    public static final String KEY_INITIALIZATION_FAILED = "KEY_INITIALIZATION_FAILED";

    /**
     * Error code for encryption key loading failures
     */
    public static final String KEY_LOAD_FAILED = "KEY_LOAD_FAILED";

    /**
     * Error code for encryption key deletion failures
     */
    public static final String KEY_DELETION_FAILED = "KEY_DELETION_FAILED";

    /**
     * Error code when vault file cannot be found
     */
    public static final String VAULT_FILE_NOT_FOUND = "VAULT_FILE_NOT_FOUND";

    /**
     * Error code for general vault operation failures
     */
    public static final String VAULT_OPERATION_FAILED = "VAULT_OPERATION_FAILED";

    /**
     * Error code when a requested secret is not found
     */
    public static final String SECRET_NOT_FOUND = "SECRET_NOT_FOUND";

    /**
     * The machine-readable error code associated with this exception
     */
    private final String errorCode;

    /**
     * Constructs a new VaultException with the specified error code and message.
     *
     * @param errorCode the machine-readable error code
     * @param message   the human-readable error message
     */
    public VaultException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new VaultException with the specified error code, message, and cause.
     *
     * @param errorCode the machine-readable error code
     * @param message   the human-readable error message
     * @param cause     the underlying cause of this exception
     */
    public VaultException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the machine-readable error code associated with this exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Factory method to create an encryption failure exception.
     *
     * @param message the detailed error message
     * @return a new VaultException with ENCRYPTION_FAILED error code
     */
    public static VaultException encryptionFailed(String message) {
        return new VaultException(ENCRYPTION_FAILED, message);
    }

    /**
     * Factory method to create a decryption failure exception.
     *
     * @param message the detailed error message
     * @return a new VaultException with DECRYPTION_FAILED error code
     */
    public static VaultException decryptionFailed(String message) {
        return new VaultException(DECRYPTION_FAILED, message);
    }

    /**
     * Factory method to create a key initialization failure exception.
     *
     * @param message the detailed error message
     * @return a new VaultException with KEY_INITIALIZATION_FAILED error code
     */
    public static VaultException keyInitializationFailed(String message) {
        return new VaultException(KEY_INITIALIZATION_FAILED, message);
    }

    /**
     * Factory method to create a key loading failure exception.
     *
     * @param message the detailed error message
     * @return a new VaultException with KEY_LOAD_FAILED error code
     */
    public static VaultException keyLoadFailed(String message) {
        return new VaultException(KEY_LOAD_FAILED, message);
    }

    /**
     * Factory method to create a vault file not found exception.
     *
     * @param filePath the path of the vault file that was not found
     * @return a new VaultException with VAULT_FILE_NOT_FOUND error code
     */
    public static VaultException vaultFileNotFound(String filePath) {
        return new VaultException(VAULT_FILE_NOT_FOUND, "Vault file not found: " + filePath);
    }

    /**
     * Factory method to create a general vault operation failure exception.
     *
     * @param message the detailed error message
     * @return a new VaultException with VAULT_OPERATION_FAILED error code
     */
    public static VaultException vaultOperationFailed(String message) {
        return new VaultException(VAULT_OPERATION_FAILED, message);
    }

    /**
     * Factory method to create a secret not found exception.
     *
     * @param key the key of the secret that was not found
     * @return a new VaultException with SECRET_NOT_FOUND error code
     */
    public static VaultException secretNotFound(String key) {
        return new VaultException(SECRET_NOT_FOUND, "Secret not found: " + key);
    }

    /**
     * Factory method to create a key deletion failure exception.
     *
     * @param message the detailed error message
     * @return a new VaultException with KEY_DELETION_FAILED error code
     */
    public static VaultException keyDeletionFailed(String message) {
        return new VaultException(KEY_DELETION_FAILED, message);
    }
}