package dev.srivatsan.config_server.service;

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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.jgit.lib.Constants.HEAD;

@Slf4j
@Service
public class ChangeLogService {

    private final ApplicationConfig applicationConfig;
    private final UtilService utilService;

    public ChangeLogService(ApplicationConfig applicationConfig, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
    }

    @Cacheable(value = "change-logs", key = "#namespace")
    public List<ChangeEntry> getChanges(String namespace) {
        utilService.validateNamespace(namespace);
        
        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        if (!namespaceDir.exists()) {
            return Collections.emptyList();
        }

        try (Git git = Git.open(namespaceDir)) {
            Repository repository = git.getRepository();
            
            if (repository.resolve(HEAD) == null) {
                log.debug("Repository '{}' has no commits yet", namespace);
                return Collections.emptyList();
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
            
            log.debug("Retrieved {} changes for namespace '{}'", changes.size(), namespace);
            return changes;
            
        } catch (IOException | GitAPIException e) {
            log.error("Error retrieving changes for namespace '{}': {}", namespace, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @CacheEvict(value = "change-logs", key = "#namespace")
    public void invalidateChanges(String namespace) {
        log.debug("Invalidated change log cache for namespace: {}", namespace);
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
}