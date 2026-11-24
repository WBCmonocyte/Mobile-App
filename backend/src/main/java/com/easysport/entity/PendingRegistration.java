package com.easysport.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pending_registrations", indexes = @Index(columnList = "expires_at"))
public class PendingRegistration {
    @Id
    @Column(length = 100)
    public String email;

    @Column(nullable = false, length = 36, unique = true)
    public String challengeId;

    @Column(nullable = false, length = 100)
    public String fullName;

    @Column(nullable = false, length = 20)
    public String phone;

    @Column(nullable = false)
    public String address;

    public LocalDate dateOfBirth;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false, length = 64)
    public String codeDigest;

    @Column(nullable = false)
    public Instant expiresAt;

    @Column(nullable = false)
    public Instant resendAt;

    public int failedAttempts;
}
