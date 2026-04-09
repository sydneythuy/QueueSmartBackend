package com.queuesmart.dto;

import com.queuesmart.model.QueueEntry;
import com.queuesmart.model.Service;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
        private QueueEntry.EntryStatus status;
        private Service.PriorityLevel priorityLevel;
    }

    @Data
    public static class QueueStatusResponse {
        private String serviceId;
        private String serviceName;
        private int totalWaiting;
        private int estimatedWaitForNew;
        private List<QueueEntryResponse> entries;
    }
}
