package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_notifications",
    indexes = {
        @Index(name = "idx_notif_user", columnList = "user_id"),
        @Index(name = "idx_notif_booking", columnList = "booking_id")
    }
)
public class UserNotification {

    @Id
    @Column(length = 64, nullable = false)
    private String id;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /** BOOKING_CONFIRMED | PAYMENT_RECEIVED | REMINDER_2H | SYSTEM */
    @Column(length = 30, nullable = false)
    private String type;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "booking_id", length = 64)
    private String bookingId;

    /** Dùng để chặn duplicate; ví dụ: "BOOKING_CONFIRMED:bookingId" */
    @Column(name = "dedup_key", length = 200, unique = true)
    private String dedupKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public UserNotification() {}

    public UserNotification(String id, String userId, String type, String title, String body,
                             String bookingId, String dedupKey, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.bookingId = bookingId;
        this.dedupKey = dedupKey;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public boolean isRead() { return readAt != null; }
}
