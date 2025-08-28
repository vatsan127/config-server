package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.api.ConfigurationAPI;
import dev.srivatsan.config_server.exception.ValidationException;
import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.model.Payload;
import dev.srivatsan.config_server.service.repository.RepositoryService;
import dev.srivatsan.config_server.service.util.UtilService;
import dev.srivatsan.config_server.service.validation.ValidationService;
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
public class ConfigurationController implements ConfigurationAPI {

    private static final String SUCCESS_MESSAGE = "success";

    private final RepositoryService repositoryService;
    private final UtilService utilService;
    private final ValidationService validationService;

    public ConfigurationController(RepositoryService repositoryService, UtilService utilService, ValidationService validationService) {
        this.repositoryService = repositoryService;
        this.utilService = utilService;
        this.validationService = validationService;
    }

    private String validateAndGetFilePath(Payload payload, ActionType expectedAction) {
        validationService.validateActionType(payload, expectedAction);
        return utilService.getRelativeFilePath(payload);
    }

    @Override
    public ResponseEntity<String> createConfig(@Valid @RequestBody Payload payload) {
        String filePath = validateAndGetFilePath(payload, ActionType.create);
        repositoryService.initializeConfigFile(filePath, payload.getAppName(), payload.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(SUCCESS_MESSAGE);
    }

    @Override
    public ResponseEntity<Payload> fetchConfig(@Valid @RequestBody Payload payload) throws IOException {
        String filePath = validateAndGetFilePath(payload, ActionType.fetch);
        String content = repositoryService.getConfigFile(filePath);
        String latestCommitId = repositoryService.getLatestCommitId(filePath);
        
        payload.setContent(content);
        payload.setCommitId(latestCommitId);
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<String> updateConfig(@Valid @RequestBody Payload payload) {
        String filePath = validateAndGetFilePath(payload, ActionType.update);
        
        if (payload.getCommitId() == null || payload.getCommitId().trim().isEmpty()) {
            throw ValidationException.missingCommitId("Commit ID is required for update operations");
        }
        
        repositoryService.updateConfigFile(filePath, payload);
        return ResponseEntity.ok(SUCCESS_MESSAGE);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCommitHistory(@Valid @RequestBody Payload payload) throws Exception {
        String filePath = validateAndGetFilePath(payload, ActionType.history);
        Map<String, Object> history = repositoryService.getConfigFileHistory(filePath);
        return ResponseEntity.ok(history);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCommitDetails(@Valid @RequestBody Payload payload) throws IOException {
        validationService.validateActionType(payload, ActionType.changes);
        validationService.validateCommitId(payload.getCommitId());
        Map<String, Object> commitDetails = repositoryService.getCommitChanges(payload.getCommitId(), payload.getNamespace());
        return ResponseEntity.ok(commitDetails);
    }
}
