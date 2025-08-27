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

/**
 * Service responsible for managing and tracking changes in Git-based configuration repositories.
 * 
 * This service provides functionality to:
 * - Retrieve change logs for specific namespaces
 * - Create detailed change entries from Git commits
 * - Generate Git diffs for commit changes
 * - Cache change log data for performance optimization
 * 
 * The service integrates with JGit to access Git repository information and creates
 * structured ChangeEntry objects that contain commit metadata, author information,
 * file changes, and Git diffs.
 * 
 * @author Config Server Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ChangeLogService {

    /** Application configuration containing settings like commit history size and base path */
    private final ApplicationConfig applicationConfig;
    
    /** Utility service for validation and path operations */
    private final UtilService utilService;

    /**
     * Constructs a new ChangeLogService with required dependencies.
     * 
     * @param applicationConfig Configuration containing application settings
     * @param utilService Utility service for validation and helper operations
     */
    public ChangeLogService(ApplicationConfig applicationConfig, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
    }

    /**
     * Retrieves a list of recent changes for a specific namespace.
     * 
     * This method:
     * - Validates the namespace parameter
     * - Opens the Git repository for the namespace
     * - Retrieves recent commits up to the configured history size
     * - Creates ChangeEntry objects with commit metadata and diffs
     * - Caches results for improved performance
     * 
     * @param namespace The namespace to retrieve changes for
     * @return List of ChangeEntry objects representing recent commits, or empty list if namespace doesn't exist or has no commits
     * @throws IllegalArgumentException if namespace validation fails
     */
    @Cacheable(value = "change-logs", key = "#namespace")
    public List<ChangeEntry> getChanges(String namespace) {
        String originalThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("ChangeLogSvc");
        
        try {
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
        } finally {
            Thread.currentThread().setName(originalThreadName);
        }
    }

    /**
     * Invalidates the cached change log data for a specific namespace.
     * 
     * This method should be called whenever changes are made to a namespace
     * that would affect the change log (e.g., new commits, repository updates).
     * 
     * @param namespace The namespace whose change log cache should be invalidated
     */
    @CacheEvict(value = "change-logs", key = "#namespace")
    public void invalidateChanges(String namespace) {
        log.debug("Invalidated change log cache for namespace: {}", namespace);
    }

    /**
     * Creates a detailed ChangeEntry object from a Git commit.
     * 
     * This method extracts:
     * - Commit ID and message
     * - Author information (name, email, timestamp)
     * - Modified file names from the commit diff
     * - Git diff content showing the actual changes
     * 
     * @param repository The Git repository containing the commit
     * @param commit The RevCommit object to process
     * @return A fully populated ChangeEntry object
     * @throws IOException if there are issues reading from the Git repository
     */
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

    /**
     * Generates a Git diff string showing the changes made in a specific commit.
     * 
     * For commits with parents, this method compares the commit with its first parent
     * to show the actual changes. For initial commits (no parents), it returns a
     * descriptive message indicating file creation.
     * 
     * @param repository The Git repository containing the commit
     * @param commit The commit to generate a diff for
     * @return String representation of the Git diff, or "Initial commit - file created" for initial commits
     * @throws IOException if there are issues reading from the Git repository or generating the diff
     */
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