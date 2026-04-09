package com.queuesmart.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "history_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryRecord {

    @Id
    @Column(length = 36)
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserCredential user;

    @Column(name = "service_id", length = 36)
    private String serviceId;

    @Column(name = "service_name", length = 100, nullable = false)
    private String serviceName;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_status", length = 10, nullable = false)
    private QueueEntry.EntryStatus finalStatus;

    @Column(name = "waited_minutes", nullable = false)
    private int waitedMinutes;

    // Convenience getters for JSON serialisation
    public String getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getUsername() {
        return user != null ? user.getEmail() : null;
    }
}
