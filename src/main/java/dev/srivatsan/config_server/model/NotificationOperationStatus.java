package dev.srivatsan.config_server.model;

/**
 * Status enumeration for tracking notification operation lifecycle across multiple API calls.
 * Represents the aggregate status of all API calls within a single notification batch.
 */
public enum NotificationOperationStatus {
    
    /**
     * Notification batch is currently being processed.
     * Some API calls may be pending or in progress.
     * Condition: (successfulApiCallCount + failedApiCallCount) < totalApiCallCount
     */
    IN_PROGRESS,
    
    /**
     * All API calls in the notification batch completed successfully.
     * All endpoints were successfully reached.
     * Condition: successfulApiCallCount == totalApiCallCount
     */
    SUCCESS,
    
    /**
     * One or more API calls failed permanently.
     * Some calls may have succeeded, but the overall notification batch is considered failed.
     * Condition: failedApiCallCount > 0 && (successfulApiCallCount + failedApiCallCount) == totalApiCallCount
     */
    FAILED
}