package dev.srivatsan.config_server.service.processor;

import dev.srivatsan.config_server.service.vault.GitVaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
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
        this.yaml = new Yaml();
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
    public String processConfigurationForInternal(String configContent) {
        // For backward compatibility - return as-is
        return configContent;
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

    @Override
    public Set<String> detectSecretKeys(String yamlContent) {
        Set<String> secretKeys = new HashSet<>();

        try {
            Map<String, Object> yamlData = yaml.load(yamlContent);
            if (yamlData == null) {
                return secretKeys;
            }

            // Flatten YAML to dot notation
            Map<String, Object> flattenedData = new LinkedHashMap<>();
            flattenProperties("", yamlData, flattenedData);

            // Check each key against secret patterns
            for (String key : flattenedData.keySet()) {
                if (isSecretKey(key)) {
                    secretKeys.add(key);
                }
            }

        } catch (Exception e) {
            log.error("Failed to detect secret keys in YAML content: {}", e.getMessage());
        }

        return secretKeys;
    }

    @Override
    public Map<String, String> extractSecretsFromYaml(String yamlContent) {
        Map<String, String> secrets = new HashMap<>();

        try {
            Map<String, Object> yamlData = yaml.load(yamlContent);
            if (yamlData == null) {
                return secrets;
            }

            // Flatten YAML to dot notation
            Map<String, Object> flattenedData = new LinkedHashMap<>();
            flattenProperties("", yamlData, flattenedData);

            // Extract values for secret keys
            for (Map.Entry<String, Object> entry : flattenedData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (isSecretKey(key) && value != null) {
                    secrets.put(key, value.toString());
                }
            }

        } catch (Exception e) {
            log.error("Failed to extract secrets from YAML content: {}", e.getMessage());
        }

        return secrets;
    }

    @Override
    public String replaceSecretsWithPlaceholders(String yamlContent, Map<String, String> secrets) {
        if (secrets.isEmpty()) {
            return yamlContent;
        }

        try {
            Map<String, Object> yamlData = yaml.load(yamlContent);
            if (yamlData == null) {
                return yamlContent;
            }

            // Replace secret values with placeholders
            Map<String, Object> processedData = new LinkedHashMap<>(yamlData);
            for (String secretKey : secrets.keySet()) {
                setNestedValue(processedData, secretKey, SECRET_PLACEHOLDER);
            }

            return yaml.dump(processedData);

        } catch (Exception e) {
            log.error("Failed to replace secrets with placeholders: {}", e.getMessage());
            return yamlContent;
        }
    }

    @Override
    public boolean containsSecrets(String configContent) {
        return configContent != null && configContent.contains(SECRET_PLACEHOLDER);
    }

    private void replaceSecretsInMap(String prefix, Map<String, Object> map, String namespace) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                replaceSecretsInMap(key, nestedMap, namespace);
            } else if (SECRET_PLACEHOLDER.equals(value)) {
                try {
                    // Get actual secret value from vault
                    Map<String, String> vaultSecrets = gitVaultService.getVault(namespace);
                    String secretValue = vaultSecrets.get(key);
                    if (secretValue != null) {
                        entry.setValue(secretValue);
                        log.debug("Replaced secret placeholder for key: {}", key);
                    } else {
                        log.warn("Secret not found in vault for key: {}", key);
                    }
                } catch (Exception e) {
                    log.warn("Failed to resolve secret for key '{}' in namespace '{}': {}", key, namespace, e.getMessage());
                    // Leave placeholder if secret cannot be resolved
                }
            }
        }
    }

    private void flattenProperties(String prefix, Map<String, Object> source, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenProperties(key, nestedMap, result);
            } else {
                result.put(key, value);
            }
        }
    }

    private boolean isSecretKey(String key) {
        return SECRET_KEY_PATTERN.matcher(key).matches();
    }

    private void setNestedValue(Map<String, Object> map, String key, Object value) {
        String[] keys = key.split("\\.");
        Map<String, Object> currentMap = map;

        // Navigate to the parent map
        for (int i = 0; i < keys.length - 1; i++) {
            String currentKey = keys[i];
            Object currentValue = currentMap.get(currentKey);

            if (currentValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) currentValue;
                currentMap = nestedMap;
            } else {
                // Create nested map if it doesn't exist
                Map<String, Object> nestedMap = new LinkedHashMap<>();
                currentMap.put(currentKey, nestedMap);
                currentMap = nestedMap;
            }
        }

        // Set the value
        currentMap.put(keys[keys.length - 1], value);
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