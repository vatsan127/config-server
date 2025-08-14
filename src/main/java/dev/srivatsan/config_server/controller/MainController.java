package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.model.Payload;
import dev.srivatsan.config_server.model.ResponseStatus;
import dev.srivatsan.config_server.service.git.RepositoryService;
import dev.srivatsan.config_server.service.util.UtilService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/config")
public class MainController {

    private final ApplicationConfig applicationConfig;
    private final RepositoryService repositoryService;
    private final UtilService utilService;

    public MainController(ApplicationConfig applicationConfig, RepositoryService repositoryService, UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.repositoryService = repositoryService;
        this.utilService = utilService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createFolder(@RequestBody Payload request) throws IOException {
        utilService.validateActionType(request, ActionType.create);
        String relativeFilePath = utilService.getRelativeFilePath(request);
        String absoluteFilePath = applicationConfig.getBasePath() + relativeFilePath;
        repositoryService.initializeConfigFile(relativeFilePath, absoluteFilePath, request.getAppName());
        return ResponseEntity.status(HttpStatus.CREATED).body("success");
    }

    @PostMapping("/fetch")
    public ResponseEntity<Payload> fetchConfig(@RequestBody Payload payload) throws GitAPIException, IOException {
        utilService.validateActionType(payload, ActionType.fetch);
        String absoluteFilePath = applicationConfig.getBasePath() + utilService.getRelativeFilePath(payload);
        String appConfigContent = utilService.getYmlFileContent(absoluteFilePath);
        payload.setContent(appConfigContent);
        payload.setStatus(ResponseStatus.success);
        return ResponseEntity.status(HttpStatus.OK).body(payload);
    }

}
