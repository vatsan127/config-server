package dev.srivatsan.config_server.service.secret;

/**
 * Service interface for processing configuration content with secret handling capabilities.
 * Provides different processing modes for client-facing and internal use cases,
 * ensuring secrets are properly handled based on the consumption context.
 */
public interface SecretProcessor {

    /**
     * Processes configuration content for client consumption by decrypting encrypted values
     * and resolving any secret references. This method prepares configuration data
     * to be safely returned to external clients.
     *
     * @param configContent the raw configuration content to process
     * @param namespace     the namespace identifier for context-aware secret resolution
     * @return the processed configuration content with secrets resolved for client use
     */
    String processConfigurationForClient(String configContent, String namespace);

    /**
     * Processes configuration content for internal server operations while maintaining
     * encrypted state where appropriate. This method handles configuration data
     * for internal processing without exposing sensitive values unnecessarily.
     *
     * @param configContent the raw configuration content to process
     * @param namespace     the namespace identifier for context-aware processing
     * @return the processed configuration content suitable for internal operations
     */
    String processConfigurationForInternal(String configContent, String namespace);

}