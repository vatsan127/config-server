package dev.srivatsan.config_server.service.repository;

import dev.srivatsan.config_server.model.Payload;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing configuration files in a version control repository.
 * Provides operations for creating, updating, and retrieving configuration files
 * along with their version history and changes.
 *
 * @author srivatsan.n
 */
public interface RepositoryService {

    String DEFAULT_CONFIG_TEMPLATE = "server:\n  port: 8080\n  servlet.context-path: /<app-name>\n\nspring:\n  application:\n    name: <app-name>\n\n";

    /**
     * Creates and initializes a new configuration file for the specified application.
     * The file is created with a default template and committed to the repository.
     *
     * @param filePath the relative path where the configuration file should be created
     * @param appName  the name of the application for which the config is being created
     * @param email    the email address of the user creating the config (used for git commit author)
     * @throws RuntimeException if the file creation or git operations fail
     */
    void initializeConfigFile(String filePath, String appName, String email);

    /**
     * Updates an existing configuration file with new content and commits the changes.
     * The commit includes author information and a custom commit message.
     *
     * @param filePath the relative path of the configuration file to update
     * @param payload  the payload containing the new configuration content and metadata
     * @throws RuntimeException if the file update or git operations fail
     */
    void updateConfigFile(String filePath, Payload payload);

    /**
     * Retrieves the current content of a configuration file from the repository.
     *
     * @param filePath the relative path of the configuration file
     * @return the file content as a string
     * @throws IOException if the file cannot be read or the repository cannot be accessed
     */
    String getConfigFile(String filePath) throws IOException;

    /**
     * Retrieves the commit history for a specific configuration file.
     * Returns metadata about all commits that affected the file.
     *
     * @param filePath the relative path of the configuration file
     * @return a map containing commit information (commit ID, author, date, message)
     * @throws Exception if the repository cannot be accessed or git operations fail
     */
    Map<String, Object> getConfigFileHistory(String filePath) throws Exception;

    /**
     * Creates and initializes a namespace directory with a git repository.
     * This should be called before creating configuration files in a new namespace.
     *
     * @param namespace the namespace identifier
     * @throws GitAPIException if git operations fail
     * @throws IOException     if directory creation fails
     */
    void createNamespace(String namespace) throws GitAPIException, IOException;

    /**
     * Retrieves the changes made in a specific commit within a namespace.
     * Returns detailed information about what was modified in the commit.
     *
     * @param commitId  the unique identifier of the commit
     * @param namespace the namespace where the commit was made
     * @return a map containing change information (files changed, additions, deletions)
     * @throws IOException if the repository cannot be accessed or the commit cannot be found
     */
    Map<String, Object> getCommitChanges(String commitId, String namespace) throws IOException;

    /**
     * Retrieves a list of all available namespaces.
     * Returns the names of all namespace directories that exist in the base path.
     *
     * @return a list of namespace names
     */
    List<String> listNamespaces();

    /**
     * Retrieves the contents of a directory within a namespace.
     * Returns only .yml files and subdirectories in the specified path.
     *
     * @param namespace the namespace identifier
     * @param path      the relative directory path within the namespace (empty string for root)
     * @return a list of .yml file names and folder names
     * @throws RuntimeException if the namespace or directory is not found or cannot be accessed
     */
    List<String> listDirectoryContents(String namespace, String path);
}