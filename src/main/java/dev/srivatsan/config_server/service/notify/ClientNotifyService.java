package dev.srivatsan.config_server.service.notify;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.Notification;
import dev.srivatsan.config_server.service.cache.CacheManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced client notification service with async notification tracking integration.
 * Tracks notification attempts and results for monitoring purposes.
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
                               NotificationStorageService notificationStorageService,
                               CacheManagerService cacheManagerService) {
        this.restClient = restClient;
        this.applicationConfig = applicationConfig;
        this.notificationStorageService = notificationStorageService;
        this.virtualThreadExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Sends refresh notifications with integrated tracking
     */
    public void sendRefreshNotifications(String namespace, String appName) {
        sendRefreshNotifications(namespace, appName, null);
    }

    /**
     * Enhanced version that tracks notifications and optionally associates with a commit
     */
    public void sendRefreshNotifications(String namespace, String appName, String commitId) {
        if (applicationConfig.getRefreshNotifyUrl().get(namespace) == null || applicationConfig.getRefreshNotifyUrl().get(namespace).isEmpty()) {
            return;
        }

        String url = applicationConfig.getRefreshNotifyUrl().get(namespace);
        String payload = String.format(NOTIFICATION_PAYLOAD_TEMPLATE, appName);
        int totalUrls = applicationConfig.getRefreshNotifyUrl().size();

        // Update existing notification or create new one if doesn't exist
        if (commitId != null) {
            // Try to update existing notification, or create new one if not found
            Notification existingNotification = notificationStorageService.getNotificationByCommitId(namespace, commitId);

            if (existingNotification != null) {
                // Reset existing notification to fresh state
                Notification resetNotification = existingNotification.resetForRetry(totalUrls);
                notificationStorageService.updateNotification(namespace, resetNotification);
            } else {
                // Create new notification if none exists
                Notification notification = Notification.createInitial(commitId, totalUrls);
                notificationStorageService.storeNotification(namespace, notification);
            }
        }

        // Send notifications to all configured URLs using virtual threads
        virtualThreadExecutorService.submit(() -> sendRequestWithTracking(url, payload, commitId, namespace));

    }

    /**
     * Sends a request with notification tracking
     */
    private void sendRequestWithTracking(String url, String payload, String commitId, String namespace) {
        try {
            log.debug("sendRefreshNotifications :: Sending to URL: '{}', payload: '{}'", url, payload);

            String response = restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("sendRefreshNotifications :: response - '{}'", response);
            updateNotificationSuccess(namespace, commitId);
        } catch (Exception e) {
            log.error("Failed to send API request to URL '{}'. Error: ", url, e);
            updateNotificationFailure(namespace, commitId);
        }
    }

    // ToDo: optimize the below code
    private void updateNotificationSuccess(String namespace, String commitId) {
        notificationStorageService.updateNotificationAtomic(
                namespace, commitId, Notification::withSuccess);
    }

    private void updateNotificationFailure(String namespace, String commitId) {
        notificationStorageService.updateNotificationAtomic(
                namespace, commitId, Notification::withFailure);
    }

    /**
     * Graceful shutdown of the executor service
     */
    public void shutdown() {
        virtualThreadExecutorService.shutdown();
        log.info("ClientNotifyService executor shutdown initiated");
    }
}