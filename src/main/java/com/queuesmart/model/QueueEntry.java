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
public class QueueEntry {
    private String id;
    private String userId;
    private String username;
    private String serviceId;
    private int position;
    private int estimatedWaitMinutes;
    private LocalDateTime joinedAt;
    private QueueStatus status;
    private Service.PriorityLevel priorityLevel;

    public enum QueueStatus {
        WAITING, SERVING, SERVED, LEFT
    }
}
