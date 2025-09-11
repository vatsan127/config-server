package dev.srivatsan.config_server.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "configserver")
public class ApplicationConfig {

    private int commitHistorySize;
    private String basePath;
    private String vaultMasterKey;
    private Map<String, String> refreshNotifyUrl;
    private VaultConfig vault = new VaultConfig();

    @PostConstruct
    public void init() {
        log.info("Global config loaded -> {}", this);
    }

    @Data
    public static class VaultConfig {
        private boolean enabled = true;
        private long cacheTtl = 600; // 10 minutes in seconds
        private int maxSecretsPerOperation = 100;
    }
}
