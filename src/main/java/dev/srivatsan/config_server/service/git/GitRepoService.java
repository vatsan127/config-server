package dev.srivatsan.config_server.service.git;

import dev.srivatsan.config_server.config.ApplicationConfig;
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
public class GitRepoService {

    private final Logger log = LoggerFactory.getLogger(GitRepoService.class);

    private final ApplicationConfig applicationConfig;

    public GitRepoService(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    public void checkAndCreateRepo() throws GitAPIException {
        File repoDir = new File(applicationConfig.getBasePath());
        try (Git git = Git.init().setDirectory(repoDir).call()) {
            log.info("Repository is ready at: {}", repoDir.getAbsolutePath());
        } catch (IllegalStateException | GitAPIException e) {
            log.error("Failed to initialize repository at: {}", repoDir.getAbsolutePath(), e);
            throw e;
        }
    }

    public boolean checkAppConfigExists(String fileName) {
        File repoDir = new File(applicationConfig.getBasePath() + "/" + fileName);
        return repoDir.exists();
    }

    public void createAppConfig(String fileName) throws IOException, GitAPIException {

        if (checkAppConfigExists(fileName)) {
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


}
