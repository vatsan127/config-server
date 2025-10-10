package com.github.config_server.service.operation;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

/**
 * Service interface for Git repository operations.
 * Provides a clean contract for executing Git operations within namespace repositories.
 */
public sealed interface GitOperationService permits GitOperationServiceImpl {

    /**
     * Functional interface for Git operations that return a result.
     *
     * @param <T> the type of result returned by the operation
     */
    @FunctionalInterface
    interface GitOperation<T> {
        T execute(Git git) throws IOException, GitAPIException;
    }

    /**
     * Functional interface for Git operations that don't return a result.
     */
    @FunctionalInterface
    interface GitVoidOperation {
        void execute(Git git) throws IOException, GitAPIException;
    }

    /**
     * Executes a Git operation that returns a result within the specified namespace repository.
     *
     * @param namespace the namespace identifier
     * @param operation the Git operation to execute
     * @param <T>       the type of result returned by the operation
     * @return the result of the Git operation
     * @throws dev.srivatsan.config_server.exception.GitOperationException if the operation fails
     * @throws dev.srivatsan.config_server.exception.NamespaceException    if the namespace is invalid or not found
     */
    <T> T executeGitOperation(String namespace, GitOperation<T> operation);

    /**
     * Executes a Git operation that doesn't return a result within the specified namespace repository.
     *
     * @param namespace the namespace identifier
     * @param operation the Git operation to execute
     * @throws dev.srivatsan.config_server.exception.GitOperationException if the operation fails
     * @throws dev.srivatsan.config_server.exception.NamespaceException    if the namespace is invalid or not found
     */
    void executeGitVoidOperation(String namespace, GitVoidOperation operation);

    /**
     * Opens a Git repository for the specified namespace.
     * This method provides direct access to the Git repository instance for advanced operations.
     *
     * @param namespace the namespace identifier
     * @return the opened Git repository instance
     * @throws IOException                                                 if the repository cannot be accessed
     * @throws dev.srivatsan.config_server.exception.NamespaceException    if the namespace is invalid or not found
     * @throws dev.srivatsan.config_server.exception.GitOperationException if the repository cannot be opened
     */
    Git openRepository(String namespace) throws IOException;
}