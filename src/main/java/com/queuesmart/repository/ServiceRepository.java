package com.queuesmart.repository;

import com.queuesmart.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, String> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Service> findByNameIgnoreCase(String name);
    List<Service> findAllByActiveTrue();
}
