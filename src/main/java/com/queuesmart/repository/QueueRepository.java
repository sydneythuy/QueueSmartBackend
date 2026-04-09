package com.queuesmart.repository;

import com.queuesmart.model.Queue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QueueRepository extends JpaRepository<Queue, String> {
    Optional<Queue> findByServiceId(String serviceId);
}
