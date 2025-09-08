package dev.srivatsan.config_server.service.cloud;

import dev.srivatsan.config_server.exception.ConfigFileException;
import dev.srivatsan.config_server.exception.GitOperationException;
import dev.srivatsan.config_server.exception.ValidationException;
import dev.srivatsan.config_server.service.repository.GitRepositoryService;
import dev.srivatsan.config_server.service.secret.SecretProcessor;
import dev.srivatsan.config_server.service.util.UtilService;
import dev.srivatsan.config_server.service.validation.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CloudConfigService implements EnvironmentRepository, Ordered {

    private final Logger log = LoggerFactory.getLogger(CloudConfigService.class);

    private final GitRepositoryService gitRepositoryService;
    private final UtilService utilService;
    private final ValidationService validationService;
    private final SecretProcessor secretProcessor;

    public CloudConfigService(GitRepositoryService gitRepositoryService,
                              UtilService utilService,
                              ValidationService validationService,
                              SecretProcessor secretProcessor) {
        this.gitRepositoryService = gitRepositoryService;
        this.utilService = utilService;
        this.validationService = validationService;
        this.secretProcessor = secretProcessor;
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        log.info("Finding configuration for application: {}, profile: {}, label: {}", application, profile, label);

        validationService.validateConfigRequest(application, profile, label);
        Environment environment = new Environment(application, profile);

        try {
            String namespace = utilService.extractNamespaceFromLabel(label);
            validationService.validateNamespace(namespace);
            String path = utilService.extractPathFromLabel(label);

            List<PropertySource> propertySources = loadConfigurationFiles(namespace, path, application, profile);
            for (PropertySource propertySource : propertySources) {
                environment.add(propertySource);
            }

            String mainFilePath = utilService.constructFilePathFromLabel(namespace, path, application, null);
            String version = gitRepositoryService.getLatestCommitId(mainFilePath);
            environment.setVersion(version);

            log.info("Successfully loaded {} configuration files for {}, version: {}", propertySources.size(), application, version);
            return environment;

        } catch (ConfigFileException | GitOperationException | ValidationException e) {
            log.error("Failed to load configuration for application: {}, profile: {}, label: {}", application, profile, label, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error loading configuration for application: {}, profile: {}, label: {}", application, profile, label, e);
            throw new ConfigFileException("CONFIG_LOAD_FAILED",
                    "Failed to load configuration for application: " + application, e);
        }
    }


    /**
     * Loads and flattens configuration files including profile-specific configurations.
     * Uses the new flattened approach: Load all sources → Flatten → Resolve secrets → Return single PropertySource
     * Supports multiple comma-separated profiles with proper precedence.
     */
    private List<PropertySource> loadConfigurationFiles(String namespace, String path, String application, String profile) throws Exception {
        log.info("Loading flattened configuration for application: {}, profile: {}", application, profile);

        // 1. Load all raw property sources (no secret processing yet)
        List<Map<String, Object>> rawPropertyMaps = new ArrayList<>();

        // Load generic application.yml (shared across all applications in namespace/path)
        loadRawPropertySource(rawPropertyMaps, namespace, path, "application", null);

        // Load main application configuration (application-specific base config)
        loadRawPropertySource(rawPropertyMaps, namespace, path, application, null);

        // Handle multiple comma-separated profiles with proper precedence
        if (profile != null && !profile.trim().isEmpty()) {
            String[] profiles = profile.split(",");
            for (String singleProfile : profiles) {
                String trimmedProfile = singleProfile.trim();
                // Skip empty profiles and don't load separate file for "default" profile
                if (!trimmedProfile.isEmpty() && !"default".equals(trimmedProfile)) {
                    loadRawPropertySource(rawPropertyMaps, namespace, path, application, trimmedProfile);
                    log.debug("Loaded profile-specific configuration for profile: {}", trimmedProfile);
                }
            }
        }

        // Check if we have any configuration loaded
        if (rawPropertyMaps.isEmpty()) {
            String mainFilePath = utilService.constructFilePathFromLabel(namespace, path, application, null);
            throw ConfigFileException.notFound(mainFilePath);
        }

        // 2. Flatten all property sources into single merged map with dot-notation keys
        Map<String, Object> flattenedConfig = utilService.flattenPropertySources(rawPropertyMaps);
        log.info("flattenedConfig - {}", flattenedConfig);

        // 3. Convert flattened config back to YAML for secret processing
        String flattenedYaml = utilService.convertMapToYaml(flattenedConfig);

        // 4. Resolve all secrets in the flattened configuration
        String resolvedYaml = secretProcessor.processConfigurationForClient(flattenedYaml, namespace);

        // 5. Parse resolved YAML back to map
        String mergedSourceName = String.format("merged-%s-%s", application, profile != null ? profile : "default");
        Map<String, Object> resolvedProperties = utilService.parseYamlContent(resolvedYaml, mergedSourceName);

        // 6. Return single PropertySource with all merged and resolved configuration
        List<PropertySource> result = new ArrayList<>();
        result.add(new PropertySource(mergedSourceName, resolvedProperties));

        log.info("Successfully created flattened configuration with {} properties for {}",
                resolvedProperties.size(), application);

        return result;
    }

    /**
     * Loads a raw property source without any secret processing.
     *
     * @param rawPropertyMaps list to add the loaded properties to
     * @param namespace       the namespace
     * @param path            the path within namespace
     * @param application     the application name
     * @param profile         the profile (can be null)
     */
    private void loadRawPropertySource(List<Map<String, Object>> rawPropertyMaps,
                                       String namespace, String path, String application, String profile) {
        String filePath = utilService.constructFilePathFromLabel(namespace, path, application, profile);

        try {
            // Use getConfigFile with forClient=false to get raw content without secret processing
            String rawContent = gitRepositoryService.getConfigFile(filePath);
            Map<String, Object> properties = utilService.parseYamlContent(rawContent, filePath);

            if (!properties.isEmpty()) {
                rawPropertyMaps.add(properties);
                log.debug("Loaded raw properties from: {}", filePath);
            }

            log.info("rawPropertyMaps - {}", rawPropertyMaps);

        } catch (Exception e) {
            log.debug("Configuration file not found or could not be loaded: {} - {}", filePath, e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // High priority for our custom repository
    }
}