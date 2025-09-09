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
 * Tracks notification attempts, retries, and results for monitoring purposes.
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

        String payload = String.format("{\"namespace\":\"%s\",\"appName\":\"%s\"}", namespace, appName);
        int totalUrls = applicationConfig.getRefreshNotifyUrl().size();
        
        // Create and store initial notification using commitId
        if (commitId != null) {
            NotificationStatus notification = NotificationStatus.createInitial(commitId, totalUrls);
            notificationStorageService.storeNotification(namespace, notification);
        }

        // Send notifications to all configured URLs using virtual threads
        applicationConfig.getRefreshNotifyUrl().forEach(url -> 
            virtualThreadExecutorService.submit(
                () -> sendRequestWithTracking(url, payload, commitId, namespace)));
    }

    /**
     * Sends a request with notification tracking
     */
    private void sendRequestWithTracking(String url, String payload, String commitId, String namespace) {
        int maxRetries = applicationConfig.getRefreshApi().getMaxRetries();
        long retryInterval = applicationConfig.getRefreshApi().getRetryIntervalMs();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("sendRefreshNotifications :: Attempt {}/{} - URL: '{}', payload: '{}'", attempt, maxRetries, url, payload);

                String response = restClient
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);
                
                log.debug("sendRefreshNotifications :: response - '{}'", response);
                log.info("Successfully sent refresh notification to '{}' on attempt {}", url, attempt);
                
                // Update notification with success if commitId exists
                if (commitId != null) {
                    updateNotificationSuccess(namespace, commitId);
                }
                return; // Success, exit retry loop
                
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.error("Failed to send API request to URL '{}' after {} attempts. Final error: ", url, maxRetries, e);
                    if (commitId != null) {
                        updateNotificationFailure(namespace, commitId);
                    }
                } else {
                    log.warn("Attempt {}/{} failed for URL '{}', retrying in {}ms. Error: {}", attempt, maxRetries, url, retryInterval, e.getMessage());
                    if (commitId != null) {
                        updateNotificationRetry(namespace, commitId);
                    }
                    
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for URL '{}'", url);
                        if (commitId != null) {
                            updateNotificationFailure(namespace, commitId);
                        }
                        return;
                    }
                }
            }
        }
    }

    private void updateNotificationSuccess(String namespace, String commitId) {
        notificationStorageService.updateNotificationAtomic(
            namespace, commitId, NotificationStatus::withSuccess);
    }

    private void updateNotificationRetry(String namespace, String commitId) {
        notificationStorageService.updateNotificationAtomic(
            namespace, commitId, NotificationStatus::withRetry);
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