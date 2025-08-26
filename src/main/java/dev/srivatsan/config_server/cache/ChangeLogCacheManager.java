package dev.srivatsan.config_server.cache;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.ChangeEntry;
import dev.srivatsan.config_server.service.util.UtilService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.eclipse.jgit.lib.Constants.HEAD;

@Slf4j
@Service
public class ChangeLogCacheManager {

    private final ApplicationConfig applicationConfig;
    private final UtilService utilService;
    
    private final Map<String, List<ChangeEntry>> namespaceChangeCache = new HashMap<>();
    private final Map<String, Object> namespaceLocks = new ConcurrentHashMap<>();

    public ChangeLogCacheManager(ApplicationConfig applicationConfig, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
    }

    private void populateNamespaceCache(String namespace) throws IOException, GitAPIException {
        utilService.validateNamespace(namespace);
        
        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        if (!namespaceDir.exists()) {
            return;
        }

        try (Git git = Git.open(namespaceDir)) {
            Repository repository = git.getRepository();
            
            // Check if HEAD exists (repository has commits)
            if (repository.resolve(HEAD) == null) {
                log.debug("Repository '{}' has no commits yet, skipping cache population", namespace);
                synchronized (getNamespaceLock(namespace)) {
                    namespaceChangeCache.put(namespace, Collections.emptyList());
                }
                return;
            }
            
            List<ChangeEntry> changes = new ArrayList<>();
            
            var logCommand = git.log()
                    .setMaxCount(applicationConfig.getCommitHistorySize())
                    .add(repository.resolve(HEAD));

            for (RevCommit commit : logCommand.call()) {
                try {
                    ChangeEntry entry = createChangeEntry(repository, commit);
                    changes.add(entry);
                } catch (Exception e) {
                    log.warn("Failed to create change entry for commit {} in namespace '{}': {}", 
                            commit.getId().getName(), namespace, e.getMessage());
                }
            }
            
            synchronized (getNamespaceLock(namespace)) {
                namespaceChangeCache.put(namespace, changes);
            }
            
            log.debug("Populated cache for namespace '{}' with {} changes", namespace, changes.size());
        }
    }

    private ChangeEntry createChangeEntry(Repository repository, RevCommit commit) throws IOException {
        PersonIdent author = commit.getAuthorIdent();
        
        ChangeEntry entry = new ChangeEntry();
        entry.setCommitId(commit.getId().getName());
        entry.setMessage(commit.getShortMessage() != null ? commit.getShortMessage() : "No message");
        entry.setAuthor(author != null ? author.getName() : "Unknown");
        entry.setEmail(author != null ? author.getEmailAddress() : "unknown@unknown.com");
        entry.setModifiedTime(author != null ? 
                LocalDateTime.ofInstant(author.getWhen().toInstant(), ZoneId.systemDefault()) :
                LocalDateTime.now());
        
        // Extract filename from commit
        try (DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream())) {
            diffFormatter.setRepository(repository);
            if (commit.getParentCount() > 0) {
                List<DiffEntry> diffs = diffFormatter.scan(commit.getParent(0), commit);
                if (!diffs.isEmpty()) {
                    String path = diffs.get(0).getNewPath();
                    if (path != null && !path.isEmpty()) {
                        entry.setFileName(path.substring(path.lastIndexOf('/') + 1));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract filename from commit {}: {}", commit.getId().getName(), e.getMessage());
        }
        
        // Generate git diff
        try {
            entry.setChanges(generateGitDiff(repository, commit));
        } catch (Exception e) {
            log.debug("Could not generate diff for commit {}: {}", commit.getId().getName(), e.getMessage());
            entry.setChanges("Diff not available");
        }
        
        return entry;
    }

    private String generateGitDiff(Repository repository, RevCommit commit) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DiffFormatter diffFormatter = new DiffFormatter(out)) {
            
            diffFormatter.setRepository(repository);
            
            if (commit.getParentCount() > 0) {
                List<DiffEntry> diffs = diffFormatter.scan(commit.getParent(0), commit);
                for (DiffEntry diff : diffs) {
                    diffFormatter.format(diff);
                }
                return out.toString();
            } else {
                return "Initial commit - file created";
            }
        }
    }

    public List<ChangeEntry> getChanges(String namespace) {
        synchronized (getNamespaceLock(namespace)) {
            return new ArrayList<>(namespaceChangeCache.getOrDefault(namespace, Collections.emptyList()));
        }
    }

    @Scheduled(fixedRateString = "#{${global.cache-refresh-interval} * 1000}", initialDelayString = "#{10 * 1000}")
    public void refreshAllCaches() {
        // Set proper thread name for easier debugging
        String originalThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("cache-refresh-scheduler");
        
        try {
            log.info("Starting scheduled cache refresh for all namespaces");
            
            File baseDir = new File(applicationConfig.getBasePath());
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                log.warn("Base directory does not exist during scheduled refresh: {}", baseDir.getAbsolutePath());
                return;
            }

            File[] namespaceDirs = baseDir.listFiles(this::isValidNamespaceDirectory);
            if (namespaceDirs != null) {
                int refreshed = 0;
                for (File namespaceDir : namespaceDirs) {
                    String namespace = namespaceDir.getName();
                    try {
                        populateNamespaceCache(namespace);
                        refreshed++;
                        log.debug("Successfully refreshed cache for namespace: {}", namespace);
                    } catch (Exception e) {
                        log.warn("Failed to refresh cache for namespace '{}' during scheduled refresh: {}", 
                                namespace, e.getMessage());
                    }
                }
                log.info("Scheduled cache refresh completed. Refreshed {} out of {} namespaces", 
                        refreshed, namespaceDirs.length);
            }
        } finally {
            // Restore original thread name
            Thread.currentThread().setName(originalThreadName);
        }
    }

    /**
     * Validates if a directory should be considered as a namespace directory
     */
    private boolean isValidNamespaceDirectory(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        
        String name = dir.getName();
        
        // Skip hidden directories (starting with .)
        if (name.startsWith(".")) {
            log.debug("Skipping hidden directory: {}", name);
            return false;
        }
        
        // Check if it's a valid git repository
        File gitDir = new File(dir, ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            log.debug("Skipping non-git directory: {}", name);
            return false;
        }
        
        // Validate namespace format
        try {
            utilService.validateNamespace(name);
            return true;
        } catch (Exception e) {
            log.debug("Skipping invalid namespace directory '{}': {}", name, e.getMessage());
            return false;
        }
    }

    private Object getNamespaceLock(String namespace) {
        return namespaceLocks.computeIfAbsent(namespace, k -> new Object());
    }
}