package dev.srivatsan.config_server.service.vault;

import java.util.Map;

public interface GitVaultService {
    
    void storeSecret(String namespace, String key, String value, String email, String commitMessage);
    
    String getSecret(String namespace, String key);
    
    void updateSecret(String namespace, String key, String value, String email, String commitMessage);
    
    void deleteSecret(String namespace, String key, String email, String commitMessage);
    
    Map<String, String> getAllSecrets(String namespace);
    
    void storeBulkSecrets(String namespace, Map<String, String> secrets, String email, String commitMessage);
    
    boolean secretExists(String namespace, String key);
    
    Map<String, Object> getVaultHistory(String namespace);
}