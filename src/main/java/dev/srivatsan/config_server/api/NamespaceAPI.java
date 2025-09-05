package dev.srivatsan.config_server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

public interface NamespaceAPI {

    String NAMESPACE_CREATED_MESSAGE = "Namespace has been created successfully and is ready for configuration files";
    String NAMESPACE_DELETED_MESSAGE = "Namespace has been deleted successfully";

    @PostMapping("/create")
    ResponseEntity<Map<String, Object>> createNamespace(@RequestBody Map<String, String> request) throws Exception;

    @PostMapping("/list")
    ResponseEntity<List<String>> listNamespaces();

    @PostMapping("/files")
    ResponseEntity<List<String>> listDirectoryContents(@RequestBody Map<String, String> request);

    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteNamespace(@RequestBody Map<String, String> request);
}