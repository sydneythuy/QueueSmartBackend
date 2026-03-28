package com.queuesmart.repository;

import com.queuesmart.model.Notification;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class NotificationRepository {

    private final Map<String, Notification> notifications = new ConcurrentHashMap<>();

    public Notification save(Notification notification) {
        notifications.put(notification.getId(), notification);
        return notification;
    }

    public List<Notification> findByUserId(String userId) {
        return notifications.values().stream()
                .filter(n -> n.getUserId().equals(userId))
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Notification> findUnreadByUserId(String userId) {
        return notifications.values().stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Notification> findById(String id) {
        return Optional.ofNullable(notifications.get(id));
    }

    public void markAsRead(String id) {
        Notification n = notifications.get(id);
        if (n != null) {
            n.setRead(true);
        }
    }

    public int countUnread(String userId) {
        return (int) notifications.values().stream()
                .filter(n -> n.getUserId().equals(userId) && !n.isRead())
                .count();
    }
}
