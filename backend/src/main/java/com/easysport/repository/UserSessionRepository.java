package com.easysport.repository;

import com.easysport.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    Optional<UserSession> findByToken(String token);

    List<UserSession> findByUserId(String userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void deactivateAllByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now OR s.isActive = false")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
}

