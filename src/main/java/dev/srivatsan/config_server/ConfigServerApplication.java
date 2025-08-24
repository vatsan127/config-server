package dev.srivatsan.config_server;

import dev.srivatsan.config_server.config.ApplicationConfig;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.File;

@EnableAspectJAutoProxy
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
    public void init() throws GitAPIException {
        File baseDir = new File(applicationConfig.getBasePath());
        if (!baseDir.exists()) {
            boolean created = baseDir.mkdirs();
            if (created) {
                log.info("Created base directory at: {}", baseDir.getAbsolutePath());
            } else {
                log.error("Failed to create base directory at: {}", baseDir.getAbsolutePath());
                throw new RuntimeException("Failed to create base directory");
            }
        } else {
            log.info("Base directory already exists at: {}", baseDir.getAbsolutePath());
        }
    }

}
