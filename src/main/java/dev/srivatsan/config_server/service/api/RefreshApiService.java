package dev.srivatsan.config_server.service.api;

import dev.srivatsan.config_server.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

@Service
public class RefreshApiService {

    private static final Logger log = LoggerFactory.getLogger(RefreshApiService.class);
    private final ExecutorService virtualThreadExecutorService;
    private final RestClient restClient;
    private final BiConsumer<String, String> refreshTask;
    private final ApplicationConfig applicationConfig;

    public RefreshApiService(RestClient restClient, ApplicationConfig applicationConfig) {
        this.restClient = restClient;
        this.applicationConfig = applicationConfig;
        this.virtualThreadExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        this.refreshTask = this::sendRefreshRequest;
    }

    public void sendRefreshNotifications(String namespace, String appName) {
        if (applicationConfig.getRefreshNotifyUrl() == null || applicationConfig.getRefreshNotifyUrl().isEmpty()) {
            return;
        }

        String payload = String.format("{\"namespace\":\"%s\",\"appName\":\"%s\"}", namespace, appName);
        applicationConfig.getRefreshNotifyUrl().forEach(url -> virtualThreadExecutorService.submit(() -> refreshTask.accept(url, payload)));
    }

    private void sendRefreshRequest(String url, String payload) {
        try { // ToDo: Add retry functionality with interval. use both values from configuration file
            log.info("sendRefreshNotifications :: URL - '{}', payload - '{}'", url, payload);
            String response = restClient
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            log.debug("sendRefreshNotifications :: response - '{}'", response);
        } catch (Exception e) {
            log.error("Error While Sending API Request to URL - '{}', payload - '{}'. Error -> ", url, payload, e);
        }
    }
}