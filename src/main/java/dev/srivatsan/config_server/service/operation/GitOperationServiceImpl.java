package dev.srivatsan.config_server.service.operation;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.GitOperationException;
import dev.srivatsan.config_server.exception.NamespaceException;
import dev.srivatsan.config_server.service.pool.GitRepositoryPool;
import dev.srivatsan.config_server.service.validation.ValidationService;
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
    private final GitRepositoryPool repositoryPool;

    public GitOperationServiceImpl(ApplicationConfig applicationConfig, 
                                 ValidationService validationService,
                                 GitRepositoryPool repositoryPool) {
        this.applicationConfig = applicationConfig;
        this.validationService = validationService;
        this.repositoryPool = repositoryPool;
    }

    @Override
    public <T> T executeGitOperation(String namespace, GitOperation<T> operation) {
        log.debug("Executing Git operation for namespace: {}", namespace);
        validationService.validateNamespace(namespace);

        try (Git git = repositoryPool.getGitInstance(namespace)) {
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
        validationService.validateNamespace(namespace);

        try (Git git = repositoryPool.getGitInstance(namespace)) {
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
     * Opens a Git repository for the specified namespace using the repository pool.
     * This method provides direct access to a NEW Git repository instance for thread-safe operations.
     *
     * @param namespace the namespace identifier
     * @return a new Git repository instance from the pool
     * @throws IOException        if the repository cannot be accessed
     * @throws NamespaceException if the namespace is invalid or not found
     */
    @Override
    public Git openRepository(String namespace) throws IOException {
        validationService.validateNamespace(namespace);
        return repositoryPool.getGitInstance(namespace);
    }
}