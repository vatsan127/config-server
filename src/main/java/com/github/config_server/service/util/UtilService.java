package com.github.config_server.service.util;

import com.github.config_server.config.ApplicationConfig;
import com.github.config_server.exception.NamespaceException;
import com.github.config_server.model.Payload;
import com.github.config_server.service.validation.ValidationService;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class UtilService {

    private static final Logger log = LoggerFactory.getLogger(UtilService.class);

    private final ApplicationConfig applicationConfig;
    private final ValidationService validationService;
    private final Yaml yaml;

    public UtilService(ApplicationConfig applicationConfig, ValidationService validationService) {
        this.applicationConfig = applicationConfig;
        this.validationService = validationService;
        this.yaml = new Yaml();
    }

    /**
     * Constructs the relative file path for a configuration file based on the payload.
     * Example: namespace="prod", path="/config/", filename="app.yml" -> "prod/config/app.yml"
     *
     * @param request the payload containing namespace, path, and application name
     * @return the complete relative file path as a string
     */
    public String getRelativeFilePath(Payload request) {
        return request.getNamespace() + request.getPath() + request.getFileName();
    }


    /**
     * Generates a short unique request identifier.
     * Creates an 8-character unique ID based on UUID for request tracking purposes.
     *
     * @return an 8-character unique request ID string
     */
    public String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Extracts the namespace from a complete file path.
     * Example: "production/config/app.yml" -> "production"
     * Example: "/staging/data/service.yml" -> "staging"
     *
     * @param filePath the complete file path containing namespace and relative path
     * @return the namespace string extracted from the file path
     */
    public String extractNamespaceFromFilePath(String filePath) {
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        int slashIndex = filePath.indexOf('/');
        if (slashIndex == -1) {
            return filePath;
        }
        return filePath.substring(0, slashIndex);
    }

    /**
     * Extracts the file path relative to the namespace directory for git operations.
     * Example: "production/config/app.yml" -> "config/app.yml"
     * Example: "/staging/data/service.yml" -> "data/service.yml"
     * Example: "dev" -> "" (no path within namespace)
     *
     * @param filePath the complete file path containing namespace and relative path
     * @return the relative path within the namespace, or empty string if no path exists
     */
    public String getRelativePathWithinNamespace(String filePath) {
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        int slashIndex = filePath.indexOf('/');
        if (slashIndex == -1) {
            return "";
        }
        return filePath.substring(slashIndex + 1);
    }


    /**
     * Retrieves a list of all available namespaces.
     * Returns the names of all namespace directories that exist in the base path.
     *
     * @return a list of namespace names
     */
    @org.springframework.cache.annotation.Cacheable(value = "namespaces", key = "'all'")
    public List<String> listNamespaces() {
        File baseDir = new File(applicationConfig.getBasePath());

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.warn("Base directory does not exist: {}", baseDir.getAbsolutePath());
            return Collections.emptyList();
        }

        File[] namespaceDirs = baseDir.listFiles(File::isDirectory);
        if (namespaceDirs == null) {
            return Collections.emptyList();
        }

        List<String> namespaces = Arrays.stream(namespaceDirs)
                .map(File::getName)
                .filter(name -> isValidNamespace(name))
                .sorted()
                .collect(Collectors.toList());

        log.debug("Found {} namespaces in base directory", namespaces.size());
        return namespaces;
    }

    /**
     * Retrieves the contents of a directory within a namespace.
     * Returns only .yml files and subdirectories in the specified path.
     *
     * @param namespace the namespace identifier
     * @param path      the relative directory path within the namespace (empty string for root)
     * @return a list of .yml file names and folder names
     * @throws RuntimeException if the namespace or directory is not found or cannot be accessed
     */
    @org.springframework.cache.annotation.Cacheable(value = "directory-listing", key = "#namespace + '_' + #path")
    public List<String> listDirectoryContents(String namespace, String path) {
        validationService.validateNamespace(namespace);

        File namespaceDir = new File(applicationConfig.getBasePath(), namespace);
        if (!namespaceDir.exists()) {
            throw NamespaceException.notFound(namespace);
        }

        String cleanPath = normalizeDirectoryPath(path);
        File targetDir = resolveTargetDirectory(namespaceDir, cleanPath);
        validateDirectoryAccess(targetDir, namespaceDir, cleanPath);

        File[] files = targetDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        List<String> fileNames = filterAndSortFiles(files);
        log.debug("Listed {} entries in namespace '{}' path '{}'", fileNames.size(), namespace, cleanPath);
        return fileNames;
    }

    private boolean isValidNamespace(String name) {
        try {
            validationService.validateNamespace(name);

            // Check if it's a valid git repository
            File namespaceDir = new File(applicationConfig.getBasePath(), name);
            File gitDir = new File(namespaceDir, ".git");
            return gitDir.exists() && gitDir.isDirectory();
        } catch (Exception e) {
            log.debug("Skipping invalid namespace directory: {}", name);
            return false;
        }
    }

    private String normalizeDirectoryPath(String path) {
        String cleanPath = (path == null || path.trim().isEmpty()) ? "" : path.trim();
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        return cleanPath;
    }

    private File resolveTargetDirectory(File namespaceDir, String cleanPath) {
        return cleanPath.isEmpty() ? namespaceDir : new File(namespaceDir, cleanPath);
    }

    private void validateDirectoryAccess(File targetDir, File namespaceDir, String cleanPath) {
        if (!targetDir.exists()) {
            throw new RuntimeException("Directory not found: " + cleanPath);
        }

        if (!targetDir.isDirectory()) {
            throw new RuntimeException("Path is not a directory: " + cleanPath);
        }

        // Security check: ensure target directory is within namespace
        try {
            if (!targetDir.getCanonicalPath().startsWith(namespaceDir.getCanonicalPath())) {
                throw new RuntimeException("Invalid path: access denied");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to validate path security", e);
        }
    }

    private List<String> filterAndSortFiles(File[] files) {
        List<String> fileNames = new ArrayList<>();
        for (File file : files) {
            if (file.getName().startsWith(".")) {
                // Skip .git directory and other hidden files/directories
            } else if (file.getName().toLowerCase().endsWith(".yml")) {
                fileNames.add(file.getName().split("\\.")[0]);
            } else if (file.isDirectory()) {
                fileNames.add(file.getName() + "/");
            }
        }

        fileNames.sort(String.CASE_INSENSITIVE_ORDER);
        return fileNames;
    }

    /**
     * Extract namespace from Spring Cloud Config label.
     * Examples:
     * - "production/config" -> "production"
     * - "production" -> "production"
     * - "test/api" -> "test"
     * - null/empty -> "main"
     *
     * @param label the label from Spring Cloud Config request
     * @return the namespace extracted from the label
     */
    public String extractNamespaceFromLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "main";
        }

        if (label.contains("/")) {
            return label.split("/")[0];
        }
        return label;
    }

    /**
     * Extract path from Spring Cloud Config label.
     * Examples:
     * - "production/config" -> "config"
     * - "test/api/v1" -> "api/v1"
     * - "production" -> "" (root path)
     * - null/empty -> "" (root path)
     *
     * @param label the label from Spring Cloud Config request
     * @return the path extracted from the label
     */
    public String extractPathFromLabel(String label) {
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
     *
     * @param namespace   the namespace name
     * @param path        the path within the namespace
     * @param application the application name
     * @param profile     the profile name (can be null)
     * @return the complete file path
     */
    public String constructFilePathFromLabel(String namespace, String path, String application, String profile) {
        StringBuilder filePath = new StringBuilder();
        filePath.append(namespace);

        if (!path.isEmpty()) {
            filePath.append("/").append(path);
        }
        filePath.append("/").append(application);

        if (profile != null && !profile.trim().isEmpty() && !"default".equals(profile)) {
            filePath.append("-").append(profile);
        }
        filePath.append(".yml");
        return filePath.toString();
    }

    /**
     * Parse YAML content into a map of properties.
     * Assumes content is already validated during config updates.
     *
     * @param content  the YAML content to parse
     * @param filePath the file path for logging purposes
     * @return a map containing the parsed YAML properties
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseYamlContent(String content, String filePath) {
        if (content == null || content.trim().isEmpty()) {
            log.debug("Empty content for file: {}, returning empty properties", filePath);
            return new LinkedHashMap<>();
        }

        try {
            Map<String, Object> yamlData = yaml.load(content);
            return yamlData != null ? yamlData : new LinkedHashMap<>();
        } catch (Exception e) {
            log.error("Failed to parse YAML content for file: {} - {}", filePath, e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * Constructs the file path for generic application.yml that applies to all applications in a namespace.
     * This file contains common configuration shared across all applications in the namespace.
     *
     * @param namespace the namespace name
     * @param path      the path within the namespace (can be empty for root)
     * @return the file path for the generic application.yml
     */
    public String constructGenericApplicationConfigPath(String namespace, String path) {
        StringBuilder filePath = new StringBuilder();
        filePath.append(namespace);

        if (!path.isEmpty()) {
            filePath.append("/").append(path);
        }

        filePath.append("/application.yml");
        return filePath.toString();
    }

    /**
     * Flattens multiple property sources into a single merged map with correct precedence.
     * Later sources in the list override earlier ones (profile-specific overrides base config).
     *
     * @param propertySources the list of property sources to flatten
     * @return a merged map containing all properties with correct precedence
     */
    public Map<String, Object> flattenPropertySources(List<Map<String, Object>> propertySources) {
        Map<String, Object> merged = new LinkedHashMap<>();

        // First merge sources in order - later sources override earlier ones
        for (Map<String, Object> source : propertySources) {
            deepMergeProperties(merged, source);
        }

        // Then flatten the merged result to dot-notation keys
        return flattenMap(merged);
    }

    /**
     * Flattens a nested map to dot-notation keys
     * Example: {server: {port: 8080}} -> {"server.port": 8080}
     */
    private Map<String, Object> flattenMap(Map<String, Object> map) {
        Map<String, Object> flattened = new LinkedHashMap<>();
        flattenMapRecursive(map, "", flattened);
        return flattened;
    }

    private void flattenMapRecursive(Map<String, Object> source, String prefix, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenMapRecursive(nestedMap, key, result);
            } else {
                result.put(key, value);
            }
        }
    }

    /**
     * Recursively merges properties from source map into target map.
     * Nested maps are merged deeply, primitive values are overwritten.
     *
     * @param target the target map to merge into
     * @param source the source map to merge from
     */
    @SuppressWarnings("unchecked")
    private void deepMergeProperties(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();

            if (target.containsKey(key) && target.get(key) instanceof Map && sourceValue instanceof Map) {
                // Both are maps, merge recursively
                deepMergeProperties((Map<String, Object>) target.get(key), (Map<String, Object>) sourceValue);
            } else {
                // Overwrite with new value
                target.put(key, sourceValue);
            }
        }
    }

    /**
     * Converts a flattened property map back to YAML string for secret processing.
     * This is needed because SecretProcessor works with YAML string content.
     *
     * @param properties the property map to convert
     * @return YAML string representation of the properties
     */
    public String convertMapToYaml(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }

        try {
            return yaml.dump(properties);
        } catch (Exception e) {
            log.error("Failed to convert properties map to YAML: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Formats git commit information into a standardized map structure.
     * Extracts commit details including ID, author, email, and formatted date.
     *
     * @param commit the RevCommit object to format
     * @return a map containing formatted commit information
     */
    public Map<String, Object> formatCommitInfo(RevCommit commit) {
        PersonIdent author = commit.getAuthorIdent();
        String commitDate = Instant.ofEpochSecond(commit.getCommitTime())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, Object> commitInfo = new HashMap<>();
        commitInfo.put("commitId", commit.getId().getName());
        commitInfo.put("author", author.getName());
        commitInfo.put("email", author.getEmailAddress());
        commitInfo.put("date", commitDate);
        return commitInfo;
    }

}
