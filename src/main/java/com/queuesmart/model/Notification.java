package com.queuesmart.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @Column(length = 36)
    private String id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserCredential user;

    @Column(length = 500, nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private NotificationType type;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Convenience getter — serialised in JSON so frontend gets userId directly
    public String getUserId() {
        return user != null ? user.getId() : null;
    }

    public enum NotificationType {
        QUEUE_JOINED, ALMOST_YOUR_TURN, YOUR_TURN, QUEUE_LEFT, QUEUE_STATUS_CHANGED
    }
}
