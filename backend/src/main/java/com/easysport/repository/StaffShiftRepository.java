package com.easysport.repository;

import com.easysport.entity.StaffShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StaffShiftRepository extends JpaRepository<StaffShift, String> {
    List<StaffShift> findAllByOrderByCreatedAtDesc();
    List<StaffShift> findByStaffIdOrderByCreatedAtDesc(String staffId);
    List<StaffShift> findByShiftDateIn(Collection<String> dates);
    List<StaffShift> findByStaffIdAndShiftDateIn(String staffId, Collection<String> dates);
}
