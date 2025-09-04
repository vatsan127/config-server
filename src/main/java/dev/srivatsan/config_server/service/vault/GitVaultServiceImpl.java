package dev.srivatsan.config_server.service.vault;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.VaultException;
import dev.srivatsan.config_server.service.encryption.EncryptionService;
import dev.srivatsan.config_server.service.operation.GitOperationService;
import dev.srivatsan.config_server.service.util.UtilService;
import dev.srivatsan.config_server.service.validation.ValidationService;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.eclipse.jgit.lib.Constants.HEAD;

@Service
public class GitVaultServiceImpl implements GitVaultService {

    private static final Logger log = LoggerFactory.getLogger(GitVaultServiceImpl.class);
    private static final String VAULT_FILE_NAME = ".vault-secrets.json";

    private final ApplicationConfig applicationConfig;
    private final EncryptionService encryptionService;
    private final GitOperationService gitOperationService;
    private final ValidationService validationService;
    private final UtilService utilService;
    private final ObjectMapper objectMapper;

    public GitVaultServiceImpl(ApplicationConfig applicationConfig,
                               EncryptionService encryptionService,
                               GitOperationService gitOperationService,
                               ValidationService validationService,
                               UtilService utilService) {
        this.applicationConfig = applicationConfig;
        this.encryptionService = encryptionService;
        this.gitOperationService = gitOperationService;
        this.validationService = validationService;
        this.utilService = utilService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @CacheEvict(value = "vault-secrets", key = "#namespace")
    public void storeSecret(String namespace, String key, String value, String email, String commitMessage) {
        validationService.validateNamespace(namespace);
        validationService.validateEmail(email);
        validationService.validateCommitMessage(commitMessage);
        
        if (key == null || key.trim().isEmpty()) {
            throw VaultException.vaultOperationFailed("Secret key cannot be null or empty");
        }
        
        if (value == null || value.trim().isEmpty()) {
            throw VaultException.vaultOperationFailed("Secret value cannot be null or empty");
        }

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            try {
                Map<String, String> secrets = loadVaultSecrets(namespace, git);
                if (secrets.containsKey(key)) {
                    throw VaultException.vaultOperationFailed("Secret already exists: " + key + ". Use update instead.");
                }

                String encryptedValue = encryptionService.encrypt(value, namespace);
                secrets.put(key, encryptedValue);

                saveVaultSecrets(namespace, secrets, git);
                git.add().addFilepattern(VAULT_FILE_NAME).call();
                git.commit()
                        .setMessage(commitMessage)
                        .setAuthor(email.substring(0, email.indexOf('@')), email)
                        .call();
                
                log.info("Stored secret '{}' in namespace '{}' vault", key, namespace);
                
            } catch (Exception e) {
                log.error("Failed to store secret '{}' in namespace '{}': {}", key, namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to store secret: " + e.getMessage());
            }
        });
    }

    @Override
    @Cacheable(value = "vault-secrets", key = "#namespace + '_' + #key")
    public String getSecret(String namespace, String key) {
        validationService.validateNamespace(namespace);
        
        if (key == null || key.trim().isEmpty()) {
            throw VaultException.secretNotFound(key);
        }

        return gitOperationService.executeGitOperation(namespace, git -> {
            try {
                Map<String, String> secrets = loadVaultSecrets(namespace, git);
                String encryptedValue = secrets.get(key);
                
                if (encryptedValue == null) {
                    throw VaultException.secretNotFound(key);
                }
                
                return encryptionService.decrypt(encryptedValue, namespace);
                
            } catch (Exception e) {
                log.error("Failed to get secret '{}' from namespace '{}': {}", key, namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to get secret: " + e.getMessage());
            }
        });
    }

    @Override
    @CacheEvict(value = "vault-secrets", key = "#namespace")
    public void updateSecret(String namespace, String key, String value, String email, String commitMessage) {
        validationService.validateNamespace(namespace);
        validationService.validateEmail(email);
        validationService.validateCommitMessage(commitMessage);
        
        if (key == null || key.trim().isEmpty()) {
            throw VaultException.vaultOperationFailed("Secret key cannot be null or empty");
        }
        
        if (value == null || value.trim().isEmpty()) {
            throw VaultException.vaultOperationFailed("Secret value cannot be null or empty");
        }

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            try {
                Map<String, String> secrets = loadVaultSecrets(namespace, git);
                if (!secrets.containsKey(key)) {
                    throw VaultException.secretNotFound(key);
                }

                String encryptedValue = encryptionService.encrypt(value, namespace);
                secrets.put(key, encryptedValue);
                saveVaultSecrets(namespace, secrets, git);
                git.add().addFilepattern(VAULT_FILE_NAME).call();
                git.commit()
                        .setMessage(commitMessage)
                        .setAuthor(email.substring(0, email.indexOf('@')), email)
                        .call();
                
                log.info("Updated secret '{}' in namespace '{}' vault", key, namespace);
                
            } catch (Exception e) {
                log.error("Failed to update secret '{}' in namespace '{}': {}", key, namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to update secret: " + e.getMessage());
            }
        });
    }

