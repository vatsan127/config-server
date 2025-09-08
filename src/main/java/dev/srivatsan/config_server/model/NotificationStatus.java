package dev.srivatsan.config_server.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Optimized immutable representation of an API call notification status.
 * Tracks execution status, retry count, and timing information for configuration management operations.
 * Uses builder pattern for flexible construction and improved memory efficiency.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class NotificationStatus {

    /**
     * The timestamp when the API call was triggered
     */
    @Builder.Default
    private final LocalDateTime triggeredAt = LocalDateTime.now();

    /**
     * The application name from the payload that triggered this notification
     */
    private final String appName;

    /**
     * The number of retry attempts for this operation
     */
    @Builder.Default
    private final int retryCount = 0;

    /**
     * The current status of the operation
     */
    @Builder.Default
    private final Status status = Status.inprogress;

    /**
     * The operation type that was performed
     */
    private final String operation;

    /**
     * The namespace where this operation occurred
     */
    private final String namespace;

    /**
     * Additional error message if the operation failed (nullable)
     */
    private final String errorMessage;

    /**
     * The commit ID associated with this operation if applicable (nullable)
     */
    private final String commitId;

    /**
     * Enumeration of possible notification statuses
     */
    public enum Status {
        /**
         * Operation completed successfully
         */
        success,

        /**
         * Operation is currently being retried
         */
        inprogress,

        /**
         * Operation failed after all retry attempts
         */
        failed
    }

    /**
     * Creates a new notification for a successful operation
     */
    public static NotificationStatus success(String appName, String operation, String namespace, String commitId) {
        return NotificationStatus.builder()
                .appName(appName)
                .operation(operation)
                .namespace(namespace)
                .status(Status.success)
                .commitId(commitId)
                .retryCount(0)
                .build();
    }

    /**
     * Creates a new notification for a failed operation
     */
    public static NotificationStatus failed(String appName, String operation, String namespace, String errorMessage, int retryCount) {
        return NotificationStatus.builder()
                .appName(appName)
                .operation(operation)
                .namespace(namespace)
                .status(Status.failed)
                .errorMessage(errorMessage)
                .retryCount(retryCount)
                .build();
    }

    /**
     * Creates a new notification for an in-progress operation
     */
    public static NotificationStatus inProgress(String appName, String operation, String namespace, int retryCount) {
        return NotificationStatus.builder()
                .appName(appName)
                .operation(operation)
                .namespace(namespace)
                .status(Status.inprogress)
                .retryCount(retryCount)
                .build();
    }

    /**
     * Creates a new notification for an in-progress operation with error message
     */
    public static NotificationStatus inProgress(String appName, String operation, String namespace, int retryCount, String errorMessage) {
        return NotificationStatus.builder()
                .appName(appName)
                .operation(operation)
                .namespace(namespace)
                .status(Status.inprogress)
                .retryCount(retryCount)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Updates the retry count and returns a new instance (immutable pattern)
     */
    public NotificationStatus withRetryCount(int newRetryCount) {
        return this.toBuilder().retryCount(newRetryCount).build();
    }

    /**
     * Updates the status and returns a new instance
     */
    public NotificationStatus withStatus(Status newStatus) {
        return this.toBuilder().status(newStatus).build();
    }

    /**
     * Updates status and error message, returns a new instance
     */
    public NotificationStatus withStatusAndError(Status newStatus, String newErrorMessage) {
        return this.toBuilder()
                .status(newStatus)
                .errorMessage(newErrorMessage)
                .build();
    }

    /**
     * Updates to success status with commit ID
     */
    public NotificationStatus withSuccess(String commitId) {
        return this.toBuilder()
                .status(Status.success)
                .commitId(commitId)
                .errorMessage(null)
                .build();
    }

    /**
     * Efficient equality check based on key fields
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationStatus that)) return false;
        return Objects.equals(appName, that.appName) &&
               Objects.equals(operation, that.operation) &&
               Objects.equals(namespace, that.namespace) &&
               Objects.equals(triggeredAt, that.triggeredAt);
    }

    /**
     * Optimized hash code for efficient storage and lookup
     */
    @Override
    public int hashCode() {
        return Objects.hash(appName, operation, namespace, triggeredAt);
    }

    /**
     * Compact string representation for logging
     */
    @Override
    public String toString() {
        return String.format("NotificationStatus{app='%s', op='%s', ns='%s', status=%s, retries=%d}",
                appName, operation, namespace, status, retryCount);
    }
}