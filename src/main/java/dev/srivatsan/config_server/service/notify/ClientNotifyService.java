package dev.srivatsan.config_server.service.notify;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.NotificationStatus;
import dev.srivatsan.config_server.service.cache.CacheManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;
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
        
        // Create and store initial notification
        NotificationStatus initialNotification = NotificationStatus.builder()
                .appName(appName)
                .operation("refresh-notify")
                .namespace(namespace)
                .status(NotificationStatus.Status.inprogress)
                .retryCount(0)
                .commitId(commitId)
                .build();
        
        notificationStorageService.storeNotification(namespace, initialNotification);
        
        // Clear namespace notifications cache since we added a new notification
        cacheManagerService.evictKey("namespace-notifications", namespace);

        // Send notifications to all configured URLs asynchronously
        CompletableFuture<Void> allNotifications = CompletableFuture.allOf(
            applicationConfig.getRefreshNotifyUrl().stream()
                .map(url -> CompletableFuture.runAsync(
                    () -> sendRequestWithTracking(url, payload, namespace, appName, commitId), 
                    virtualThreadExecutorService))
                .toArray(CompletableFuture[]::new)
        );

        // Handle completion
        allNotifications.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Some refresh notifications failed for app '{}' in namespace '{}'", appName, namespace, throwable);
            } else {
                log.debug("All refresh notifications completed for app '{}' in namespace '{}'", appName, namespace);
            }
        });
    }

    /**
     * Sends a request with integrated notification tracking
     */
    private void sendRequestWithTracking(String url, String payload, String namespace, String appName, String commitId) {
        int maxRetries = applicationConfig.getRefreshApi().getMaxRetries();
        long retryInterval = applicationConfig.getRefreshApi().getRetryIntervalMs();
        
        NotificationStatus currentNotification = NotificationStatus.builder()
                .appName(appName)
                .operation("refresh-notify")
                .namespace(namespace)
                .status(NotificationStatus.Status.inprogress)
                .retryCount(0)
                .commitId(commitId)
                .build();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("sendRefreshNotifications :: Attempt {}/{} - URL: '{}', payload: '{}'", attempt, maxRetries, url, payload);
                
                // Update notification for retry attempts
                if (attempt > 1) {
                    currentNotification = currentNotification.toBuilder()
                            .retryCount(attempt - 1)
                            .errorMessage(String.format("Retrying attempt %d/%d", attempt, maxRetries))
                            .build();
                    notificationStorageService.updateNotification(namespace, currentNotification);
                    cacheManagerService.evictKey("namespace-notifications", namespace);
                }

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
                
                // Update notification to success
                NotificationStatus successNotification = currentNotification.toBuilder()
                        .status(NotificationStatus.Status.success)
                        .retryCount(attempt - 1)
                        .errorMessage(null)
                        .build();
                notificationStorageService.updateNotification(namespace, successNotification);
                cacheManagerService.evictKey("namespace-notifications", namespace);
                
                return; // Success, exit retry loop
                
            } catch (Exception e) {
                String errorMessage = String.format("Attempt %d failed: %s", attempt, e.getMessage());
                
                if (attempt == maxRetries) {
                    log.error("Failed to send API request to URL '{}' after {} attempts. Final error: ", url, maxRetries, e);
                    
                    // Update notification to failed
                    NotificationStatus failedNotification = currentNotification.toBuilder()
                            .status(NotificationStatus.Status.failed)
                            .retryCount(maxRetries)
                            .errorMessage(String.format("Failed after %d attempts: %s", maxRetries, e.getMessage()))
                            .build();
                    notificationStorageService.updateNotification(namespace, failedNotification);
                    cacheManagerService.evictKey("namespace-notifications", namespace);
                    
                } else {
                    log.warn("Attempt {}/{} failed for URL '{}', retrying in {}ms. Error: {}", attempt, maxRetries, url, retryInterval, e.getMessage());
                    
                    // Update notification with retry info
                    currentNotification = currentNotification.toBuilder()
                            .retryCount(attempt)
                            .errorMessage(errorMessage)
                            .build();
                    notificationStorageService.updateNotification(namespace, currentNotification);
                    cacheManagerService.evictKey("namespace-notifications", namespace);
                    
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for URL '{}'", url);
                        
                        // Update notification to failed due to interruption
                        NotificationStatus interruptedNotification = currentNotification.toBuilder()
                                .status(NotificationStatus.Status.failed)
                                .errorMessage("Operation interrupted during retry")
                                .build();
                        notificationStorageService.updateNotification(namespace, interruptedNotification);
                        cacheManagerService.evictKey("namespace-notifications", namespace);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Graceful shutdown of the executor service
     */
    public void shutdown() {
        virtualThreadExecutorService.shutdown();
        log.info("ClientNotifyService executor shutdown initiated");
    }
}