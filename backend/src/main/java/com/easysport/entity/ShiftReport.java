package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Báo cáo bàn giao két tiền và tình trạng sân cuối ca của Nhân viên gửi Chủ sân duyệt.
 */
@Entity
@Table(name = "shift_reports")
public class ShiftReport {

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

    @Column(name = "shift_date", nullable = false, length = 80)
    private String shiftDate;

    @Column(name = "cash_amount", nullable = false)
    private Integer cashAmount = 0;

    @Column(name = "qr_amount", nullable = false)
    private Integer qrAmount = 0;

    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders = 0;

    @Column(name = "handover_note", length = 500)
    private String handoverNote = "";

    /** PENDING, CONFIRMED */
    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "shift_id", length = 50)
    private String shiftId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ShiftReport() {}

    public ShiftReport(String id, String courtId, String courtName, String staffId, String staffName,
                       String shiftDate, Integer cashAmount, Integer qrAmount, Integer totalOrders,
                       String handoverNote, String status, String confirmedBy) {
        this.id = id;
        this.courtId = courtId != null ? courtId : "court_thanh_phat";
        this.courtName = courtName != null ? courtName : "Sân bóng Thành Phát";
        this.staffId = staffId;
        this.staffName = staffName;
        this.shiftDate = shiftDate;
        this.cashAmount = cashAmount != null ? cashAmount : 0;
        this.qrAmount = qrAmount != null ? qrAmount : 0;
        this.totalOrders = totalOrders != null ? totalOrders : 0;
        this.handoverNote = handoverNote != null ? handoverNote : "";
        this.status = status != null ? status : "PENDING";
        this.confirmedBy = confirmedBy;
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

    public String getShiftDate() { return shiftDate; }
    public void setShiftDate(String shiftDate) { this.shiftDate = shiftDate; }

    public Integer getCashAmount() { return cashAmount; }
    public void setCashAmount(Integer cashAmount) { this.cashAmount = cashAmount; }

    public Integer getQrAmount() { return qrAmount; }
    public void setQrAmount(Integer qrAmount) { this.qrAmount = qrAmount; }

    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }

    public String getHandoverNote() { return handoverNote; }
    public void setHandoverNote(String handoverNote) { this.handoverNote = handoverNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }

    public String getShiftId() { return shiftId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
