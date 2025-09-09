package dev.srivatsan.config_server.service.notify;

import dev.srivatsan.config_server.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple queue-based service for storing recent notification status information.
 * Each namespace maintains a bounded queue that automatically removes old data.
 */
@Service
public class NotificationStorageService {

    private static final Logger log = LoggerFactory.getLogger(NotificationStorageService.class);
    private static final int MAX_NOTIFICATIONS_PER_NAMESPACE = 20;

    /**
     * Thread-safe storage: namespace -> queue of recent notifications
     * Each queue is bounded to MAX_NOTIFICATIONS_PER_NAMESPACE
     */
    private final Map<String, Queue<NotificationStatus>> notificationQueues = new ConcurrentHashMap<>();

    public NotificationStorageService() {
    }

    /**
     * Stores a notification in the namespace queue
     * Automatically removes oldest notification if queue exceeds size limit
     *
     * @param namespace the namespace identifier
     * @param notification the notification status to store
     */
    public void storeNotification(String namespace, NotificationStatus notification) {
        Queue<NotificationStatus> queue = notificationQueues.computeIfAbsent(
            namespace, k -> new ConcurrentLinkedQueue<>());
        
        // Add new notification
        queue.offer(notification);
        
        // Remove oldest if exceeds limit
        while (queue.size() > MAX_NOTIFICATIONS_PER_NAMESPACE) {
            NotificationStatus removed = queue.poll();
            if (removed != null) {
                log.debug("Removed oldest notification from namespace '{}': {}", namespace, removed.getId());
            }
        }
        
        log.debug("Stored notification in namespace '{}': {} -> {}", namespace, notification.getId(), notification.getStatus());
    }


    /**
     * Retrieves recent notifications for a namespace
     *
     * @param namespace the namespace identifier
     * @param maxCount the maximum number of notifications to return
     * @return list of most recent notification statuses
     */
    public List<NotificationStatus> getRecentNotifications(String namespace, int maxCount) {
        Queue<NotificationStatus> queue = notificationQueues.get(namespace);
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        
        return queue.stream()
                .sorted((n1, n2) -> n2.getInitiatedTime().compareTo(n1.getInitiatedTime()))
                .limit(maxCount)
                .toList();
    }

    /**
     * Updates an existing notification in the queue
     * Since queues don't support direct update, this removes the old and adds the new
     *
     * @param namespace the namespace identifier
     * @param notification the updated notification status
     */
    public void updateNotification(String namespace, NotificationStatus notification) {
        Queue<NotificationStatus> queue = notificationQueues.get(namespace);
        if (queue != null) {
            // Remove existing notification with same id if present
            queue.removeIf(existing -> Objects.equals(existing.getId(), notification.getId()));
            // Add updated notification
            queue.offer(notification);
            log.debug("Updated notification in namespace '{}': {} -> {}", namespace, notification.getId(), notification.getStatus());
        }
    }

    /**
     * Updates a notification using the provided function
     * Note: Not truly atomic with queues, but sufficient for this use case
     *
     * @param namespace the namespace identifier  
     * @param commitId the commit ID identifier
     * @param updateFunction function to transform the current notification
     * @return the updated notification, or null if not found
     */
    public NotificationStatus updateNotificationAtomic(String namespace, String commitId, 
                                                      java.util.function.UnaryOperator<NotificationStatus> updateFunction) {
        Queue<NotificationStatus> queue = notificationQueues.get(namespace);
        if (queue == null) {
            return null;
        }
        
        // Find and update the notification
        NotificationStatus found = null;
        for (NotificationStatus notification : queue) {
            if (Objects.equals(notification.getId(), commitId)) {
                found = notification;
                break;
            }
        }
        
        if (found != null) {
            NotificationStatus updated = updateFunction.apply(found);
            queue.remove(found);
            queue.offer(updated);
            log.debug("Updated notification in namespace '{}': {} -> {}", namespace, commitId, updated.getStatus());
            return updated;
        }
        
        return null;
    }

    /**
     * Clears all notifications for a namespace
     *
     * @param namespace the namespace identifier
     */
    public void clearNotifications(String namespace) {
        Queue<NotificationStatus> removed = notificationQueues.remove(namespace);
        if (removed != null) {
            log.debug("Cleared {} notifications for namespace '{}'", removed.size(), namespace);
        }
    }


    /**
     * Retrieves a specific notification by commit ID
     */
    public NotificationStatus getNotificationByCommitId(String namespace, String commitId) {
        Queue<NotificationStatus> queue = notificationQueues.get(namespace);
        if (queue == null) {
            return null;
        }
        
        return queue.stream()
                .filter(notification -> Objects.equals(notification.getId(), commitId))
                .findFirst()
                .orElse(null);
    }


    /**
     * Returns storage statistics for monitoring
     */
    public Map<String, Object> getStorageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNamespaces", notificationQueues.size());
        
        // Count total notifications across all namespaces
        int totalNotifications = notificationQueues.values().stream()
            .mapToInt(Queue::size)
            .sum();
        stats.put("totalNotifications", totalNotifications);
        stats.put("maxNotificationsPerNamespace", MAX_NOTIFICATIONS_PER_NAMESPACE);
        return stats;
    }
}