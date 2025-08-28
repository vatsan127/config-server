package dev.srivatsan.config_server.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }

    @Service
    public static class CacheEvictionService {
        
        private static final Logger log = LoggerFactory.getLogger(CacheEvictionService.class);
        
        private final CacheManager cacheManager;

        public CacheEvictionService(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        public void evictKey(String cacheName, String key) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.debug("Evicted key '{}' from cache '{}'", key, cacheName);
            } else {
                log.warn("Cache '{}' not found for key eviction", cacheName);
            }
        }

        public void evictKeys(String cacheName, List<String> keys) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                keys.forEach(key -> {
                    cache.evict(key);
                    log.debug("Evicted key '{}' from cache '{}'", key, cacheName);
                });
            } else {
                log.warn("Cache '{}' not found for key eviction", cacheName);
            }
        }

        public void evictAllFromCache(String cacheName) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared all entries from cache '{}'", cacheName);
            } else {
                log.warn("Cache '{}' not found for clearing", cacheName);
            }
        }

        public void evictFromMultipleCaches(List<String> cacheNames, String key) {
            cacheNames.forEach(cacheName -> evictKey(cacheName, key));
        }

        public boolean isCachePresent(String cacheName) {
            return cacheManager.getCache(cacheName) != null;
        }
    }
}