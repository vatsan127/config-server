package dev.srivatsan.config_server.controller;

import dev.srivatsan.config_server.api.ChangeLogAPI;
import dev.srivatsan.config_server.model.ChangeEntry;
import dev.srivatsan.config_server.service.ChangeLogService;
import dev.srivatsan.config_server.service.util.UtilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/changelog")
public class ChangeLogController implements ChangeLogAPI {

    private final ChangeLogService changeLogService;
    private final UtilService utilService;

    public ChangeLogController(ChangeLogService changeLogService, UtilService utilService) {
        this.changeLogService = changeLogService;
        this.utilService = utilService;
    }

    @Override
    public ResponseEntity<List<ChangeEntry>> getCachedChanges(@RequestBody Map<String, String> request) {
        String namespace = request.get("namespace");
        log.info("Getting cached changes for namespace: {}", namespace);

        utilService.validateNamespace(namespace);
        List<ChangeEntry> changes = changeLogService.getChanges(namespace);

        log.info("Successfully retrieved {} cached changes for namespace: {}", changes.size(), namespace);
        return ResponseEntity.ok(changes);
    }
}