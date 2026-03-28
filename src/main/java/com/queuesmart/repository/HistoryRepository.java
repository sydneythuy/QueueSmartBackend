package com.queuesmart.repository;

import com.queuesmart.model.HistoryRecord;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class HistoryRepository {

    private final Map<String, HistoryRecord> records = new ConcurrentHashMap<>();

    public HistoryRecord save(HistoryRecord record) {
        records.put(record.getId(), record);
        return record;
    }

    public List<HistoryRecord> findByUserId(String userId) {
        return records.values().stream()
                .filter(r -> r.getUserId().equals(userId))
                .sorted(Comparator.comparing(HistoryRecord::getJoinedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<HistoryRecord> findAll() {
        return new ArrayList<>(records.values());
    }

    public List<HistoryRecord> findByServiceId(String serviceId) {
        return records.values().stream()
                .filter(r -> r.getServiceId().equals(serviceId))
                .collect(Collectors.toList());
    }

    public long countByServiceId(String serviceId) {
        return records.values().stream()
                .filter(r -> r.getServiceId().equals(serviceId))
                .count();
    }

    public int count() {
        return records.size();
    }
}
