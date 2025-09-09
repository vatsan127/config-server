package dev.srivatsan.config_server.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.Objects;
/**
 * Simplified notification status with essential tracking fields.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class NotificationStatus {

    /**
     * The commit ID - unique identifier for this notification
     */
    private final String id;

    /**
     * The current status of the notification
     */
    @Builder.Default
    private final NotificationOperationStatus status = NotificationOperationStatus.IN_PROGRESS;

    /**
     * Total number of retry attempts
     */
    @Builder.Default
    private final int retryCount = 0;

    /**
     * Total number of API calls expected
     */
    private final int totalCount;

    /**
     * The timestamp when the notification was initiated
     */
    @Builder.Default
    private final LocalDateTime initiatedTime = LocalDateTime.now();

    /**
     * The timestamp when successfully completed (null if not completed)
     */
    private final LocalDateTime completedTime;

    /**
     * Creates initial notification with commit ID
     */
    public static NotificationStatus createInitial(String commitId, int totalCount) {
        return NotificationStatus.builder()
                .id(commitId)
                .totalCount(totalCount)
                .build();
    }

    /**
     * Marks notification as successfully completed
     */
    public NotificationStatus withSuccess() {
        return this.toBuilder()
                .status(NotificationOperationStatus.SUCCESS)
                .completedTime(LocalDateTime.now())
                .build();
    }

    /**
     * Updates notification after a retry attempt
     */
    public NotificationStatus withRetry() {
        return this.toBuilder()
                .retryCount(this.retryCount + 1)
                .build();
    }

    /**
     * Marks notification as failed
     */
    public NotificationStatus withFailure() {
        return this.toBuilder()
                .status(NotificationOperationStatus.FAILED)
                .completedTime(LocalDateTime.now())
                .build();
    }

    /**
     * Equality check based on id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationStatus that)) return false;
        return Objects.equals(id, that.id);
    }

    /**
     * Hash code based on id
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Compact string representation for logging
     */
    @Override
    public String toString() {
        return String.format("NotificationStatus{id='%s', status=%s, retries=%d, total=%d}",
                id, status, retryCount, totalCount);
    }
}