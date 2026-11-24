package com.easysport.repository;

import com.easysport.entity.PosSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosSaleRepository extends JpaRepository<PosSale, String> {
    List<PosSale> findAllByOrderByCreatedAtDesc();
    List<PosSale> findByStaffIdAndSaleDateOrderByCreatedAtDesc(String staffId, String saleDate);
    List<PosSale> findByShiftIdOrderByCreatedAtDesc(String shiftId);
}
