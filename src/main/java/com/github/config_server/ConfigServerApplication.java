package com.github.config_server;

import com.github.config_server.config.ApplicationConfig;
import com.github.config_server.exception.NamespaceException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class ConfigServerApplication {

    private final Logger log = LoggerFactory.getLogger(ConfigServerApplication.class);
    private final ApplicationConfig applicationConfig;

    public ConfigServerApplication(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // TODO: Validate that the base directory exists (should be created by Dockerfile)
        File baseDir = new File(applicationConfig.getBasePath());
        log.info("Using base directory at: {}", baseDir.getAbsolutePath());

        if (!baseDir.exists()) {
            log.error("Base directory does not exist at: {}. Ensure the directory is created in the container.", baseDir.getAbsolutePath());
            throw NamespaceException.creationFailed("base",
                    new RuntimeException("Base directory does not exist. Check container configuration."));
        }
    }
}
