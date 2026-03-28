package com.queuesmart.repository;

import com.queuesmart.model.QueueEntry;
import com.queuesmart.model.Service;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class QueueRepository {

    private final Map<String, QueueEntry> entries = new ConcurrentHashMap<>();

    public QueueEntry save(QueueEntry entry) {
        entries.put(entry.getId(), entry);
        return entry;
    }

    public Optional<QueueEntry> findById(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    public List<QueueEntry> findByServiceId(String serviceId) {
        return entries.values().stream()
                .filter(e -> e.getServiceId().equals(serviceId))
                .collect(Collectors.toList());
    }

    public List<QueueEntry> findActiveByServiceId(String serviceId) {
        return entries.values().stream()
                .filter(e -> e.getServiceId().equals(serviceId)
                        && e.getStatus() == QueueEntry.QueueStatus.WAITING)
                .sorted(Comparator
                        .comparing((QueueEntry e) -> priorityOrder(e.getPriorityLevel()))
                        .thenComparing(QueueEntry::getJoinedAt))
                .collect(Collectors.toList());
    }

    public Optional<QueueEntry> findActiveByUserId(String userId) {
        return entries.values().stream()
                .filter(e -> e.getUserId().equals(userId)
                        && e.getStatus() == QueueEntry.QueueStatus.WAITING)
                .findFirst();
    }

    public Optional<QueueEntry> findByUserIdAndServiceId(String userId, String serviceId) {
        return entries.values().stream()
                .filter(e -> e.getUserId().equals(userId)
                        && e.getServiceId().equals(serviceId)
                        && e.getStatus() == QueueEntry.QueueStatus.WAITING)
                .findFirst();
    }

    public List<QueueEntry> findByUserId(String userId) {
        return entries.values().stream()
                .filter(e -> e.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public void delete(String id) {
        entries.remove(id);
    }

    public int countActiveByServiceId(String serviceId) {
        return (int) entries.values().stream()
                .filter(e -> e.getServiceId().equals(serviceId)
                        && e.getStatus() == QueueEntry.QueueStatus.WAITING)
                .count();
    }

    private int priorityOrder(Service.PriorityLevel level) {
        if (level == null) return 2;
        return switch (level) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
