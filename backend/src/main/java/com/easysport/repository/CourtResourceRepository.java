package com.easysport.repository;

import com.easysport.entity.CourtResource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourtResourceRepository extends JpaRepository<CourtResource, String> {

    List<CourtResource> findByCourtId(String courtId);

    /**
     * Khóa bi quan (Pessimistic Write Lock) trên các tài nguyên theo thứ tự tăng dần cố định.
     * Ngăn chặn hoàn toàn tranh chấp và loại trừ nguy cơ deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CourtResource r WHERE r.id IN :ids ORDER BY r.id ASC")
    List<CourtResource> lockResourcesForUpdate(@Param("ids") List<String> ids);
}

