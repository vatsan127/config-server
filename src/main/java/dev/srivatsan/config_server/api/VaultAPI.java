package dev.srivatsan.config_server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

public interface VaultAPI {

    // ToDo: we can move all the api to post method only just like other controllers
    @GetMapping
    ResponseEntity<Map<String, Object>> getVault(@PathVariable String namespace) throws Exception;

    @PutMapping
    ResponseEntity<Map<String, Object>> updateVault(@PathVariable String namespace, @RequestBody Map<String, String> request) throws Exception;

    @GetMapping("/history")
    ResponseEntity<Map<String, Object>> getVaultHistory(@PathVariable String namespace) throws Exception;
}