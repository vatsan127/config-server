package dev.srivatsan.config_server.model;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
public class Payload {

    /** application name */
    @NotBlank(message = "Application name ('appName') must be provided.")
    private String appName;

    /** K8s Namespace */
    @NotBlank(message = "Namespace ('namespace') must be provided.")
    private String namespace;

    /** path inside repository */
    @NotBlank(message = "Path ('path') must be provided and must start with a '/'.")
    @Pattern(regexp = "^/.*", message = "Path ('path') must start with a '/'.")
    private String path;

    /** application configuration */
    private String content;

    /** ActionType - create, fetch, update */
    @NotNull(message = "Action ('action') must be provided and must match the operation.")
    private ActionType action;

    /** configuration file name */
    private String fileName;

    /** response status */
    private ResponseStatus status;

    public String getFileName() {
        return this.appName + ".yml";
    }
}
