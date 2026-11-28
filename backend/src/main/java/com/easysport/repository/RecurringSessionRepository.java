package com.easysport.repository;

import com.easysport.entity.RecurringSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringSessionRepository extends JpaRepository<RecurringSession, String> {

    List<RecurringSession> findByPackageIdOrderBySessionIndexAsc(String packageId);

    List<RecurringSession> findByCourtIdInOrderBySessionIndexAsc(List<String> courtIds);

    Optional<RecurringSession> findByBookingId(String bookingId);

    Optional<RecurringSession> findByReplacementBookingId(String replacementBookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RecurringSession s WHERE s.id = :id")
    Optional<RecurringSession> lockById(@Param("id") String id);
}
