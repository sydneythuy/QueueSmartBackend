package com.queuesmart.repository;

import com.queuesmart.model.HistoryRecord;
import com.queuesmart.model.QueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, String> {
    List<HistoryRecord> findByUser_IdOrderByJoinedAtDesc(String userId);
    List<HistoryRecord> findByServiceId(String serviceId);
    long countByFinalStatus(QueueEntry.EntryStatus status);

    @Query("SELECT h.serviceName, COUNT(h) FROM HistoryRecord h GROUP BY h.serviceName")
    List<Object[]> countGroupedByServiceName();

    @Query("SELECT h.serviceName, AVG(h.waitedMinutes) FROM HistoryRecord h GROUP BY h.serviceName")
    List<Object[]> avgWaitGroupedByServiceName();
}
