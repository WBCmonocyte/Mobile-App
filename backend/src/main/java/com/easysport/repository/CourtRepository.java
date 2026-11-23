package com.easysport.repository;

import com.easysport.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Court c WHERE c.id = :id")
    java.util.Optional<Court> lockById(@org.springframework.data.repository.query.Param("id") String id);
    List<Court> findByNameContainingIgnoreCase(String name);
}
