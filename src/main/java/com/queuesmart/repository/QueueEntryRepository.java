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
        AND e.status = 'WAITING'
      ORDER BY
        CASE e.priorityLevel
          WHEN 'HIGH'   THEN 0
          WHEN 'MEDIUM' THEN 1
          ELSE 2
        END ASC,
        e.joinedAt ASC
    """)
  List<QueueEntry> findActiveByQueueIdOrdered(@Param("queueId") String queueId);

    // Check if a user already has a WAITING entry in a specific queue
    Optional<QueueEntry> findByQueue_IdAndUser_IdAndStatus(String queueId, String userId, QueueEntry.EntryStatus status);

    List<QueueEntry> findByUser_IdOrderByJoinedAtDesc(String userId);

    long countByQueue_IdAndStatus(String queueId, QueueEntry.EntryStatus status);
}
