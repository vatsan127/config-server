package dev.srivatsan.config_server.service.notify;

import dev.srivatsan.config_server.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

@Service
public class ClientNotifyService {

    private static final Logger log = LoggerFactory.getLogger(ClientNotifyService.class);
    private final ExecutorService virtualThreadExecutorService;
    private final RestClient restClient;
    private final BiConsumer<String, String> refreshTask;
    private final ApplicationConfig applicationConfig;

    public ClientNotifyService(RestClient restClient, ApplicationConfig applicationConfig) {
        this.restClient = restClient;
        this.applicationConfig = applicationConfig;
        this.virtualThreadExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        this.refreshTask = this::sendRequest;
    }

    public void sendRefreshNotifications(String namespace, String appName) {
        if (applicationConfig.getRefreshNotifyUrl() == null || applicationConfig.getRefreshNotifyUrl().isEmpty()) {
            return;
        }

        String payload = String.format("{\"namespace\":\"%s\",\"appName\":\"%s\"}", namespace, appName);
        applicationConfig.getRefreshNotifyUrl().forEach(url -> virtualThreadExecutorService.submit(() -> refreshTask.accept(url, payload)));
    }

    private void sendRequest(String url, String payload) {
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
                return; // Success, exit retry loop
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.error("Failed to send API request to URL '{}' after {} attempts. Final error: ", url, maxRetries, e);
                } else {
                    log.warn("Attempt {}/{} failed for URL '{}', retrying in {}ms. Error: {}", attempt, maxRetries, url, retryInterval, e.getMessage());
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for URL '{}'", url);
                        return;
                    }
                }
            }
        }
    }
}