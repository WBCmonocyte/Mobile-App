package com.easysport.repository;

import com.easysport.entity.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, String> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(String userId);

    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.readAt IS NULL")
    List<UserNotification> findUnreadByUserId(String userId);

    boolean existsByDedupKey(String dedupKey);
}
