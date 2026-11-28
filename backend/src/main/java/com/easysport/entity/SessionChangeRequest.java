package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_change_requests", indexes = {
        @Index(name = "idx_scr_court_status", columnList = "court_id,status"),
        @Index(name = "idx_scr_user_counts", columnList = "user_id,counts_toward_cancel_limit,created_at"),
        @Index(name = "idx_scr_session", columnList = "session_id")
})
public class SessionChangeRequest {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "package_id", nullable = false, length = 64)
    private String packageId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "customer_name", length = 100)
    private String customerName = "";

    @Column(name = "customer_phone", length = 20)
    private String customerPhone = "";

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(name = "court_name", length = 150)
    private String courtName = "";

    @Column(name = "session_date", nullable = false, length = 20)
    private String sessionDate;

    @Column(name = "time_range", nullable = false, length = 100)
    private String timeRange;

    /** WEATHER_POSTPONE, LATE_CANCEL_OR_REFUND, EXCESSIVE_CANCEL, SELF_EARLY_CANCEL */
    @Column(name = "request_type", nullable = false, length = 40)
    private String requestType;

    /** SLIDE, KEEP_CREDIT, REFUND_WALLET */
    @Column(name = "requested_action", length = 40)
    private String requestedAction;

    @Column(name = "reason_note", length = 500)
    private String reasonNote = "";

    /** PENDING, APPROVED, REJECTED, AUTO_APPLIED */
    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "counts_toward_cancel_limit", nullable = false)
    private Boolean countsTowardCancelLimit = false;

    @Column(name = "max_refundable_amount", nullable = false)
    private Integer maxRefundableAmount = 0;

    @Column(name = "approved_refund_amount")
    private Integer approvedRefundAmount;

    @Column(name = "resolved_outcome", length = 40)
    private String resolvedOutcome;

    @Column(name = "owner_note", length = 500)
    private String ownerNote;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Version
    private Long version = 0L;

    public SessionChangeRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getSessionDate() { return sessionDate; }
    public void setSessionDate(String sessionDate) { this.sessionDate = sessionDate; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getRequestedAction() { return requestedAction; }
    public void setRequestedAction(String requestedAction) { this.requestedAction = requestedAction; }

    public String getReasonNote() { return reasonNote; }
    public void setReasonNote(String reasonNote) { this.reasonNote = reasonNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getCountsTowardCancelLimit() { return Boolean.TRUE.equals(countsTowardCancelLimit); }
    public void setCountsTowardCancelLimit(Boolean countsTowardCancelLimit) { this.countsTowardCancelLimit = Boolean.TRUE.equals(countsTowardCancelLimit); }

    public Integer getMaxRefundableAmount() { return maxRefundableAmount != null ? maxRefundableAmount : 0; }
    public void setMaxRefundableAmount(Integer maxRefundableAmount) { this.maxRefundableAmount = maxRefundableAmount != null ? maxRefundableAmount : 0; }

    public Integer getApprovedRefundAmount() { return approvedRefundAmount; }
    public void setApprovedRefundAmount(Integer approvedRefundAmount) { this.approvedRefundAmount = approvedRefundAmount; }

    public String getResolvedOutcome() { return resolvedOutcome; }
    public void setResolvedOutcome(String resolvedOutcome) { this.resolvedOutcome = resolvedOutcome; }

    public String getOwnerNote() { return ownerNote; }
    public void setOwnerNote(String ownerNote) { this.ownerNote = ownerNote; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
