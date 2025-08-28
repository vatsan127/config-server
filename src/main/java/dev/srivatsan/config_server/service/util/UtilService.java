package dev.srivatsan.config_server.service.util;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.NamespaceException;
import dev.srivatsan.config_server.exception.ValidationException;
import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.model.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class UtilService {

    private static final Logger log = LoggerFactory.getLogger(UtilService.class);
    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9/_.-]+$");
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]+$");
    
    private final ApplicationConfig applicationConfig;

    public UtilService(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
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
     * Validates that the action type in the payload matches the expected action type.
     * Ensures that the request action is consistent with the endpoint being called.
     *
     * @param request    the payload containing the action to validate
     * @param actionType the expected action type for the current operation
     * @throws RuntimeException if the action types do not match
     */
    public void validateActionType(Payload request, ActionType actionType) {
        if (!actionType.equals(request.getAction())) {
            throw ValidationException.invalidActionType(actionType.toString(), request.getAction() != null ? request.getAction().toString() : "null");
        }
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
     * Validates that a file path is safe and doesn't contain path traversal attacks.
     * Checks for dangerous patterns like "../" and ensures only safe characters are used.
     *
     * @param filePath the file path to validate
     * @throws ValidationException if the path contains unsafe characters or patterns
     */
    public void validateSafePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw ValidationException.invalidPath(filePath, "Path cannot be null or empty");
        }

        String cleanPath = filePath.trim();

        // Check for path traversal attacks
        if (cleanPath.contains("..") || cleanPath.contains("./") || cleanPath.contains("\\")) {
            throw ValidationException.invalidPath(filePath, "Path contains potentially dangerous traversal patterns");
        }

        // Check for absolute paths (should be relative)
        if (cleanPath.startsWith("/") && cleanPath.length() > 1) {
            cleanPath = cleanPath.substring(1);
        }

        // Validate safe characters only
        if (!SAFE_PATH_PATTERN.matcher(cleanPath).matches()) {
            throw ValidationException.invalidPath(filePath, "Path contains unsafe characters. Only alphanumeric, dash, underscore, slash, and dot are allowed");
        }
    }

    /**
     * Validates that a namespace name is safe and follows naming conventions.
     *
     * @param namespace the namespace to validate
     * @throws ValidationException if the namespace is invalid
     */
    public void validateNamespace(String namespace) {
        if (namespace == null || namespace.trim().isEmpty()) {
            throw ValidationException.invalidNamespace(namespace, "Namespace cannot be null or empty");
        }

        String cleanNamespace = namespace.trim();

        if (cleanNamespace.length() > 50) {
            throw ValidationException.invalidNamespace(namespace, "Namespace too long (max 50 characters)");
        }

        if (!SAFE_NAME_PATTERN.matcher(cleanNamespace).matches()) {
            throw ValidationException.invalidNamespace(namespace, "Invalid format. Only alphanumeric, dash, and underscore are allowed");
        }

        if (    // Reserved namespace names
                "system".equalsIgnoreCase(cleanNamespace) ||
                        "admin".equalsIgnoreCase(cleanNamespace) ||
                        "dashboard".equalsIgnoreCase(cleanNamespace) ||
                        "default".equalsIgnoreCase(cleanNamespace) ||
                        "log".equalsIgnoreCase(cleanNamespace) ||
                        "root".equalsIgnoreCase(cleanNamespace)
        ) {
            throw ValidationException.invalidNamespace(namespace, "Reserved namespace name");
        }
    }

    /**
     * Validates that an application name is safe and follows naming conventions.
     *
     * @param appName the application name to validate
     * @throws ValidationException if the app name is invalid
     */
    public void validateAppName(String appName) {
        if (appName == null || appName.trim().isEmpty()) {
            throw ValidationException.invalidAppName(appName, "Application name cannot be null or empty");
        }

        String cleanAppName = appName.trim();

        if (cleanAppName.length() > 50) {
            throw ValidationException.invalidAppName(appName, "Application name too long (max 50 characters)");
        }

        if (!SAFE_NAME_PATTERN.matcher(cleanAppName).matches()) {
            throw ValidationException.invalidAppName(appName, "Invalid format. Only alphanumeric, dash, and underscore are allowed");
        }
    }

    /**
     * Validates email format.
     *
     * @param email the email to validate
     * @throws ValidationException if the email is invalid
     */
    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw ValidationException.invalidEmail(email);
        }

        String cleanEmail = email.trim();

        // Basic email validation
        if (!cleanEmail.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$")) {
            throw ValidationException.invalidEmail(email);
        }

        if (cleanEmail.length() > 100) {
            throw ValidationException.invalidEmail("Email too long (max 100 characters)");
        }
    }

    /**
     * Validates commit ID format.
     *
     * @param commitId the commit ID to validate
     * @throws ValidationException if the commit ID is invalid
     */
    public void validateCommitId(String commitId) {
        if (commitId == null || commitId.trim().isEmpty()) {
            throw ValidationException.invalidCommitId(commitId);
        }

        String cleanCommitId = commitId.trim();

        // Git commit IDs are typically 40 characters (SHA-1) or 64 characters (SHA-256)
        if (!cleanCommitId.matches("^[a-fA-F0-9]{7,64}$")) {
            throw ValidationException.invalidCommitId("Invalid commit ID format");
        }
    }

    /**
     * Validates YAML configuration content for basic syntax.
     *
     * @param content the YAML content to validate
     * @throws ValidationException if the content is invalid
     */
    public void validateYamlContent(String content) {
        if (content == null) {
            throw ValidationException.invalidContent("Configuration content cannot be null");
        }

        try {
            Yaml yaml = new Yaml();
            yaml.load(content);

        } catch (YAMLException e) {
            log.warn("Invalid YAML content: {}", e.getMessage());
            throw ValidationException.invalidYaml("Invalid YAML syntax: " + e.getMessage());
        }
    }

    /**
     * Validates that a commit message is appropriate and not empty.
     *
     * @param message the commit message to validate
     * @throws ValidationException if the message is invalid
     */
    public void validateCommitMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw ValidationException.invalidCommitMessage("Commit message cannot be null or empty");
        }

        String cleanMessage = message.trim();

        if (cleanMessage.length() > 500) {
            throw ValidationException.invalidCommitMessage("Commit message exceeds maximum length of 500 characters");
        }

        if (cleanMessage.contains("<script") || cleanMessage.contains("javascript:") ||
                cleanMessage.contains("data:text/html")) {
            throw ValidationException.invalidCommitMessage("Commit message contains potentially malicious content");
        }
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
        validateNamespace(namespace);

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
            validateNamespace(name);

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

}
