package com.github.config_server.service.secret;


import com.github.config_server.service.vault.VaultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SecretProcessorImpl implements SecretProcessor {

    private static final Logger log = LoggerFactory.getLogger(SecretProcessorImpl.class);

    private final VaultService vaultService;
    private final Yaml yaml;

    public SecretProcessorImpl(VaultService vaultService) {
        this.vaultService = vaultService;

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
            Map<String, String> vaultSecrets = vaultService.getVault(namespace);

            if (vaultSecrets.isEmpty()) {
                return configContent;
            }

            Map<String, Object> yamlData = yaml.load(configContent);
            if (yamlData == null) {
                return configContent;
            }

            Map<String, Object> processedData = new LinkedHashMap<>(yamlData);
            replaceWithVaultSecrets(processedData, vaultSecrets, "");
            return yaml.dump(processedData);
        } catch (Exception e) {
            log.error("Failed to process configuration for client in namespace '{}': {}", namespace, e.getMessage());
            return configContent;
        }
    }

    @Override
    public String processConfigurationForInternal(String configContent, String namespace) {
        try {
            Map<String, String> vaultSecrets = vaultService.getVault(namespace);

            if (vaultSecrets.isEmpty()) { // No vault secrets, return as-is
                return configContent;
            }

            Map<String, Object> yamlData = yaml.load(configContent);
            if (yamlData == null) {
                return configContent;
            }

            Map<String, Object> processedData = new LinkedHashMap<>(yamlData);
            replaceVaultKeysWithPlaceholder(processedData, vaultSecrets, "");
            return yaml.dump(processedData);
        } catch (Exception e) {
            log.error("Failed to process configuration for internal use in namespace '{}': {}", namespace, e.getMessage());
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
            } else if (vaultSecrets.containsKey(currentKey)) {
                entry.setValue("<ENCRYPTED_VALUE>");
                log.info("Replaced vault key '{}' with <ENCRYPTED_VALUE> for management UI", currentKey);
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
                String secretValue = vaultSecrets.get(currentKey);
                if (secretValue != null) {
                    entry.setValue(secretValue);
                    log.debug("Replaced vault key '{}' with actual secret for client", currentKey);
                } else if ("<ENCRYPTED_VALUE>".equals(value)) {
                    log.warn("Found <ENCRYPTED_VALUE> placeholder for key '{}' but no corresponding vault secret", currentKey);
                }
            }
        }
    }
}