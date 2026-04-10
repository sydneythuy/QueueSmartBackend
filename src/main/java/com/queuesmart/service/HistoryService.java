package com.queuesmart.service;

import com.queuesmart.model.HistoryRecord;
import com.queuesmart.model.QueueEntry;
import com.queuesmart.repository.HistoryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRecordRepository historyRepo;

    @Transactional(readOnly = true)
    public List<HistoryRecord> getUserHistory(String userId) {
        return historyRepo.findByUserIdOrderByJoinedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<HistoryRecord> getAllHistory() {
        return historyRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getUsageStatsByService() {
        Map<String, Long> result = new LinkedHashMap<>();
        historyRepo.countGroupedByServiceName()
                .forEach(row -> result.put((String) row[0], (Long) row[1]));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Double> getAverageWaitByService() {
        Map<String, Double> result = new LinkedHashMap<>();
        historyRepo.avgWaitGroupedByServiceName()
                .forEach(row -> result.put((String) row[0], (Double) row[1]));
        return result;
    }

    @Transactional(readOnly = true)
    public long getTotalServed() {
        return historyRepo.countByFinalStatus(QueueEntry.EntryStatus.SERVED);
    }
}
