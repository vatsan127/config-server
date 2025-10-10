package com.github.config_server.exception;

public class GitOperationException extends ConfigServerException {

    public static final String GIT_INIT_FAILED = "GIT_INIT_FAILED";
    public static final String GIT_COMMIT_FAILED = "GIT_COMMIT_FAILED";
    public static final String GIT_LOG_FAILED = "GIT_LOG_FAILED";
    public static final String GIT_DIFF_FAILED = "GIT_DIFF_FAILED";
    public static final String GIT_REPOSITORY_ACCESS_FAILED = "GIT_REPOSITORY_ACCESS_FAILED";

    public GitOperationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public GitOperationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static GitOperationException initFailed(String namespace, Throwable cause) {
        return new GitOperationException(GIT_INIT_FAILED,
                "Failed to initialize Git repository for namespace: " + namespace, cause);
    }

    public static GitOperationException commitFailed(String filePath, Throwable cause) {
        return new GitOperationException(GIT_COMMIT_FAILED,
                "Failed to commit changes for file: " + filePath, cause);
    }

    public static GitOperationException logFailed(String filePath, Throwable cause) {
        return new GitOperationException(GIT_LOG_FAILED,
                "Failed to retrieve Git log for file: " + filePath, cause);
    }

    public static GitOperationException diffFailed(String commitId, Throwable cause) {
        return new GitOperationException(GIT_DIFF_FAILED,
                "Failed to retrieve diff for commit: " + commitId, cause);
    }

    public static GitOperationException repositoryAccessFailed(String namespace, Throwable cause) {
        return new GitOperationException(GIT_REPOSITORY_ACCESS_FAILED,
                "Failed to access Git repository for namespace: " + namespace, cause);
    }

    public static GitOperationException operationFailed(String namespace, Throwable cause) {
        return new GitOperationException(GIT_REPOSITORY_ACCESS_FAILED,
                "Git operation failed for namespace: " + namespace, cause);
    }
}