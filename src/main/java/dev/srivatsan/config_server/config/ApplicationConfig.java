package dev.srivatsan.config_server.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "global")
public class ApplicationConfig {

    private String basePath;
    private String environment;

    @PostConstruct
    public void init() {
        log.info("Global config loaded -> {}", this);
    }

}
