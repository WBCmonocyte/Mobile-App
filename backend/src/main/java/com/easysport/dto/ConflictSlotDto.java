package com.easysport.dto;

import java.time.LocalDateTime;

public class ConflictSlotDto {
    private String slot;
    private String status; // HELD or BOOKED
    private LocalDateTime expiresAt;

    public ConflictSlotDto() {}

    public ConflictSlotDto(String slot, String status, LocalDateTime expiresAt) {
        this.slot = slot;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}

