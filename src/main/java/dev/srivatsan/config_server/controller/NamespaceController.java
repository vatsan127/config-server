package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.api.NamespaceAPI;
import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.service.notify.ClientNotifyService;
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

    private final GitRepositoryService gitRepositoryService;
    private final UtilService utilService;
    private final ValidationService validationService;
    private final ClientNotifyService clientNotifyService;

    public NamespaceController(GitRepositoryService gitRepositoryService, UtilService utilService, 
                             ValidationService validationService, ClientNotifyService clientNotifyService) {
        this.gitRepositoryService = gitRepositoryService;
        this.utilService = utilService;
        this.validationService = validationService;
        this.clientNotifyService = clientNotifyService;
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

    @Override
    public ResponseEntity<Map<String, Object>> getNamespaceEvents(@RequestBody Map<String, String> request) throws Exception {
        String namespace = request.get("namespace");
        validationService.validateNamespace(namespace);
        
        Map<String, Object> events = gitRepositoryService.getNamespaceEvents(namespace.trim());
        return ResponseEntity.ok(events);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getNamespaceNotifications(@RequestBody Map<String, String> request) throws Exception {
        String namespace = request.get("namespace");
        validationService.validateNamespace(namespace);
        
        Map<String, Object> notifications = gitRepositoryService.getNamespaceNotifications(namespace.trim());
        return ResponseEntity.ok(notifications);
    }

    @Override
    public ResponseEntity<Map<String, Object>> triggerNotifications(@RequestBody Map<String, String> request) throws Exception {
        String namespace = request.get("namespace");
        String commitId = request.get("commitid"); // Note: using "commitid" as per your specification
        
        // Validate required parameters
        validationService.validateNamespace(namespace);
        validationService.validateCommitId(commitId);
        
        try {
            // Reinitialize notification from scratch - this will create a fresh notification
            // with new timestamp, reset status, and all tracking details
            clientNotifyService.sendRefreshNotifications(namespace.trim(), null, commitId.trim());
            
            log.info("Triggered notification reinitialization for namespace: '{}', commitId: '{}'", 
                    namespace, commitId);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Failed to reinitialize notifications for namespace: '{}', commitId: '{}': {}", 
                     namespace, commitId, e.getMessage(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Failed to reinitialize notifications",
                "error", e.getMessage(),
                "namespace", namespace.trim(),
                "commitId", commitId != null ? commitId.trim() : null,
                "status", "failed"
            ));
        }
    }

}