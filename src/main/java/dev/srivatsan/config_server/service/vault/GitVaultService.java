package dev.srivatsan.config_server.service.vault;

import java.util.Map;

/**
 * Service interface for managing secrets vault operations using Git as the storage backend.
 * Provides functionality to store, retrieve, and track changes to encrypted secrets
 * with full version control capabilities through Git integration.
 */
public interface GitVaultService {

    /**
     * Retrieves all secrets stored in the vault for the specified namespace.
     *
     * @param namespace the namespace identifier to retrieve secrets for
     * @return a map containing all key-value secret pairs for the namespace
     */
    Map<String, String> getVault(String namespace);

    /**
     * Updates the vault with new or modified secrets for the specified namespace.
     * Creates a new Git commit with the provided commit message and author information.
     *
     * @param namespace     the namespace identifier to update secrets for
     * @param secrets       a map of secret key-value pairs to store in the vault
     * @param email         the email address of the user making the update (for Git commit author)
     * @param commitMessage the commit message describing the vault changes
     */
    void updateVault(String namespace, Map<String, String> secrets, String email, String commitMessage);

    /**
     * Retrieves the complete history of vault changes for the specified namespace.
     * Returns Git commit history with metadata about each vault modification.
     *
     * @param namespace the namespace identifier to get history for
     * @return a map containing vault history information including commits, timestamps, and authors
     */
    Map<String, Object> getVaultHistory(String namespace);

    /**
     * Retrieves specific changes made to the vault in a particular commit.
     * Shows the differences introduced by the specified commit for the given namespace.
     *
     * @param commitId  the Git commit identifier to examine
     * @param namespace the namespace identifier to get changes for
     * @return a map containing the specific changes made in the commit for the namespace
     */
    Map<String, Object> getVaultChanges(String commitId, String namespace);
}