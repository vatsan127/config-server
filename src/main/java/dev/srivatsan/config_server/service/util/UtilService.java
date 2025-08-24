package dev.srivatsan.config_server.service.util;

import dev.srivatsan.config_server.model.ActionType;
import dev.srivatsan.config_server.model.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Component
public class UtilService {

    /**
     * Constructs the relative file path for a configuration file based on the payload.
     * Combines namespace, path, and filename to create the complete relative path.
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
     * @param request the payload containing the action to validate
     * @param actionType the expected action type for the current operation
     * @throws RuntimeException if the action types do not match
     */
    public void validateActionType(Payload request, ActionType actionType) {
        if (!actionType.equals(request.getAction())) {
            throw new RuntimeException("Invalid action Type for request " + request.getAction());
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
     * Parses the file path to identify the namespace (first directory component).
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
     * Gets the relative path within a namespace by removing the namespace prefix.
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

}
