package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Giao dịch bán lẻ đồ uống / phụ kiện tại quầy POS (không gắn vào hóa đơn sân).
 */
@Entity
@Table(name = "pos_sales", indexes = {
        @Index(name = "idx_pos_sales_staff_date", columnList = "staff_id,sale_date"),
        @Index(name = "idx_pos_sales_shift", columnList = "shift_id")
})
public class PosSale {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "court_id", length = 50)
    private String courtId = "court_thanh_phat";

    @Column(name = "court_name", length = 150)
    private String courtName = "Sân bóng Thành Phát";

    @Column(name = "staff_id", nullable = false, length = 50)
    private String staffId;

    @Column(name = "staff_name", nullable = false, length = 100)
    private String staffName;

    @Column(name = "shift_id", length = 50)
    private String shiftId;

    @Column(name = "items_note", length = 500)
    private String itemsNote = "";

    @Column(nullable = false)
    private Integer amount = 0;

    /** CASH hoặc QR_TRANSFER */
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod = "CASH";

    @Column(name = "sale_date", length = 20)
    private String saleDate;

    @Column(name = "settlement_booking_id", length = 50)
    private String settlementBookingId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public PosSale() {}

    public PosSale(String id, String courtId, String courtName, String staffId, String staffName,
                   String shiftId, String itemsNote, Integer amount, String paymentMethod, String saleDate) {
        this.id = id;
        this.courtId = courtId != null ? courtId : "court_thanh_phat";
        this.courtName = courtName != null ? courtName : "Sân bóng Thành Phát";
        this.staffId = staffId;
        this.staffName = staffName;
        this.shiftId = shiftId;
        this.itemsNote = itemsNote != null ? itemsNote : "";
        this.amount = amount != null ? amount : 0;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.saleDate = saleDate;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getShiftId() { return shiftId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }

    public String getItemsNote() { return itemsNote; }
    public void setItemsNote(String itemsNote) { this.itemsNote = itemsNote; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSaleDate() { return saleDate; }
    public void setSaleDate(String saleDate) { this.saleDate = saleDate; }

    public String getSettlementBookingId() { return settlementBookingId; }
    public void setSettlementBookingId(String settlementBookingId) { this.settlementBookingId = settlementBookingId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
