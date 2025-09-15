package dev.srivatsan.config_server.service.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

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
        } else {
            log.warn("Cache '{}' not found for key eviction", cacheName);
        }
    }

    @Override
    public void evictAllFromCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            log.warn("Cache '{}' not found for clearing", cacheName);
        }
    }

    @Override
    public void evictByPrefix(String cacheName, String prefix) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            @SuppressWarnings("unchecked")
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();

            caffeineCache.asMap().keySet().parallelStream()
                    .filter(key -> key instanceof String && ((String) key).startsWith(prefix))
                    .forEach(cache::evict);
        } else {
            log.warn("Cache '{}' not found for prefix eviction", cacheName);
        }
    }
}