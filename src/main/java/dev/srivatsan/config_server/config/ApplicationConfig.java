package dev.srivatsan.config_server.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "configserver")
public class ApplicationConfig {

    private int commitHistorySize;
    private String basePath;
    private List<String> refreshNotifyUrl;

    @PostConstruct
    public void init() {
        log.info("Global config loaded -> {}", this);
    }

}
