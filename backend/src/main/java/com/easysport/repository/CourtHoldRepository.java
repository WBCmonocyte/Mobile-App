package com.easysport.repository;

import com.easysport.entity.CourtHold;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourtHoldRepository extends JpaRepository<CourtHold, String> {

    Optional<CourtHold> findByIdempotencyKey(String idempotencyKey);

    Optional<CourtHold> findByBookingId(String bookingId);

    Optional<CourtHold> findByRecurringPackageId(String recurringPackageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM CourtHold h WHERE h.id = :id")
    Optional<CourtHold> findByIdWithLock(@Param("id") String id);

    @Query("SELECT h FROM CourtHold h WHERE h.status IN ('HELD', 'AWAITING_PAYMENT', 'VERIFYING_PAYMENT') " +
           "AND (h.verificationDeadline <= :now OR (h.verificationDeadline IS NULL AND h.expiresAt <= :now))")
    List<CourtHold> findExpiredHolds(@Param("now") LocalDateTime now);
}
