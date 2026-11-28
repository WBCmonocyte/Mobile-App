package com.easysport.dto;

import java.time.LocalDateTime;

public class ConflictSlotDto {
    private String slot;
    private String status; // HELD or BOOKED
    private LocalDateTime expiresAt;
    private String dateLabel;
    private Integer sessionIndex;

    public ConflictSlotDto() {}

    public ConflictSlotDto(String slot, String status, LocalDateTime expiresAt) {
        this(slot, status, expiresAt, null, null);
    }

    public ConflictSlotDto(String slot, String status, LocalDateTime expiresAt, String dateLabel, Integer sessionIndex) {
        this.slot = slot;
        this.status = status;
        this.expiresAt = expiresAt;
        this.dateLabel = dateLabel;
        this.sessionIndex = sessionIndex;
    }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public Integer getSessionIndex() { return sessionIndex; }
    public void setSessionIndex(Integer sessionIndex) { this.sessionIndex = sessionIndex; }
}

