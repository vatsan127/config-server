package dev.srivatsan.config_server.model;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class CommitDetailsRequest {

    /** commit ID to get details for */
    @NotBlank(message = "Commit ID ('commitId') must be provided.")
    private String commitId;

    /** optional file path to filter changes for specific file */
    private String filePath;
}