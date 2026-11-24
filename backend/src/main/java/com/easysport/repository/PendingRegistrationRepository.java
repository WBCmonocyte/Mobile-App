package com.easysport.repository;

import com.easysport.entity.PendingRegistration;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingRegistration> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingRegistration> findByChallengeId(String challengeId);

    long deleteByExpiresAtBefore(Instant cutoff);
}
