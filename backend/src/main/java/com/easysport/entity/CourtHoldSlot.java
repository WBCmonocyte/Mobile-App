package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "court_hold_slots", indexes = {
        @Index(name = "idx_hold_slots_resource_date", columnList = "resource_id,booking_date,status"),
        @Index(name = "idx_hold_slots_hold", columnList = "hold_id"),
        @Index(name = "idx_hold_slots_expires", columnList = "expires_at")
})
public class CourtHoldSlot {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "hold_id", nullable = false, length = 64)
    private String holdId;

    @Column(name = "court_id", nullable = false, length = 50)
    private String courtId;

    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;

    @Column(name = "booking_date", nullable = false, length = 50)
    private String dateLabel;

    @Column(name = "start_minute", nullable = false)
    private Integer startMinute;

    @Column(name = "end_minute", nullable = false)
    private Integer endMinute;

    @Column(name = "slot_label", nullable = false, length = 50)
    private String slotLabel;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "HELD"; // HELD, BOOKED, EXPIRED, RELEASED

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public CourtHoldSlot() {}

    public CourtHoldSlot(String id, String holdId, String courtId, String resourceId,
                         String dateLabel, Integer startMinute, Integer endMinute,
                         String slotLabel, String status, LocalDateTime expiresAt) {
        this.id = id;
        this.holdId = holdId;
        this.courtId = courtId;
        this.resourceId = resourceId;
        this.dateLabel = dateLabel;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.slotLabel = slotLabel;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHoldId() { return holdId; }
    public void setHoldId(String holdId) { this.holdId = holdId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public Integer getStartMinute() { return startMinute; }
    public void setStartMinute(Integer startMinute) { this.startMinute = startMinute; }

    public Integer getEndMinute() { return endMinute; }
    public void setEndMinute(Integer endMinute) { this.endMinute = endMinute; }

    public String getSlotLabel() { return slotLabel; }
    public void setSlotLabel(String slotLabel) { this.slotLabel = slotLabel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}

