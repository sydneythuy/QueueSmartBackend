package com.queuesmart.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 100, nullable = false, unique = true)
    private String name;

    @Column(length = 500, nullable = false)
    private String description;

    @Column(name = "expected_duration_minutes", nullable = false)
    private int expectedDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", length = 10, nullable = false)
    private PriorityLevel priorityLevel;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by_admin_id", length = 36)
    private String createdByAdminId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum PriorityLevel { LOW, MEDIUM, HIGH }
}
