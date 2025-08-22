package dev.srivatsan.config_server.service.git;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.service.util.UtilService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
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
    private final UtilService utilService;

    String defaultConfig = "server:\n  port: 8080\n  servlet.context-path: /<app-name>\n\nspring:\n  application:\n    name: <app-name>\n\n  datasource:\n    url: jdbc:postgresql://<ip>:5432/<database-name>?currentSchema=<schema-name>\n    username: <user>\n    password: <password>\n    driver-class-name: org.postgresql.Driver\n    hikari:\n      maximum-pool-size: 30\n      minimum-idle: 15\n      pool-name: postgres-con\n      auto-commit: false\n\n  kafka:\n    bootstrap-servers: <kafka-ip>:9092\n\n    producer:\n      topic: <app-name>\n      key-serializer: org.apache.kafka.common.serialization.StringSerializer\n      value-serializer: org.apache.kafka.common.serialization.StringSerializer\n      acks: 0  # Acknowledgment level (0, 1, all/-1)\n      retries: 3\n      batch-size: 16384  # Batch size in bytes\n      properties:\n        linger.ms: 5\n        delivery.timeout.ms: 300000\n        allow.auto.create.topics: false\n\n    consumer:\n      topic: <app-name>\n      group-id: consumer-group-<app-name>\n      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer\n      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer\n      properties:\n        session.timeout.ms: 30000\n        max.poll.records: 500\n        max.poll.interval.ms: 300000\n        allow.auto.create.topics: false\n\n    listener:   # Listener configuration\n      concurrency: 3   # Concurrency level\n";

    public RepositoryService(ApplicationConfig applicationConfig, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
    }

    public void initializeConfigFile(String filePath, String absoluteFilePath, String appName) throws IOException {

        if (Files.exists(Paths.get(absoluteFilePath))) { /** ToDO: Handle with proper response */
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

    public void updateConfigFile(String filePath, String absoluteFilePath, String content, String appName) throws IOException, GitAPIException {

        if (!Files.exists(Paths.get(absoluteFilePath))) {
            log.error("Configuration file does not exist: {}", absoluteFilePath);
            throw new RuntimeException("Configuration file not found: " + absoluteFilePath);
        }

        File repoDir = new File(applicationConfig.getBasePath());
        try (Git git = Git.open(repoDir)) {

            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(filePath);

            Files.writeString(configFilePath, content);

            git.add().addFilepattern(filePath).call();
            git.commit().setMessage("Update config for ApplicationName - " + appName).call();
            log.info("Updated file: '{}'", configFilePath);

        } catch (RepositoryNotFoundException e) {
            log.error("Repository not found at path: {}", repoDir.getAbsolutePath(), e);
            throw e;
        } catch (IOException e) {
            log.error("Error updating file '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to update file due to IO error", e);
        } catch (GitAPIException e) {
            log.error("A Git command failed for '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to perform Git operation", e);
        }
    }



    public Map<String, Object> getCommitHistory(String filePath) throws IOException, GitAPIException {
        File repoDir = new File(applicationConfig.getBasePath());
        
        try (Git git = Git.open(repoDir)) {
            Repository repository = git.getRepository();
            
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> commits = new ArrayList<>();
            
            try (RevWalk revWalk = new RevWalk(repository)) {
                ObjectId headCommit = repository.resolve("HEAD");
                if (headCommit == null) {
                    result.put("message", "No commits found in repository");
                    result.put("filePath", filePath);
                    result.put("commits", commits);
                    return result;
                }

                revWalk.markStart(revWalk.parseCommit(headCommit));
                
                int count = 0;
                for (RevCommit commit : revWalk) {
                    if (count >= 10) break;
                    
                    boolean fileAffected = true;
                    if (filePath != null) {
                        fileAffected = isFileAffectedInCommit(repository, commit, filePath);
                    }
                    
                    if (fileAffected) {
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
                        commitInfo.put("message", commit.getShortMessage());
                        
                        commits.add(commitInfo);
                    }
                    count++;
                }
            }
            
            result.put("message", "Commit history (last 10 commits)");
            result.put("filePath", filePath);
            result.put("commits", commits);
            return result;
            
        } catch (RepositoryNotFoundException e) {
            log.error("Repository not found at path: {}", repoDir.getAbsolutePath(), e);
            throw e;
        } catch (IOException | GitAPIException e) {
            log.error("Error getting commit history: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isFileAffectedInCommit(Repository repository, RevCommit commit, String filePath) throws IOException {
        if (commit.getParentCount() == 0) {
            return true; // Initial commit affects all files
        }
        
        RevCommit parent = commit.getParent(0);
        
        try (DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream())) {
            formatter.setRepository(repository);
            
            AbstractTreeIterator parentTreeIterator = prepareTreeParser(repository, parent);
            AbstractTreeIterator commitTreeIterator = prepareTreeParser(repository, commit);
            
            List<DiffEntry> diffs = formatter.scan(parentTreeIterator, commitTreeIterator);
            
            for (DiffEntry diff : diffs) {
                if (diff.getNewPath().equals(filePath) || diff.getOldPath().equals(filePath)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    public Map<String, Object> getCommitDetails(String commitId, String filePath) throws IOException, GitAPIException {
        File repoDir = new File(applicationConfig.getBasePath());
        
        try (Git git = Git.open(repoDir)) {
            Repository repository = git.getRepository();
            
            ObjectId commitObjectId = repository.resolve(commitId);
            if (commitObjectId == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Commit not found: " + commitId);
                return errorResult;
            }
            
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(commitObjectId);
                
                PersonIdent author = commit.getAuthorIdent();
                String commitDate = Instant.ofEpochSecond(commit.getCommitTime())
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                Map<String, Object> result = new HashMap<>();
                result.put("commitId", commit.getId().getName());
                result.put("shortCommitId", commit.getId().abbreviate(7).name());
                result.put("author", author.getName());
                result.put("email", author.getEmailAddress());
                result.put("date", commitDate);
                result.put("message", commit.getFullMessage());
                
                List<DiffEntry> diffs = git.diff()
                        .setOldTree(commit.getParentCount() > 0 ? 
                            prepareTreeParser(repository, commit.getParent(0)) : null)
                        .setNewTree(prepareTreeParser(repository, commit))
                        .call();
                
                List<Map<String, Object>> changes = new ArrayList<>();
                
                for (DiffEntry diff : diffs) {
                    if (filePath == null || diff.getNewPath().equals(filePath) || diff.getOldPath().equals(filePath)) {
                        Map<String, Object> changeInfo = new HashMap<>();
                        changeInfo.put("changeType", diff.getChangeType().toString());
                        changeInfo.put("oldPath", diff.getOldPath());
                        changeInfo.put("newPath", diff.getNewPath());
                        
                        String cleanDiff = getSimpleCleanDiff(repository, diff);
                        changeInfo.put("cleanDiff", cleanDiff);
                        
                        changes.add(changeInfo);
                    }
                }
                
                result.put("changes", changes);
                result.put("totalChanges", changes.size());
                
                if (commit.getParentCount() == 0) {
                    result.put("note", "Initial commit");
                }
                
                return result;
            }
            
        } catch (RepositoryNotFoundException e) {
            log.error("Repository not found at path: {}", repoDir.getAbsolutePath(), e);
            throw e;
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


    private AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();
            return treeParser;
        }
    }

}
