package com.github.config_server.service.notify;


import com.github.config_server.config.ApplicationConfig;
import com.github.config_server.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client notification service with async API call tracking
 */
@Service
public class ClientNotifyService {

    private static final Logger log = LoggerFactory.getLogger(ClientNotifyService.class);
    private static final String NOTIFICATION_PAYLOAD_TEMPLATE = "{\"appName\":\"%s\"}";
    private final ExecutorService virtualThreadExecutorService;
    private final RestClient restClient;
    private final ApplicationConfig applicationConfig;
    private final NotificationStorageService notificationStorageService;

    public ClientNotifyService(RestClient restClient, ApplicationConfig applicationConfig,
                               NotificationStorageService notificationStorageService) {
        this.restClient = restClient;
        this.applicationConfig = applicationConfig;
        this.notificationStorageService = notificationStorageService;
        this.virtualThreadExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void sendRefreshNotifications(String namespace, String appName, String commitId) {
        String url = applicationConfig.getRefreshNotifyUrl().get(namespace);
        String payload = String.format(NOTIFICATION_PAYLOAD_TEMPLATE, appName);

        // Generate unique tracking ID if commitId is null
        final String trackingId = (commitId != null) ? 
            commitId : 
            "notify-" + System.currentTimeMillis() + "-" + appName;

        // Always create notification entry for tracking, regardless of URL configuration
        Notification notification = Notification.createInitial(trackingId);
        notificationStorageService.storeNotification(namespace, notification);
        log.debug("Created notification for tracking ID: {}", trackingId);

        // Only send API call if URL is configured
        if (url == null || url.trim().isEmpty()) {
            log.debug("No refresh notification URL configured for namespace '{}', skipping API call but tracking commit event", namespace);
            // Mark as success since there's no URL to call
            updateNotificationStatus(namespace, trackingId, true);
            return;
        }

        // Send API call asynchronously
        virtualThreadExecutorService.submit(() -> sendRequestWithTracking(url, payload, trackingId, namespace));
    }

    private void sendRequestWithTracking(String url, String payload, String trackingId, String namespace) {
        try {
            log.debug("Sending notification to URL: '{}', payload: '{}'", url, payload);

            String response = restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("Notification response: '{}'", response);
            updateNotificationStatus(namespace, trackingId, true);
        } catch (Exception e) {
            log.error("Failed to send notification to URL '{}': {}", url, e.getMessage());
            updateNotificationStatus(namespace, trackingId, false);
        }
    }

    private void updateNotificationStatus(String namespace, String trackingId, boolean success) {
        Notification updated = notificationStorageService.updateNotificationAtomic(
                namespace, trackingId, success ? Notification::withSuccess : Notification::withFailure);
        
        if (updated == null) {
            log.error("Failed to update notification status for trackingId '{}' in namespace '{}'", 
                     trackingId, namespace);
        } else {
            log.debug("Updated notification status to {} for trackingId '{}' in namespace '{}'", 
                     success ? "SUCCESS" : "FAILED", trackingId, namespace);
        }
    }

    /**
     * Graceful shutdown of the executor service
     */
    // ToDo: this should be annotated with predestry
    public void shutdown() {
        virtualThreadExecutorService.shutdown();
        log.info("ClientNotifyService executor shutdown initiated");
    }
}