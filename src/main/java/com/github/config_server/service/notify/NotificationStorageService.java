package com.github.config_server.service.notify;


import com.github.config_server.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* ToDo: Deprecate this feature */

/**
 * Simplified in-memory storage for notification status tracking using List-based approach
 */
@Service
public class NotificationStorageService {

    private static final Logger log = LoggerFactory.getLogger(NotificationStorageService.class);
    private static final int MAX_NOTIFICATIONS_PER_NAMESPACE = 20;

    /**
     * Thread-safe storage: namespace -> list of recent notifications (insertion order maintained)
     * Each list is bounded to MAX_NOTIFICATIONS_PER_NAMESPACE
     */
    private final Map<String, List<Notification>> notificationStorage = new ConcurrentHashMap<>();

    public NotificationStorageService() {
    }

    /**
     * Stores a notification in the namespace list
     * Automatically removes oldest notification if list exceeds size limit
     *
     * @param namespace    the namespace identifier
     * @param notification the notification status to store
     */
    public void storeNotification(String namespace, Notification notification) {
        List<Notification> notifications = notificationStorage.computeIfAbsent(
                namespace, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (notifications) {
            // Remove oldest if at max capacity (before adding new one)
            if (notifications.size() >= MAX_NOTIFICATIONS_PER_NAMESPACE) {
                Notification removed = notifications.removeFirst();
                log.debug("Removed oldest notification from namespace '{}': {}", namespace, removed.getId());
            }
            
            // Add new notification at the end
            notifications.add(notification);
        }

        log.debug("Stored notification in namespace '{}': {} -> {}", namespace, notification.getId(), notification.getStatus());
    }


    /**
     * Retrieves recent notifications for a namespace
     *
     * @param namespace the namespace identifier
     * @param maxCount  the maximum number of notifications to return
     * @return list of most recent notification statuses (newest first)
     */
    public List<Notification> getRecentNotifications(String namespace, int maxCount) {
        List<Notification> notifications = notificationStorage.get(namespace);
        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }

        synchronized (notifications) {
            return notifications.stream()
                    .sorted((n1, n2) -> n2.getInitiatedTime().compareTo(n1.getInitiatedTime()))
                    .limit(maxCount)
                    .toList();
        }
    }



    /**
     * Updates a notification using the provided function
     * Simplified with direct list access and replacement
     *
     * @param namespace      the namespace identifier
     * @param commitId       the commit ID identifier
     * @param updateFunction function to transform the current notification
     * @return the updated notification, or null if not found
     */
    public Notification updateNotificationAtomic(String namespace, String commitId,
                                                 java.util.function.UnaryOperator<Notification> updateFunction) {
        if (namespace == null || commitId == null) {
            log.warn("Cannot update notification: namespace or commitId is null (namespace: '{}', commitId: '{}')", 
                    namespace, commitId);
            return null;
        }

        List<Notification> notifications = notificationStorage.get(namespace);
        if (notifications == null) {
            log.warn("No notifications exist for namespace '{}', cannot update commitId '{}'", 
                    namespace, commitId);
            return null;
        }

        // Find and update the notification by index
        // No synchronization needed since only one thread updates a specific notification
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            if (Objects.equals(notification.getId(), commitId)) {
                try {
                    Notification updated = updateFunction.apply(notification);
                    if (updated == null) {
                        log.error("Update function returned null for notification '{}' in namespace '{}'", 
                                commitId, namespace);
                        return null;
                    }
                    
                    // Replace at the same index to maintain order
                    notifications.set(i, updated);
                    log.debug("Updated notification in namespace '{}': {} -> {}", namespace, commitId, updated.getStatus());
                    return updated;
                } catch (Exception e) {
                    log.error("Error updating notification '{}' in namespace '{}': {}", 
                            commitId, namespace, e.getMessage(), e);
                    return null;
                }
            }
        }

        log.warn("Notification with commitId '{}' not found in namespace '{}'", commitId, namespace);
        return null;
    }
}