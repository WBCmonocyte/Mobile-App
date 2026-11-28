package com.easysport.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_wallets", indexes = {
        @Index(name = "uk_user_wallets_user_id", columnList = "user_id", unique = true)
})
public class UserWallet {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true, length = 50)
    private String userId;

    @Column(nullable = false)
    private Integer balance = 0;

    @Column(nullable = false, length = 10)
    private String currency = "VND";

    @Version
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserWallet() {}

    public UserWallet(String id, String userId, Integer balance, LocalDateTime now) {
        this.id = id;
        this.userId = userId;
        this.balance = balance != null ? Math.max(0, balance) : 0;
        this.currency = "VND";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Integer getBalance() { return balance != null ? balance : 0; }
    public void setBalance(Integer balance) {
        if (balance != null && balance < 0) {
            throw new IllegalStateException("Số dư ví không thể âm.");
        }
        this.balance = balance != null ? balance : 0;
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
