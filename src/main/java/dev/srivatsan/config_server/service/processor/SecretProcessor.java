package dev.srivatsan.config_server.service.processor;

import java.util.Map;
import java.util.Set;

public interface SecretProcessor {
    
    String processConfigurationForClient(String configContent, String namespace);
    
    String processConfigurationForInternal(String configContent);
    
    String processConfigurationForInternal(String configContent, String namespace);
    
    Set<String> detectSecretKeys(String yamlContent);
    
    Map<String, String> extractSecretsFromYaml(String yamlContent);
    
    String replaceSecretsWithPlaceholders(String yamlContent, Map<String, String> secrets);
    
    boolean containsSecrets(String configContent);
}