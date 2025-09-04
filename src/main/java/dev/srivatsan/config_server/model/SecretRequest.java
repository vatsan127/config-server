package dev.srivatsan.config_server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object representing a request to perform operations on a single secret.
 * 
 * This class encapsulates all the necessary information required to store, update,
 * or delete a secret in the vault system. It includes validation constraints to
 * ensure data integrity and proper Git commit attribution.
 */
@Schema(description = "Request payload for vault secret operations")
public class SecretRequest {
    
    /** The unique identifier for the secret within a namespace (1-255 characters) */
    @Schema(description = "Unique identifier for the secret within the namespace", example = "db_password", required = true)
    @NotBlank(message = "Secret key is required")
    @Size(min = 1, max = 255, message = "Secret key must be between 1 and 255 characters")
    private String key;
    
    /** The plain text value of the secret to be encrypted and stored (1-10000 characters) */
    @Schema(description = "Plain text value of the secret (will be encrypted when stored)", example = "super_secure_password_123", required = true)
    @NotBlank(message = "Secret value is required")
    @Size(min = 1, max = 10000, message = "Secret value must be between 1 and 10000 characters")
    private String value;
    
    /** Email address of the user performing the operation for Git commit attribution */
    @Schema(description = "Email address for Git commit attribution", example = "admin@company.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    /** Message to be used for the Git commit when storing/updating/deleting the secret */
    @Schema(description = "Git commit message for the vault operation", example = "Add database password for production environment", required = true)
    @NotBlank(message = "Commit message is required")
    @Size(min = 1, max = 500, message = "Commit message must be between 1 and 500 characters")
    private String commitMessage;

    /** Default constructor for JSON deserialization */
    public SecretRequest() {}

    /**
     * Constructor to create a complete secret request.
     * 
     * @param key the unique identifier for the secret
     * @param value the plain text value of the secret
     * @param email the email address for Git commit attribution
     * @param commitMessage the message for the Git commit
     */
    public SecretRequest(String key, String value, String email, String commitMessage) {
        this.key = key;
        this.value = value;
        this.email = email;
        this.commitMessage = commitMessage;
    }

    /**
     * Gets the secret key.
     * 
     * @return the unique identifier for the secret
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the secret key.
     * 
     * @param key the unique identifier for the secret
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the secret value.
     * 
     * @return the plain text value of the secret
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the secret value.
     * 
     * @param value the plain text value of the secret
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the user email.
     * 
     * @return the email address for Git commit attribution
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user email.
     * 
     * @param email the email address for Git commit attribution
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the commit message.
     * 
     * @return the message for the Git commit
     */
    public String getCommitMessage() {
        return commitMessage;
    }

    /**
     * Sets the commit message.
     * 
     * @param commitMessage the message for the Git commit
     */
    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
}