package com.github.config_server.service.repository;

import com.github.config_server.model.Payload;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Map;

/**
 * Service interface for managing configuration files in a version control repository.
 * Provides operations for creating, updating, and retrieving configuration files
 * along with their version history and changes.
 *
 * @author srivatsan.n
 */
public sealed interface GitRepositoryService permits GitRepositoryServiceImpl {

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
     * @return
     * @throws RuntimeException if the file update or git operations fail
     */
    String updateConfigFile(String filePath, Payload payload);

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
     * Retrieves the latest commit ID for a specific configuration file.
     * Returns the commit ID of the most recent commit that affected the file.
     *
     * @param filePath the relative path of the configuration file
     * @return the latest commit ID as a string
     * @throws RuntimeException if the file is not found or repository cannot be accessed
     */
    String getLatestCommitId(String filePath);

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
     * Deletes an existing configuration file and commits the change to the repository.
     * The commit includes author information and a custom commit message.
     *
     * @param filePath the relative path of the configuration file to delete
     * @param payload  the payload containing metadata including commit message and author email
     * @throws RuntimeException if the file deletion or git operations fail
     */
    void deleteConfigFile(String filePath, Payload payload);

    /**
     * Deletes an entire namespace directory and all its contents.
     * This operation permanently removes the namespace and cannot be undone.
     *
     * @param namespace the namespace identifier to delete
     * @throws RuntimeException if the namespace deletion fails or namespace doesn't exist
     */
    void deleteNamespace(String namespace);

    /**
     * Retrieves the complete event history (git log) for an entire namespace.
     * Returns Git history for all files and activities within the namespace root directory.
     * The number of events returned is limited by the commit-history-size configuration.
     *
     * @param namespace the namespace identifier to get events for
     * @return a map containing namespace event history (commit ID, author, date, message)
     * @throws Exception if the namespace is not found or git operations fail
     */
    Map<String, Object> getNamespaceEvents(String namespace) throws Exception;

    /**
     * Retrieves API call status notifications for the last commit-history-size operations.
     * Returns status information including trigger time, app name, success/failure counts, and status
     * (success, in-progress, failed) for each namespace operation.
     *
     * @param namespace the namespace identifier to get notifications for
     * @return a map containing notification status information
     * @throws Exception if the namespace is not found or operation fails
     */
    Map<String, Object> getNamespaceNotifications(String namespace) throws Exception;

}