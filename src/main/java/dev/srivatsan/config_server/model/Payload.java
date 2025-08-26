package dev.srivatsan.config_server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Schema(description = "Configuration file operation payload")
public class Payload {

    @Schema(description = "Application name", example = "user-service", required = true)
    @NotBlank(message = "Application name ('appName') must be provided.")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Invalid app name format")
    private String appName;

    @Schema(description = "Namespace for configuration isolation", example = "test", required = true)
    @NotBlank(message = "Namespace ('namespace') must be provided.")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Invalid namespace format")
    private String namespace;

    @Schema(description = "Path within the namespace", example = "/config/", required = true)
    @NotBlank(message = "Path ('path') must be provided and must start with a '/'.")
    @Pattern(regexp = "^/.*", message = "Path ('path') must start with a '/'.")
    private String path;

    @Schema(description = "YAML configuration content",
            example = "server:\n  port: 8080\nspring:\n  application:\n    name: user-service")
    private String content;

    @Schema(description = "Operation type", required = true)
    @NotNull(message = "Action ('action') must be provided and must match the operation.")
    private ActionType action;

    @Schema(description = "Git commit message for updates", example = "Update user service configuration")
    private String message;

    @Schema(description = "User email for Git commits", example = "developer@company.com", required = true)
    @NotBlank(message = "Email is mandatory!!!")
    private String email;

    @Schema(description = "Git commit ID for retrieving specific changes", example = "abc123def456")
    private String commitId;

    /**
     * configuration file name
     */
    @JsonIgnore
    private String fileName;

    public String getFileName() {
        return this.appName + ".yml";
    }
}
