package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "court_holds", indexes = {
        @Index(name = "idx_court_holds_slot", columnList = "court_id,field_id,booking_date,status"),
        @Index(name = "idx_court_holds_idempotency", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_court_holds_booking", columnList = "booking_id"),
        @Index(name = "idx_court_holds_expires", columnList = "expires_at"),
        @Index(name = "idx_court_holds_verify_deadline", columnList = "verification_deadline")
})
public class CourtHold {

    @Id
    @Column(length = 64)
    private String id; // holdId

    @Column(name = "booking_id", length = 64)
    private String bookingId;

    @Column(name = "payment_attempt_id", length = 64)
    private String paymentAttemptId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(name = "field_id", nullable = false, length = 50)
    private String fieldId;

    @Column(name = "field_detail", length = 100)
    private String fieldDetail;

    @Column(name = "booking_date", nullable = false, length = 50)
    private String dateLabel;

    @Column(name = "time_range", nullable = false, length = 100)
    private String timeRange;

    @Column(name = "slots_json", length = 1000)
    private String slotsJson;

    @Column(name = "resource_ids", length = 500)
    private String resourceIds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Thời hạn thanh toán (5 phút sau T0).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;

    /**
     * Thời hạn xác minh (1 phút sau thời hạn thanh toán, tức 6 phút sau T0).
     */
    @Column(name = "verification_deadline")
    private LocalDateTime verificationDeadline;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "AWAITING_PAYMENT"; // AWAITING_PAYMENT, VERIFYING_PAYMENT, CONFIRMED, EXPIRED, RELEASED, REVIEW_REQUIRED, HELD, BOOKED

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "recurring_package_id", length = 64)
    private String recurringPackageId;

    @Version
    private Long version;

    public CourtHold() {}

    public CourtHold(String id, String bookingId, String paymentAttemptId, String userId,
                     String courtId, String fieldId, String fieldDetail, String dateLabel,
                     String timeRange, String slotsJson, String resourceIds,
                     LocalDateTime createdAt, LocalDateTime paymentDeadline, LocalDateTime verificationDeadline,
                     String idempotencyKey) {
        this.id = id;
        this.bookingId = bookingId;
        this.paymentAttemptId = paymentAttemptId;
        this.userId = userId;
        this.courtId = courtId;
        this.fieldId = fieldId;
        this.fieldDetail = fieldDetail;
        this.dateLabel = dateLabel;
        this.timeRange = timeRange;
        this.slotsJson = slotsJson;
        this.resourceIds = resourceIds;
        this.createdAt = createdAt;
        this.expiresAt = paymentDeadline;
        this.paymentDeadline = paymentDeadline;
        this.verificationDeadline = verificationDeadline != null ? verificationDeadline : paymentDeadline.plusMinutes(1);
        this.status = "AWAITING_PAYMENT";
        this.idempotencyKey = idempotencyKey;
    }

    // Constructor tương thích ngược
    public CourtHold(String id, String bookingId, String paymentAttemptId, String userId,
                     String courtId, String fieldId, String fieldDetail, String dateLabel,
                     String timeRange, String slotsJson, String resourceIds,
                     LocalDateTime createdAt, LocalDateTime expiresAt, String idempotencyKey) {
        this(id, bookingId, paymentAttemptId, userId, courtId, fieldId, fieldDetail, dateLabel,
                timeRange, slotsJson, resourceIds, createdAt, expiresAt,
                expiresAt != null ? expiresAt.plusMinutes(1) : (createdAt != null ? createdAt.plusMinutes(6) : null),
                idempotencyKey);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getPaymentAttemptId() { return paymentAttemptId; }
    public void setPaymentAttemptId(String paymentAttemptId) { this.paymentAttemptId = paymentAttemptId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getFieldId() { return fieldId; }
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }

    public String getFieldDetail() { return fieldDetail; }
    public void setFieldDetail(String fieldDetail) { this.fieldDetail = fieldDetail; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public String getSlotsJson() { return slotsJson; }
    public void setSlotsJson(String slotsJson) { this.slotsJson = slotsJson; }

    public String getResourceIds() { return resourceIds; }
    public void setResourceIds(String resourceIds) { this.resourceIds = resourceIds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() {
        return paymentDeadline != null ? paymentDeadline : expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        if (this.paymentDeadline == null) {
            this.paymentDeadline = expiresAt;
        }
        if (this.verificationDeadline == null && expiresAt != null) {
            this.verificationDeadline = expiresAt.plusMinutes(1);
        }
    }

    public LocalDateTime getPaymentDeadline() {
        return paymentDeadline != null ? paymentDeadline : expiresAt;
    }

    public void setPaymentDeadline(LocalDateTime paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
        this.expiresAt = paymentDeadline;
    }

    public LocalDateTime getVerificationDeadline() {
        if (verificationDeadline != null) {
            return verificationDeadline;
        }
        LocalDateTime pDeadline = getPaymentDeadline();
        return pDeadline != null ? pDeadline.plusMinutes(1) : null;
    }

    public void setVerificationDeadline(LocalDateTime verificationDeadline) {
        this.verificationDeadline = verificationDeadline;
    }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getRecurringPackageId() { return recurringPackageId; }
    public void setRecurringPackageId(String recurringPackageId) { this.recurringPackageId = recurringPackageId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    /**
     * Xác định trạng thái động dựa trên đồng hồ hiện tại của server:
     * - AWAITING_PAYMENT: trong 5 phút đầu
     * - VERIFYING_PAYMENT: phút thứ 6
     * - CONFIRMED / BOOKED: đã xác nhận thành công
     * - EXPIRED: quá 6 phút chưa xác nhận
     * - REVIEW_REQUIRED: đối soát
     * - RELEASED: đã hủy
     */
    public String getDynamicStatus(LocalDateTime now) {
        if ("CONFIRMED".equalsIgnoreCase(status) || "BOOKED".equalsIgnoreCase(status)) {
            return "CONFIRMED";
        }
        if ("RELEASED".equalsIgnoreCase(status)) {
            return "RELEASED";
        }
        if ("REVIEW_REQUIRED".equalsIgnoreCase(status)) {
            return "REVIEW_REQUIRED";
        }
        if ("EXPIRED".equalsIgnoreCase(status)) {
            return "EXPIRED";
        }

        LocalDateTime pDeadline = getPaymentDeadline();
        LocalDateTime vDeadline = getVerificationDeadline();

        if (now != null && pDeadline != null && vDeadline != null) {
            if (now.isBefore(pDeadline)) {
                return "AWAITING_PAYMENT";
            } else if (now.isBefore(vDeadline)) {
                return "VERIFYING_PAYMENT";
            } else {
                return "EXPIRED";
            }
        }
        return status;
    }

    public boolean isExpired(LocalDateTime now) {
        String dyn = getDynamicStatus(now);
        return "EXPIRED".equalsIgnoreCase(dyn) || "RELEASED".equalsIgnoreCase(dyn);
    }

    public boolean isAwaitingPayment(LocalDateTime now) {
        return "AWAITING_PAYMENT".equalsIgnoreCase(getDynamicStatus(now));
    }

    public boolean isVerifyingPayment(LocalDateTime now) {
        return "VERIFYING_PAYMENT".equalsIgnoreCase(getDynamicStatus(now));
    }

    public boolean isInVerificationWindow(LocalDateTime now) {
        return isVerifyingPayment(now);
    }

    public boolean isConfirmed() {
        return "CONFIRMED".equalsIgnoreCase(status) || "BOOKED".equalsIgnoreCase(status);
    }
}
