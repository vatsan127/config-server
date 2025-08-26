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
            List<ChangeEntry> changes = new ArrayList<>();
            
            var logCommand = git.log()
                    .setMaxCount(applicationConfig.getCommitHistorySize())
                    .add(git.getRepository().resolve(HEAD));

            for (RevCommit commit : logCommand.call()) {
                ChangeEntry entry = createChangeEntry(git.getRepository(), commit);
                changes.add(entry);
            }
            
            synchronized (getNamespaceLock(namespace)) {
                namespaceChangeCache.put(namespace, changes);
            }
        }
    }

    private ChangeEntry createChangeEntry(Repository repository, RevCommit commit) throws IOException {
        PersonIdent author = commit.getAuthorIdent();
        
        ChangeEntry entry = new ChangeEntry();
        entry.setCommitId(commit.getId().getName());
        entry.setMessage(commit.getShortMessage());
        entry.setAuthor(author.getName());
        entry.setEmail(author.getEmailAddress());
        entry.setModifiedTime(LocalDateTime.ofInstant(
                author.getWhen().toInstant(), ZoneId.systemDefault()));
        
        // Extract filename from commit
        try (DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream())) {
            diffFormatter.setRepository(repository);
            if (commit.getParentCount() > 0) {
                List<DiffEntry> diffs = diffFormatter.scan(commit.getParent(0), commit);
                if (!diffs.isEmpty()) {
                    String path = diffs.get(0).getNewPath();
                    entry.setFileName(path.substring(path.lastIndexOf('/') + 1));
                }
            }
        }
        
        // Generate git diff
        entry.setChanges(generateGitDiff(repository, commit));
        
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
        log.info("Starting scheduled cache refresh for all namespaces");
        
        File baseDir = new File(applicationConfig.getBasePath());
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.warn("Base directory does not exist during scheduled refresh: {}", baseDir.getAbsolutePath());
            return;
        }

        File[] namespaceDirs = baseDir.listFiles(File::isDirectory);
        if (namespaceDirs != null) {
            int refreshed = 0;
            for (File namespaceDir : namespaceDirs) {
                String namespace = namespaceDir.getName();
                try {
                    populateNamespaceCache(namespace);
                    refreshed++;
                } catch (Exception e) {
                    log.warn("Failed to refresh cache for namespace '{}' during scheduled refresh: {}", 
                            namespace, e.getMessage());
                }
            }
            log.info("Scheduled cache refresh completed. Refreshed {} out of {} namespaces", 
                    refreshed, namespaceDirs.length);
        }
    }

    private Object getNamespaceLock(String namespace) {
        return namespaceLocks.computeIfAbsent(namespace, k -> new Object());
    }
}