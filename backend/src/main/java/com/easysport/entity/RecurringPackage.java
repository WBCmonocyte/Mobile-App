package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_packages", indexes = {
        @Index(name = "uk_rec_pkg_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_rec_pkg_user", columnList = "user_id,created_at"),
        @Index(name = "idx_rec_pkg_court", columnList = "court_id")
})
public class RecurringPackage {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(name = "court_name", nullable = false, length = 150)
    private String courtName;

    @Column(name = "court_address", length = 255)
    private String courtAddress = "";

    @Column(name = "field_id", nullable = false, length = 50)
    private String fieldId;

    @Column(name = "field_detail", nullable = false, length = 100)
    private String fieldDetail;

    @Column(name = "package_type", nullable = false, length = 40)
    private String packageType = "WEEKLY_12";

    @Column(name = "package_title", nullable = false, length = 120)
    private String packageTitle = "3 tháng · 12 buổi · 1 buổi/tuần";

    @Column(name = "start_date", nullable = false, length = 20)
    private String startDate;

    @Column(name = "weekday_label", nullable = false, length = 30)
    private String weekdayLabel;

    @Column(name = "time_range", nullable = false, length = 100)
    private String timeRange;

    @Column(name = "slots_csv", nullable = false, length = 500)
    private String slotsCsv;

    @Column(name = "slot_count", nullable = false)
    private Integer slotCount = 1;

    @Column(name = "total_sessions", nullable = false)
    private Integer totalSessions = 12;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "total_paid", nullable = false)
    private Integer totalPaid = 0;

    @Column(name = "total_refunded", nullable = false)
    private Integer totalRefunded = 0;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod = "QR_TRANSFER";

    /** AWAITING_PAYMENT, ACTIVE, EXPIRED, RELEASED, REVIEW_REQUIRED, COMPLETED, CANCELLED */
    @Column(nullable = false, length = 30)
    private String status = "AWAITING_PAYMENT";

    @Column(name = "hold_id", length = 64)
    private String holdId;

    @Column(name = "payment_id", length = 64)
    private String paymentId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Version
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RecurringPackage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getCourtAddress() { return courtAddress; }
    public void setCourtAddress(String courtAddress) { this.courtAddress = courtAddress; }

    public String getFieldId() { return fieldId; }
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }

    public String getFieldDetail() { return fieldDetail; }
    public void setFieldDetail(String fieldDetail) { this.fieldDetail = fieldDetail; }

    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }

    public String getPackageTitle() { return packageTitle; }
    public void setPackageTitle(String packageTitle) { this.packageTitle = packageTitle; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getWeekdayLabel() { return weekdayLabel; }
    public void setWeekdayLabel(String weekdayLabel) { this.weekdayLabel = weekdayLabel; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public String getSlotsCsv() { return slotsCsv; }
    public void setSlotsCsv(String slotsCsv) { this.slotsCsv = slotsCsv; }

    public Integer getSlotCount() { return slotCount != null ? slotCount : 1; }
    public void setSlotCount(Integer slotCount) { this.slotCount = slotCount != null ? slotCount : 1; }

    public Integer getTotalSessions() { return totalSessions != null ? totalSessions : 12; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions != null ? totalSessions : 12; }

    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

    public Integer getTotalPaid() { return totalPaid != null ? totalPaid : 0; }
    public void setTotalPaid(Integer totalPaid) { this.totalPaid = totalPaid != null ? totalPaid : 0; }

    public Integer getTotalRefunded() { return totalRefunded != null ? totalRefunded : 0; }
    public void setTotalRefunded(Integer totalRefunded) { this.totalRefunded = totalRefunded != null ? totalRefunded : 0; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
