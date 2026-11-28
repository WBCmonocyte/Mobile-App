package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_sessions", indexes = {
        @Index(name = "idx_rec_sess_pkg", columnList = "package_id,session_index"),
        @Index(name = "idx_rec_sess_booking", columnList = "booking_id"),
        @Index(name = "idx_rec_sess_court_date", columnList = "court_id,session_date")
})
public class RecurringSession {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "package_id", nullable = false, length = 64)
    private String packageId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(name = "field_id", nullable = false, length = 50)
    private String fieldId;

    @Column(name = "field_detail", nullable = false, length = 100)
    private String fieldDetail;

    @Column(name = "session_index", nullable = false)
    private Integer sessionIndex;

    @Column(name = "session_date", nullable = false, length = 20)
    private String sessionDate;

    @Column(name = "time_range", nullable = false, length = 100)
    private String timeRange;

    @Column(name = "unit_price_snapshot", nullable = false)
    private Integer unitPriceSnapshot;

    @Column(name = "paid_amount_snapshot", nullable = false)
    private Integer paidAmountSnapshot = 0;

    @Column(name = "refunded_amount", nullable = false)
    private Integer refundedAmount = 0;

    /**
     * PENDING_PAYMENT, EXPIRED, REVIEW_REQUIRED,
     * UPCOMING, CHECKED_IN, COMPLETED,
     * WEATHER_PENDING, WEATHER_APPROVED, PENDING_OWNER_REVIEW,
     * RESCHEDULED, CREDIT_GRANTED, REFUNDED, CANCELLED
     */
    @Column(nullable = false, length = 30)
    private String status = "PENDING_PAYMENT";

    @Column(name = "booking_id", length = 64)
    private String bookingId;

    @Column(name = "rescheduled_to_date", length = 20)
    private String rescheduledToDate;

    @Column(name = "replacement_booking_id", length = 64)
    private String replacementBookingId;

    @Column(name = "credit_id", length = 64)
    private String creditId;

    @Column(name = "wallet_transaction_id", length = 64)
    private String walletTransactionId;

    @Column(name = "resolution_type", length = 40)
    private String resolutionType;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Version
    private Long version = 0L;

    public RecurringSession() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getFieldId() { return fieldId; }
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }

    public String getFieldDetail() { return fieldDetail; }
    public void setFieldDetail(String fieldDetail) { this.fieldDetail = fieldDetail; }

    public Integer getSessionIndex() { return sessionIndex; }
    public void setSessionIndex(Integer sessionIndex) { this.sessionIndex = sessionIndex; }

    public String getSessionDate() { return sessionDate; }
    public void setSessionDate(String sessionDate) { this.sessionDate = sessionDate; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public Integer getUnitPriceSnapshot() { return unitPriceSnapshot != null ? unitPriceSnapshot : 0; }
    public void setUnitPriceSnapshot(Integer unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot != null ? unitPriceSnapshot : 0; }

    public Integer getPaidAmountSnapshot() { return paidAmountSnapshot != null ? paidAmountSnapshot : 0; }
    public void setPaidAmountSnapshot(Integer paidAmountSnapshot) { this.paidAmountSnapshot = paidAmountSnapshot != null ? paidAmountSnapshot : 0; }

    public Integer getRefundedAmount() { return refundedAmount != null ? refundedAmount : 0; }
    public void setRefundedAmount(Integer refundedAmount) { this.refundedAmount = refundedAmount != null ? refundedAmount : 0; }

    public int getRemainingRefundableAmount() {
        return Math.max(0, getPaidAmountSnapshot() - getRefundedAmount());
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getRescheduledToDate() { return rescheduledToDate; }
    public void setRescheduledToDate(String rescheduledToDate) { this.rescheduledToDate = rescheduledToDate; }

    public String getReplacementBookingId() { return replacementBookingId; }
    public void setReplacementBookingId(String replacementBookingId) { this.replacementBookingId = replacementBookingId; }

    public String getCreditId() { return creditId; }
    public void setCreditId(String creditId) { this.creditId = creditId; }

    public String getWalletTransactionId() { return walletTransactionId; }
    public void setWalletTransactionId(String walletTransactionId) { this.walletTransactionId = walletTransactionId; }

    public String getResolutionType() { return resolutionType; }
    public void setResolutionType(String resolutionType) { this.resolutionType = resolutionType; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
