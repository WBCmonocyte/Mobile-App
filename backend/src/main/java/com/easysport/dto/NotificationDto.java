package com.easysport.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDto {

    private String id;
    private String userId;
    private String type;
    private String title;
    private String body;
    private String bookingId;
    private String createdAt;
    private String readAt;
    private boolean read;

    public NotificationDto() {}

    public static NotificationDto from(com.easysport.entity.UserNotification n) {
        NotificationDto dto = new NotificationDto();
        dto.id = n.getId();
        dto.userId = n.getUserId();
        dto.type = n.getType();
        dto.title = n.getTitle();
        dto.body = n.getBody();
        dto.bookingId = n.getBookingId();
        dto.createdAt = n.getCreatedAt() != null
                ? n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        dto.readAt = n.getReadAt() != null
                ? n.getReadAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        dto.read = n.isRead();
        return dto;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getBookingId() { return bookingId; }
    public String getCreatedAt() { return createdAt; }
    public String getReadAt() { return readAt; }
    public boolean isRead() { return read; }
}
