package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "uk_wallet_tx_dedup", columnList = "dedup_key", unique = true),
        @Index(name = "idx_wallet_tx_user", columnList = "user_id,created_at"),
        @Index(name = "idx_wallet_tx_wallet", columnList = "wallet_id,created_at")
})
public class WalletTransaction {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "wallet_id", nullable = false, length = 64)
    private String walletId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    /** CREDIT_REFUND, DEBIT_BOOKING, DEBIT_PACKAGE */
    @Column(nullable = false, length = 40)
    private String type;

    /** CREDIT hoặc DEBIT */
    @Column(nullable = false, length = 10)
    private String direction;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "reference_booking_id", length = 64)
    private String referenceBookingId;

    @Column(name = "reference_package_id", length = 64)
    private String referencePackageId;

    @Column(name = "reference_session_id", length = 64)
    private String referenceSessionId;

    @Column(name = "dedup_key", length = 128, unique = true)
    private String dedupKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public WalletTransaction() {}

    public WalletTransaction(String id, String walletId, String userId, String type, String direction,
                             Integer amount, Integer balanceAfter, String reason,
                             String referenceBookingId, String referencePackageId,
                             String referenceSessionId, String dedupKey, LocalDateTime createdAt) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.type = type;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.referenceBookingId = referenceBookingId;
        this.referencePackageId = referencePackageId;
        this.referenceSessionId = referenceSessionId;
        this.dedupKey = dedupKey;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReferenceBookingId() { return referenceBookingId; }
    public void setReferenceBookingId(String referenceBookingId) { this.referenceBookingId = referenceBookingId; }

    public String getReferencePackageId() { return referencePackageId; }
    public void setReferencePackageId(String referencePackageId) { this.referencePackageId = referencePackageId; }

    public String getReferenceSessionId() { return referenceSessionId; }
    public void setReferenceSessionId(String referenceSessionId) { this.referenceSessionId = referenceSessionId; }

    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
