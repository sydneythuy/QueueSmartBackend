package com.queuesmart.dto;

import com.queuesmart.model.QueueEntry;
import com.queuesmart.model.Service;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

public class QueueDto {

    @Data
    public static class JoinQueueRequest {
        @NotBlank(message = "Service ID is required")
        private String serviceId;

        private Service.PriorityLevel priorityLevel; // optional override
    }

    @Data
    public static class QueueEntryResponse {
        private String id;
        private String userId;
        private String username;
        private String serviceId;
        private int position;
        private int estimatedWaitMinutes;
        private LocalDateTime joinedAt;
        private QueueEntry.QueueStatus status;
        private Service.PriorityLevel priorityLevel;

        public QueueEntryResponse(QueueEntry entry) {
            this.id = entry.getId();
            this.userId = entry.getUserId();
            this.username = entry.getUsername();
            this.serviceId = entry.getServiceId();
            this.position = entry.getPosition();
            this.estimatedWaitMinutes = entry.getEstimatedWaitMinutes();
            this.joinedAt = entry.getJoinedAt();
            this.status = entry.getStatus();
            this.priorityLevel = entry.getPriorityLevel();
        }
    }

    @Data
    public static class QueueStatusResponse {
        private String serviceId;
        private String serviceName;
        private int totalWaiting;
        private int estimatedWaitForNew;
        private java.util.List<QueueEntryResponse> entries;
    }
}
