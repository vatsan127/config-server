package dev.srivatsan.config_server.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class BulkSecretsRequest {
    
    @NotEmpty(message = "Secrets map cannot be empty")
    @Size(min = 1, message = "At least one secret is required")
    private Map<String, String> secrets;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Commit message is required")
    @Size(min = 1, max = 500, message = "Commit message must be between 1 and 500 characters")
    private String commitMessage;

    public BulkSecretsRequest() {}

    public BulkSecretsRequest(Map<String, String> secrets, String email, String commitMessage) {
        this.secrets = secrets;
        this.email = email;
        this.commitMessage = commitMessage;
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public void setSecrets(Map<String, String> secrets) {
        this.secrets = secrets;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
}