package dev.srivatsan.config_server.service.util;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.GitOperationException;
import dev.srivatsan.config_server.exception.NamespaceException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class GitOperationHelper {

    private final ApplicationConfig applicationConfig;
    private final UtilService utilService;

    public GitOperationHelper(ApplicationConfig applicationConfig, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
    }

    @FunctionalInterface
    public interface GitOperation<T> {
        T execute(Git git) throws IOException, GitAPIException;
    }

    @FunctionalInterface
    public interface GitVoidOperation {
        void execute(Git git) throws IOException, GitAPIException;
    }

    public <T> T executeGitOperation(String namespace, GitOperation<T> operation) {
        try (Git git = openRepository(namespace)) {
            return operation.execute(git);
        } catch (IOException e) {
            throw GitOperationException.repositoryAccessFailed(namespace, e);
        } catch (GitAPIException e) {
            throw GitOperationException.operationFailed(namespace, e);
        }
    }

    public void executeGitVoidOperation(String namespace, GitVoidOperation operation) {
        try (Git git = openRepository(namespace)) {
            operation.execute(git);
        } catch (IOException e) {
            throw GitOperationException.repositoryAccessFailed(namespace, e);
        } catch (GitAPIException e) {
            throw GitOperationException.operationFailed(namespace, e);
        }
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
}