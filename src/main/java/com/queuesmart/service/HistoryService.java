package com.queuesmart.service;

import com.queuesmart.model.HistoryRecord;
import com.queuesmart.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;

    public List<HistoryRecord> getUserHistory(String userId) {
        return historyRepository.findByUserId(userId);
    }

    public List<HistoryRecord> getAllHistory() {
        return historyRepository.findAll();
    }

    // Admin: usage stats per service
    public Map<String, Long> getUsageStatsByService() {
        return historyRepository.findAll().stream()
                .collect(Collectors.groupingBy(HistoryRecord::getServiceName, Collectors.counting()));
    }

    // Admin: average wait time per service
    public Map<String, Double> getAverageWaitByService() {
        return historyRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        HistoryRecord::getServiceName,
                        Collectors.averagingInt(HistoryRecord::getWaitedMinutes)));
    }

    public int getTotalServed() {
        return (int) historyRepository.findAll().stream()
                .filter(r -> r.getFinalStatus() == com.queuesmart.model.QueueEntry.QueueStatus.SERVED)
                .count();
    }


    /**
     * Returns the N most recent history records for a user, sorted by completion time.
     */
    public java.util.List<HistoryRecord> getRecentHistory(String userId, int limit) {
        return historyRepository.findByUserId(userId).stream()
                .sorted((a, b) -> b.getCompletedAt().compareTo(a.getCompletedAt()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Returns total number of times a user has joined a queue.
     */
    public long countUserHistory(String userId) {
        return historyRepository.findByUserId(userId).size();
    }

    /**
     * Returns history records filtered by final status (SERVED or LEFT).
     */
    public java.util.List<HistoryRecord> getHistoryByStatus(String userId,
            com.queuesmart.model.QueueEntry.QueueStatus status) {
        return historyRepository.findByUserId(userId).stream()
                .filter(r -> r.getFinalStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Returns the service the user has visited most frequently.
     */
    public String getMostVisitedService(String userId) {
        return historyRepository.findByUserId(userId).stream()
                .collect(Collectors.groupingBy(HistoryRecord::getServiceName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
    }

}
