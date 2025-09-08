package dev.srivatsan.config_server.service.notify;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Optimized service for storing and managing notification status information.
 * Features size limits, TTL cleanup, and efficient data structures to prevent memory leaks.
 */
@Service
public class NotificationStorageService {

    private static final Logger log = LoggerFactory.getLogger(NotificationStorageService.class);
    private static final int DEFAULT_TTL_HOURS = 24;
    private static final int MAX_NOTIFICATIONS_PER_NAMESPACE = 1000;

    private final ApplicationConfig applicationConfig;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Optimized storage using LinkedHashMap for LRU behavior
     */
    private final Map<String, LinkedHashMap<String, NotificationStatus>> notificationStore = new ConcurrentHashMap<>();

    public NotificationStorageService(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    /**
     * Stores a notification status for a specific namespace with automatic size management
     *
     * @param namespace the namespace identifier
     * @param notification the notification status to store
     */
    public void storeNotification(String namespace, NotificationStatus notification) {
        lock.writeLock().lock();
        try {
            LinkedHashMap<String, NotificationStatus> namespaceNotifications = 
                notificationStore.computeIfAbsent(namespace, k -> new LinkedHashMap<String, NotificationStatus>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, NotificationStatus> eldest) {
                        return size() > MAX_NOTIFICATIONS_PER_NAMESPACE;
                    }
                });

            String key = generateNotificationKey(notification);
            namespaceNotifications.put(key, notification);
            
            log.debug("Stored notification for namespace '{}': {} (total: {})", 
                namespace, key, namespaceNotifications.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves recent notifications for a namespace, sorted by trigger time
     *
     * @param namespace the namespace identifier
     * @param maxCount the maximum number of notifications to return
     * @return list of most recent notification statuses
     */
    public List<NotificationStatus> getRecentNotifications(String namespace, int maxCount) {
        lock.readLock().lock();
        try {
            Map<String, NotificationStatus> namespaceNotifications = notificationStore.get(namespace);
            if (namespaceNotifications == null || namespaceNotifications.isEmpty()) {
                return List.of();
            }

            return namespaceNotifications.values().stream()
                    .filter(this::isNotExpired)
                    .sorted((n1, n2) -> n2.getTriggeredAt().compareTo(n1.getTriggeredAt())) // Most recent first
                    .limit(maxCount)
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates an existing notification status efficiently using key lookup
     * Since NotificationStatus is immutable, this replaces the existing instance
     *
     * @param namespace the namespace identifier
     * @param notification the updated notification status
     */
    public void updateNotification(String namespace, NotificationStatus notification) {
        lock.writeLock().lock();
        try {
            Map<String, NotificationStatus> namespaceNotifications = notificationStore.get(namespace);
            if (namespaceNotifications != null) {
                String key = generateNotificationKey(notification);
                
                // For immutable objects, simply replace the existing notification
                NotificationStatus existing = namespaceNotifications.put(key, notification);
                
                if (existing != null) {
                    log.debug("Updated notification for namespace '{}': {} -> status: {}", 
                        namespace, key, notification.getStatus());
                } else {
                    log.debug("Stored new notification (update) for namespace '{}': {}", namespace, key);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Efficiently clears all notifications for a namespace
     *
     * @param namespace the namespace identifier
     */
    public void clearNotifications(String namespace) {
        lock.writeLock().lock();
        try {
            Map<String, NotificationStatus> removed = notificationStore.remove(namespace);
            if (removed != null) {
                log.debug("Cleared {} notifications for namespace '{}'", removed.size(), namespace);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Scheduled cleanup of expired notifications to prevent memory leaks
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredNotifications() {
        lock.writeLock().lock();
        try {
            int totalRemoved = 0;
            LocalDateTime cutoff = LocalDateTime.now().minus(DEFAULT_TTL_HOURS, ChronoUnit.HOURS);

            for (Map.Entry<String, LinkedHashMap<String, NotificationStatus>> namespaceEntry : notificationStore.entrySet()) {
                String namespace = namespaceEntry.getKey();
                LinkedHashMap<String, NotificationStatus> notifications = namespaceEntry.getValue();
                
                int initialSize = notifications.size();
                notifications.values().removeIf(notification -> notification.getTriggeredAt().isBefore(cutoff));
                int removed = initialSize - notifications.size();
                
                if (removed > 0) {
                    totalRemoved += removed;
                    log.debug("Cleaned up {} expired notifications from namespace '{}'", removed, namespace);
                }

                // Remove empty namespace entries
                if (notifications.isEmpty()) {
                    notificationStore.remove(namespace);
                    log.debug("Removed empty namespace entry: '{}'", namespace);
                }
            }

            if (totalRemoved > 0) {
                log.info("Cleanup completed: removed {} expired notifications across all namespaces", totalRemoved);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Generates a unique key for notification storage and lookup
     */
    private String generateNotificationKey(NotificationStatus notification) {
        return String.format("%s-%s-%s", 
            notification.getAppName(), 
            notification.getOperation(),
            notification.getTriggeredAt().toString());
    }

    /**
     * Checks if a notification has not expired based on TTL
     */
    private boolean isNotExpired(NotificationStatus notification) {
        return notification.getTriggeredAt().isAfter(
            LocalDateTime.now().minus(DEFAULT_TTL_HOURS, ChronoUnit.HOURS));
    }

    /**
     * Returns storage statistics for monitoring
     */
    public Map<String, Object> getStorageStats() {
        lock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalNamespaces", notificationStore.size());
            stats.put("totalNotifications", notificationStore.values().stream()
                .mapToInt(Map::size).sum());
            stats.put("maxNotificationsPerNamespace", MAX_NOTIFICATIONS_PER_NAMESPACE);
            stats.put("ttlHours", DEFAULT_TTL_HOURS);
            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }
}