package dev.srivatsan.config_server.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SecretRequest {
    
    @NotBlank(message = "Secret key is required")
    @Size(min = 1, max = 255, message = "Secret key must be between 1 and 255 characters")
    private String key;
    
    @NotBlank(message = "Secret value is required")
    @Size(min = 1, max = 10000, message = "Secret value must be between 1 and 10000 characters")
    private String value;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Commit message is required")
    @Size(min = 1, max = 500, message = "Commit message must be between 1 and 500 characters")
    private String commitMessage;

    public SecretRequest() {}

    public SecretRequest(String key, String value, String email, String commitMessage) {
        this.key = key;
        this.value = value;
        this.email = email;
        this.commitMessage = commitMessage;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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