package dev.srivatsan.config_server.model;

import dev.srivatsan.config_server.constants.NotificationStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/**
 * refresh notify api status with essential tracking fields.
 */
@Data
@Builder(toBuilder = true)
@Jacksonized
public class Notification {

    /**
     * The commit ID - unique identifier for this notification
     */
    private final String id;

    /**
     * The current status of the notification
     */
    @Builder.Default
    private final NotificationStatus status = NotificationStatus.IN_PROGRESS;


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
    public static Notification createInitial(String commitId, int totalCount) {
        return Notification.builder()
                .id(commitId)
                .totalCount(totalCount)
                .build();
    }

    /**
     * Resets the notification for retry - keeps the same ID but resets all tracking data
     */
    public Notification resetForRetry(int newTotalCount) {
        return Notification.builder()
                .id(this.id)
                .status(NotificationStatus.IN_PROGRESS)
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
    public Notification withSuccess() {
        int newSuccessCount = this.successCount + 1;
        NotificationStatus newStatus = determineOverallStatus(newSuccessCount, this.failureCount);
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
    public Notification withFailure() {
        int newFailureCount = this.failureCount + 1;
        NotificationStatus newStatus = determineOverallStatus(this.successCount, newFailureCount);
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
    private NotificationStatus determineOverallStatus(int successCount, int failureCount) {
        int completedCount = successCount + failureCount;

        if (completedCount < totalCount) {
            return NotificationStatus.IN_PROGRESS;
        } else if (successCount == totalCount) {
            return NotificationStatus.SUCCESS;
        } else {
            return NotificationStatus.FAILED;
        }
    }

    /**
     * Checks if all API calls are complete (either success or failure)
     */
    private boolean isComplete(int successCount, int failureCount) {
        return (successCount + failureCount) >= totalCount;
    }

}