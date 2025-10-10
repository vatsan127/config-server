package com.github.config_server.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
    private long cacheTTL;

    @PostConstruct
    public void init() {
        log.info("Global config loaded -> {}", this);
    }

}
