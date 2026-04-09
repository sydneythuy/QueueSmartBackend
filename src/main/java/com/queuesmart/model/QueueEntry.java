package com.queuesmart.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntry {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private Queue queue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserCredential user;

    @Column(nullable = false)
    private int position;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private EntryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", length = 10, nullable = false)
    private Service.PriorityLevel priorityLevel;

    @Column(name = "estimated_wait_minutes")
    private int estimatedWaitMinutes;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
    }

    public enum EntryStatus { WAITING, SERVING, SERVED, LEFT }
}
