package dev.srivatsan.config_server.service.repository;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.Payload;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.eclipse.jgit.lib.Constants.HEAD;

@Service
public class GitBasedConfigService implements RepositoryService {

    private final Logger log = LoggerFactory.getLogger(GitBasedConfigService.class);
    private final ApplicationConfig applicationConfig;

    public GitBasedConfigService(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    private Git openRepository(String namespace) throws IOException {
        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        if (!namespaceDir.exists()) {
            throw new IOException("Namespace directory does not exist: " + namespace + 
                ". Please create namespace first using /namespace/create endpoint.");
        }
        return Git.open(namespaceDir);
    }

    public void createNamespace(String namespace) throws GitAPIException, IOException {
        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        
        if (namespaceDir.exists()) {
            log.info("Namespace directory already exists: {}", namespaceDir.getAbsolutePath());
            return;
        }
        
        boolean created = namespaceDir.mkdirs();
        if (!created) {
            throw new IOException("Failed to create namespace directory: " + namespaceDir.getAbsolutePath());
        }
        
        try (Git git = Git.init().setDirectory(namespaceDir).call()) {
            log.info("Created and initialized namespace '{}' at: {}", namespace, namespaceDir.getAbsolutePath());
        } catch (GitAPIException e) {
            log.error("Failed to initialize git repository for namespace '{}': {}", namespace, e.getMessage());
            throw e;
        }
    }

    public void initializeConfigFile(String filePath, String appName) {
        String namespace = extractNamespaceFromFilePath(filePath);
        String relativePath = getRelativePathWithinNamespace(filePath);
        try (Git git = openRepository(namespace)) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path newFilePath = workTree.resolve(relativePath);

            if (Files.exists(newFilePath)) {
                log.info("Application Config already exists: {}", newFilePath);
                return;
            }

            Files.createDirectories(newFilePath.getParent());
            Files.writeString(newFilePath, DEFAULT_CONFIG_TEMPLATE.replace("<app-name>", appName));

            git.add().addFilepattern(relativePath).call();
            git.commit().setMessage("First commit ApplicationName - " + appName).call();
            log.info("Created file: '{}'", newFilePath);

        } catch (IOException | GitAPIException e) {
            log.error("Error initializing config file '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to initialize config file", e);
        }
    }

    public void updateConfigFile(String filePath, Payload payload) {
        String commitMessage = payload.getMessage();
        String email = payload.getEmail();
        String namespace = extractNamespaceFromFilePath(filePath);
        String relativePath = getRelativePathWithinNamespace(filePath);

        try (Git git = openRepository(namespace)) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(relativePath);

            if (!Files.exists(configFilePath)) {
                log.error("Configuration file does not exist: {}", configFilePath);
                throw new RuntimeException("Configuration file not found: " + configFilePath);
            }

            Files.writeString(configFilePath, payload.getContent());

            git.add().addFilepattern(relativePath).call();
            git.commit()
                    .setMessage(commitMessage)
                    .setAuthor(email.substring(0, email.indexOf('@')), email)
                    .call();
            log.info("Updated file: '{}', with message: '{}'", configFilePath, commitMessage);

        } catch (IOException | GitAPIException e) {
            log.error("Error updating config file '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to update config file", e);
        }
    }

    public String getConfigFile(String filePath) throws IOException {
        String namespace = extractNamespaceFromFilePath(filePath);
        String relativePath = getRelativePathWithinNamespace(filePath);
        try (Git git = openRepository(namespace)) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(relativePath);

            if (!Files.exists(configFilePath)) {
                throw new RuntimeException("Configuration file not found: " + configFilePath);
            }

            return Files.readString(configFilePath);

        } catch (IOException e) {
            log.error("Error reading file '{}': {}", filePath, e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, Object> getConfigFileHistory(String filePath) throws Exception {
        String namespace = extractNamespaceFromFilePath(filePath);
        try (Git git = openRepository(namespace)) {
            var logCommand = git.log()
                    .setMaxCount(applicationConfig.getCommitHistorySize())
                    .add(git.getRepository().resolve(HEAD));

            String relativePath = getRelativePathWithinNamespace(filePath);
            logCommand.addPath(relativePath);

            List<Map<String, Object>> commits = new ArrayList<>();
            for (RevCommit commit : logCommand.call()) {
                Map<String, Object> commitInfo = formatCommitInfo(commit);
                commitInfo.put("message", commit.getShortMessage());
                commits.add(commitInfo);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("filePath", filePath);
            result.put("commits", commits);
            return result;

        } catch (IOException | GitAPIException e) {
            log.error("Error getting commit history: {}", e.getMessage(), e);
            throw e;
        }
    }

    private Map<String, Object> formatCommitInfo(RevCommit commit) {
        PersonIdent author = commit.getAuthorIdent();
        String commitDate = Instant.ofEpochSecond(commit.getCommitTime())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, Object> commitInfo = new HashMap<>();
        commitInfo.put("commitId", commit.getId().getName());
        commitInfo.put("author", author.getName());
        commitInfo.put("email", author.getEmailAddress());
        commitInfo.put("date", commitDate);
        return commitInfo;
    }

    public Map<String, Object> getCommitChanges(String commitId, String namespace) throws IOException {
        Map<String, Object> result = new HashMap<>();

        try (Git git = openRepository(namespace);
             Repository repository = git.getRepository();
             RevWalk revWalk = new RevWalk(repository)) {

            RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
            result.put("commitId", commit.getName());
            result.put("message", commit.getFullMessage());
            result.put("author", commit.getAuthorIdent().getName());
            result.put("commitTime", new Date(commit.getCommitTime() * 1000L));

            if (commit.getParentCount() > 0) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream(); DiffFormatter df = new DiffFormatter(out)) {
                    df.setRepository(repository);
                    df.format(df.scan(commit.getParent(0), commit).getFirst());
                    result.put("changes", out.toString());
                }
            } else {
                result.put("changes", "Initial commit - file created");
            }
        }

        return result;
    }

    private String extractNamespaceFromFilePath(String filePath) {
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        int slashIndex = filePath.indexOf('/');
        if (slashIndex == -1) {
            return filePath;
        }
        return filePath.substring(0, slashIndex);
    }

    private String getRelativePathWithinNamespace(String filePath) {
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        int slashIndex = filePath.indexOf('/');
        if (slashIndex == -1) {
            return "";
        }
        return filePath.substring(slashIndex + 1);
    }

}
