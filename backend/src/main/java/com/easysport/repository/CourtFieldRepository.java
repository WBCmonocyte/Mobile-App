package com.easysport.repository;

import com.easysport.entity.CourtField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourtFieldRepository extends JpaRepository<CourtField, String> {
    List<CourtField> findByCourtId(String courtId);
}

