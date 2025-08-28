package dev.srivatsan.config_server.service.repository;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.config.CacheConfig;
import dev.srivatsan.config_server.exception.ConfigFileException;
import dev.srivatsan.config_server.exception.ConfigConflictException;
import dev.srivatsan.config_server.exception.GitOperationException;
import dev.srivatsan.config_server.exception.NamespaceException;
import dev.srivatsan.config_server.model.Payload;
import dev.srivatsan.config_server.service.util.UtilService;
import dev.srivatsan.config_server.service.util.GitOperationHelper;
import dev.srivatsan.config_server.service.validation.ValidationService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.stream.Collectors;

import static org.eclipse.jgit.lib.Constants.HEAD;

@Service
public class GitBasedConfigService implements RepositoryService {

    private final Logger log = LoggerFactory.getLogger(GitBasedConfigService.class);
    private final ApplicationConfig applicationConfig;
    private final UtilService utilService;
    private final GitOperationHelper gitOperationHelper;
    private final CacheConfig.CacheEvictionService cacheEvictionService;
    private final ValidationService validationService;

    public GitBasedConfigService(ApplicationConfig applicationConfig, UtilService utilService, GitOperationHelper gitOperationHelper, CacheConfig.CacheEvictionService cacheEvictionService, ValidationService validationService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
        this.gitOperationHelper = gitOperationHelper;
        this.cacheEvictionService = cacheEvictionService;
        this.validationService = validationService;
    }

    public void createNamespace(String namespace) {
        validationService.validateNamespace(namespace);

        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);

        if (namespaceDir.exists()) {
            throw NamespaceException.alreadyExists(namespace);
        }

        boolean created = namespaceDir.mkdirs();
        if (!created) {
            throw NamespaceException.creationFailed(namespace,
                    new IOException("Failed to create namespace directory: " + namespaceDir.getAbsolutePath()));
        }

        try (Git git = Git.init().setDirectory(namespaceDir).call()) {
            log.info("Created and initialized namespace '{}' at: {}", namespace, namespaceDir.getAbsolutePath());
            
            // Clear namespace list cache and directory listings
            cacheEvictionService.evictKey("namespaces", "all");
            cacheEvictionService.evictAllFromCache("directory-listing");
        } catch (GitAPIException e) {
            log.error("Failed to initialize git repository for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.initFailed(namespace, e);
        }
    }

    public void initializeConfigFile(String filePath, String appName, String email) {
        validationService.validateSafePath(filePath);
        validationService.validateAppName(appName);
        validationService.validateEmail(email);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        gitOperationHelper.executeGitVoidOperation(namespace, git -> {
            createConfigFileWithContent(git, relativePath, appName, filePath);
            commitNewFile(git, relativePath, appName, email);
            
            // Clear directory listings for this namespace
            String directoryPath = relativePath.contains("/") ? relativePath.substring(0, relativePath.lastIndexOf("/")) : "/";
            cacheEvictionService.evictKey("directory-listing", namespace + "_" + directoryPath);
        });
    }

    public void updateConfigFile(String filePath, Payload payload) {
        validationService.validateSafePath(filePath);
        validationService.validateEmail(payload.getEmail());
        validationService.validateYamlContent(payload.getContent());
        validationService.validateCommitMessage(payload.getMessage());
        validationService.validateCommitId(payload.getCommitId());

        String commitMessage = payload.getMessage();
        String email = payload.getEmail();
        String expectedCommitId = payload.getCommitId();
        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        gitOperationHelper.executeGitVoidOperation(namespace, git -> {
            // Optimistic lock check - validate current commit ID matches expected
            var logCommand = git.log()
                    .setMaxCount(1)
                    .add(git.getRepository().resolve(HEAD))
                    .addPath(relativePath);

            String currentCommitId = null;
            for (RevCommit commit : logCommand.call()) {
                currentCommitId = commit.getId().getName();
                break;
            }

            if (currentCommitId == null) {
                throw ConfigFileException.notFound(filePath);
            }

            if (!expectedCommitId.equals(currentCommitId)) {
                throw ConfigConflictException.conflictDetected(filePath);
            }

            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(relativePath);

            if (!Files.exists(configFilePath)) {
                throw ConfigFileException.notFound(filePath);
            }

            Files.writeString(configFilePath, payload.getContent());

            git.add().addFilepattern(relativePath).call();
            git.commit()
                    .setMessage(commitMessage)
                    .setAuthor(email.substring(0, email.indexOf('@')), email)
                    .call();
            
            log.info("Updated file: '{}', with message: '{}'", configFilePath, commitMessage);
            
            // Evict specific cache entries for this file
            cacheEvictionService.evictKey("config-content", filePath);
            cacheEvictionService.evictKey("commit-history", filePath);
            cacheEvictionService.evictKey("latest-commit", filePath);
        });
    }

