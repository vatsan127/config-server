package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.model.IncomingRequest;
import dev.srivatsan.config_server.service.git.GitRepoService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class MainController {

    private final GitRepoService gitRepoService;

    public MainController(GitRepoService gitRepoService) {
        this.gitRepoService = gitRepoService;
    }

    @PostMapping("/create/file")
    public String createFolder(@RequestBody IncomingRequest request) throws GitAPIException, IOException {
        gitRepoService.createAppConfig(request.fileName());
        return "success";
    }

}
