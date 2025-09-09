package dev.srivatsan.config_server.service.notify;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.NotificationStatus;
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
    private final ExecutorService virtualThreadExecutorService;
    private final RestClient restClient;
    private final ApplicationConfig applicationConfig;
    private final NotificationStorageService notificationStorageService;
    private final CacheManagerService cacheManagerService;

    public ClientNotifyService(RestClient restClient, ApplicationConfig applicationConfig,
                               NotificationStorageService notificationStorageService,
                               CacheManagerService cacheManagerService) {
        this.restClient = restClient;
        this.applicationConfig = applicationConfig;
        this.notificationStorageService = notificationStorageService;
        this.cacheManagerService = cacheManagerService;
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
        if (applicationConfig.getRefreshNotifyUrl() == null || applicationConfig.getRefreshNotifyUrl().isEmpty()) {
            return;
        }

        String payload = String.format("{\"namespace\":\"%s\"}", namespace);
        int totalUrls = applicationConfig.getRefreshNotifyUrl().size();

        // Update existing notification or create new one if doesn't exist
        if (commitId != null) {
            // Try to update existing notification, or create new one if not found
            NotificationStatus existingNotification = notificationStorageService.getNotificationByCommitId(namespace, commitId);

            if (existingNotification != null) {
                // Reset existing notification to fresh state
                NotificationStatus resetNotification = existingNotification.resetForRetry(totalUrls);
                notificationStorageService.updateNotification(namespace, resetNotification);
            } else {
                // Create new notification if none exists
                NotificationStatus notification = NotificationStatus.createInitial(commitId, totalUrls);
                notificationStorageService.storeNotification(namespace, notification);
            }
        }

        // Send notifications to all configured URLs using virtual threads
        applicationConfig.getRefreshNotifyUrl().forEach(url ->
                virtualThreadExecutorService.submit(
                        () -> sendRequestWithTracking(url, payload, commitId, namespace)
                )
        );
    }

    /**
     * Sends a request with notification tracking
     */
    private void sendRequestWithTracking(String url, String payload, String commitId, String namespace) {
        try {
            log.info("sendRefreshNotifications :: Sending to URL: '{}', payload: '{}'", url, payload);

            String response = restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("sendRefreshNotifications :: response - '{}'", response);
            log.info("Successfully sent refresh notification to '{}'", url);

            updateNotificationSuccess(namespace, commitId);

        } catch (Exception e) {
            log.error("Failed to send API request to URL '{}'. Error: ", url, e);
            if (commitId != null) {
                updateNotificationFailure(namespace, commitId);
            }
        }
    }

    private void updateNotificationSuccess(String namespace, String commitId) {
        notificationStorageService.updateNotificationAtomic(
                namespace, commitId, NotificationStatus::withSuccess);
    }

    private void updateNotificationFailure(String namespace, String commitId) {
        notificationStorageService.updateNotificationAtomic(
                namespace, commitId, NotificationStatus::withFailure);
    }

    /**
     * Graceful shutdown of the executor service
     */
    public void shutdown() {
        virtualThreadExecutorService.shutdown();
        log.info("ClientNotifyService executor shutdown initiated");
    }
}