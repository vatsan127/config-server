package dev.srivatsan.config_server.service.repository;

import dev.srivatsan.config_server.service.util.UtilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NamespaceAwareEnvironmentRepository implements EnvironmentRepository, Ordered {

    private final Logger log = LoggerFactory.getLogger(NamespaceAwareEnvironmentRepository.class);
    
    private final GitRepositoryService gitRepositoryService;
    private final UtilService utilService;
    private final Yaml yaml = new Yaml();

    public NamespaceAwareEnvironmentRepository(GitRepositoryService gitRepositoryService, UtilService utilService) {
        this.gitRepositoryService = gitRepositoryService;
        this.utilService = utilService;
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        log.info("Finding configuration for application: {}, profile: {}, label: {}", application, profile, label);
        
        Environment environment = new Environment(application, profile);
        
        try {
            // Extract namespace and path from label: "production/config" or "test" or "dev/api"
            String namespace = extractNamespaceFromLabel(label);
            String path = extractPathFromLabel(label);
            String filePath = constructFilePathFromLabel(namespace, path, application);
            
            log.debug("Resolved namespace: {}, path: {}, application: {}, filePath: {}", namespace, path, application, filePath);
            
            // Get configuration content from Git
            String configContent = gitRepositoryService.getConfigFile(filePath);
            
            // Parse YAML content
            Map<String, Object> properties = parseYamlContent(configContent);
            
            // Create property source
            PropertySource propertySource = new PropertySource(filePath, properties);
            List<PropertySource> propertySources = new ArrayList<>();
            propertySources.add(propertySource);
            
            environment.add(propertySource);
            
            // Set version (latest commit ID)
            String version = gitRepositoryService.getLatestCommitId(filePath);
            environment.setVersion(version);
            
            log.info("Successfully loaded configuration for {}, version: {}", application, version);
            return environment;
            
        } catch (Exception e) {
            log.error("Failed to load configuration for application: {}, profile: {}, label: {}, error: {}", application, profile, label, e.getMessage());
            // Return empty environment instead of failing
            return environment;
        }
    }

    /**
     * Extract namespace from label.
     * Examples:
     * - "production/config" -> "production"
     * - "production" -> "production"  
     * - "test/api" -> "test"
     * - null/empty -> "default"
     */
    private String extractNamespaceFromLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "default";
        }
        
        if (label.contains("/")) {
            return label.split("/")[0];
        }
        return label;
    }

    /**
     * Extract path from label.
     * Examples:
     * - "production/config" -> "config"
     * - "test/api/v1" -> "api/v1"
     * - "production" -> "" (root path)
     * - null/empty -> "" (root path)
     */
    private String extractPathFromLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "";
        }
        
        if (label.contains("/")) {
            int firstSlash = label.indexOf('/');
            return label.substring(firstSlash + 1);
        }
        return ""; // root path
    }

    /**
     * Construct the complete file path using label-based namespace/path and application name.
     * Examples:
     * - namespace="production", path="config", application="user-service" -> "production/config/user-service.yml"
     * - namespace="test", path="", application="api-service" -> "test/api-service.yml"
     * - namespace="dev", path="api/v1", application="gateway" -> "dev/api/v1/gateway.yml"
     */
    private String constructFilePathFromLabel(String namespace, String path, String application) {
        StringBuilder filePath = new StringBuilder();
        
        // Add namespace
        filePath.append(namespace);
        
        // Add path if present
        if (!path.isEmpty()) {
            filePath.append("/").append(path);
        }
        
        // Add application and extension
        filePath.append("/").append(application).append(".yml");
        
        return filePath.toString();
    }

    /**
     * Parse YAML content into a map of properties.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYamlContent(String content) {
        try {
            Map<String, Object> yamlData = yaml.load(content);
            return yamlData != null ? flattenProperties("", yamlData, new LinkedHashMap<>()) : new LinkedHashMap<>();
        } catch (Exception e) {
            log.warn("Failed to parse YAML content, returning empty properties: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * Flatten nested YAML properties into dot notation.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> flattenProperties(String prefix, Map<String, Object> source, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                flattenProperties(key, (Map<String, Object>) value, result);
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // High priority for our custom repository
    }
}