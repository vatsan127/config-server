package dev.srivatsan.config_server.service.git;

import dev.srivatsan.config_server.config.ApplicationConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.lib.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepositoryService {

    private final Logger log = LoggerFactory.getLogger(RepositoryService.class);
    private final ApplicationConfig applicationConfig;

    private static final String DEFAULT_CONFIG_TEMPLATE = "server:\n  port: 8080\n  servlet.context-path: /<app-name>\n\nspring:\n  application:\n    name: <app-name>\n\n  datasource:\n    url: jdbc:postgresql://<ip>:5432/<database-name>?currentSchema=<schema-name>\n    username: <user>\n    password: <password>\n    driver-class-name: org.postgresql.Driver\n    hikari:\n      maximum-pool-size: 30\n      minimum-idle: 15\n      pool-name: postgres-con\n      auto-commit: false\n\n  kafka:\n    bootstrap-servers: <kafka-ip>:9092\n\n    producer:\n      topic: <app-name>\n      key-serializer: org.apache.kafka.common.serialization.StringSerializer\n      value-serializer: org.apache.kafka.common.serialization.StringSerializer\n      acks: 0  # Acknowledgment level (0, 1, all/-1)\n      retries: 3\n      batch-size: 16384  # Batch size in bytes\n      properties:\n        linger.ms: 5\n        delivery.timeout.ms: 300000\n        allow.auto.create.topics: false\n\n    consumer:\n      topic: <app-name>\n      group-id: consumer-group-<app-name>\n      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer\n      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer\n      properties:\n        session.timeout.ms: 30000\n        max.poll.records: 500\n        max.poll.interval.ms: 300000\n        allow.auto.create.topics: false\n\n    listener:   # Listener configuration\n      concurrency: 3   # Concurrency level\n";

    public RepositoryService(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    private Git openRepository() throws IOException {
        File repoDir = new File(applicationConfig.getBasePath());
        return Git.open(repoDir);
    }

    private Map<String, Object> formatCommitInfo(RevCommit commit) {
        PersonIdent author = commit.getAuthorIdent();
        String commitDate = Instant.ofEpochSecond(commit.getCommitTime())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, Object> commitInfo = new HashMap<>();
        commitInfo.put("commitId", commit.getId().getName());
        commitInfo.put("shortCommitId", commit.getId().abbreviate(7).name());
        commitInfo.put("author", author.getName());
        commitInfo.put("email", author.getEmailAddress());
        commitInfo.put("date", commitDate);
        return commitInfo;
    }

    private Map<String, Object> createChangeInfo(DiffEntry diff, Repository repository) throws IOException {
        Map<String, Object> changeInfo = new HashMap<>();
        changeInfo.put("changeType", diff.getChangeType().toString());
        changeInfo.put("oldPath", diff.getOldPath());
        changeInfo.put("newPath", diff.getNewPath());
        changeInfo.put("cleanDiff", getSimpleCleanDiff(repository, diff));
        return changeInfo;
    }

    public void initializeConfigFile(String filePath, String appName) throws IOException {
        try (Git git = openRepository()) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path newFilePath = workTree.resolve(filePath);

            if (Files.exists(newFilePath)) {
                log.info("Application Config already exists: {}", newFilePath);
                return;
            }

            Files.createDirectories(newFilePath.getParent());
            Files.writeString(newFilePath, DEFAULT_CONFIG_TEMPLATE.replace("<app-name>", appName));

            git.add().addFilepattern(filePath).call();
            git.commit().setMessage("First commit ApplicationName - " + appName).call();
            log.info("Created file: '{}'", newFilePath);

        } catch (IOException | GitAPIException e) {
            log.error("Error initializing config file '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize config file", e);
        }
    }

    public void updateConfigFile(String filePath, String content, String appName) throws IOException, GitAPIException {
        try (Git git = openRepository()) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(filePath);

            if (!Files.exists(configFilePath)) {
                log.error("Configuration file does not exist: {}", configFilePath);
                throw new RuntimeException("Configuration file not found: " + configFilePath);
            }

            Files.writeString(configFilePath, content);

            git.add().addFilepattern(filePath).call();
            git.commit().setMessage("Update config for ApplicationName - " + appName).call();
            log.info("Updated file: '{}'", configFilePath);

        } catch (IOException | GitAPIException e) {
            log.error("Error updating config file '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to update config file", e);
        }
    }

    public String getFileContent(String filePath) throws IOException {
        try (Git git = openRepository()) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(filePath);

            if (!Files.exists(configFilePath)) {
                throw new RuntimeException("Configuration file not found: " + configFilePath);
            }

            return Files.readString(configFilePath);
            
        } catch (IOException e) {
            log.error("Error reading file '{}': {}", filePath, e.getMessage(), e);
            throw e;
        }
    }


    public Map<String, Object> getCommitHistory(String filePath) throws IOException, GitAPIException {
        try (Git git = openRepository()) {
            var logCommand = git.log()
                    .setMaxCount(10)
                    .add(git.getRepository().resolve("HEAD"));
            
            if (filePath != null) {
                logCommand.addPath(filePath);
            }
            
            List<Map<String, Object>> commits = new ArrayList<>();
            for (RevCommit commit : logCommand.call()) {
                Map<String, Object> commitInfo = formatCommitInfo(commit);
                commitInfo.put("message", commit.getShortMessage());
                commits.add(commitInfo);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Commit history (last 10 commits)");
            result.put("filePath", filePath);
            result.put("commits", commits);
            return result;
            
        } catch (IOException | GitAPIException e) {
            log.error("Error getting commit history: {}", e.getMessage(), e);
            throw e;
        }
    }


    public Map<String, Object> getCommitDetails(String commitId, String filePath) throws IOException, GitAPIException {
        try (Git git = openRepository()) {
            Repository repository = git.getRepository();
            
            ObjectId commitObjectId = repository.resolve(commitId);
            if (commitObjectId == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Commit not found: " + commitId);
                return errorResult;
            }
            
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(commitObjectId);
                
                Map<String, Object> result = formatCommitInfo(commit);
                result.put("message", commit.getFullMessage());
                
                try (ObjectReader reader = repository.newObjectReader()) {
                    CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                    newTreeParser.reset(reader, commit.getTree().getId());
                    
                    List<DiffEntry> diffs;
                    if (commit.getParentCount() > 0) {
                        // Compare with parent commit
                        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                        oldTreeParser.reset(reader, commit.getParent(0).getTree().getId());
                        
                        diffs = git.diff()
                                .setOldTree(oldTreeParser)
                                .setNewTree(newTreeParser)
                                .call();
                    } else {
                        // Initial commit - show all files as additions
                        diffs = git.diff()
                                .setOldTree(null)
                                .setNewTree(newTreeParser)
                                .call();
                    }
                    
                    List<Map<String, Object>> changes = new ArrayList<>();
                    for (DiffEntry diff : diffs) {
                        if (filePath == null || diff.getNewPath().equals(filePath) || diff.getOldPath().equals(filePath)) {
                            changes.add(createChangeInfo(diff, repository));
                        }
                    }
                    
                    result.put("changes", changes);
                    result.put("totalChanges", changes.size());
                    
                    if (commit.getParentCount() == 0) {
                        result.put("note", "Initial commit");
                    }
                }
                
                return result;
            }
            
        } catch (IOException | GitAPIException e) {
            log.error("Error getting commit details for {}: {}", commitId, e.getMessage(), e);
            throw e;
        }
    }
    
    private String getSimpleCleanDiff(Repository repository, DiffEntry diff) throws IOException {
        StringBuilder cleanDiff = new StringBuilder();
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DiffFormatter formatter = new DiffFormatter(out)) {
            
            formatter.setRepository(repository);
            formatter.format(diff);
            
            String fullDiff = out.toString("UTF-8");
            String[] lines = fullDiff.split("\n");
            
            for (String line : lines) {
                if (line.startsWith("+++") || line.startsWith("---") || 
                    line.startsWith("@@") || line.startsWith("diff --git") || 
                    line.startsWith("index ")) {
                    continue;
                }
                if (line.startsWith("+") || line.startsWith("-")) {
                    cleanDiff.append(line).append("\n");
                }
            }
        }
        
        return cleanDiff.toString();
    }



}