    @Cacheable(value = "config-content", key = "#filePath")
    public String getConfigFile(String filePath) {
        validationService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        return gitOperationHelper.executeGitOperation(namespace, git -> {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(relativePath);

            if (!Files.exists(configFilePath)) {
                throw ConfigFileException.notFound(filePath);
            }

            return Files.readString(configFilePath);
        });
    }

    @Cacheable(value = "latest-commit", key = "#filePath")
    public String getLatestCommitId(String filePath) {
        validationService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        return gitOperationHelper.executeGitOperation(namespace, git -> {
            var logCommand = git.log()
                    .setMaxCount(1)
                    .add(git.getRepository().resolve(HEAD))
                    .addPath(relativePath);

            for (RevCommit commit : logCommand.call()) {
                return commit.getId().getName();
            }
            throw ConfigFileException.notFound(filePath);
        });
    }

    @Override
    @Cacheable(value = "commit-history", key = "#filePath")
    public Map<String, Object> getConfigFileHistory(String filePath) {
        validationService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        return gitOperationHelper.executeGitOperation(namespace, git -> {
            var logCommand = git.log()
                    .setMaxCount(applicationConfig.getCommitHistorySize())
                    .add(git.getRepository().resolve(HEAD))
                    .addPath(relativePath);

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
        });
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

    @Cacheable(value = "commit-details", key = "#commitId + '_' + #namespace")
    public Map<String, Object> getCommitChanges(String commitId, String namespace) {
        validationService.validateCommitId(commitId);
        validationService.validateNamespace(namespace);

        return gitOperationHelper.executeGitOperation(namespace, git -> {
            Map<String, Object> result = new HashMap<>();
            
            try (Repository repository = git.getRepository();
                 RevWalk revWalk = new RevWalk(repository)) {

                RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
                result.put("commitId", commit.getName());
                result.put("message", commit.getFullMessage());
                result.put("author", commit.getAuthorIdent().getName());
                result.put("commitTime", new Date(commit.getCommitTime() * 1000L));

                if (commit.getParentCount() > 0) {
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream(); 
                         DiffFormatter df = new DiffFormatter(out)) {
                        df.setRepository(repository);
                        df.format(df.scan(commit.getParent(0), commit).getFirst());
                        result.put("changes", out.toString());
                    }
                } else {
                    result.put("changes", "Initial commit - file created");
                }
            }
            
            return result;
        });
    }


    private void createConfigFileWithContent(Git git, String relativePath, String appName, String filePath) throws IOException {
        Path workTree = git.getRepository().getWorkTree().toPath();
        Path newFilePath = workTree.resolve(relativePath);

        if (Files.exists(newFilePath)) {
            throw ConfigFileException.alreadyExists(filePath);
        }

        Files.createDirectories(newFilePath.getParent());
        String configContent = DEFAULT_CONFIG_TEMPLATE.replace("<app-name>", appName);
        validationService.validateYamlContent(configContent);
        Files.writeString(newFilePath, configContent);
        
        log.info("Created file: '{}'", newFilePath);
    }

    private void commitNewFile(Git git, String relativePath, String appName, String email) throws GitAPIException {
        git.add().addFilepattern(relativePath).call();
        git.commit()
                .setMessage("First commit ApplicationName - " + appName)
                .setAuthor(email.substring(0, email.indexOf('@')), email)
                .call();
    }


}
