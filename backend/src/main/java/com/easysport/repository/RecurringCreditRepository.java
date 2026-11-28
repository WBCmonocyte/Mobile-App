package com.easysport.repository;

import com.easysport.entity.RecurringCredit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringCreditRepository extends JpaRepository<RecurringCredit, String> {

    List<RecurringCredit> findByPackageIdOrderByCreatedAtDesc(String packageId);

    List<RecurringCredit> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<RecurringCredit> findBySourceSessionId(String sourceSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM RecurringCredit c WHERE c.id = :id")
    Optional<RecurringCredit> lockById(@Param("id") String id);
}
