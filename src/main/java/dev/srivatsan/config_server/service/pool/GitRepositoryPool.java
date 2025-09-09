package dev.srivatsan.config_server.service.pool;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.GitOperationException;
import dev.srivatsan.config_server.exception.NamespaceException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe Git repository pool that manages Repository instances per namespace.
 * Each Git operation creates a new Git wrapper around the cached Repository for thread safety.
 */
@Service
public class GitRepositoryPool {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryPool.class);

    private final ApplicationConfig applicationConfig;
    private final ConcurrentHashMap<String, RepositoryHolder> repositoryCache;
    private final ReentrantReadWriteLock cacheLock;

    public GitRepositoryPool(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        this.repositoryCache = new ConcurrentHashMap<>();
        this.cacheLock = new ReentrantReadWriteLock();
    }

    /**
     * Gets a NEW Git instance for the namespace. Each call returns a fresh Git object
     * wrapping the cached Repository, ensuring thread safety.
     *
     * @param namespace the namespace identifier
     * @return a new Git instance for thread-safe operations
     * @throws IOException if repository access fails
     */
    public Git getGitInstance(String namespace) throws IOException {
        RepositoryHolder holder = getOrCreateRepositoryHolder(namespace);
        
        // Create a NEW Git instance each time for thread safety
        // The Repository underneath is thread-safe, but Git wrapper is not
        return new Git(holder.repository);
    }

    /**
     * Removes repository from cache when namespace is deleted.
     *
     * @param namespace the namespace to remove from cache
     */
    public void evictNamespace(String namespace) {
        cacheLock.writeLock().lock();
        try {
            RepositoryHolder holder = repositoryCache.remove(namespace);
            if (holder != null) {
                try {
                    holder.repository.close();
                    log.debug("Evicted and closed repository for namespace: {}", namespace);
                } catch (Exception e) {
                    log.warn("Error closing repository for namespace '{}': {}", namespace, e.getMessage());
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Gets cache statistics for monitoring.
     */
    public CacheStats getCacheStats() {
        cacheLock.readLock().lock();
        try {
            return new CacheStats(repositoryCache.size(), repositoryCache.keySet());
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    private RepositoryHolder getOrCreateRepositoryHolder(String namespace) throws IOException {
        // Fast path - check if already cached
        RepositoryHolder holder = repositoryCache.get(namespace);
        if (holder != null && !holder.repository.isClosed()) {
            return holder;
        }

        // Slow path - need to create/recreate
        cacheLock.writeLock().lock();
        try {
            // Double-check after acquiring write lock
            holder = repositoryCache.get(namespace);
            if (holder != null && !holder.repository.isClosed()) {
                return holder;
            }

            // Create new repository holder
            Repository repository = openRepository(namespace);
            holder = new RepositoryHolder(repository);
            repositoryCache.put(namespace, holder);
            
            log.debug("Cached new repository for namespace: {}", namespace);
            return holder;
            
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private Repository openRepository(String namespace) throws IOException {
        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        
        if (!namespaceDir.exists() || !namespaceDir.isDirectory()) {
            throw NamespaceException.notFound(namespace);
        }

        File gitDir = new File(namespaceDir, ".git");
        if (!gitDir.exists()) {
            throw GitOperationException.repositoryAccessFailed(namespace, 
                new IOException("Git repository not found in namespace directory"));
        }

        try {
            return new FileRepositoryBuilder()
                    .setGitDir(gitDir)
                    .readEnvironment()
                    .findGitDir()
                    .build();
        } catch (IOException e) {
            log.error("Failed to open repository for namespace '{}': {}", namespace, e.getMessage());
            throw GitOperationException.repositoryAccessFailed(namespace, e);
        }
    }

    @PreDestroy
    public void cleanup() {
        cacheLock.writeLock().lock();
        try {
            log.info("Closing {} cached repositories", repositoryCache.size());
            for (RepositoryHolder holder : repositoryCache.values()) {
                try {
                    holder.repository.close();
                } catch (Exception e) {
                    log.warn("Error closing repository during cleanup: {}", e.getMessage());
                }
            }
            repositoryCache.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Holder for Repository with metadata
     */
    private static class RepositoryHolder {
        final Repository repository;
        final long createdAt;

        RepositoryHolder(Repository repository) {
            this.repository = repository;
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * Cache statistics for monitoring
     */
    public record CacheStats(int cacheSize, java.util.Set<String> cachedNamespaces) {}
}