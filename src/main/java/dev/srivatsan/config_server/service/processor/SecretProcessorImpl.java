package dev.srivatsan.config_server.service.processor;

import dev.srivatsan.config_server.service.vault.GitVaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SecretProcessorImpl implements SecretProcessor {

    private static final Logger log = LoggerFactory.getLogger(SecretProcessorImpl.class);
    private static final String SECRET_PLACEHOLDER = "#ENCODED";
    
    // Patterns for secret detection (keys containing password, secret, key, token, etc.)
    private static final Pattern SECRET_KEY_PATTERN = Pattern.compile(
        "(?i).*(password|secret|key|token|credential|auth|api[_-]?key|private[_-]?key|access[_-]?key|secret[_-]?key).*"
    );

    private final GitVaultService gitVaultService;
    private final Yaml yaml;

    public SecretProcessorImpl(GitVaultService gitVaultService) {
        this.gitVaultService = gitVaultService;
        
        // Configure YAML dumper for proper block-style formatting
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setCanonical(false);
        
        this.yaml = new Yaml(options);
    }

    @Override
    public String processConfigurationForClient(String configContent, String namespace) {
        try {
            // Get vault secrets for this namespace
            Map<String, String> vaultSecrets = gitVaultService.getVault(namespace);
            if (vaultSecrets.isEmpty()) {
                return configContent; // No vault secrets, return as-is
            }
            
            // Parse YAML content
            Map<String, Object> yamlData = yaml.load(configContent);
            if (yamlData == null) {
                return configContent;
            }
            
            // Process the YAML to replace vault keys or <ENCRYPTED_VALUE> with actual decrypted values
            Map<String, Object> processedData = new LinkedHashMap<>(yamlData);
            replaceWithVaultSecrets(processedData, vaultSecrets, "");
            
            // Convert back to YAML
            return yaml.dump(processedData);
            
        } catch (Exception e) {
            log.error("Failed to process configuration for client in namespace '{}': {}", namespace, e.getMessage());
            // Return original content if processing fails to avoid breaking client
            return configContent;
        }
    }
    
    @Override
    public String processConfigurationForInternal(String configContent, String namespace) {
        // For management UI - replace vault keys with <ENCRYPTED_VALUE>
        try {
            // Get vault secrets for this namespace
            Map<String, String> vaultSecrets = gitVaultService.getVault(namespace);
            if (vaultSecrets.isEmpty()) {
                return configContent; // No vault secrets, return as-is
            }
            
            // Parse YAML content
            Map<String, Object> yamlData = yaml.load(configContent);
            if (yamlData == null) {
                return configContent;
            }
            
            // Process the YAML to replace vault keys with <ENCRYPTED_VALUE>
            Map<String, Object> processedData = new LinkedHashMap<>(yamlData);
            replaceVaultKeysWithPlaceholder(processedData, vaultSecrets, "");
            
            // Convert back to YAML
            return yaml.dump(processedData);
            
        } catch (Exception e) {
            log.error("Failed to process configuration for internal use in namespace '{}': {}", namespace, e.getMessage());
            // Return original content if processing fails
            return configContent;
        }
    }
    
    /**
     * Replaces vault keys in YAML with <ENCRYPTED_VALUE> placeholder for management UI
     */
    private void replaceVaultKeysWithPlaceholder(Map<String, Object> yamlData, Map<String, String> vaultSecrets, String prefix) {
        for (Map.Entry<String, Object> entry : yamlData.entrySet()) {
            String currentKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                replaceVaultKeysWithPlaceholder(nestedMap, vaultSecrets, currentKey);
            } else {
                // Check if this key exists in vault (try both full path and just the key name)
                if (vaultSecrets.containsKey(currentKey) || vaultSecrets.containsKey(entry.getKey())) {
                    entry.setValue("<ENCRYPTED_VALUE>");
                    log.debug("Replaced vault key '{}' with <ENCRYPTED_VALUE> for management UI", currentKey);
                }
            }
        }
    }
    
    /**
     * Replaces vault keys or <ENCRYPTED_VALUE> placeholders with actual decrypted values for client apps
     */
    private void replaceWithVaultSecrets(Map<String, Object> yamlData, Map<String, String> vaultSecrets, String prefix) {
        for (Map.Entry<String, Object> entry : yamlData.entrySet()) {
            String currentKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                replaceWithVaultSecrets(nestedMap, vaultSecrets, currentKey);
            } else {
                // Check if this key exists in vault (try both full path and just the key name)
                String secretValue = vaultSecrets.get(currentKey);
                if (secretValue == null) {
                    secretValue = vaultSecrets.get(entry.getKey());
                }
                
                if (secretValue != null) {
                    entry.setValue(secretValue);
                    log.debug("Replaced vault key '{}' with actual secret for client", currentKey);
                } else if ("<ENCRYPTED_VALUE>".equals(value)) {
                    // If we find <ENCRYPTED_VALUE> placeholder but no corresponding vault secret, log warning
                    log.warn("Found <ENCRYPTED_VALUE> placeholder for key '{}' but no corresponding vault secret", currentKey);
                }
            }
        }
    }
}