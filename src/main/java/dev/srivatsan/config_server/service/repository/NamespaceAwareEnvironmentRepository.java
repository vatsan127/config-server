package dev.srivatsan.config_server.service.repository;

import dev.srivatsan.config_server.exception.ConfigFileException;
import dev.srivatsan.config_server.exception.GitOperationException;
import dev.srivatsan.config_server.exception.ValidationException;
import dev.srivatsan.config_server.service.util.UtilService;
import dev.srivatsan.config_server.service.validation.ValidationService;
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
    private final ValidationService validationService;
    private final Yaml yaml;

    public NamespaceAwareEnvironmentRepository(GitRepositoryService gitRepositoryService, 
                                               UtilService utilService, 
                                               ValidationService validationService) {
        this.gitRepositoryService = gitRepositoryService;
        this.utilService = utilService;
        this.validationService = validationService;
        this.yaml = new Yaml();
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        log.info("Finding configuration for application: {}, profile: {}, label: {}", application, profile, label);
        
        // Input validation
        validateInputs(application, profile, label);
        
        Environment environment = new Environment(application, profile);
        
        try {
            // Extract namespace and path from label: "production/config" or "test" or "dev/api"
            String namespace = extractNamespaceFromLabel(label);
            String path = extractPathFromLabel(label);
            
            // Validate namespace
            validationService.validateNamespace(namespace);
            
            // Try to load profile-specific configuration first (e.g., application-dev.yml)
            List<PropertySource> propertySources = loadConfigurationFiles(namespace, path, application, profile);
            
            for (PropertySource propertySource : propertySources) {
                environment.add(propertySource);
            }
            
            // Set version (latest commit ID for the main application file)
            String mainFilePath = constructFilePathFromLabel(namespace, path, application, null);
            String version = gitRepositoryService.getLatestCommitId(mainFilePath);
            environment.setVersion(version);
            
            log.info("Successfully loaded {} configuration files for {}, version: {}", propertySources.size(), application, version);
            return environment;
            
        } catch (ConfigFileException | GitOperationException | ValidationException e) {
            log.error("Failed to load configuration for application: {}, profile: {}, label: {}", application, profile, label, e);
            throw e; // Re-throw specific exceptions
        } catch (Exception e) {
            log.error("Unexpected error loading configuration for application: {}, profile: {}, label: {}", application, profile, label, e);
            throw new ConfigFileException("CONFIG_LOAD_FAILED", 
                "Failed to load configuration for application: " + application, e);
        }
    }

    /**
     * Validates input parameters to prevent security issues and provide clear error messages.
     */
    private void validateInputs(String application, String profile, String label) {
        if (application == null || application.trim().isEmpty()) {
            throw ValidationException.invalidAppName(application, "Application name cannot be null or empty");
        }
        
        if (application.contains("../") || application.contains("..\\")) {
            throw ValidationException.invalidAppName(application, "Application name contains invalid path characters");
        }
        
        // Profile can be null (will default to "default")
        if (profile != null && (profile.contains("../") || profile.contains("..\\"))) {
            throw ValidationException.invalidPath(profile, "Profile contains invalid path characters");
        }
        
        // Label can be null (will default to "default")
        if (label != null && (label.contains("../") || label.contains("..\\"))) {
            throw ValidationException.invalidPath(label, "Label contains invalid path characters");
        }
    }
    
    /**
     * Loads configuration files including profile-specific configurations.
     */
    private List<PropertySource> loadConfigurationFiles(String namespace, String path, String application, String profile) throws Exception {
        List<PropertySource> propertySources = new ArrayList<>();
        
        // Load main application configuration (application.yml)
        String mainFilePath = constructFilePathFromLabel(namespace, path, application, null);
        try {
            String mainContent = gitRepositoryService.getConfigFile(mainFilePath, true);
            Map<String, Object> mainProperties = parseYamlContent(mainContent, mainFilePath);
            propertySources.add(new PropertySource(mainFilePath, mainProperties));
        } catch (Exception e) {
            log.debug("Main configuration file not found: {}", mainFilePath);
        }
        
        // Load profile-specific configuration if profile is specified (application-{profile}.yml)
        if (profile != null && !profile.trim().isEmpty() && !"default".equals(profile)) {
            String profileFilePath = constructFilePathFromLabel(namespace, path, application, profile);
            try {
                String profileContent = gitRepositoryService.getConfigFile(profileFilePath, true);
                Map<String, Object> profileProperties = parseYamlContent(profileContent, profileFilePath);
                propertySources.add(new PropertySource(profileFilePath, profileProperties));
            } catch (Exception e) {
                log.debug("Profile-specific configuration file not found: {}", profileFilePath);
            }
        }
        
        if (propertySources.isEmpty()) {
            throw ConfigFileException.notFound(mainFilePath);
        }
        
        return propertySources;
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
     * - namespace="production", path="config", application="user-service", profile=null -> "production/config/user-service.yml"
     * - namespace="production", path="config", application="user-service", profile="dev" -> "production/config/user-service-dev.yml"
     * - namespace="test", path="", application="api-service", profile=null -> "test/api-service.yml"
     * - namespace="dev", path="api/v1", application="gateway", profile="staging" -> "dev/api/v1/gateway-staging.yml"
     */
    private String constructFilePathFromLabel(String namespace, String path, String application, String profile) {
        StringBuilder filePath = new StringBuilder();
        
        // Add namespace
        filePath.append(namespace);
        
        // Add path if present
        if (!path.isEmpty()) {
            filePath.append("/").append(path);
        }
        
        // Add application name
        filePath.append("/").append(application);
        
        // Add profile suffix if specified
        if (profile != null && !profile.trim().isEmpty() && !"default".equals(profile)) {
            filePath.append("-").append(profile);
        }
        
        // Add extension
        filePath.append(".yml");
        
        return filePath.toString();
    }

    /**
     * Parse YAML content into a map of properties.
     * Assumes content is already validated during config updates.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYamlContent(String content, String filePath) {
        if (content == null || content.trim().isEmpty()) {
            log.debug("Empty content for file: {}, returning empty properties", filePath);
            return new LinkedHashMap<>();
        }
        
        try {
            Map<String, Object> yamlData = yaml.load(content);
            return yamlData != null ? flattenProperties("", yamlData, new LinkedHashMap<>()) : new LinkedHashMap<>();
        } catch (Exception e) {
            log.error("Failed to parse YAML content for file: {} - {}", filePath, e.getMessage());
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