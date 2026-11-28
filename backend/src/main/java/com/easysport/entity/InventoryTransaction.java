package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Lịch sử biến động kho hàng (Nhập kho, Bán POS, Điều chỉnh kiểm kê).
 */
@Entity
@Table(name = "inventory_transactions", indexes = {
        @Index(name = "idx_inv_tx_court_time", columnList = "court_id,created_at"),
        @Index(name = "idx_inv_tx_item", columnList = "item_id")
})
public class InventoryTransaction {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "item_id", nullable = false, length = 50)
    private String itemId;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    /** IMPORT, SALE, ADJUST, DAMAGE, EXPIRED, INTERNAL_USE, TRANSFER_OUT, TRANSFER_IN */
    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "unit_cost")
    private Integer unitCost = 0;

    @Column(length = 300)
    private String note = "";

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public InventoryTransaction() {}

    public InventoryTransaction(String id, String itemId, String itemName, String courtId,
                                String type, Integer quantityDelta, Integer stockAfter,
                                Integer unitCost, String note, String createdBy) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.courtId = courtId;
        this.type = type;
        this.quantityDelta = quantityDelta;
        this.stockAfter = stockAfter;
        this.unitCost = unitCost != null ? unitCost : 0;
        this.note = note != null ? note : "";
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getQuantityDelta() { return quantityDelta; }
    public void setQuantityDelta(Integer quantityDelta) { this.quantityDelta = quantityDelta; }

    public Integer getStockAfter() { return stockAfter; }
    public void setStockAfter(Integer stockAfter) { this.stockAfter = stockAfter; }

    public Integer getUnitCost() { return unitCost; }
    public void setUnitCost(Integer unitCost) { this.unitCost = unitCost; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
