package dev.srivatsan.config_server.service.vault;

import java.util.Map;

public interface GitVaultService {

    Map<String, String> getVault(String namespace);

    void updateVault(String namespace, Map<String, String> secrets, String email, String commitMessage);

    Map<String, Object> getVaultHistory(String namespace);
}