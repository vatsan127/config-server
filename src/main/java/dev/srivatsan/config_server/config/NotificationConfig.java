package dev.srivatsan.config_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the notification tracking system.
 * Allows fine-tuning of memory usage, cleanup intervals, and performance settings.
 */
@Configuration // ToDo: This needs to be removed
@ConfigurationProperties(prefix = "config-server.notifications")
public class NotificationConfig {

    /**
     * Maximum number of notifications to store per namespace (default: 1000)
     */
    private int maxNotificationsPerNamespace = 1000;

    /**
     * TTL for notifications in hours (default: 24 hours)
     */
    private int ttlHours = 24;

    /**
     * Cleanup interval in milliseconds (default: 1 hour)
     */
    private long cleanupIntervalMs = 3600000;

    /**
     * Enable parallel processing for large notification lists (default: true)
     */
    private boolean enableParallelProcessing = true;

    /**
     * Threshold for switching to parallel processing (default: 50 items)
     */
    private int parallelProcessingThreshold = 50;

    /**
     * Enable debug storage statistics in API responses (default: false)
     */
    private boolean enableDebugStats = false;

    /**
     * Maximum size for notification cache entries (default: 100MB)
     */
    private String maxCacheSize = "100MB";

    /**
     * Cache expiration time for notification responses in minutes (default: 5)
     */
    private int cacheExpirationMinutes = 5;

    // Getters and setters
    public int getMaxNotificationsPerNamespace() {
        return maxNotificationsPerNamespace;
    }

    public void setMaxNotificationsPerNamespace(int maxNotificationsPerNamespace) {
        this.maxNotificationsPerNamespace = maxNotificationsPerNamespace;
    }

    public int getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(int ttlHours) {
        this.ttlHours = ttlHours;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public boolean isEnableParallelProcessing() {
        return enableParallelProcessing;
    }

    public void setEnableParallelProcessing(boolean enableParallelProcessing) {
        this.enableParallelProcessing = enableParallelProcessing;
    }

    public int getParallelProcessingThreshold() {
        return parallelProcessingThreshold;
    }

    public void setParallelProcessingThreshold(int parallelProcessingThreshold) {
        this.parallelProcessingThreshold = parallelProcessingThreshold;
    }

    public boolean isEnableDebugStats() {
        return enableDebugStats;
    }

    public void setEnableDebugStats(boolean enableDebugStats) {
        this.enableDebugStats = enableDebugStats;
    }

    public String getMaxCacheSize() {
        return maxCacheSize;
    }

    public void setMaxCacheSize(String maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    public int getCacheExpirationMinutes() {
        return cacheExpirationMinutes;
    }

    public void setCacheExpirationMinutes(int cacheExpirationMinutes) {
        this.cacheExpirationMinutes = cacheExpirationMinutes;
    }
}