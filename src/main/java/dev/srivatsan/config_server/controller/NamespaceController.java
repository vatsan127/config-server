package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.api.NamespaceAPI;
import dev.srivatsan.config_server.service.repository.GitRepositoryService;
import dev.srivatsan.config_server.service.util.UtilService;
import dev.srivatsan.config_server.service.validation.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/namespace")
public class NamespaceController implements NamespaceAPI {

    private static final String NAMESPACE_CREATED_MESSAGE = "Namespace has been created successfully and is ready for configuration files";
    private static final String NAMESPACE_DELETED_MESSAGE = "Namespace has been deleted successfully";

    private final GitRepositoryService gitRepositoryService;
    private final UtilService utilService;
    private final ValidationService validationService;

    public NamespaceController(GitRepositoryService gitRepositoryService, UtilService utilService, ValidationService validationService) {
        this.gitRepositoryService = gitRepositoryService;
        this.utilService = utilService;
        this.validationService = validationService;
    }

    @Override
    public ResponseEntity<Map<String, Object>> createNamespace(@RequestBody Map<String, String> request) throws Exception {
        String namespace = request.get("namespace");
        validationService.validateNamespace(namespace);
        gitRepositoryService.createNamespace(namespace.trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", NAMESPACE_CREATED_MESSAGE));
    }

    @Override
    public ResponseEntity<List<String>> listNamespaces() {
        List<String> namespaces = utilService.listNamespaces();
        return ResponseEntity.ok(namespaces);
    }

    @Override
    public ResponseEntity<List<String>> listDirectoryContents(@RequestBody Map<String, String> request) {
        String namespace = request.get("namespace");
        String path = request.get("path");
        validationService.validateNamespace(namespace);
        List<String> fileNames = utilService.listDirectoryContents(namespace, path);
        return ResponseEntity.ok(fileNames);
    }

    @Override
    public ResponseEntity<Map<String, Object>> deleteNamespace(@RequestBody Map<String, String> request) {
        String namespace = request.get("namespace");
        validationService.validateNamespace(namespace);
        gitRepositoryService.deleteNamespace(namespace.trim());
        return ResponseEntity.ok(Map.of("message", NAMESPACE_DELETED_MESSAGE));
    }
}