package dev.srivatsan.config_server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * Namespace Management API Interface
 * 
 * <p>Provides REST API endpoints for managing configuration namespaces and directory operations.
 * Namespaces provide complete isolation between different environments, applications, or teams.</p>
 * 
 * <h2>Core Features</h2>
 * <ul>
 *   <li><strong>Namespace Isolation</strong> - Complete isolation between different environments</li>
 *   <li><strong>Git Repository Management</strong> - Each namespace is a separate Git repository</li>
 *   <li><strong>Directory Operations</strong> - Browse and manage directory structures within namespaces</li>
 *   <li><strong>Secure Deletion</strong> - Safe namespace deletion with all associated data</li>
 *   <li><strong>Automatic Initialization</strong> - Automated setup of Git repositories and encryption keys</li>
 * </ul>
 * 
 * <h2>Namespace Lifecycle</h2>
 * <ol>
 *   <li><strong>Creation</strong> - Initialize Git repository and encryption keys</li>
 *   <li><strong>Usage</strong> - Store configuration files and vault secrets</li>
 *   <li><strong>Management</strong> - Browse directories and manage content</li>
 *   <li><strong>Deletion</strong> - Clean removal of all namespace data</li>
 * </ol>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>Each namespace has its own encryption key for vault secrets</li>
 *   <li>Directory permissions are set securely during namespace creation</li>
 *   <li>Deletion operations are irreversible - use with caution</li>
 *   <li>File browsing is restricted to .yml files and directories</li>
 * </ul>
 * 
 * @author Config Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see dev.srivatsan.config_server.controller.NamespaceController
 * @see dev.srivatsan.config_server.service.repository.GitRepositoryService
 */
public interface NamespaceAPI {

    @PostMapping("/create")
    ResponseEntity<Map<String, Object>> createNamespace(@RequestBody Map<String, String> request) throws Exception;

    @PostMapping("/list")
    ResponseEntity<List<String>> listNamespaces();

    @PostMapping("/files")
    ResponseEntity<List<String>> listDirectoryContents(@RequestBody Map<String, String> request);

    @PostMapping("/delete")
    ResponseEntity<Map<String, Object>> deleteNamespace(@RequestBody Map<String, String> request);
}