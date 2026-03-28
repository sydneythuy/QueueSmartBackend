package com.queuesmart.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String id;
    private String userId;
    private String message;
    private NotificationType type;
    private LocalDateTime createdAt;
    private boolean read;

    public enum NotificationType {
        QUEUE_JOINED,
        ALMOST_YOUR_TURN,
        YOUR_TURN,
        QUEUE_LEFT,
        QUEUE_STATUS_CHANGED
    }
}
