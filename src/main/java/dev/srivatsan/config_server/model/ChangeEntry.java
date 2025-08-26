package dev.srivatsan.config_server.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Configuration change entry for cache")
public class ChangeEntry {

    @Schema(description = "Git commit ID", example = "abc123def456")
    private String commitId;

    @Schema(description = "Commit message describing the change", example = "Updated database configuration")
    private String message;

    @Schema(description = "Author of the change", example = "developer")
    private String author;

    @Schema(description = "Author email", example = "developer@company.com")
    private String email;

    @Schema(description = "Timestamp when the change was made")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedTime;

    @Schema(description = "Configuration file name", example = "user-service.yml")
    private String fileName;

    @Schema(description = "Git diff showing the changes made")
    private String changes;
}