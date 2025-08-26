package dev.srivatsan.config_server.service.repository;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.ConfigFileException;
import dev.srivatsan.config_server.exception.GitOperationException;
import dev.srivatsan.config_server.exception.NamespaceException;
import dev.srivatsan.config_server.model.DirectoryEntry;
import dev.srivatsan.config_server.model.Payload;
import dev.srivatsan.config_server.service.util.UtilService;
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
import java.time.LocalDateTime;
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

    public GitBasedConfigService(ApplicationConfig applicationConfig, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
    }

    private Git openRepository(String namespace) throws IOException {
        utilService.validateNamespace(namespace);

        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        if (!namespaceDir.exists()) {
            throw NamespaceException.notFound(namespace);
        }

        try {
            return Git.open(namespaceDir);
        } catch (IOException e) {
            throw GitOperationException.repositoryAccessFailed(namespace, e);
        }
    }

    @CacheEvict(value = {"namespaces", "directory-listing"}, allEntries = true)
    public void createNamespace(String namespace) {
        utilService.validateNamespace(namespace);

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
        } catch (GitAPIException e) {
            log.error("Failed to initialize git repository for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.initFailed(namespace, e);
        }
    }

    @CacheEvict(value = {"config-content", "commit-history"}, key = "#filePath")
    public void initializeConfigFile(String filePath, String appName, String email) {
        utilService.validateSafePath(filePath);
        utilService.validateAppName(appName);
        utilService.validateEmail(email);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        try (Git git = openRepository(namespace)) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path newFilePath = workTree.resolve(relativePath);

            if (Files.exists(newFilePath)) {
                throw ConfigFileException.alreadyExists(filePath);
            }

            Files.createDirectories(newFilePath.getParent());
            String configContent = DEFAULT_CONFIG_TEMPLATE.replace("<app-name>", appName);
            utilService.validateYamlContent(configContent);
            Files.writeString(newFilePath, configContent);

            git.add().addFilepattern(relativePath).call();
            RevCommit commit = git.commit()
                    .setMessage("First commit ApplicationName - " + appName)
                    .setAuthor(email.substring(0, email.indexOf('@')), email)
                    .call();
            log.info("Created file: '{}'", newFilePath);

        } catch (IOException e) {
            log.error("Error initializing config file '{}': {}", filePath, e.getMessage(), e);
            throw ConfigFileException.creationFailed(filePath, e);
        } catch (GitAPIException e) {
            log.error("Git error initializing config file '{}': {}", filePath, e.getMessage(), e);
            throw GitOperationException.commitFailed(filePath, e);
        }
    }

    @CacheEvict(value = {"config-content", "commit-history", "change-logs"}, key = "#filePath")
    public void updateConfigFile(String filePath, Payload payload) {
        utilService.validateSafePath(filePath);
        utilService.validateEmail(payload.getEmail());
        utilService.validateYamlContent(payload.getContent());
        utilService.validateCommitMessage(payload.getMessage());

        String commitMessage = payload.getMessage();
        String email = payload.getEmail();
        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        try (Git git = openRepository(namespace)) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(relativePath);

            if (!Files.exists(configFilePath)) {
                throw ConfigFileException.notFound(filePath);
            }

            Files.writeString(configFilePath, payload.getContent());

            git.add().addFilepattern(relativePath).call();
            RevCommit commit = git.commit()
                    .setMessage(commitMessage)
                    .setAuthor(email.substring(0, email.indexOf('@')), email)
                    .call();
            log.info("Updated file: '{}', with message: '{}'", configFilePath, commitMessage);

        } catch (IOException e) {
            log.error("Error updating config file '{}': {}", filePath, e.getMessage(), e);
            throw ConfigFileException.updateFailed(filePath, e);
        } catch (GitAPIException e) {
            log.error("Git error updating config file '{}': {}", filePath, e.getMessage(), e);
            throw GitOperationException.commitFailed(filePath, e);
        }
    }

    @Cacheable(value = "config-content", key = "#filePath")
    public String getConfigFile(String filePath) {
        utilService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        try (Git git = openRepository(namespace)) {
            Path workTree = git.getRepository().getWorkTree().toPath();
            Path configFilePath = workTree.resolve(relativePath);

            if (!Files.exists(configFilePath)) {
                throw ConfigFileException.notFound(filePath);
            }

            return Files.readString(configFilePath);

        } catch (IOException e) {
            log.error("Error reading file '{}': {}", filePath, e.getMessage(), e);
            throw ConfigFileException.readFailed(filePath, e);
        }
    }

    @Override
    @Cacheable(value = "commit-history", key = "#filePath")
    public Map<String, Object> getConfigFileHistory(String filePath) {
        utilService.validateSafePath(filePath);

        String namespace = utilService.extractNamespaceFromFilePath(filePath);
        String relativePath = utilService.getRelativePathWithinNamespace(filePath);

        try (Git git = openRepository(namespace)) {
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

        } catch (IOException e) {
            log.error("Error getting commit history: {}", e.getMessage(), e);
            throw GitOperationException.repositoryAccessFailed(namespace, e);
        } catch (GitAPIException e) {
            log.error("Git error getting commit history: {}", e.getMessage(), e);
            throw GitOperationException.logFailed(filePath, e);
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

    @Cacheable(value = "commit-details", key = "#commitId + '_' + #namespace")
    public Map<String, Object> getCommitChanges(String commitId, String namespace) {
        utilService.validateCommitId(commitId);
        utilService.validateNamespace(namespace);

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
        } catch (IOException e) {
            log.error("Error getting commit changes: {}", e.getMessage(), e);
            throw GitOperationException.diffFailed(commitId, e);
        }

        return result;
    }

    @Override
    @Cacheable(value = "namespaces", key = "'all'")
    public List<String> listNamespaces() {
        File baseDir = new File(applicationConfig.getBasePath());
        
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.warn("Base directory does not exist: {}", baseDir.getAbsolutePath());
            return Collections.emptyList();
        }

        File[] namespaceDirs = baseDir.listFiles(File::isDirectory);
        if (namespaceDirs == null) {
            return Collections.emptyList();
        }

        List<String> namespaces = Arrays.stream(namespaceDirs)
                .map(File::getName)
                .filter(name -> isValidNamespace(name))
                .sorted()
                .collect(Collectors.toList());

        log.debug("Found {} namespaces in base directory", namespaces.size());
        return namespaces;
    }

    private boolean isValidNamespace(String name) {
        try {
            utilService.validateNamespace(name);
            
            // Check if it's a valid git repository
            File namespaceDir = new File(applicationConfig.getBasePath(), name);
            File gitDir = new File(namespaceDir, ".git");
            return gitDir.exists() && gitDir.isDirectory();
        } catch (Exception e) {
            log.debug("Skipping invalid namespace directory: {}", name);
            return false;
        }
    }

    @Override
    @Cacheable(value = "directory-listing", key = "#namespace + '_' + #path")
    public List<String> listDirectoryContents(String namespace, String path) {
        utilService.validateNamespace(namespace);
        
        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        if (!namespaceDir.exists()) {
            throw NamespaceException.notFound(namespace);
        }

        // Clean and validate the path
        String cleanPath = (path == null || path.trim().isEmpty()) ? "" : path.trim();
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        
        File targetDir = cleanPath.isEmpty() ? namespaceDir : new File(namespaceDir, cleanPath);
        
        if (!targetDir.exists()) {
            throw new RuntimeException("Directory not found: " + cleanPath);
        }
        
        if (!targetDir.isDirectory()) {
            throw new RuntimeException("Path is not a directory: " + cleanPath);
        }

        // Security check: ensure target directory is within namespace
        try {
            if (!targetDir.getCanonicalPath().startsWith(namespaceDir.getCanonicalPath())) {
                throw new RuntimeException("Invalid path: access denied");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to validate path security", e);
        }

        File[] files = targetDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            // Skip .git directory and other hidden files/directories
            if (file.getName().startsWith(".")) {
                continue;
            }
            
            // Include only directories and .yml files
            if (file.isDirectory() || file.getName().toLowerCase().endsWith(".yml")) {
                fileNames.add(file.getName());
            }
        }

        // Sort alphabetically
        Collections.sort(fileNames, String.CASE_INSENSITIVE_ORDER);

        log.debug("Listed {} entries in namespace '{}' path '{}'", fileNames.size(), namespace, cleanPath);
        return fileNames;
    }

}
