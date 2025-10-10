package com.github.config_server.service.repository;

import com.github.config_server.config.ApplicationConfig;
import com.github.config_server.exception.ConfigConflictException;
import com.github.config_server.exception.ConfigFileException;
import com.github.config_server.exception.GitOperationException;
import com.github.config_server.exception.NamespaceException;
import com.github.config_server.model.Notification;
import com.github.config_server.model.Payload;
import com.github.config_server.service.cache.CacheManagerService;
import com.github.config_server.service.notify.ClientNotifyService;
import com.github.config_server.service.notify.NotificationStorageService;
import com.github.config_server.service.operation.GitOperationService;
import com.github.config_server.service.secret.SecretProcessor;
import com.github.config_server.service.util.UtilService;
import com.github.config_server.service.validation.ValidationService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.eclipse.jgit.lib.Constants.HEAD;

@Service
public non-sealed class GitRepositoryServiceImpl implements GitRepositoryService {

    private final Logger log = LoggerFactory.getLogger(GitRepositoryServiceImpl.class);
    private final ApplicationConfig applicationConfig;
    private final UtilService utilService;
    private final GitOperationService gitOperationService;
    private final CacheManagerService cacheManagerService;
    private final ValidationService validationService;
    private final ClientNotifyService clientNotifyService;
    private final NotificationStorageService notificationStorageService;
    private final SecretProcessor secretProcessor;

    public GitRepositoryServiceImpl(ApplicationConfig applicationConfig,
                                    UtilService utilService,
                                    GitOperationService gitOperationService,
                                    CacheManagerService cacheManagerService,
                                    ValidationService validationService,
                                    ClientNotifyService clientNotifyService,
                                    NotificationStorageService notificationStorageService,
                                    SecretProcessor secretProcessor) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
        this.gitOperationService = gitOperationService;
        this.cacheManagerService = cacheManagerService;
        this.validationService = validationService;
        this.clientNotifyService = clientNotifyService;
        this.notificationStorageService = notificationStorageService;
        this.secretProcessor = secretProcessor;
    }

    public void createNamespace(String namespace) {
        validationService.validateNamespace(namespace);

        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);

        if (namespaceDir.exists()) {
            throw NamespaceException.alreadyExists(namespace);
        }

        boolean created = namespaceDir.mkdirs();
        if (!created) {
            throw NamespaceException.creationFailed(namespace, new IOException("Failed to create namespace directory: " + namespaceDir.getAbsolutePath()));
        }

        try (Git git = Git.init().setDirectory(namespaceDir).call()) {
            File vaultDir = new File(namespaceDir, ".vault");
            if (!vaultDir.exists()) {
                boolean vaultCreated = vaultDir.mkdirs();
                if (vaultCreated) {
                    log.info("Created vault directory for namespace '{}': {}", namespace, vaultDir.getAbsolutePath());
                } else {
                    log.warn("Failed to create vault directory for namespace '{}': {}", namespace, vaultDir.getAbsolutePath());
                }
            }

            log.info("Created and initialized namespace '{}' at: {}", namespace, namespaceDir.getAbsolutePath());

            // Clear namespace list cache and directory listings
            cacheManagerService.evictKey("namespaces", "all");
            // Clear directory listings cache since parent directories now contain the new namespace
            cacheManagerService.evictAllFromCache("directory-listing");
        } catch (GitAPIException e) {
            log.error("Failed to initialize git repository for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.initFailed(namespace, e);
        }
    }

    public void initializeConfigFile(String filePath, String appName, String email) {
        validationService.validateSafePath(filePath);
        validationService.validateAppName(appName);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            createConfigFileWithContent(git, relativePath, appName, filePath);
            commitNewFile(git, relativePath, appName, email);

            // Clear all directory listings for this namespace
            cacheManagerService.evictAllFromCache("directory-listing");

            // Clear namespace-level caches since a new file was created
            cacheManagerService.evictKey("namespace-events", namespace);
            cacheManagerService.evictKey("namespace-notifications", namespace);

        });
    }

    public String updateConfigFile(String filePath, Payload payload) {
        validationService.validateSafePath(filePath);
        validationService.validateYamlContent(payload.getContent());
        validationService.validateCommitMessage(payload.getMessage());
        validationService.validateCommitId(payload.getCommitId());

        String commitMessage = payload.getMessage();
        String email = payload.getEmail();
        String expectedCommitId = payload.getCommitId();
        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        String commitId = gitOperationService.executeGitOperation(namespace, git -> {
                    var headRef = git.getRepository().resolve(HEAD);
                    if (headRef == null) {
                        throw ConfigFileException.notFound("No commits found in repository for file: " + filePath);
                    }

                    var logCommand = git.log()
                            .setMaxCount(1)
                            .add(headRef)
                            .addPath(relativePath);

                    String currentCommitId = null;
                    for (RevCommit commit : logCommand.call()) {
                        currentCommitId = commit.getId().getName();
                        break;
                    }

                    if (!expectedCommitId.equals(currentCommitId)) {
                        throw ConfigConflictException.conflictDetected(payload.getAppName());
                    }

                    Path workTree = git.getRepository().getWorkTree().toPath();
                    Path configFilePath = workTree.resolve(relativePath);

                    if (!Files.exists(configFilePath)) {
                        throw ConfigFileException.notFound(filePath);
                    }

                    // Process configuration to replace vault keys with encrypted placeholders before storing in git
                    String processedContent = secretProcessor.processConfigurationForInternal(payload.getContent(), namespace);
                    Files.writeString(configFilePath, processedContent);

                    git.add().addFilepattern(relativePath).call();
                    RevCommit revCommit = git.commit()
                            .setMessage(commitMessage)
                            .setAuthor(email.substring(0, email.indexOf('@')), email)
                            .call();

                    log.info("Updated file: '{}', with message: '{}'", configFilePath, commitMessage);

                    // Evict specific cache entries for this file
                    cacheManagerService.evictKey("config-content", filePath);
                    cacheManagerService.evictKey("commit-history", filePath);
                    cacheManagerService.evictKey("latest-commit", filePath);

                    // Clear namespace-level caches since a file was updated
                    cacheManagerService.evictKey("namespace-events", namespace);
                    cacheManagerService.evictKey("namespace-notifications", namespace);

                    return revCommit.getId().getName();
                }
        );
        clientNotifyService.sendRefreshNotifications(namespace, payload.getAppName(), commitId);
        return commitId;

    }

    @Override
    @Cacheable(value = "config-content", key = "#filePath")
    public String getConfigFile(String filePath) {
        validationService.validateSafePath(filePath);
        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        String rawContent = gitOperationService.executeGitOperation(namespace, git -> {
                    Path workTree = git.getRepository().getWorkTree().toPath();
                    Path configFilePath = workTree.resolve(relativePath);

                    if (!Files.exists(configFilePath)) {
                        throw ConfigFileException.notFound(filePath);
                    }

                    return Files.readString(configFilePath);
                }
        );

        return secretProcessor.processConfigurationForInternal(rawContent, namespace);
    }

    @Cacheable(value = "latest-commit", key = "#filePath")
    public String getLatestCommitId(String filePath) {
        validationService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        return gitOperationService.executeGitOperation(namespace, git -> {
                    var headRef = git.getRepository().resolve(HEAD);
                    if (headRef == null) {
                        throw ConfigFileException.notFound("No commits found in repository for file: " + filePath);
                    }

                    var logCommand = git.log()
                            .setMaxCount(1)
                            .add(headRef)
                            .addPath(relativePath);

                    for (RevCommit commit : logCommand.call()) {
                        return commit.getId().getName();
                    }
                    throw ConfigFileException.notFound(filePath);
                }
        );
    }

    @Override
    @Cacheable(value = "commit-history", key = "#filePath")
    public Map<String, Object> getConfigFileHistory(String filePath) {
        validationService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        return gitOperationService.executeGitOperation(namespace, git -> {
                    List<Map<String, Object>> commits = new ArrayList<>();

                    var headRef = git.getRepository().resolve(HEAD);
                    if (headRef != null) {
                        var logCommand = git.log()
                                .setMaxCount(applicationConfig.getCommitHistorySize())
                                .add(headRef)
                                .addPath(relativePath);

                        for (RevCommit commit : logCommand.call()) {
                            Map<String, Object> commitInfo = utilService.formatCommitInfo(commit);
                            commitInfo.put("commitMessage", commit.getShortMessage());
                            commits.add(commitInfo);
                        }
                    }

                    Map<String, Object> result = new HashMap<>();
                    result.put("filePath", filePath);
                    result.put("commits", commits);
                    return result;
                }
        );
    }


    @Cacheable(value = "commit-details", key = "#commitId + '_' + #namespace")
    public Map<String, Object> getCommitChanges(String commitId, String namespace) {
        validationService.validateCommitId(commitId);
        validationService.validateNamespace(namespace);

        return gitOperationService.executeGitOperation(namespace, git -> {
                    Map<String, Object> result = new HashMap<>();

                    Repository repository = git.getRepository();
                    try (RevWalk revWalk = new RevWalk(repository)) {

                        RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
                        result.put("commitId", commit.getName());
                        result.put("commitMessage", commit.getFullMessage());
                        result.put("author", commit.getAuthorIdent().getName());
                        result.put("commitTime", new Date(commit.getCommitTime() * 1000L));

                        // Use DiffFormatter to show changes (equivalent to 'git show')
                        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                             DiffFormatter df = new DiffFormatter(out)) {
                            df.setRepository(repository);

                            // Get the tree changes for this commit
                            var diffs = df.scan(commit.getParentCount() > 0 ? commit.getParent(0) : null, commit);
                            for (var diff : diffs) {
                                df.format(diff);
                            }

                            String rawDiff = out.toString();
                            String cleanedDiff = filterGitDiffMetadata(rawDiff);
                            result.put("changes", cleanedDiff);
                        }
                    }

                    return result;
                }
        );
    }


    private void createConfigFileWithContent(Git git, String relativePath, String appName, String filePath) throws IOException {
        Path workTree = git.getRepository().getWorkTree().toPath();
        Path newFilePath = workTree.resolve(relativePath);

        if (Files.exists(newFilePath)) {
            throw ConfigFileException.alreadyExists(filePath);
        }

        Files.createDirectories(newFilePath.getParent());
        String configContent = DEFAULT_CONFIG_TEMPLATE.replace("<app-name>", appName);
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

    @Override
    public void deleteConfigFile(String filePath, Payload payload) {
        validationService.validateSafePath(filePath);
        validationService.validateCommitMessage(payload.getMessage());

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        gitOperationService.executeGitVoidOperation(namespace, git -> {
                    Path workTree = git.getRepository().getWorkTree().toPath();
                    Path configFilePath = workTree.resolve(relativePath);

                    if (!Files.exists(configFilePath)) {
                        throw ConfigFileException.notFound(filePath);
                    }
                    Files.delete(configFilePath);

                    git.rm().addFilepattern(relativePath).call();
                    git.commit()
                            .setMessage(payload.getMessage())
                            .setAuthor(payload.getEmail().substring(0, payload.getEmail().indexOf('@')), payload.getEmail())
                            .call();

                    log.info("Committed deletion of file: '{}' with message: '{}'", filePath, payload.getMessage());

                    // Clear relevant caches
                    cacheManagerService.evictKey("config-content", filePath);
                    cacheManagerService.evictKey("commit-history", filePath);
                    cacheManagerService.evictKey("latest-commit", filePath);
                    cacheManagerService.evictAllFromCache("directory-listing");

                    // Clear namespace-level caches since a file was deleted
                    cacheManagerService.evictKey("namespace-events", namespace);
                    cacheManagerService.evictKey("namespace-notifications", namespace);

                }
        );
    }

    @Override
    public void deleteNamespace(String namespace) {
        validationService.validateNamespace(namespace);

        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);

        if (!namespaceDir.exists()) {
            throw NamespaceException.notFound(namespace);
        }

        if (!namespaceDir.isDirectory()) {
            throw new RuntimeException("Namespace path is not a directory: " + namespace);
        }

        try {
            // Note: Using shared master key approach - no need to delete individual namespace keys
            // The master key is shared across all namespaces and should not be deleted

            // Recursively delete the entire namespace directory
            deleteDirectoryRecursively(namespaceDir.toPath());
            log.info("Successfully deleted namespace '{}' at: {}", namespace, namespaceDir.getAbsolutePath());

            // Clear all related caches immediately to prevent serving stale data from deleted namespace
            // Use prefix-based eviction to only clear caches for this specific namespace
            cacheManagerService.evictKey("namespaces", "all");
            cacheManagerService.evictAllFromCache("directory-listing"); // No prefix - directory listings are mixed
            cacheManagerService.evictByPrefix("config-content", namespace + "/");
            cacheManagerService.evictByPrefix("commit-history", namespace + "/");
            cacheManagerService.evictByPrefix("latest-commit", namespace + "/");
            cacheManagerService.evictByPrefix("commit-details", "_" + namespace);

            // Clear vault-related caches for this namespace
            cacheManagerService.evictKey("vault-secrets", namespace);
            cacheManagerService.evictKey("vault-history", namespace);

        } catch (IOException e) {
            log.error("Failed to delete namespace '{}': {}", namespace, e.getMessage());
            throw new RuntimeException("Failed to delete namespace: " + namespace, e);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     * Uses try-with-resources to ensure proper cleanup of file system resources.
     *
     * @param dirPath the path of the directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectoryRecursively(Path dirPath) throws IOException {
        if (Files.exists(dirPath)) {
            try (var pathStream = Files.walk(dirPath)) {
                pathStream
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                log.debug("Deleted: {}", path);
                            } catch (IOException e) {
                                log.warn("Failed to delete file: {}, error: {}", path, e.getMessage());
                                throw new RuntimeException("Failed to delete file: " + path, e);
                            }
                        });
            }
        }
    }

    /**
     * Filters out git diff metadata headers while preserving content and hunk information.
     * Removes only the specific git metadata lines but keeps hunk headers (@@ lines) and all content.
     *
     * @param rawDiff the raw diff output from git
     * @return cleaned diff with metadata headers removed but content and line numbers preserved
     */
    private String filterGitDiffMetadata(String rawDiff) {
        if (rawDiff == null || rawDiff.trim().isEmpty()) {
            return rawDiff;
        }

        StringBuilder cleanedDiff = new StringBuilder();
        String[] lines = rawDiff.split("\\r?\\n");

        for (String line : lines) {
            if (!line.startsWith("diff --git") &&
                    !line.startsWith("index ") &&
                    !line.startsWith("--- ") &&
                    !line.startsWith("+++ ") &&
                    !line.startsWith("new file mode") &&
                    !line.startsWith("deleted file mode") &&
                    !line.startsWith("similarity index") &&
                    !line.startsWith("rename from") &&
                    !line.startsWith("rename to") &&
                    !line.startsWith("copy from") &&
                    !line.startsWith("copy to")) {
                cleanedDiff.append(line).append("\n");
            }
        }

        return cleanedDiff.toString().trim();
    }

    @Override
    @Cacheable(value = "namespace-events", key = "#namespace")
    public Map<String, Object> getNamespaceEvents(String namespace) throws Exception {
        validationService.validateNamespace(namespace);

        return gitOperationService.executeGitOperation(namespace, git -> {
            List<Map<String, Object>> commits = new ArrayList<>();

            // Check if repository has any commits by resolving HEAD
            var headRef = git.getRepository().resolve(HEAD);
            if (headRef != null) {
                var logCommand = git.log()
                        .setMaxCount(applicationConfig.getCommitHistorySize())
                        .add(headRef);

                for (RevCommit commit : logCommand.call()) {
                    Map<String, Object> commitInfo = utilService.formatCommitInfo(commit);
                    commitInfo.put("commitMessage", commit.getShortMessage());
                    commits.add(commitInfo);
                }
            } else {
                log.debug("Repository for namespace '{}' has no commits yet", namespace);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("namespace", namespace);
            result.put("commits", commits);
            result.put("totalCommits", commits.size());
            return result;
        });
    }

    @Override
    @Cacheable(value = "namespace-notifications", key = "#namespace",
            condition = "#namespace != null",
            unless = "#result.get('totalNotifications') == 0")
    public Map<String, Object> getNamespaceNotifications(String namespace) throws Exception {
        validationService.validateNamespace(namespace);

        // Get recent notifications limited by commit-history-size
        List<Notification> notifications = notificationStorageService
                .getRecentNotifications(namespace, applicationConfig.getCommitHistorySize());

        // Convert to response format
        List<Map<String, Object>> notificationDetails = notifications.stream()
                .map(this::formatNotificationInfo)
                .toList();

        // Pre-size map for better performance
        Map<String, Object> result = new HashMap<>(8);
        result.put("namespace", namespace);
        result.put("notifications", notificationDetails);
        result.put("totalNotifications", notificationDetails.size());
        result.put("maxNotifications", applicationConfig.getCommitHistorySize());

        return result;
    }

    /**
     * Formatter for simplified notification status information.
     *
     * @param notification the notification status to format
     * @return formatted notification information map
     */
    private Map<String, Object> formatNotificationInfo(Notification notification) {
        Map<String, Object> info = new HashMap<>(3);

        info.put("id", notification.getId());
        info.put("status", notification.getStatus().toString());
        info.put("initiatedTime", notification.getInitiatedTime().toString());

        return info;
    }

}
