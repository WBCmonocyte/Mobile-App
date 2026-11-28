package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_credits", indexes = {
        @Index(name = "uk_rec_credit_source_session", columnList = "source_session_id", unique = true),
        @Index(name = "idx_rec_credit_pkg_status", columnList = "package_id,status"),
        @Index(name = "idx_rec_credit_user", columnList = "user_id,status")
})
public class RecurringCredit {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "package_id", nullable = false, length = 64)
    private String packageId;

    @Column(name = "source_session_id", nullable = false, unique = true, length = 64)
    private String sourceSessionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(name = "court_name", nullable = false, length = 150)
    private String courtName;

    @Column(name = "field_id", nullable = false, length = 50)
    private String fieldId;

    @Column(name = "field_detail", nullable = false, length = 100)
    private String fieldDetail;

    @Column(name = "slot_count", nullable = false)
    private Integer slotCount = 1;

    @Column(name = "original_price_snapshot", nullable = false)
    private Integer originalPriceSnapshot = 0;

    /** AVAILABLE hoặc USED */
    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE";

    @Column(name = "redeemed_booking_id", length = 64)
    private String redeemedBookingId;

    @Column(name = "redeemed_date", length = 20)
    private String redeemedDate;

    @Column(name = "redeemed_time_range", length = 100)
    private String redeemedTimeRange;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Version
    private Long version = 0L;

    public RecurringCredit() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }

    public String getSourceSessionId() { return sourceSessionId; }
    public void setSourceSessionId(String sourceSessionId) { this.sourceSessionId = sourceSessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getFieldId() { return fieldId; }
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }

    public String getFieldDetail() { return fieldDetail; }
    public void setFieldDetail(String fieldDetail) { this.fieldDetail = fieldDetail; }

    public Integer getSlotCount() { return slotCount != null ? slotCount : 1; }
    public void setSlotCount(Integer slotCount) { this.slotCount = slotCount != null ? slotCount : 1; }

    public Integer getOriginalPriceSnapshot() { return originalPriceSnapshot != null ? originalPriceSnapshot : 0; }
    public void setOriginalPriceSnapshot(Integer originalPriceSnapshot) { this.originalPriceSnapshot = originalPriceSnapshot != null ? originalPriceSnapshot : 0; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRedeemedBookingId() { return redeemedBookingId; }
    public void setRedeemedBookingId(String redeemedBookingId) { this.redeemedBookingId = redeemedBookingId; }

    public String getRedeemedDate() { return redeemedDate; }
    public void setRedeemedDate(String redeemedDate) { this.redeemedDate = redeemedDate; }

    public String getRedeemedTimeRange() { return redeemedTimeRange; }
    public void setRedeemedTimeRange(String redeemedTimeRange) { this.redeemedTimeRange = redeemedTimeRange; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
