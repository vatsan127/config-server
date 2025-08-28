package dev.srivatsan.config_server.service.util;

import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.NamespaceException;
import dev.srivatsan.config_server.model.Payload;
import dev.srivatsan.config_server.service.validation.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class UtilService {

    private static final Logger log = LoggerFactory.getLogger(UtilService.class);
    
    private final ApplicationConfig applicationConfig;
    private final ValidationService validationService;

    public UtilService(ApplicationConfig applicationConfig, ValidationService validationService) {
        this.applicationConfig = applicationConfig;
        this.validationService = validationService;
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

}
