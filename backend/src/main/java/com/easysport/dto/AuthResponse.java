package com.easysport.dto;

import com.easysport.entity.User;
import java.time.LocalDateTime;

public class AuthResponse {
    private String token;
    private User user;
    private LocalDateTime expiresAt;

    public AuthResponse() {}

    public AuthResponse(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}

