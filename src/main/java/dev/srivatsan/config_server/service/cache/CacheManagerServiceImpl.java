package dev.srivatsan.config_server.service.cache;

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
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared all entries from cache '{}'", cacheName);
        } else {
            log.warn("Cache '{}' not found for clearing", cacheName);
        }
    }
}