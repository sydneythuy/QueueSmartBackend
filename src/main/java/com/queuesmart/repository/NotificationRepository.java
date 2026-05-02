package com.queuesmart.repository;

import com.queuesmart.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(String userId);
    List<Notification> findByUser_IdAndReadFalseOrderByCreatedAtDesc(String userId);
    long countByUser_IdAndReadFalse(String userId);
}
