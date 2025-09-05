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
    private static final String VAULT_DIR = ".vault";
    private static final String VAULT_FILE_SUFFIX = "-vault.json";

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
    @Cacheable(value = "vault-secrets", key = "#namespace")
    public Map<String, String> getVault(String namespace) {
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
                log.error("Failed to get vault from namespace '{}': {}", namespace, e.getMessage());
                if (e instanceof VaultException) {
                    throw e;
                }
                throw VaultException.vaultOperationFailed("Failed to get vault: " + e.getMessage());
            }
        });
    }

    @Override
    @CacheEvict(value = "vault-secrets", key = "#namespace")
    public void updateVault(String namespace, Map<String, String> secrets, String email, String commitMessage) {
        validationService.validateNamespace(namespace);
        validationService.validateEmail(email);
        validationService.validateCommitMessage(commitMessage);
        
        if (secrets == null || secrets.isEmpty()) {
            throw VaultException.vaultOperationFailed("Secrets map cannot be null or empty");
        }

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            try {
                // Load existing secrets and merge with new ones
                Map<String, String> existingSecrets = loadVaultSecrets(namespace, git);
                
                // Encrypt and merge new secrets
                for (Map.Entry<String, String> entry : secrets.entrySet()) {
                    String encryptedValue = encryptionService.encrypt(entry.getValue(), namespace);
                    existingSecrets.put(entry.getKey(), encryptedValue);
                }

                saveVaultSecrets(namespace, existingSecrets, git);
                
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
    }

    @Override
    @Cacheable(value = "vault-history", key = "#namespace")
    public Map<String, Object> getVaultHistory(String namespace) {
        validationService.validateNamespace(namespace);

        return gitOperationService.executeGitOperation(namespace, git -> {
            try {
                String vaultFileName = namespace + VAULT_FILE_SUFFIX;
                String vaultFilePath = VAULT_DIR + "/" + vaultFileName;
                
                var logCommand = git.log()
                        .setMaxCount(applicationConfig.getCommitHistorySize())
                        .add(git.getRepository().resolve(HEAD))
                        .addPath(vaultFilePath);

                List<Map<String, Object>> commits = new ArrayList<>();
                for (RevCommit commit : logCommand.call()) {
                    Map<String, Object> commitInfo = utilService.formatCommitInfo(commit);
                    commitInfo.put("commitMessage", commit.getShortMessage());
                    commits.add(commitInfo);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("namespace", namespace);
                result.put("vaultFile", vaultFilePath);
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
            
            return objectMapper.readValue(jsonContent, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse vault file for namespace '{}': {}", namespace, e.getMessage());
            throw VaultException.vaultOperationFailed("Failed to parse vault file: " + e.getMessage());
        }
    }

    private void saveVaultSecrets(String namespace, Map<String, String> secrets, org.eclipse.jgit.api.Git git) throws IOException {
        Path workTree = git.getRepository().getWorkTree().toPath();
        String vaultFileName = namespace + VAULT_FILE_SUFFIX;
        Path vaultDirPath = workTree.resolve(VAULT_DIR);
        Path vaultFilePath = vaultDirPath.resolve(vaultFileName);

        try {
            // Create vault directory if it doesn't exist
            if (!Files.exists(vaultDirPath)) {
                Files.createDirectories(vaultDirPath);
                log.info("Created vault directory: {}", vaultDirPath);
            }
            
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(secrets);
            Files.writeString(vaultFilePath, jsonContent);
        } catch (Exception e) {
            log.error("Failed to save vault file for namespace '{}': {}", namespace, e.getMessage());
            throw VaultException.vaultOperationFailed("Failed to save vault file: " + e.getMessage());
        }
    }
}