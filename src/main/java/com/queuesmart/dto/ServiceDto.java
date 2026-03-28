package com.queuesmart.dto;

import com.queuesmart.model.Service;
import jakarta.validation.constraints.*;
import lombok.Data;

public class ServiceDto {

    @Data
    public static class CreateServiceRequest {
        @NotBlank(message = "Service name is required")
        @Size(min = 2, max = 100, message = "Service name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;

        @Min(value = 1, message = "Expected duration must be at least 1 minute")
        @Max(value = 480, message = "Expected duration must not exceed 480 minutes")
        private int expectedDurationMinutes;

        @NotNull(message = "Priority level is required")
        private Service.PriorityLevel priorityLevel;
    }

    @Data
    public static class UpdateServiceRequest {
        @Size(min = 2, max = 100, message = "Service name must be between 2 and 100 characters")
        private String name;

        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;

        @Min(value = 1, message = "Expected duration must be at least 1 minute")
        @Max(value = 480, message = "Expected duration must not exceed 480 minutes")
        private Integer expectedDurationMinutes;

        private Service.PriorityLevel priorityLevel;
        private Boolean active;
    }

    @Data
    public static class ServiceResponse {
        private String id;
        private String name;
        private String description;
        private int expectedDurationMinutes;
        private Service.PriorityLevel priorityLevel;
        private boolean active;
        private int currentQueueSize;

        public ServiceResponse(Service service, int queueSize) {
            this.id = service.getId();
            this.name = service.getName();
            this.description = service.getDescription();
            this.expectedDurationMinutes = service.getExpectedDurationMinutes();
            this.priorityLevel = service.getPriorityLevel();
            this.active = service.isActive();
            this.currentQueueSize = queueSize;
        }
    }
}
