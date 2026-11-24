package com.easysport.repository;

import com.easysport.entity.ShiftReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftReportRepository extends JpaRepository<ShiftReport, String> {
    List<ShiftReport> findAllByOrderByCreatedAtDesc();
    boolean existsByShiftId(String shiftId);
    List<ShiftReport> findByStaffIdOrderByCreatedAtDesc(String staffId);
}
