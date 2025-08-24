package dev.srivatsan.config_server.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class CommitDetailsRequest {

    /**
     * commit ID to get details for
     */
    @NotBlank(message = "Commit ID ('commitId') must be provided.")
    private String commitId;

    /**
     * K8s Namespace
     */
    @NotBlank(message = "Namespace ('namespace') must be provided.")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Invalid namespace format")
    private String namespace;

}