package com.easysport.dto;

import java.time.LocalDateTime;

public class AvailabilitySlotDto {
    private String slot; // e.g. "18:00 - 19:30"
    private String status; // "AVAILABLE", "HELD", "BOOKED"
    private LocalDateTime expiresAt; // if HELD

    public AvailabilitySlotDto() {}

    public AvailabilitySlotDto(String slot, String status, LocalDateTime expiresAt) {
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

