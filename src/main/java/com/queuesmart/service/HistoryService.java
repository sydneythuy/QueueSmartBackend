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
}
