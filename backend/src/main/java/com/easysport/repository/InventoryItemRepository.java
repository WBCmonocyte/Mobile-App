package com.easysport.repository;

import com.easysport.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {
    List<InventoryItem> findByActiveTrueOrderByCourtIdAscNameAsc();
    List<InventoryItem> findByCourtIdAndActiveTrueOrderByNameAsc(String courtId);
}
