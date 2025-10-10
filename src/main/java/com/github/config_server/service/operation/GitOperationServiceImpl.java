package com.github.config_server.service.operation;

import com.github.config_server.config.ApplicationConfig;
import com.github.config_server.exception.GitOperationException;
import com.github.config_server.exception.NamespaceException;
import com.github.config_server.service.validation.ValidationService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * Implementation of GitOperationService that handles Git repository operations
 * within namespace directories.
 */
@Service
public non-sealed class GitOperationServiceImpl implements GitOperationService {

    private static final Logger log = LoggerFactory.getLogger(GitOperationServiceImpl.class);

    private final ApplicationConfig applicationConfig;
    private final ValidationService validationService;

    public GitOperationServiceImpl(ApplicationConfig applicationConfig, ValidationService validationService) {
        this.applicationConfig = applicationConfig;
        this.validationService = validationService;
    }

    @Override
    public <T> T executeGitOperation(String namespace, GitOperation<T> operation) {
        log.debug("Executing Git operation for namespace: {}", namespace);

        try (Git git = openRepository(namespace)) {
            T result = operation.execute(git);
            log.debug("Git operation completed successfully for namespace: {}", namespace);
            return result;
        } catch (IOException e) {
            log.error("Repository access failed for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.repositoryAccessFailed(namespace, e);
        } catch (GitAPIException e) {
            log.error("Git operation failed for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.operationFailed(namespace, e);
        }
    }

    @Override
    public void executeGitVoidOperation(String namespace, GitVoidOperation operation) {
        log.debug("Executing Git void operation for namespace: {}", namespace);

        try (Git git = openRepository(namespace)) {
            operation.execute(git);
            log.debug("Git void operation completed successfully for namespace: {}", namespace);
        } catch (IOException e) {
            log.error("Repository access failed for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.repositoryAccessFailed(namespace, e);
        } catch (GitAPIException e) {
            log.error("Git operation failed for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.operationFailed(namespace, e);
        }
    }

    /**
     * Opens a Git repository for the specified namespace.
     * This method provides direct access to the Git repository instance for advanced operations.
     *
     * @param namespace the namespace identifier
     * @return the opened Git repository instance
     * @throws IOException        if the repository cannot be accessed
     * @throws NamespaceException if the namespace is invalid or not found
     */
    @Override
    public Git openRepository(String namespace) throws IOException {
        validationService.validateNamespace(namespace);

        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        if (!namespaceDir.exists()) {
            log.warn("Namespace directory not found: {}", namespaceDir.getAbsolutePath());
            throw NamespaceException.notFound(namespace);
        }

        if (!namespaceDir.isDirectory()) {
            log.error("Namespace path is not a directory: {}", namespaceDir.getAbsolutePath());
            throw NamespaceException.notFound(namespace);
        }

        try {
            Git git = Git.open(namespaceDir);
            log.debug("Successfully opened Git repository for namespace: {}", namespace);
            return git;
        } catch (IOException e) {
            log.error("Failed to open Git repository for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.repositoryAccessFailed(namespace, e);
        }
    }
}