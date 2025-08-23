package dev.srivatsan.config_server.model;

import jakarta.validation.constraints.NotBlank;
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

}