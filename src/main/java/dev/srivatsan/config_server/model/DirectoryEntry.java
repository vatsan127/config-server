package dev.srivatsan.config_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a file or directory entry in a namespace directory listing.
 * Contains metadata about files and folders within a configuration namespace.
 *
 * @author srivatsan.n
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryEntry {

    /**
     * The name of the file or directory
     */
    private String name;

    /**
     * The type of entry - either "file" or "directory"
     */
    private String type;

    /**
     * The relative path from the namespace root
     */
    private String path;

    /**
     * File size in bytes (null for directories)
     */
    private Long size;

    /**
     * Last modified timestamp
     */
    private LocalDateTime lastModified;

    /**
     * File extension (null for directories or files without extension)
     */
    private String extension;
}