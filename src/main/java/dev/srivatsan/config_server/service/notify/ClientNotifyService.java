package dev.srivatsan.config_server.service.notify;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.Notification;
import dev.srivatsan.config_server.service.cache.CacheManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        Map<String, List<String>> refreshUrlConfig = applicationConfig.getRefreshNotifyUrl();
        List<String> urls = (refreshUrlConfig != null) ? refreshUrlConfig.get(namespace) : null;
        String payload = String.format(NOTIFICATION_PAYLOAD_TEMPLATE, appName);

        final String trackingId = (commitId != null) ? 
            commitId : 
            "notify-" + System.currentTimeMillis() + "-" + appName;

        Notification notification = Notification.createInitial(trackingId);
        notificationStorageService.storeNotification(namespace, notification);

        if (urls == null || urls.isEmpty()) {
            updateNotificationStatus(namespace, trackingId, true);
            return;
        }

        List<String> validUrls = urls.stream()
            .filter(url -> url != null && !url.trim().isEmpty())
            .toList();

        if (validUrls.isEmpty()) {
            updateNotificationStatus(namespace, trackingId, true);
            return;
        }

        virtualThreadExecutorService.submit(() -> sendRequestsToMultipleEndpoints(validUrls, payload, trackingId, namespace));
    }

    private void sendRequestsToMultipleEndpoints(List<String> urls, String payload, String trackingId, String namespace) {
        int successCount = 0;
        int totalCount = urls.size();
        
        for (String url : urls) {
            try {
                restClient
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(String.class);

                successCount++;
            } catch (Exception e) {
                log.error("Failed to send notification to URL '{}': {}", url, e.getMessage());
            }
        }
        
        boolean overallSuccess = successCount > 0;
        if (successCount != urls.size()) {
            log.warn("Notification batch completed for namespace '{}': {}/{} endpoints succeeded", 
                    namespace, successCount, urls.size());
        }
        
        updateNotificationStatus(namespace, trackingId, overallSuccess);
    }


    private void updateNotificationStatus(String namespace, String trackingId, boolean success) {
        Notification updated = notificationStorageService.updateNotificationAtomic(
                namespace, trackingId, success ? Notification::withSuccess : Notification::withFailure);
        
        if (updated == null) {
            log.error("Failed to update notification status for trackingId '{}' in namespace '{}'", 
                     trackingId, namespace);
        }
    }

    @PreDestroy
    public void shutdown() {
        virtualThreadExecutorService.shutdown();
        log.info("ClientNotifyService executor shutdown initiated");
    }
}