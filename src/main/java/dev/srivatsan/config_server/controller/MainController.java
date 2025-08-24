package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.model.Payload;
import dev.srivatsan.config_server.model.ResponseStatus;
import dev.srivatsan.config_server.service.repository.GitBasedConfigService;
import dev.srivatsan.config_server.service.repository.RepositoryService;
import dev.srivatsan.config_server.service.util.UtilService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/config")
public class MainController {

    private final RepositoryService repositoryService;
    private final UtilService utilService;

    public MainController(GitBasedConfigService repositoryService, UtilService utilService) {
        this.repositoryService = repositoryService;
        this.utilService = utilService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createConfig(@Valid @RequestBody Payload request) {
        utilService.validateActionType(request, ActionType.create);
        String relativeFilePath = utilService.getRelativeFilePath(request);
        repositoryService.initializeConfigFile(relativeFilePath, request.getAppName(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body("success");
    }

    @PostMapping("/fetch")
    public ResponseEntity<Payload> fetchConfig(@Valid @RequestBody Payload payload) throws IOException {
        utilService.validateActionType(payload, ActionType.fetch);
        String relativeFilePath = utilService.getRelativeFilePath(payload);
        String appConfigContent = repositoryService.getConfigFile(relativeFilePath);
        payload.setContent(appConfigContent);
        payload.setStatus(ResponseStatus.success);
        return ResponseEntity.status(HttpStatus.OK).body(payload);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateConfig(@Valid @RequestBody Payload payload) {
        utilService.validateActionType(payload, ActionType.update);
        String relativeFilePath = utilService.getRelativeFilePath(payload);
        repositoryService.updateConfigFile(relativeFilePath, payload);
        return ResponseEntity.status(HttpStatus.OK).body("success");
    }

    @PostMapping("/history")
    public ResponseEntity<Map<String, Object>> getCommitHistory(@Valid @RequestBody Payload payload) throws Exception {
        utilService.validateActionType(payload, ActionType.history);
        String relativeFilePath = utilService.getRelativeFilePath(payload);
        Map<String, Object> history = repositoryService.getConfigFileHistory(relativeFilePath);
        return ResponseEntity.status(HttpStatus.OK).body(history);
    }

    @PostMapping("/changes")
    public ResponseEntity<Map<String, Object>> getCommitDetails(@Valid @RequestBody Payload payload) throws IOException {
        utilService.validateActionType(payload, ActionType.changes);
        
        if (payload.getCommitId() == null || payload.getCommitId().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Commit ID is required"));
        }
        
        Map<String, Object> commitDetails = repositoryService.getCommitChanges(payload.getCommitId(), payload.getNamespace());
        return ResponseEntity.status(HttpStatus.OK).body(commitDetails);
    }

    @PostMapping("/namespace/create")
    public ResponseEntity<String> createNamespace(@RequestBody Map<String, String> request) {
        try {
            String namespace = request.get("namespace");
            if (namespace == null || namespace.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Namespace is required");
            }
            
            if (!namespace.matches("^[a-zA-Z0-9-_]+$")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid namespace format");
            }
            
            repositoryService.createNamespace(namespace.trim());
            return ResponseEntity.status(HttpStatus.CREATED).body("Namespace created successfully");
            
        } catch (Exception e) {
            log.error("Error creating namespace: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create namespace: " + e.getMessage());
        }
    }

}
