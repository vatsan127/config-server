package dev.srivatsan.config_server.service.vault;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.srivatsan.config_server.config.ApplicationConfig;
import dev.srivatsan.config_server.exception.VaultException;
import dev.srivatsan.config_server.service.encryption.EncryptionService;
import dev.srivatsan.config_server.service.operation.GitOperationService;
import dev.srivatsan.config_server.service.util.UtilService;
import dev.srivatsan.config_server.service.validation.ValidationService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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
                
                // Decrypt values in-place for better performance
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
    @CacheEvict(value = {"vault-secrets", "vault-history"}, key = "#namespace")
    public void updateVault(String namespace, Map<String, String> secrets, String email, String commitMessage) {
        validationService.validateNamespace(namespace);
        validationService.validateEmail(email);
        validationService.validateCommitMessage(commitMessage);
        
        if (secrets == null || secrets.isEmpty()) {
            throw VaultException.vaultOperationFailed("Secrets map cannot be null or empty");
        }

        gitOperationService.executeGitVoidOperation(namespace, git -> {
            try {

                // Replace all secrets completely (don't merge with existing ones)
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
            
            return objectMapper.readValue(jsonContent, new TypeReference<>() {});
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
            // Vault directory is created during namespace initialization
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(secrets);
            Files.writeString(vaultFilePath, jsonContent);
        } catch (Exception e) {
            log.error("Failed to save vault file for namespace '{}': {}", namespace, e.getMessage());
            throw VaultException.vaultOperationFailed("Failed to save vault file: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getVaultChanges(String commitId, String namespace) {
        validationService.validateCommitId(commitId);
        validationService.validateNamespace(namespace);

        return gitOperationService.executeGitOperation(namespace, git -> {
            Map<String, Object> result = new HashMap<>();

            Repository repository = git.getRepository();
            try (RevWalk revWalk = new RevWalk(repository)) {

                RevCommit commit = revWalk.parseCommit(repository.resolve(commitId));
                result.put("commitId", commit.getName());
                result.put("commitMessage", commit.getFullMessage());
                result.put("author", commit.getAuthorIdent().getName());
                result.put("commitTime", new Date(commit.getCommitTime() * 1000L));

                // Use DiffFormatter to show changes (equivalent to 'git show')
                try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                     DiffFormatter df = new DiffFormatter(out)) {
                    df.setRepository(repository);
                    
                    // Get the tree changes for this commit
                    var diffs = df.scan(commit.getParentCount() > 0 ? commit.getParent(0) : null, commit);
                    for (var diff : diffs) {
                        df.format(diff);
                    }
                    
                    String rawDiff = out.toString();
                    String cleanedDiff = filterGitDiffMetadata(rawDiff);
                    result.put("changes", cleanedDiff);
                }
            } catch (Exception e) {
                log.error("Failed to get vault changes for commit '{}' in namespace '{}': {}", commitId, namespace, e.getMessage());
                throw VaultException.vaultOperationFailed("Failed to get vault changes: " + e.getMessage());
            }

            return result;
        });
    }

    /**
     * Filters out git diff metadata headers while preserving content and hunk information.
     */
    private String filterGitDiffMetadata(String rawDiff) {
        if (rawDiff == null || rawDiff.trim().isEmpty()) {
            return rawDiff;
        }

        StringBuilder cleanedDiff = new StringBuilder();
        String[] lines = rawDiff.split("\\r?\\n");

        for (String line : lines) {
            if (!line.startsWith("diff --git") &&
                    !line.startsWith("index ") &&
                    !line.startsWith("--- ") &&
                    !line.startsWith("+++ ") &&
                    !line.startsWith("new file mode") &&
                    !line.startsWith("deleted file mode") &&
                    !line.startsWith("similarity index") &&
                    !line.startsWith("rename from") &&
                    !line.startsWith("rename to") &&
                    !line.startsWith("copy from") &&
                    !line.startsWith("copy to")) {
                cleanedDiff.append(line).append("\n");
            }
        }

        return cleanedDiff.toString().trim();
    }
}