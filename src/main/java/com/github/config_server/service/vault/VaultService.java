package com.github.config_server.service.vault;

import java.util.Map;

/**
 * Interface for managing secrets vault operations using Git as the storage backend.
 */
public interface VaultService {

    /**
     * Retrieves all secrets stored in the vault for the specified namespace.
     */
    Map<String, String> getVault(String namespace);

    /**
     * Updates the vault with new or modified secrets for the specified namespace.
     */
    void updateVault(String namespace, Map<String, String> secrets, String email, String commitMessage);

}