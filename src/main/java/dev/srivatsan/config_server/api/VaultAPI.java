package dev.srivatsan.config_server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public interface VaultAPI {

    @PostMapping("/get")
    ResponseEntity<Map<String, Object>> getVault(@RequestBody Map<String, String> request) throws Exception;

    @PostMapping("/update")
    ResponseEntity<Map<String, Object>> updateVault(@RequestBody Map<String, String> request) throws Exception;

    @PostMapping("/history")
    ResponseEntity<Map<String, Object>> getVaultHistory(@RequestBody Map<String, String> request) throws Exception;

    @PostMapping("/changes")
    ResponseEntity<Map<String, Object>> getVaultChanges(@RequestBody Map<String, String> request) throws Exception;
}