package dev.srivatsan.config_server.service.notification;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.service.api.RefreshApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RefreshNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RefreshNotificationService.class);
    
    private final ApplicationConfig applicationConfig;
    private final RefreshApiService refreshApiService;

    public RefreshNotificationService(ApplicationConfig applicationConfig, RefreshApiService refreshApiService) {
        this.applicationConfig = applicationConfig;
        this.refreshApiService = refreshApiService;
    }

    public void sendRefreshNotifications(String namespace, String appName) {
        log.info("Sending refresh notifications for app: {}, namespace: {}", appName, namespace);
        refreshApiService.sendRefreshNotifications(applicationConfig.getRefreshURL(), namespace, appName);
    }
}