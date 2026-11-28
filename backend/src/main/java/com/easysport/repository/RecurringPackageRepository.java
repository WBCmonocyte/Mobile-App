package com.easysport.repository;

import com.easysport.entity.RecurringPackage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringPackageRepository extends JpaRepository<RecurringPackage, String> {

    Optional<RecurringPackage> findByIdempotencyKey(String idempotencyKey);

    List<RecurringPackage> findByUserIdOrderByCreatedAtDesc(String userId);

    List<RecurringPackage> findByCourtIdInOrderByCreatedAtDesc(List<String> courtIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM RecurringPackage p WHERE p.id = :id")
    Optional<RecurringPackage> lockById(@Param("id") String id);
}
