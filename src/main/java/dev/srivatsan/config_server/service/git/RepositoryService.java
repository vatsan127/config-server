package dev.srivatsan.config_server.service.git;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.service.util.UtilService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class RepositoryService {

    private final Logger log = LoggerFactory.getLogger(RepositoryService.class);
    private final ApplicationConfig applicationConfig;
    private final UtilService utilService;

    String defaultConfig = "server:\n  port: 8080\n  servlet.context-path: /<app-name>\n\nspring:\n  application:\n    name: <app-name>\n\n  datasource:\n    url: jdbc:postgresql://<ip>:5432/<database-name>?currentSchema=<schema-name>\n    username: <user>\n    password: <password>\n    driver-class-name: org.postgresql.Driver\n    hikari:\n      maximum-pool-size: 30\n      minimum-idle: 15\n      pool-name: postgres-con\n      auto-commit: false\n\n  kafka:\n    bootstrap-servers: <kafka-ip>:9092\n\n    producer:\n      topic: <app-name>\n      key-serializer: org.apache.kafka.common.serialization.StringSerializer\n      value-serializer: org.apache.kafka.common.serialization.StringSerializer\n      acks: 0  # Acknowledgment level (0, 1, all/-1)\n      retries: 3\n      batch-size: 16384  # Batch size in bytes\n      properties:\n        linger.ms: 5\n        delivery.timeout.ms: 300000\n        allow.auto.create.topics: false\n\n    consumer:\n      topic: <app-name>\n      group-id: consumer-group-<app-name>\n      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer\n      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer\n      properties:\n        session.timeout.ms: 30000\n        max.poll.records: 500\n        max.poll.interval.ms: 300000\n        allow.auto.create.topics: false\n\n    listener:   # Listener configuration\n      concurrency: 3   # Concurrency level\n";

    public RepositoryService(ApplicationConfig applicationConfig, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
    }

    public void createAppConfig(String filePath, String absoluteFilePath, String appName) throws IOException {

        if (Files.exists(Paths.get(absoluteFilePath))) {
            log.info("Application Config already exists: {}", absoluteFilePath);
            return;
        }

        File repoDir = new File(applicationConfig.getBasePath());
        try (Git git = Git.open(repoDir)) {

            Path workTree = git.getRepository().getWorkTree().toPath();
            Path newFilePath = workTree.resolve(filePath);

            Files.createDirectories(newFilePath.getParent());
            Files.writeString(newFilePath, defaultConfig.replace("<app-name>", appName));

            git.add().addFilepattern(filePath).call();
            git.commit().setMessage("First commit ApplicationName - " + appName).call();
            log.info("Created file: '{}'", newFilePath);

        } catch (RepositoryNotFoundException e) {
            log.error("Repository not found at path: {}", repoDir.getAbsolutePath(), e);
            throw e;
        } catch (IOException e) {
            log.error("Error writing file '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to write file due to IO error", e);
        } catch (GitAPIException e) {
            log.error("A Git command failed for '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to perform Git operation", e);
        }
    }

}
