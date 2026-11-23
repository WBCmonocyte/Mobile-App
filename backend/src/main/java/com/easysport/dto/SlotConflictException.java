package com.easysport.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SlotConflictException extends RuntimeException {

    private final String errorCode;
    private final List<ConflictSlotDto> conflictingSlots;
    private final LocalDateTime serverNow;

    public SlotConflictException(List<ConflictSlotDto> conflictingSlots, LocalDateTime serverNow) {
        super("Khung giờ này đã được người khác chọn hoặc đang thanh toán. Vui lòng chọn khung giờ khác.");
        this.errorCode = "SLOT_UNAVAILABLE";
        this.conflictingSlots = conflictingSlots;
        this.serverNow = serverNow;
    }

    public String getErrorCode() { return errorCode; }
    public List<ConflictSlotDto> getConflictingSlots() { return conflictingSlots; }
    public LocalDateTime getServerNow() { return serverNow; }
}

