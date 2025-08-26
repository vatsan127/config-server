package dev.srivatsan.config_server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a directory listing response that separates files and directories.
 * Provides a clear structure showing both files and subdirectories within a namespace path.
 *
 * @author srivatsan.n
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirectoryListing {
    
    /**
     * The namespace being listed
     */
    private String namespace;
    
    /**
     * The path within the namespace being listed
     */
    private String path;
    
    /**
     * List of files in the directory
     */
    private List<DirectoryEntry> files;
    
    /**
     * List of subdirectories in the directory
     */
    private List<DirectoryEntry> directories;
    
    /**
     * Total count of items (files + directories)
     */
    private int totalCount;
    
    /**
     * Count of files
     */
    private int fileCount;
    
    /**
     * Count of directories
     */
    private int directoryCount;
}