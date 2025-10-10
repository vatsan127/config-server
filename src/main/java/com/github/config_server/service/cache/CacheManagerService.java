package com.github.config_server.service.cache;

/**
 * Service interface for cache eviction operations.
 * Provides methods to manage cache entries across different cache regions.
 */
public sealed interface CacheManagerService permits CacheManagerServiceImpl {

    /**
     * Evicts a specific key from the specified cache.
     *
     * @param cacheName the name of the cache
     * @param key       the key to evict
     */
    void evictKey(String cacheName, String key);

    /**
     * Evicts all entries from the specified cache.
     *
     * @param cacheName the name of the cache to clear
     */
    void evictAllFromCache(String cacheName);

    /**
     * Evicts cache entries whose keys start with the specified prefix.
     *
     * @param cacheName the name of the cache
     * @param prefix    the key prefix to match
     */
    void evictByPrefix(String cacheName, String prefix);
}