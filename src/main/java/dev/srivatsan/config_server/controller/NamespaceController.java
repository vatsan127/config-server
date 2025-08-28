package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.api.NamespaceAPI;
import dev.srivatsan.config_server.service.repository.RepositoryService;
import dev.srivatsan.config_server.service.util.UtilService;
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

    private static final String NAMESPACE_CREATED_MESSAGE = "Namespace created successfully";

    private final RepositoryService repositoryService;
    private final UtilService utilService;

    public NamespaceController(RepositoryService repositoryService, UtilService utilService) {
        this.repositoryService = repositoryService;
        this.utilService = utilService;
    }

    @Override
    public ResponseEntity<String> createNamespace(@RequestBody Map<String, String> request) throws Exception {
        String namespace = request.get("namespace");
        utilService.validateNamespace(namespace);
        repositoryService.createNamespace(namespace.trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(NAMESPACE_CREATED_MESSAGE);
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
        utilService.validateNamespace(namespace);
        List<String> fileNames = utilService.listDirectoryContents(namespace, path);
        return ResponseEntity.ok(fileNames);
    }
}