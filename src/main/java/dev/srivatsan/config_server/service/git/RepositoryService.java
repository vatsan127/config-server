package dev.srivatsan.config_server.service.git;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.service.util.UtilService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Service
public class RepositoryService {

    private final Logger log = LoggerFactory.getLogger(RepositoryService.class);
    private final ApplicationConfig applicationConfig;
    private final UtilService utilService;

    public RepositoryService(ApplicationConfig applicationConfig, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.utilService = utilService;
    }

    public boolean checkAppConfigExists(String fileName) {
        File repoDir = new File(applicationConfig.getBasePath() + "/" + fileName);
        return repoDir.exists();
    }

    public void createAppConfig(String fileName) throws IOException, GitAPIException {

        if (checkAppConfigExists(fileName + applicationConfig.getFileExtension())) {
            log.info("Application Config already exists: {}", fileName);
            return;
        }

        File repoDir = new File(applicationConfig.getBasePath());
        try (Git git = Git.open(repoDir)) {

            File newFile = new File(git.getRepository().getWorkTree(), fileName);
            try (FileWriter writer = new FileWriter(newFile)) {
                writer.write("spring.application.name: " + fileName);
            }
            log.info("Created file: '{}'", newFile.getAbsolutePath());

            git.add().addFilepattern(fileName).call();
            git.commit().setMessage("First commit ApplicationName - " + fileName).call();
        } catch (RepositoryNotFoundException e) {
            log.error("Repository not found at path: {}", repoDir.getAbsolutePath());
            throw e;
        }
    }


    public String getAppConfigContent(String path, String fileName) throws IOException {
        String temp = utilService.getYamlValueByKey(applicationConfig.getBasePath() + path + fileName, "spring");
        log.info("Getting app config content from '{}'", temp);
        String ymlFileContent = utilService.getYmlFileContent(applicationConfig.getBasePath() + path + fileName);
        return ymlFileContent;
    }


}
