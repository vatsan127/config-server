package com.github.config_server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.config_server.constants.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class Payload {

    @NotBlank(message = "Application name ('appName') must be provided.")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Invalid app name format")
    private String appName;

    @NotBlank(message = "Namespace ('namespace') must be provided.")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Invalid namespace format")
    private String namespace;

    @NotBlank(message = "Path ('path') must be provided and must start with a '/'.")
    @Pattern(regexp = "^/.*", message = "Path ('path') must start with a '/'.")
    private String path;

    private String content;

    @NotNull(message = "Action ('action') must be provided and must match the operation.")
    private ActionType action;

    private String message;

    @NotBlank(message = "Email is mandatory!!!")
    private String email;

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
