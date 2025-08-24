package dev.srivatsan.config_server.service.repository;

import dev.srivatsan.config_server.model.Payload;
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
public interface RepositoryService {

    String DEFAULT_CONFIG_TEMPLATE = "server:\n  port: 8080\n  servlet.context-path: /<app-name>\n\nspring:\n  application:\n    name: <app-name>\n\n  datasource:\n    url: jdbc:postgresql://<ip>:5432/<database-name>?currentSchema=<schema-name>\n    username: <user>\n    password: <password>\n    driver-class-name: org.postgresql.Driver\n    hikari:\n      maximum-pool-size: 30\n      minimum-idle: 15\n      pool-name: postgres-con\n      auto-commit: false\n\n  kafka:\n    bootstrap-servers: <kafka-ip>:9092\n\n    producer:\n      topic: <app-name>\n      key-serializer: org.apache.kafka.common.serialization.StringSerializer\n      value-serializer: org.apache.kafka.common.serialization.StringSerializer\n      acks: 0  # Acknowledgment level (0, 1, all/-1)\n      retries: 3\n      batch-size: 16384  # Batch size in bytes\n      properties:\n        linger.ms: 5\n        delivery.timeout.ms: 300000\n        allow.auto.create.topics: false\n\n    consumer:\n      topic: <app-name>\n      group-id: consumer-group-<app-name>\n      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer\n      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer\n      properties:\n        session.timeout.ms: 30000\n        max.poll.records: 500\n        max.poll.interval.ms: 300000\n        allow.auto.create.topics: false\n\n    listener:   # Listener configuration\n      concurrency: 3   # Concurrency level\n";

    /**
     * Creates and initializes a new configuration file for the specified application.
     * The file is created with a default template and committed to the repository.
     *
     * @param filePath the relative path where the configuration file should be created
     * @param appName  the name of the application for which the config is being created
     * @throws RuntimeException if the file creation or git operations fail
     */
    void initializeConfigFile(String filePath, String appName);

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
     * @throws IOException if directory creation fails
     */
    void createNamespace(String namespace) throws GitAPIException, IOException;

    /**
     * Retrieves the changes made in a specific commit within a namespace.
     * Returns detailed information about what was modified in the commit.
     *
     * @param commitId the unique identifier of the commit
     * @param namespace the namespace where the commit was made
     * @return a map containing change information (files changed, additions, deletions)
     * @throws IOException if the repository cannot be accessed or the commit cannot be found
     */
    Map<String, Object> getCommitChanges(String commitId, String namespace) throws IOException;
}