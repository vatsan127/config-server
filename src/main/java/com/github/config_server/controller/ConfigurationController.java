package com.github.config_server.controller;

import com.github.config_server.api.ConfigurationAPI;
import com.github.config_server.constants.ActionType;
import com.github.config_server.exception.ValidationException;
import com.github.config_server.model.Payload;
import com.github.config_server.service.repository.GitRepositoryService;
import com.github.config_server.service.util.UtilService;
import com.github.config_server.service.validation.ValidationService;
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
public class ConfigurationController implements ConfigurationAPI {

    private final GitRepositoryService gitRepositoryService;
    private final UtilService utilService;
    private final ValidationService validationService;

    public ConfigurationController(GitRepositoryService gitRepositoryService, UtilService utilService, ValidationService validationService) {
        this.gitRepositoryService = gitRepositoryService;
        this.utilService = utilService;
        this.validationService = validationService;
    }

    private String validateAndGetFilePath(Payload payload, ActionType expectedAction) {
        validationService.validateActionType(payload, expectedAction);
        return utilService.getRelativeFilePath(payload);
    }

    @Override
    public ResponseEntity<Map<String, Object>> createConfig(@Valid @RequestBody Payload payload) {
        String filePath = validateAndGetFilePath(payload, ActionType.create);
        gitRepositoryService.initializeConfigFile(filePath, payload.getAppName(), payload.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", CONFIG_CREATED_MESSAGE));
    }

    @Override
    public ResponseEntity<Payload> fetchConfig(@Valid @RequestBody Payload payload) throws IOException {
        String filePath = validateAndGetFilePath(payload, ActionType.fetch);
        String content = gitRepositoryService.getConfigFile(filePath);
        String latestCommitId = gitRepositoryService.getLatestCommitId(filePath);

        payload.setContent(content);
        payload.setCommitId(latestCommitId);
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateConfig(@Valid @RequestBody Payload payload) {
        String filePath = validateAndGetFilePath(payload, ActionType.update);

        if (payload.getCommitId() == null || payload.getCommitId().trim().isEmpty()) {
            throw ValidationException.missingCommitId("Commit ID is required for update operations");
        }

        String commitId = gitRepositoryService.updateConfigFile(filePath, payload);
        return ResponseEntity.ok(Map.of("message", CONFIG_UPDATED_MESSAGE, "commitId", commitId));
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCommitHistory(@Valid @RequestBody Payload payload) throws Exception {
        String filePath = validateAndGetFilePath(payload, ActionType.history);
        Map<String, Object> history = gitRepositoryService.getConfigFileHistory(filePath);
        return ResponseEntity.ok(history);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getCommitDetails(@Valid @RequestBody Payload payload) throws IOException {
        validationService.validateActionType(payload, ActionType.changes);
        validationService.validateCommitId(payload.getCommitId());
        Map<String, Object> commitDetails = gitRepositoryService.getCommitChanges(payload.getCommitId(), payload.getNamespace());
        return ResponseEntity.ok(commitDetails);
    }

    @Override
    public ResponseEntity<Map<String, Object>> deleteConfig(@Valid @RequestBody Payload payload) {
        String filePath = validateAndGetFilePath(payload, ActionType.delete);
        gitRepositoryService.deleteConfigFile(filePath, payload);
        return ResponseEntity.ok(Map.of("message", CONFIG_DELETED_MESSAGE));
    }
}
