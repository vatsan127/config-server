package dev.srivatsan.config_server.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Data transfer object representing a request to perform bulk operations on multiple secrets.
 * 
 * This class encapsulates all the necessary information required to store multiple
 * secrets in a single vault operation. It includes validation constraints to ensure
 * data integrity and proper Git commit attribution for batch operations.
 */
@Schema(description = "Request payload for bulk vault secret operations")
public class BulkSecretsRequest {
    
    /** Map of secret keys to their plain text values to be encrypted and stored */
    @Schema(description = "Map of secret keys to their plain text values (will be encrypted when stored)", 
            example = "{\"db_host\": \"prod-db.company.com\", \"db_password\": \"secure_password_123\"}", 
            required = true)
    @NotEmpty(message = "Secrets map cannot be empty")
    @Size(min = 1, message = "At least one secret is required")
    private Map<String, String> secrets;
    
    /** Email address of the user performing the operation for Git commit attribution */
    @Schema(description = "Email address for Git commit attribution", example = "devops@company.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    /** Message to be used for the Git commit when storing the bulk secrets */
    @Schema(description = "Git commit message for the bulk vault operation", example = "Initial database configuration setup", required = true)
    @NotBlank(message = "Commit message is required")
    @Size(min = 1, max = 500, message = "Commit message must be between 1 and 500 characters")
    private String commitMessage;

    /** Default constructor for JSON deserialization */
    public BulkSecretsRequest() {}

    /**
     * Constructor to create a complete bulk secrets request.
     * 
     * @param secrets map of secret keys to their plain text values
     * @param email the email address for Git commit attribution
     * @param commitMessage the message for the Git commit
     */
    public BulkSecretsRequest(Map<String, String> secrets, String email, String commitMessage) {
        this.secrets = secrets;
        this.email = email;
        this.commitMessage = commitMessage;
    }

    /**
     * Gets the secrets map.
     * 
     * @return map of secret keys to their plain text values
     */
    public Map<String, String> getSecrets() {
        return secrets;
    }

    /**
     * Sets the secrets map.
     * 
     * @param secrets map of secret keys to their plain text values
     */
    public void setSecrets(Map<String, String> secrets) {
        this.secrets = secrets;
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