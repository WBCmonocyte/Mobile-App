package com.easysport.repository;

import com.easysport.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, String> {
    List<InventoryTransaction> findTop50ByOrderByCreatedAtDesc();
    List<InventoryTransaction> findTop50ByCourtIdOrderByCreatedAtDesc(String courtId);
    List<InventoryTransaction> findByItemIdOrderByCreatedAtDesc(String itemId);
}
