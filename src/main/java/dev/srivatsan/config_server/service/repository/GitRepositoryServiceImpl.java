package dev.srivatsan.config_server.service.repository;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.ConfigConflictException;
import dev.srivatsan.config_server.exception.ConfigFileException;
import dev.srivatsan.config_server.exception.GitOperationException;
import dev.srivatsan.config_server.exception.NamespaceException;
import dev.srivatsan.config_server.model.Payload;
import dev.srivatsan.config_server.service.cache.CacheManagerService;
import dev.srivatsan.config_server.service.encryption.EncryptionService;
import dev.srivatsan.config_server.service.notification.RefreshNotificationService;
import dev.srivatsan.config_server.service.operation.GitOperationService;
import dev.srivatsan.config_server.service.util.UtilService;
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
public non-sealed class GitRepositoryServiceImpl implements GitRepositoryService {

    private final Logger log = LoggerFactory.getLogger(GitRepositoryServiceImpl.class);
    private final ApplicationConfig applicationConfig;
    private final UtilService utilService;
    private final GitOperationService gitOperationService;
    private final CacheManagerService cacheManagerService;
    private final ValidationService validationService;
    private final EncryptionService encryptionService;
    private final RefreshNotificationService refreshNotificationService;

    public GitRepositoryServiceImpl(ApplicationConfig applicationConfig, UtilService utilService, GitOperationService gitOperationService, CacheManagerService cacheManagerService, ValidationService validationService, EncryptionService encryptionService, RefreshNotificationService refreshNotificationService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
        this.gitOperationService = gitOperationService;
        this.cacheManagerService = cacheManagerService;
        this.validationService = validationService;
        this.encryptionService = encryptionService;
        this.refreshNotificationService = refreshNotificationService;
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
            cacheManagerService.evictKey("namespaces", "all");
            cacheManagerService.evictAllFromCache("directory-listing");
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

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            createConfigFileWithContent(git, relativePath, appName, filePath);
            commitNewFile(git, relativePath, appName, email);

            // Clear all directory listings for this namespace
            cacheManagerService.evictAllFromCache("directory-listing");
            
        });
    }

    public String updateConfigFile(String filePath, Payload payload) {
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

        String commitId = gitOperationService.executeGitOperation(namespace, git -> {
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

                    if (!expectedCommitId.equals(currentCommitId)) {
                        throw ConfigConflictException.conflictDetected(payload.getAppName());
                    }

                    Path workTree = git.getRepository().getWorkTree().toPath();
                    Path configFilePath = workTree.resolve(relativePath);

                    if (!Files.exists(configFilePath)) {
                        throw ConfigFileException.notFound(filePath);
                    }

                    // Encrypt the content line by line before writing to file
                    String encryptedContent = encryptionService.encryptContent(payload.getContent());
                    Files.writeString(configFilePath, encryptedContent);

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
                    
                    return revCommit.getId().getName();
                }
        );

        // Send refresh notifications asynchronously using virtual threads
        refreshNotificationService.sendRefreshNotifications(namespace, payload.getAppName());

        return commitId;
    }

    @Cacheable(value = "config-content", key = "#filePath")
    public String getConfigFile(String filePath) {
        validationService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        return gitOperationService.executeGitOperation(namespace, git -> {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(relativePath);

            if (!Files.exists(configFilePath)) {
                throw ConfigFileException.notFound(filePath);
            }

            String encryptedContent = Files.readString(configFilePath);
            // Decrypt the content line by line before returning
            return encryptionService.decryptContent(encryptedContent);
        });
    }

    @Cacheable(value = "latest-commit", key = "#filePath")
    public String getLatestCommitId(String filePath) {
        validationService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        return gitOperationService.executeGitOperation(namespace, git -> {
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

        return gitOperationService.executeGitOperation(namespace, git -> {
            var logCommand = git.log()
                    .setMaxCount(applicationConfig.getCommitHistorySize())
                    .add(git.getRepository().resolve(HEAD))
                    .addPath(relativePath);

            List<Map<String, Object>> commits = new ArrayList<>();
            for (RevCommit commit : logCommand.call()) {
                Map<String, Object> commitInfo = formatCommitInfo(commit);
                commitInfo.put("commitMessage", commit.getShortMessage());
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

        return gitOperationService.executeGitOperation(namespace, git -> {
            Map<String, Object> result = new HashMap<>();

            try (Repository repository = git.getRepository();
                 RevWalk revWalk = new RevWalk(repository)) {

                RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
                result.put("commitId", commit.getName());
                result.put("commitMessage", commit.getFullMessage());
                result.put("author", commit.getAuthorIdent().getName());
                result.put("commitTime", new Date(commit.getCommitTime() * 1000L));

                if (commit.getParentCount() > 0) {
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                         DiffFormatter df = new DiffFormatter(out)) {
                        df.setRepository(repository);
                        df.format(df.scan(commit.getParent(0), commit).getFirst());
                        String rawChanges = out.toString();
                        String decryptedChanges = decryptDiffContent(rawChanges);
                        result.put("changes", decryptedChanges);
                    }
                } else {
                    result.put("changes", "");
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
        
        // Encrypt the content line by line before writing to file
        String encryptedContent = encryptionService.encryptContent(configContent);
        Files.writeString(newFilePath, encryptedContent);

        log.info("Created and encrypted file: '{}'", newFilePath);
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
        validationService.validateEmail(payload.getEmail());
        validationService.validateCommitMessage(payload.getMessage());

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(relativePath);

            if (!Files.exists(configFilePath)) {
                throw ConfigFileException.notFound(filePath);
            }

            // Delete the file from filesystem
            Files.delete(configFilePath);
            log.info("Deleted file: '{}'", configFilePath);

            // Stage the deletion for commit
            git.rm().addFilepattern(relativePath).call();

            // Commit the deletion
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
            
        });
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
            // Recursively delete the entire namespace directory
            deleteDirectoryRecursively(namespaceDir.toPath());
            log.info("Successfully deleted namespace '{}' at: {}", namespace, namespaceDir.getAbsolutePath());

            // Clear all related caches
            cacheManagerService.evictKey("namespaces", "all");
            cacheManagerService.evictAllFromCache("directory-listing");
            cacheManagerService.evictAllFromCache("config-content");
            cacheManagerService.evictAllFromCache("commit-history");
            cacheManagerService.evictAllFromCache("latest-commit");
            cacheManagerService.evictAllFromCache("commit-details");

        } catch (IOException e) {
            log.error("Failed to delete namespace '{}': {}", namespace, e.getMessage());
            throw new RuntimeException("Failed to delete namespace: " + namespace, e);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * @param dirPath the path of the directory to delete
     * @throws IOException if deletion fails
     */
    private void deleteDirectoryRecursively(Path dirPath) throws IOException {
        if (Files.exists(dirPath)) {
            Files.walk(dirPath)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Decrypts any encrypted content found in diff output using sequential processing.
     * Looks for {cipher} prefixed content and decrypts it for readable diffs.
     *
     * @param diffContent the raw diff content that may contain encrypted text
     * @return the diff with encrypted content decrypted
     */
    private String decryptDiffContent(String diffContent) {
        if (diffContent == null || diffContent.isEmpty()) {
            return diffContent;
        }
        
        // Split into lines and process sequentially for thread safety and reliability
        String[] lines = diffContent.split("\n");
        List<String> linesList = Arrays.asList(lines);
        
        // Filter out Git metadata and process only content lines
        List<String> processedLines = linesList.stream()
            .filter(this::isContentLine)
            .map(this::decryptDiffLine)
            .collect(Collectors.toList());
        
        return String.join("\n", processedLines);
    }
    
    /**
     * Checks if a line is actual content (not Git metadata).
     * Filters out diff headers, file paths, hunk headers, etc.
     *
     * @param line the diff line to check
     * @return true if it's a content line (starts with +, -, or space)
     */
    private boolean isContentLine(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        
        // Skip Git metadata lines
        return !line.startsWith("diff --git") &&
               !line.startsWith("index ") &&
               !line.startsWith("--- ") &&
               !line.startsWith("+++ ") &&
               !line.startsWith("@@ ") &&
               !line.startsWith("new file mode") &&
               !line.startsWith("deleted file mode") &&
               !line.startsWith("similarity index") &&
               !line.startsWith("rename from") &&
               !line.startsWith("rename to") &&
               !line.startsWith("copy from") &&
               !line.startsWith("copy to") &&
               // Keep actual content lines that start with +, -, or space
               (line.startsWith("+") || line.startsWith("-") || line.startsWith(" "));
    }

    /**
     * Decrypts a single diff line if it contains encrypted content.
     *
     * @param line the diff line to process
     * @return the line with decrypted content if applicable
     */
    private String decryptDiffLine(String line) {
        // Check if line contains encrypted content (starting with +, -, or space followed by {cipher})
        if ((line.startsWith("+") || line.startsWith("-") || line.startsWith(" ")) && 
            line.contains("{cipher}")) {
            
            try {
                // Extract the prefix (+ - or space) and the rest
                String prefix = line.substring(0, 1);
                String content = line.substring(1);
                
                // Decrypt if it's encrypted
                if (encryptionService.isEncrypted(content)) {
                    String decrypted = encryptionService.decrypt(content);
                    return prefix + decrypted;
                }
            } catch (Exception e) {
                log.warn("Failed to decrypt line in diff: {}", e.getMessage());
                // Keep original line if decryption fails
            }
        }
        
        return line; // Return original line if no encryption found or decryption failed
    }




}
