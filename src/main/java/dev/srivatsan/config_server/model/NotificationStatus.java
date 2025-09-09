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
     * Total number of API calls expected
     */
    private final int totalCount;

    /**
     * Number of API calls that succeeded
     */
    @Builder.Default
    private final int successCount = 0;

    /**
     * Number of API calls that failed permanently
     */
    @Builder.Default
    private final int failureCount = 0;

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
     * Resets the notification for retry - keeps the same ID but resets all tracking data
     */
    public NotificationStatus resetForRetry(int newTotalCount) {
        return NotificationStatus.builder()
                .id(this.id)
                .status(NotificationOperationStatus.IN_PROGRESS)
                .totalCount(newTotalCount)
                .successCount(0)
                .failureCount(0)
                .initiatedTime(LocalDateTime.now())
                .completedTime(null)
                .build();
    }

    /**
     * Marks one API call as successful and updates overall status if all calls are complete
     */
    public NotificationStatus withSuccess() {
        int newSuccessCount = this.successCount + 1;
        NotificationOperationStatus newStatus = determineOverallStatus(newSuccessCount, this.failureCount);
        LocalDateTime completedTime = isComplete(newSuccessCount, this.failureCount) ? LocalDateTime.now() : this.completedTime;
        
        return this.toBuilder()
                .successCount(newSuccessCount)
                .status(newStatus)
                .completedTime(completedTime)
                .build();
    }


    /**
     * Marks one API call as failed and updates overall status if all calls are complete
     */
    public NotificationStatus withFailure() {
        int newFailureCount = this.failureCount + 1;
        NotificationOperationStatus newStatus = determineOverallStatus(this.successCount, newFailureCount);
        LocalDateTime completedTime = isComplete(this.successCount, newFailureCount) ? LocalDateTime.now() : this.completedTime;
        
        return this.toBuilder()
                .failureCount(newFailureCount)
                .status(newStatus)
                .completedTime(completedTime)
                .build();
    }

    /**
     * Determines the overall status based on success and failure counts
     */
    private NotificationOperationStatus determineOverallStatus(int successCount, int failureCount) {
        int completedCount = successCount + failureCount;
        
        if (completedCount < totalCount) {
            return NotificationOperationStatus.IN_PROGRESS;
        } else if (successCount == totalCount) {
            return NotificationOperationStatus.SUCCESS;
        } else {
            return NotificationOperationStatus.FAILED;
        }
    }

    /**
     * Checks if all API calls are complete (either success or failure)
     */
    private boolean isComplete(int successCount, int failureCount) {
        return (successCount + failureCount) >= totalCount;
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
        return String.format("NotificationStatus{id='%s', status=%s, success=%d/%d, failed=%d}",
                id, status, successCount, totalCount, failureCount);
    }
}