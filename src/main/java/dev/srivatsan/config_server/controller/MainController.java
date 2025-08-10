package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.model.IncomingRequest;
import dev.srivatsan.config_server.service.git.RepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
public class MainController {

    private final RepositoryService repositoryService;

    public MainController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping("/create/file")
    public String createFolder(@RequestBody IncomingRequest request) throws GitAPIException, IOException {
        repositoryService.createAppConfig(request.getFileName());
        return "success";
    }

    @PostMapping("/get")
    public String getConfig(@RequestBody IncomingRequest request) throws GitAPIException, IOException {
        log.info("Request: {}", request);
        String appConfigContent = repositoryService.getAppConfigContent(request.getPath(), request.getFileName());
        return appConfigContent;
    }

}
