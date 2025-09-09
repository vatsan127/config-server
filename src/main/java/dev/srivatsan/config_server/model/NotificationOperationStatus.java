package dev.srivatsan.config_server.model;

/**
 * Status enumeration for tracking notification operation lifecycle across multiple API calls.
 * Represents the aggregate status of all API calls within a single notification batch.
 */
public enum NotificationOperationStatus {
    
    /**
     * Notification batch is currently being processed.
     * Some API calls may be pending, in progress, or retrying.
     * Condition: successfulApiCallCount < totalApiCallCount
     */
    IN_PROGRESS,
    
    /**
     * All API calls in the notification batch completed successfully.
     * May have required retries, but ultimately all endpoints were reached.
     * Condition: successfulApiCallCount == totalApiCallCount
     */
    SUCCESS,
    
    /**
     * One or more API calls failed permanently after exhausting all retry attempts.
     * Some calls may have succeeded, but the overall notification batch is considered failed.
     * Condition: Some URLs failed after max retries
     */
    FAILED
}