    @Override
    @CacheEvict(value = "vault-secrets", key = "#namespace")
    public void deleteSecret(String namespace, String key, String email, String commitMessage) {
        validationService.validateNamespace(namespace);
        validationService.validateEmail(email);
        validationService.validateCommitMessage(commitMessage);
        
        if (key == null || key.trim().isEmpty()) {
            throw VaultException.secretNotFound(key);
        }

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            try {
                Map<String, String> secrets = loadVaultSecrets(namespace, git);
                if (!secrets.containsKey(key)) {
                    throw VaultException.secretNotFound(key);
                }

                secrets.remove(key);
                saveVaultSecrets(namespace, secrets, git);
                git.add().addFilepattern(VAULT_FILE_NAME).call();
                git.commit()
                        .setMessage(commitMessage)
                        .setAuthor(email.substring(0, email.indexOf('@')), email)
                        .call();
                
                log.info("Deleted secret '{}' from namespace '{}' vault", key, namespace);
                
            } catch (Exception e) {
                log.error("Failed to delete secret '{}' from namespace '{}': {}", key, namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to delete secret: " + e.getMessage());
            }
        });
    }

    @Override
    @Cacheable(value = "vault-secrets", key = "#namespace + '_all'")
    public Map<String, String> getAllSecrets(String namespace) {
        validationService.validateNamespace(namespace);

        return gitOperationService.executeGitOperation(namespace, git -> {
            try {
                Map<String, String> secrets = loadVaultSecrets(namespace, git);
                Map<String, String> decryptedSecrets = new HashMap<>();
                
                for (Map.Entry<String, String> entry : secrets.entrySet()) {
                    String decryptedValue = encryptionService.decrypt(entry.getValue(), namespace);
                    decryptedSecrets.put(entry.getKey(), decryptedValue);
                }
                
                return decryptedSecrets;
                
            } catch (Exception e) {
                log.error("Failed to get all secrets from namespace '{}': {}", namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to get all secrets: " + e.getMessage());
            }
        });
    }

    @Override
    @CacheEvict(value = "vault-secrets", key = "#namespace")
    public void storeBulkSecrets(String namespace, Map<String, String> secrets, String email, String commitMessage) {
        validationService.validateNamespace(namespace);
        validationService.validateEmail(email);
        validationService.validateCommitMessage(commitMessage);
        
        if (secrets == null || secrets.isEmpty()) {
            throw VaultException.vaultOperationFailed("Secrets map cannot be null or empty");
        }
        
        if (secrets.size() > applicationConfig.getVault().getMaxSecretsPerOperation()) {
            throw VaultException.vaultOperationFailed(
                String.format("Cannot store more than %d secrets in a single operation. Provided: %d",
                    applicationConfig.getVault().getMaxSecretsPerOperation(), secrets.size()));
        }

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            try {

                Map<String, String> existingSecrets = loadVaultSecrets(namespace, git);
                for (Map.Entry<String, String> entry : secrets.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    if (key == null || key.trim().isEmpty()) {
                        throw VaultException.vaultOperationFailed("Secret key cannot be null or empty");
                    }
                    
                    if (value == null || value.trim().isEmpty()) {
                        throw VaultException.vaultOperationFailed("Secret value cannot be null or empty for key: " + key);
                    }
                    
                    String encryptedValue = encryptionService.encrypt(value, namespace);
                    existingSecrets.put(key, encryptedValue);
                }

                saveVaultSecrets(namespace, existingSecrets, git);
                git.add().addFilepattern(VAULT_FILE_NAME).call();
                git.commit()
                        .setMessage(commitMessage)
                        .setAuthor(email.substring(0, email.indexOf('@')), email)
                        .call();
                
                log.info("Stored {} secrets in namespace '{}' vault", secrets.size(), namespace);
                
            } catch (Exception e) {
                log.error("Failed to store bulk secrets in namespace '{}': {}", namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to store bulk secrets: " + e.getMessage());
            }
        });
    }

    @Override
    public boolean secretExists(String namespace, String key) {
        validationService.validateNamespace(namespace);
        
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        return gitOperationService.executeGitOperation(namespace, git -> {
            try {
                Map<String, String> secrets = loadVaultSecrets(namespace, git);
                return secrets.containsKey(key);
                
            } catch (Exception e) {
                log.debug("Error checking if secret exists '{}' in namespace '{}': {}", key, namespace, e.getMessage());
                return false;
            }
        });
    }

    @Override
    @Cacheable(value = "vault-history", key = "#namespace")
    public Map<String, Object> getVaultHistory(String namespace) {
        validationService.validateNamespace(namespace);

        return gitOperationService.executeGitOperation(namespace, git -> {
            try {
                var logCommand = git.log()
                        .setMaxCount(applicationConfig.getCommitHistorySize())
                        .add(git.getRepository().resolve(HEAD))
                        .addPath(VAULT_FILE_NAME);

                List<Map<String, Object>> commits = new ArrayList<>();
                for (RevCommit commit : logCommand.call()) {
                    Map<String, Object> commitInfo = utilService.formatCommitInfo(commit);
                    commitInfo.put("commitMessage", commit.getShortMessage());
                    commits.add(commitInfo);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("namespace", namespace);
                result.put("vaultFile", VAULT_FILE_NAME);
                result.put("commits", commits);
                return result;

            } catch (Exception e) {
                log.error("Failed to get vault history for namespace '{}': {}", namespace, e.getMessage());
                throw VaultException.vaultOperationFailed("Failed to get vault history: " + e.getMessage());
            }
        });
    }

    private Map<String, String> loadVaultSecrets(String namespace, org.eclipse.jgit.api.Git git) throws IOException {
        Path workTree = git.getRepository().getWorkTree().toPath();
        Path vaultFilePath = workTree.resolve(VAULT_FILE_NAME);

        if (!Files.exists(vaultFilePath)) {
            log.debug("Vault file does not exist for namespace '{}', returning empty map", namespace);
            return new HashMap<>();
        }

        try {
            String jsonContent = Files.readString(vaultFilePath);
            if (jsonContent.trim().isEmpty()) {
                return new HashMap<>();
            }
            
            return objectMapper.readValue(jsonContent, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse vault file for namespace '{}': {}", namespace, e.getMessage());
            throw VaultException.vaultOperationFailed("Failed to parse vault file: " + e.getMessage());
        }
    }

    private void saveVaultSecrets(String namespace, Map<String, String> secrets, org.eclipse.jgit.api.Git git) throws IOException {
        Path workTree = git.getRepository().getWorkTree().toPath();
        Path vaultFilePath = workTree.resolve(VAULT_FILE_NAME);

        try {
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(secrets);
            Files.writeString(vaultFilePath, jsonContent);
        } catch (Exception e) {
            log.error("Failed to save vault file for namespace '{}': {}", namespace, e.getMessage());
            throw VaultException.vaultOperationFailed("Failed to save vault file: " + e.getMessage());
        }
    }

}