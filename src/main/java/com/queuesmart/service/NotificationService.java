package com.queuesmart.service;

import com.queuesmart.model.Notification;
import com.queuesmart.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification sendQueueJoined(String userId, String serviceName, int position) {
        String message = String.format("You have joined the queue for %s. Your position: #%d", serviceName, position);
        return save(userId, message, Notification.NotificationType.QUEUE_JOINED);
    }

    public Notification sendAlmostYourTurn(String userId, String serviceName, int position) {
        String message = String.format("Almost your turn for %s — you are now #%d in queue!", serviceName, position);
        return save(userId, message, Notification.NotificationType.ALMOST_YOUR_TURN);
    }

    public Notification sendYourTurn(String userId, String serviceName) {
        String message = String.format("It's your turn! Please proceed to %s.", serviceName);
        return save(userId, message, Notification.NotificationType.YOUR_TURN);
    }

    public Notification sendQueueLeft(String userId, String serviceName) {
        String message = String.format("You have left the queue for %s.", serviceName);
        return save(userId, message, Notification.NotificationType.QUEUE_LEFT);
    }

    public Notification sendStatusChanged(String userId, String serviceName, String status) {
        String message = String.format("Queue status for %s has changed: %s", serviceName, status);
        return save(userId, message, Notification.NotificationType.QUEUE_STATUS_CHANGED);
    }

    public List<Notification> getNotificationsForUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotificationsForUser(String userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    public int getUnreadCount(String userId) {
        return notificationRepository.countUnread(userId);
    }

    public void markAsRead(String notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    private Notification save(String userId, String message, Notification.NotificationType type) {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .message(message)
                .type(type)
                .createdAt(LocalDateTime.now())
                .read(false)
                .build();

        log.info("[NOTIFICATION] userId={} type={} msg={}", userId, type, message);
        return notificationRepository.save(notification);
    }
}
