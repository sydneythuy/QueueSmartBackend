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
public class HistoryRecord {
    private String id;
    private String userId;
    private String username;
    private String serviceId;
    private String serviceName;
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;
    private QueueEntry.QueueStatus finalStatus;
    private int waitedMinutes;
}
