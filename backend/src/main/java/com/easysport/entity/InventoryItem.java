package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Mặt hàng trong kho của từng chi nhánh sân thể thao (nước uống, phụ kiện, dụng cụ).
 */
@Entity
@Table(name = "inventory_items", indexes = {
        @Index(name = "idx_inventory_court_active", columnList = "court_id,active")
})
public class InventoryItem {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(name = "court_name", nullable = false, length = 150)
    private String courtName;

    @Column(nullable = false, length = 150)
    private String name;

    /** DRINK, EQUIPMENT, ACCESSORY */
    @Column(nullable = false, length = 50)
    private String category = "DRINK";

    @Column(nullable = false, length = 30)
    private String unit = "chai";

    @Column(name = "import_price", nullable = false)
    private Integer importPrice = 0;

    @Column(name = "sell_price", nullable = false)
    private Integer sellPrice = 0;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "min_alert_threshold", nullable = false)
    private Integer minAlertThreshold = 10;

    @Column(length = 50)
    private String sku;

    @Column(length = 80)
    private String barcode;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public InventoryItem() {}

    public InventoryItem(String id, String courtId, String courtName, String name,
                         String category, String unit, Integer importPrice, Integer sellPrice,
                         Integer stockQuantity, Integer minAlertThreshold) {
        this.id = id;
        this.courtId = courtId;
        this.courtName = courtName;
        this.name = name;
        this.category = category != null ? category : "DRINK";
        this.unit = unit != null ? unit : "chai";
        this.importPrice = importPrice != null ? importPrice : 0;
        this.sellPrice = sellPrice != null ? sellPrice : 0;
        this.stockQuantity = stockQuantity != null ? stockQuantity : 0;
        this.minAlertThreshold = minAlertThreshold != null ? minAlertThreshold : 10;
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Integer getImportPrice() { return importPrice; }
    public void setImportPrice(Integer importPrice) { this.importPrice = importPrice; }

    public Integer getSellPrice() { return sellPrice; }
    public void setSellPrice(Integer sellPrice) { this.sellPrice = sellPrice; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getMinAlertThreshold() { return minAlertThreshold; }
    public void setMinAlertThreshold(Integer minAlertThreshold) { this.minAlertThreshold = minAlertThreshold; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isLowStock() {
        return stockQuantity != null && minAlertThreshold != null && stockQuantity <= minAlertThreshold;
    }
}
