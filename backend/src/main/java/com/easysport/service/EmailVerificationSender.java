package com.easysport.service;

public interface EmailVerificationSender {
    void sendRegistrationCode(String email, String code);
}
