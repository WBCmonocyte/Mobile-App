package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Ca làm việc được Chủ sân phân công cho Nhân viên trực sân / lễ tân.
 */
@Entity
@Table(name = "staff_shifts", indexes = {
        @Index(name = "idx_staff_shifts_staff", columnList = "staff_id"),
        @Index(name = "idx_staff_shifts_date", columnList = "shift_date")
})
public class StaffShift {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "staff_id", nullable = false, length = 50)
    private String staffId;

    @Column(name = "staff_name", nullable = false, length = 100)
    private String staffName;

    @Column(name = "staff_phone", length = 20)
    private String staffPhone = "";

    @Column(name = "court_id", length = 50)
    private String courtId = "court_thanh_phat";

    @Column(name = "court_name", length = 150)
    private String courtName = "Sân bóng Thành Phát";

    @Column(name = "shift_date", nullable = false, length = 50)
    private String shiftDate;

    @Column(name = "shift_slot", nullable = false, length = 80)
    private String shiftSlot;

    @Column(length = 150)
    private String duty = "Trực lễ tân & Thu ngân";

    /** UPCOMING, ACTIVE, COMPLETED */
    @Column(nullable = false, length = 30)
    private String status = "UPCOMING";

    @Column(name = "check_in_time", length = 30)
    private String checkInTime;

    @Column(name = "check_out_time", length = 30)
    private String checkOutTime;

    @Column(name = "late_minutes")
    private Integer lateMinutes;

    @Column(length = 255)
    private String notes = "";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public StaffShift() {}

    public StaffShift(String id, String staffId, String staffName, String staffPhone,
                      String courtId, String courtName, String shiftDate, String shiftSlot,
                      String duty, String status, String checkInTime, String checkOutTime, String notes) {
        this.id = id;
        this.staffId = staffId;
        this.staffName = staffName;
        this.staffPhone = staffPhone != null ? staffPhone : "";
        this.courtId = courtId != null ? courtId : "court_thanh_phat";
        this.courtName = courtName != null ? courtName : "Sân bóng Thành Phát";
        this.shiftDate = shiftDate;
        this.shiftSlot = shiftSlot;
        this.duty = duty != null ? duty : "Trực lễ tân & Thu ngân";
        this.status = status != null ? status : "UPCOMING";
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.notes = notes != null ? notes : "";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getStaffPhone() { return staffPhone; }
    public void setStaffPhone(String staffPhone) { this.staffPhone = staffPhone; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getShiftDate() { return shiftDate; }
    public void setShiftDate(String shiftDate) { this.shiftDate = shiftDate; }

    public String getShiftSlot() { return shiftSlot; }
    public void setShiftSlot(String shiftSlot) { this.shiftSlot = shiftSlot; }

    public String getDuty() { return duty; }
    public void setDuty(String duty) { this.duty = duty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }

    public String getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(String checkOutTime) { this.checkOutTime = checkOutTime; }

    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
