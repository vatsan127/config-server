package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.model.IncomingRequest;
import dev.srivatsan.config_server.model.YamlConfigResponse;
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
    public ResponseEntity<String> createFolder(@RequestBody IncomingRequest request) throws IOException {
        validateIncomingRequest(request, "create");
        String relativeFilePath = utilService.getRelativeFilePath(request);
        String absoluteFilePath = applicationConfig.getBasePath() + relativeFilePath;
        repositoryService.createAppConfig(relativeFilePath, absoluteFilePath, request.getAppName());
        return ResponseEntity.status(HttpStatus.CREATED).body("success");
    }

    @PostMapping("/fetch")
    public ResponseEntity<YamlConfigResponse> fetchConfig(@RequestBody IncomingRequest request) throws GitAPIException, IOException {
        validateIncomingRequest(request, "fetch");
        String absoluteFilePath = applicationConfig.getBasePath() + utilService.getRelativeFilePath(request);
        String appConfigContent = utilService.getYmlFileContent(absoluteFilePath);
        YamlConfigResponse response = new YamlConfigResponse(request.getAppName(), request.getNamespace(), request.getPath(), appConfigContent);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    public void validateIncomingRequest(IncomingRequest request, String requiredActionType) {
        if (request.getAction() == null || requiredActionType.equals(request.getAction())) {
            throw new IllegalArgumentException("Action ('action') must be provided and must match the operation.");
        }

        if (request.getAppName() == null || request.getAppName().trim().isEmpty()) {
            throw new IllegalArgumentException("Application name ('appName') must be provided.");
        }

        if (request.getNamespace() == null || request.getNamespace().trim().isEmpty()) {
            throw new IllegalArgumentException("Namespace ('namespace') must be provided.");
        }

        String path = request.getPath();
        if (path == null || path.trim().isEmpty() || !path.startsWith("/")) {
            throw new IllegalArgumentException("Path ('path') must be provided and must start with a '/'.");
        }
    }

}
