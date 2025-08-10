package dev.srivatsan.config_server;

import dev.srivatsan.config_server.config.ApplicationConfig;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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
    public void init() throws GitAPIException {
        File repoDir = new File(applicationConfig.getBasePath());
        try (Git git = Git.init().setDirectory(repoDir).call()) {
            log.info("Repository is ready at: {}", repoDir.getAbsolutePath());
        } catch (IllegalStateException | GitAPIException e) {
            log.error("Failed to initialize repository at: {}", repoDir.getAbsolutePath(), e);
            throw e;
        }
    }

}
