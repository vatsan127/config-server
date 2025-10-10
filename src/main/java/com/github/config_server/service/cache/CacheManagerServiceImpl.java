package com.github.config_server.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Implementation of CacheEvictionService that manages cache eviction operations
 * using Spring's CacheManager.
 */
@Service
public non-sealed class CacheManagerServiceImpl implements CacheManagerService {

    private static final Logger log = LoggerFactory.getLogger(CacheManagerServiceImpl.class);

    private final CacheManager cacheManager;

    public CacheManagerServiceImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void evictKey(String cacheName, String key) {
        log.debug("Evicting key '{}' from cache '{}'", key, cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted key '{}' from cache '{}'", key, cacheName);
        } else {
            log.warn("Cache '{}' not found for key eviction", cacheName);
        }
    }

    @Override
    public void evictAllFromCache(String cacheName) {
        log.debug("Clearing all entries from cache '{}'", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cleared all entries from cache '{}'", cacheName);
        } else {
            log.warn("Cache '{}' not found for clearing", cacheName);
        }
    }

    @Override
    public void evictByPrefix(String cacheName, String prefix) {
        log.debug("Evicting entries with prefix '{}' from cache '{}'", prefix, cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            @SuppressWarnings("unchecked")
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();

            // Use parallel stream for efficient prefix matching and eviction
            long evictedCount = caffeineCache.asMap().keySet().parallelStream()
                    .filter(key -> key instanceof String && ((String) key).startsWith(prefix))
                    .peek(cache::evict)
                    .count();

            log.debug("Evicted {} entries with prefix '{}' from cache '{}'", evictedCount, prefix, cacheName);
        } else {
            log.warn("Cache '{}' not found for prefix eviction", cacheName);
        }
    }
}