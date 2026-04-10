package com.queuesmart.repository;

import com.queuesmart.model.QueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, String> {

    // All WAITING entries for a queue, sorted by priority then join time
    @Query("""
        SELECT e FROM QueueEntry e
        WHERE e.queue.id = :queueId
          AND e.status = com.queuesmart.model.QueueEntry.EntryStatus.WAITING
        ORDER BY
          CASE e.priorityLevel
            WHEN com.queuesmart.model.Service.PriorityLevel.HIGH   THEN 0
            WHEN com.queuesmart.model.Service.PriorityLevel.MEDIUM THEN 1
            ELSE 2
          END ASC,
          e.joinedAt ASC
        """)
    List<QueueEntry> findActiveByQueueIdOrdered(@Param("queueId") String queueId);

    // Check if a user already has a WAITING entry in a specific queue
    Optional<QueueEntry> findByQueueIdAndUserIdAndStatus(
            String queueId, String userId, QueueEntry.EntryStatus status);

    // All entries (any status) for a user — used for history display
    List<QueueEntry> findByUserIdOrderByJoinedAtDesc(String userId);

    // Count waiting entries in a queue
    long countByQueueIdAndStatus(String queueId, QueueEntry.EntryStatus status);
}
