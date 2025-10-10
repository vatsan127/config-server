package com.github.config_server.service.vault;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.config_server.config.ApplicationConfig;
import com.github.config_server.exception.VaultException;
import com.github.config_server.service.cache.CacheManagerService;
import com.github.config_server.service.encryption.EncryptionService;
import com.github.config_server.service.operation.GitOperationService;
import com.github.config_server.service.util.UtilService;
import com.github.config_server.service.validation.ValidationService;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class VaultServiceImpl implements VaultService {

    private static final Logger log = LoggerFactory.getLogger(VaultServiceImpl.class);
    private static final String VAULT_DIR = ".vault";
    private static final String VAULT_FILE_SUFFIX = "-vault.json";

    private final ApplicationConfig applicationConfig;
    private final EncryptionService encryptionService;
    private final GitOperationService gitOperationService;
    private final ValidationService validationService;
    private final UtilService utilService;
    private final CacheManagerService cacheManagerService;
    private final ObjectMapper objectMapper;

    public VaultServiceImpl(ApplicationConfig applicationConfig,
                            EncryptionService encryptionService,
                            GitOperationService gitOperationService,
                            ValidationService validationService,
                            UtilService utilService,
                            CacheManagerService cacheManagerService) {
        this.applicationConfig = applicationConfig;
        this.encryptionService = encryptionService;
        this.gitOperationService = gitOperationService;
        this.validationService = validationService;
        this.utilService = utilService;
        this.cacheManagerService = cacheManagerService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Cacheable(value = "vault-secrets", key = "#namespace")
    public Map<String, String> getVault(String namespace) {
        validationService.validateNamespace(namespace);

        return gitOperationService.executeGitOperation(namespace, git -> {
            try {
                Map<String, String> secrets = loadVaultSecrets(namespace, git);
                secrets.replaceAll((key, encryptedValue) -> encryptionService.decrypt(encryptedValue, namespace));
                return secrets;
            } catch (Exception e) {
                log.error("Failed to get vault from namespace '{}': {}", namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to get vault: " + e.getMessage());
            }
        });
    }

    @Override
    public void updateVault(String namespace, Map<String, String> secrets, String email, String commitMessage) {
        validationService.validateNamespace(namespace);
        validationService.validateCommitMessage(commitMessage);

        if (secrets == null) {
            throw VaultException.vaultOperationFailed("Secrets map cannot be null");
        }

        // Validate all secret keys before processing
        for (String secretKey : secrets.keySet()) {
            validationService.validateSecretKey(secretKey);
        }

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            try {

                Map<String, String> encryptedSecrets = new HashMap<>();
                for (Map.Entry<String, String> entry : secrets.entrySet()) {
                    String encryptedValue = encryptionService.encrypt(entry.getValue(), namespace);
                    encryptedSecrets.put(entry.getKey(), encryptedValue);
                }

                saveVaultSecrets(namespace, encryptedSecrets, git);

                String vaultFileName = namespace + VAULT_FILE_SUFFIX;
                git.add().addFilepattern(VAULT_DIR + "/" + vaultFileName).call();
                git.commit()
                        .setMessage(commitMessage)
                        .setAuthor(email.substring(0, email.indexOf('@')), email)
                        .call();

                log.info("Updated {} secrets in namespace '{}' vault", secrets.size(), namespace);
            } catch (Exception e) {
                log.error("Failed to update vault in namespace '{}': {}", namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to update vault: " + e.getMessage());
            }
        });

        cacheManagerService.evictKey("vault-secrets", namespace);
        cacheManagerService.evictByPrefix("config-content", namespace + "/");
        cacheManagerService.evictByPrefix("commit-history", namespace + "/");
        cacheManagerService.evictByPrefix("latest-commit", namespace + "/");
        cacheManagerService.evictByPrefix("commit-details", "_" + namespace);

        log.debug("Evicted vault and config cache entries for namespace '{}'", namespace);
    }


    private Map<String, String> loadVaultSecrets(String namespace, Git git) throws IOException {
        Path workTree = git.getRepository().getWorkTree().toPath();
        String vaultFileName = namespace + VAULT_FILE_SUFFIX;
        Path vaultDirPath = workTree.resolve(VAULT_DIR);
        Path vaultFilePath = vaultDirPath.resolve(vaultFileName);

        if (!Files.exists(vaultFilePath)) {
            log.debug("Vault file does not exist for namespace '{}', returning empty map", namespace);
            return new HashMap<>();
        }

        try {
            String jsonContent = Files.readString(vaultFilePath);
            if (jsonContent.trim().isEmpty()) {
                return new HashMap<>();
            }

            return objectMapper.readValue(jsonContent, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse vault file for namespace '{}': {}", namespace, e.getMessage());
            throw VaultException.vaultOperationFailed("Failed to parse vault file: " + e.getMessage());
        }
    }

    private void saveVaultSecrets(String namespace, Map<String, String> secrets, Git git) throws IOException {
        Path workTree = git.getRepository().getWorkTree().toPath();
        String vaultFileName = namespace + VAULT_FILE_SUFFIX;
        Path vaultDirPath = workTree.resolve(VAULT_DIR);
        Path vaultFilePath = vaultDirPath.resolve(vaultFileName);

        try {
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(secrets);
            Files.writeString(vaultFilePath, jsonContent);
        } catch (Exception e) {
            log.error("Failed to save vault file for namespace '{}': {}", namespace, e.getMessage());
            throw VaultException.vaultOperationFailed("Failed to save vault file: " + e.getMessage());
        }
    }


}