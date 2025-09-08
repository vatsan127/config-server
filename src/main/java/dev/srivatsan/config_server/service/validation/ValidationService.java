package dev.srivatsan.config_server.service.validation;

import dev.srivatsan.config_server.exception.ValidationException;
import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.model.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.regex.Pattern;

@Service
public class ValidationService {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);
    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9/_.-]+$");
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$");

    /**
     * Validates that the action type in the payload matches the expected action type.
     * Ensures that the request action is consistent with the endpoint being called.
     *
     * @param request    the payload containing the action to validate
     * @param actionType the expected action type for the current operation
     * @throws ValidationException if the action types do not match
     */
    public void validateActionType(Payload request, ActionType actionType) {
        if (!actionType.equals(request.getAction())) {
            throw ValidationException.invalidActionType(actionType.toString(),
                    request.getAction() != null ? request.getAction().toString() : "null");
        }
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
            throw ValidationException.invalidPath(filePath,
                    "Path contains unsafe characters. Only alphanumeric, dash, underscore, slash, and dot are allowed");
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
            throw ValidationException.invalidNamespace(namespace,
                    "Invalid format. Only alphanumeric, dash, and underscore are allowed");
        }

        if (isReservedNamespace(cleanNamespace)) {
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
            throw ValidationException.invalidAppName(appName,
                    "Invalid format. Only alphanumeric, dash, and underscore are allowed");
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
     * Supports both single and multi-document YAML (separated by ---).
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
            // Use loadAll() to validate multi-document YAML with --- separators
            for (Object document : yaml.loadAll(content)) {
                // Just iterate through all documents to ensure they're valid
                // The iteration itself validates the YAML syntax
            }
        } catch (YAMLException e) {
            log.error("Invalid YAML content: {}", e.getMessage());
            throw ValidationException.invalidYaml("Invalid YAML syntax");
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
     * Validates Spring Cloud Config request parameters to prevent security issues.
     * Supports comma-separated multiple profiles.
     *
     * @param application the application name
     * @param profile     the profile name (can be null, supports comma-separated values)
     * @param label       the label/branch name (can be null)
     * @throws ValidationException if any parameter is invalid
     */
    public void validateConfigRequest(String application, String profile, String label) {
        if (application == null || application.trim().isEmpty()) {
            throw ValidationException.invalidAppName(application, "Application name cannot be null or empty");
        }

        if (application.contains("../") || application.contains("..\\")) {
            throw ValidationException.invalidAppName(application, "Application name contains invalid path characters");
        }

        // Validate multiple comma-separated profiles
        if (profile != null && (profile.contains("../") || profile.contains("..\\"))) {
            throw ValidationException.invalidPath(profile, "Profile contains invalid path characters");
        }
        
        // Validate individual profiles if comma-separated
        validateProfile(profile);

        // Label can be null (will default to "default")
        if (label != null && (label.contains("../") || label.contains("..\\"))) {
            throw ValidationException.invalidPath(label, "Label contains invalid path characters");
        }
    }

    /**
     * Validates a profile parameter for Spring Cloud Config requests.
     * Supports comma-separated multiple profiles (e.g., "dev,local,debug" or "default,dev").
     *
     * @param profile the profile to validate (can contain comma-separated values)
     * @throws ValidationException if the profile is invalid
     */
    public void validateProfile(String profile) {
        if (profile != null && !profile.trim().isEmpty()) {
            String cleanProfile = profile.trim();

            // Check total length limit for the entire profile string
            if (cleanProfile.length() > 200) {
                throw ValidationException.invalidPath(profile, "Profile string too long (max 200 characters)");
            }

            // Split by comma and validate each individual profile
            String[] profiles = cleanProfile.split(",");
            for (String singleProfile : profiles) {
                validateSingleProfile(singleProfile.trim());
            }
        }
    }

    /**
     * Validates a single profile name.
     *
     * @param profile the individual profile name to validate
     * @throws ValidationException if the profile is invalid
     */
    private void validateSingleProfile(String profile) {
        if (profile.isEmpty()) {
            throw ValidationException.invalidPath(profile, "Profile name cannot be empty");
        }

        if (profile.length() > 50) {
            throw ValidationException.invalidPath(profile, "Individual profile name too long (max 50 characters)");
        }

        // Allow "default" profile and standard profile naming pattern
        if (!"default".equals(profile) && !SAFE_NAME_PATTERN.matcher(profile).matches()) {
            throw ValidationException.invalidPath(profile,
                    "Invalid profile format '" + profile + "'. Only alphanumeric, dash, and underscore are allowed");
        }
    }

    /**
     * Checks if a namespace name is reserved.
     *
     * @param namespace the namespace to check
     * @return true if the namespace is reserved
     */
    private boolean isReservedNamespace(String namespace) {
        return "system".equalsIgnoreCase(namespace) ||
                "admin".equalsIgnoreCase(namespace) ||
                "dashboard".equalsIgnoreCase(namespace) ||
                "default".equalsIgnoreCase(namespace) ||
                "log".equalsIgnoreCase(namespace) ||
                "root".equalsIgnoreCase(namespace);
    }
}