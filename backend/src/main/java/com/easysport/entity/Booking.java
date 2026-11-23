package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_slot", columnList = "court_id,field_detail,booking_date"),
        @Index(name = "idx_bookings_user", columnList = "user_id"),
        @Index(name = "idx_bookings_status", columnList = "status")
})
public class Booking {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(name = "court_name", length = 150)
    private String courtName;

    @Column(name = "field_detail", nullable = false, length = 100)
    private String fieldDetail;

    @Column(name = "time_range", nullable = false, length = 100)
    private String timeRange;

    @Column(name = "booking_date", nullable = false, length = 50)
    private String dateLabel;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "deposit_amount")
    private Integer depositAmount = 0;

    @Column(name = "remaining_amount")
    private Integer remainingAmount = 0;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod = "QR_TRANSFER";

    @Column(name = "payment_status", length = 30)
    private String payment = "PENDING";

    @Column(name = "status", length = 30, nullable = false)
    private String status = "PENDING";

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(length = 255)
    private String address = "";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt = LocalDateTime.now().plusMinutes(15);

    public Booking() {}

    public Booking(String id, String userId, String courtId, String courtName, String fieldDetail,
                   String timeRange, String dateLabel, Integer totalPrice, Integer depositAmount,
                   Integer remainingAmount, String paymentMethod, String status,
                   String customerName, String customerPhone, String address, LocalDateTime holdExpiresAt) {
        this.id = id;
        this.userId = userId;
        this.courtId = courtId;
        this.courtName = courtName;
        this.fieldDetail = fieldDetail;
        this.timeRange = timeRange;
        this.dateLabel = dateLabel;
        this.totalPrice = totalPrice;
        this.depositAmount = depositAmount != null ? depositAmount : 0;
        this.remainingAmount = remainingAmount != null ? remainingAmount : 0;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "QR_TRANSFER";
        this.status = status != null ? status : "PENDING";
        this.payment = this.status;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.address = address;
        this.createdAt = LocalDateTime.now();
        this.holdExpiresAt = holdExpiresAt != null ? holdExpiresAt : this.createdAt.plusMinutes(15);
    }

    // Convenience constructor for legacy code
    public Booking(String id, String userId, String courtId, String courtName, String fieldDetail,
                   String timeRange, String dateLabel, Integer totalPrice, Integer depositAmount,
                   String paymentMethod, String payment, String customerName, String customerPhone, String address) {
        this(id, userId, courtId, courtName, fieldDetail, timeRange, dateLabel, totalPrice,
             depositAmount, totalPrice != null && depositAmount != null ? totalPrice - depositAmount : 0,
             paymentMethod, payment, customerName, customerPhone, address, LocalDateTime.now().plusMinutes(15));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getFieldDetail() { return fieldDetail; }
    public void setFieldDetail(String fieldDetail) { this.fieldDetail = fieldDetail; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

    public Integer getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Integer depositAmount) { this.depositAmount = depositAmount; }

    public Integer getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Integer remainingAmount) { this.remainingAmount = remainingAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPayment() { return payment; }
    public void setPayment(String payment) {
        this.payment = payment;
        this.status = payment;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.payment = status;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }

    public boolean isExpired() {
        return "PENDING".equalsIgnoreCase(this.status) &&
                this.holdExpiresAt != null &&
                this.holdExpiresAt.isBefore(LocalDateTime.now());
    }
}
