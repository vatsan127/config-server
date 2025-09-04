package dev.srivatsan.config_server.service.api;

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

    public RefreshApiService(RestClient restClient) {
        this.restClient = restClient;
        this.virtualThreadExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        this.refreshTask = this::sendRefreshRequest;
    }

    public void sendRefreshNotifications(List<String> urls, String namespace, String appName) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        String payload = String.format("{\"namespace\":\"%s\",\"appName\":\"%s\"}", namespace, appName);
        urls.forEach(url -> virtualThreadExecutorService.submit(() -> refreshTask.accept(url, payload)));
    }

    private void sendRefreshRequest(String url, String payload) {
        try {
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