package dev.srivatsan.config_server.api;

import dev.srivatsan.config_server.model.Payload;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.Map;

/**
 * Configuration Management API Interface
 * 
 * <p>Provides REST API endpoints for managing application configuration files with complete Git version control
 * and namespace isolation. This interface defines the contract for CRUD operations on YAML configuration files
 * stored in Git repositories.</p>
 * 
 * <h2>Core Features</h2>
 * <ul>
 *   <li><strong>Namespace Isolation</strong> - Each namespace contains isolated configuration files</li>
 *   <li><strong>Git Version Control</strong> - Full version history with commit messages and author tracking</li>
 *   <li><strong>YAML Configuration</strong> - Standardized YAML format for application configurations</li>
 *   <li><strong>Template Generation</strong> - Automatic default configuration template creation</li>
 *   <li><strong>Secret Integration</strong> - Seamless integration with vault secrets using placeholders</li>
 *   <li><strong>Conflict Resolution</strong> - Commit ID-based conflict detection and resolution</li>
 *   <li><strong>Audit Trail</strong> - Complete history tracking for compliance requirements</li>
 * </ul>
 * 
 * <h2>File Structure</h2>
 * <pre>
 * /config/
 * ├── production/
 * │   ├── config/
 * │   │   ├── user-service.yml
 * │   │   └── payment-service.yml
 * │   ├── .vault-secrets.json (encrypted)
 * │   └── .vault-keys/ (encryption keys)
 * └── staging/
 *     ├── config/
 *     │   ├── user-service.yml
 *     │   └── payment-service.yml
 *     ├── .vault-secrets.json (encrypted)
 *     └── .vault-keys/ (encryption keys)
 * </pre>
 * 
 * <h2>Secret Integration</h2>
 * <p>Configuration files can reference vault secrets using placeholder syntax:</p>
 * <pre>
 * database:
 *   password: ${vault:db_password}
 *   host: ${vault:db_host}
 * </pre>
 * 
 * <h2>Conflict Resolution</h2>
 * <p>All update operations use commit ID-based optimistic locking to prevent conflicts:</p>
 * <ul>
 *   <li>Client must provide the current commit ID</li>
 *   <li>Server validates the commit ID before applying changes</li>
 *   <li>Returns conflict error if commit ID doesn't match</li>
 * </ul>
 * 
 * @author Config Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see dev.srivatsan.config_server.controller.ConfigurationController
 * @see dev.srivatsan.config_server.service.repository.GitRepositoryService
 */
public interface ConfigurationAPI {

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