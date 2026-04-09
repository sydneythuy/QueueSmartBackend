package com.queuesmart.repository;

import com.queuesmart.model.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, String> {
    Optional<UserCredential> findByEmail(String email);
    boolean existsByEmail(String email);
}
