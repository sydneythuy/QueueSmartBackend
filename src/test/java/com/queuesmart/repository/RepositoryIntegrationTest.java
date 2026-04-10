package com.queuesmart.service;

import com.queuesmart.model.Notification;
import com.queuesmart.model.UserCredential;
import com.queuesmart.repository.NotificationRepository;
import com.queuesmart.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository  notificationRepository;
    private final UserCredentialRepository credentialRepository;

    @Transactional
    public Notification sendQueueJoined(String userId, String serviceName, int position) {
        return save(userId,
                String.format("You joined the queue for %s. Your position: #%d", serviceName, position),
                Notification.NotificationType.QUEUE_JOINED);
    }

    @Transactional
    public Notification sendAlmostYourTurn(String userId, String serviceName, int position) {
        return save(userId,
                String.format("Almost your turn for %s — you are now #%d!", serviceName, position),
                Notification.NotificationType.ALMOST_YOUR_TURN);
    }

    @Transactional
    public Notification sendYourTurn(String userId, String serviceName) {
        return save(userId,
                String.format("It's your turn! Please proceed to %s.", serviceName),
                Notification.NotificationType.YOUR_TURN);
    }

    @Transactional
    public Notification sendQueueLeft(String userId, String serviceName) {
        return save(userId,
                String.format("You have left the queue for %s.", serviceName),
                Notification.NotificationType.QUEUE_LEFT);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForUser(String userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(String userId) {
        return (int) notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    // ── private ───────────────────────────────────────────────
    private Notification save(String userId, String message, Notification.NotificationType type) {
        UserCredential user = credentialRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Notification n = Notification.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .message(message)
                .type(type)
                .read(false)
                .build();

        log.info("[NOTIFICATION] userId={} type={} msg={}", userId, type, message);
        return notificationRepository.save(n);
    }
}



