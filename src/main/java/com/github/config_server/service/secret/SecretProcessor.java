package com.github.config_server.service.secret;

/**
 * Service interface for processing configuration content with secret handling capabilities.
 */
public interface SecretProcessor {

    /**
     * Processes configuration content for client consumption by decrypting encrypted values.
     */
    String processConfigurationForClient(String configContent, String namespace);

    /**
     * Processes configuration content for internal server operations while maintaining
     * encrypted state where appropriate.
     */
    String processConfigurationForInternal(String configContent, String namespace);

}