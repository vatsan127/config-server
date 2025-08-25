package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.api.ConfigurationAPI;
import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.model.Payload;
import dev.srivatsan.config_server.service.repository.RepositoryService;
import dev.srivatsan.config_server.service.util.UtilService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/config")
public class MainController implements ConfigurationAPI {

    private static final String SUCCESS_MESSAGE = "success";
    private static final String NAMESPACE_CREATED_MESSAGE = "Namespace created successfully";

    private final RepositoryService repositoryService;
    private final UtilService utilService;

    public MainController(RepositoryService repositoryService, UtilService utilService) {
        this.repositoryService = repositoryService;
        this.utilService = utilService;
    }

    private String validateAndGetFilePath(Payload payload, ActionType expectedAction) {
        utilService.validateActionType(payload, expectedAction);
        return utilService.getRelativeFilePath(payload);
    }

    @Override
    public ResponseEntity<String> createConfig(@Valid @RequestBody Payload request) {
        log.info("Creating config file for app: {} in namespace: {}", request.getAppName(), request.getNamespace());
        
        String filePath = validateAndGetFilePath(request, ActionType.create);
        repositoryService.initializeConfigFile(filePath, request.getAppName(), request.getEmail());
        
        log.info("Successfully created config file: {}", filePath);
        return ResponseEntity.status(HttpStatus.CREATED).body(SUCCESS_MESSAGE);
    }

    @Override
    public ResponseEntity<Payload> fetchConfig(@Valid @RequestBody Payload payload) throws IOException {
        log.info("Fetching config file for app: {} in namespace: {}", payload.getAppName(), payload.getNamespace());
        
        String filePath = validateAndGetFilePath(payload, ActionType.fetch);
        String content = repositoryService.getConfigFile(filePath);
        
        payload.setContent(content);
        
        log.info("Successfully fetched config file: {}", filePath);
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<String> updateConfig(@Valid @RequestBody Payload payload) {
        log.info("Updating config file for app: {} in namespace: {}", payload.getAppName(), payload.getNamespace());
        
        String filePath = validateAndGetFilePath(payload, ActionType.update);
        repositoryService.updateConfigFile(filePath, payload);
        
        log.info("Successfully updated config file: {} with message: {}", filePath, payload.getMessage());
        return ResponseEntity.ok(SUCCESS_MESSAGE);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCommitHistory(@Valid @RequestBody Payload payload) throws Exception {
        log.info("Getting commit history for app: {} in namespace: {}", payload.getAppName(), payload.getNamespace());
        
        String filePath = validateAndGetFilePath(payload, ActionType.history);
        Map<String, Object> history = repositoryService.getConfigFileHistory(filePath);
        
        log.info("Successfully retrieved commit history for: {}", filePath);
        return ResponseEntity.ok(history);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCommitDetails(@Valid @RequestBody Payload payload) throws IOException {
        log.info("Getting commit details for commit: {} in namespace: {}", payload.getCommitId(), payload.getNamespace());
        
        utilService.validateActionType(payload, ActionType.changes);
        utilService.validateCommitId(payload.getCommitId());
        
        Map<String, Object> commitDetails = repositoryService.getCommitChanges(payload.getCommitId(), payload.getNamespace());
        
        log.info("Successfully retrieved commit details for: {}", payload.getCommitId());
        return ResponseEntity.ok(commitDetails);
    }

    @Override
    public ResponseEntity<String> createNamespace(@RequestBody Map<String, String> request) throws Exception {
        String namespace = request.get("namespace");
        log.info("Creating namespace: {}", namespace);
        
        utilService.validateNamespace(namespace);
        repositoryService.createNamespace(namespace.trim());
        
        log.info("Successfully created namespace: {}", namespace);
        return ResponseEntity.status(HttpStatus.CREATED).body(NAMESPACE_CREATED_MESSAGE);
    }

}
