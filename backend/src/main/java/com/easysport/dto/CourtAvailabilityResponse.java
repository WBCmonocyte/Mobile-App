package com.easysport.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CourtAvailabilityResponse {
    private String courtId;
    private String fieldDetail;
    private String dateLabel;
    private List<AvailabilitySlotDto> slots;
    private LocalDateTime serverNow;

    public CourtAvailabilityResponse() {}

    public CourtAvailabilityResponse(String courtId, String fieldDetail, String dateLabel,
                                   List<AvailabilitySlotDto> slots, LocalDateTime serverNow) {
        this.courtId = courtId;
        this.fieldDetail = fieldDetail;
        this.dateLabel = dateLabel;
        this.slots = slots;
        this.serverNow = serverNow;
    }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getFieldDetail() { return fieldDetail; }
    public void setFieldDetail(String fieldDetail) { this.fieldDetail = fieldDetail; }

    public String getDateLabel() { return dateLabel; }
    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }

    public List<AvailabilitySlotDto> getSlots() { return slots; }
    public void setSlots(List<AvailabilitySlotDto> slots) { this.slots = slots; }

    public LocalDateTime getServerNow() { return serverNow; }
    public void setServerNow(LocalDateTime serverNow) { this.serverNow = serverNow; }
}

