package com.github.config_server.model;


import com.github.config_server.constants.NotificationStatus;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

/*ToDo: THis should be deprecated*/

/**
 * Simple notification tracking for API call status
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
     * The timestamp when the notification was initiated
     */
    @Builder.Default
    private final LocalDateTime initiatedTime = LocalDateTime.now();

    /**
     * Creates initial notification with commit ID
     */
    public static Notification createInitial(String commitId) {
        return Notification.builder()
                .id(commitId)
                .status(NotificationStatus.IN_PROGRESS)
                .build();
    }

    /**
     * Marks API call as successful
     */
    public Notification withSuccess() {
        return this.toBuilder()
                .status(NotificationStatus.SUCCESS)
                .build();
    }

    /**
     * Marks API call as failed
     */
    public Notification withFailure() {
        return this.toBuilder()
                .status(NotificationStatus.FAILED)
                .build();
    }

}