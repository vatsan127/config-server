package dev.srivatsan.config_server.api;

import dev.srivatsan.config_server.model.Payload;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.Map;

public interface ConfigurationAPI {

    String CONFIG_CREATED_MESSAGE = "Configuration file has been created successfully";
    String CONFIG_UPDATED_MESSAGE = "Configuration file has been updated successfully";
    String CONFIG_DELETED_MESSAGE = "Configuration file has been deleted successfully";

    @PostMapping("/create")
    ResponseEntity<Map<String, Object>> createConfig(@Valid @RequestBody Payload request);

    @PostMapping("/fetch")
    ResponseEntity<Payload> fetchConfig(@Valid @RequestBody Payload payload) throws IOException;

    @PostMapping("/update")
    ResponseEntity<Map<String, Object>> updateConfig(@Valid @RequestBody Payload payload);

    @PostMapping("/history")
    ResponseEntity<Map<String, Object>> getCommitHistory(@Valid @RequestBody Payload payload) throws Exception;

    @PostMapping("/changes")
    ResponseEntity<Map<String, Object>> getCommitDetails(@Valid @RequestBody Payload payload) throws IOException;

    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteConfig(@Valid @RequestBody Payload payload);

}