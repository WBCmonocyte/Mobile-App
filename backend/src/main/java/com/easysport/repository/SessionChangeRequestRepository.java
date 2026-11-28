package com.easysport.repository;

import com.easysport.entity.SessionChangeRequest;
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
public interface SessionChangeRequestRepository extends JpaRepository<SessionChangeRequest, String> {

    List<SessionChangeRequest> findByCourtIdInOrderByCreatedAtDesc(List<String> courtIds);

    List<SessionChangeRequest> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    List<SessionChangeRequest> findByPackageIdOrderByCreatedAtDesc(String packageId);

    @Query("SELECT COUNT(r) FROM SessionChangeRequest r WHERE r.userId = :userId " +
           "AND r.countsTowardCancelLimit = true AND r.createdAt >= :since")
    long countSelfCancellationsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SessionChangeRequest r WHERE r.id = :id")
    Optional<SessionChangeRequest> lockById(@Param("id") String id);
}
