package com.easysport.service;

import com.easysport.dto.RegisterRequest;
import com.easysport.entity.PendingRegistration;
import com.easysport.repository.PendingRegistrationRepository;
import com.easysport.repository.UserRepository;
import com.easysport.repository.UserSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationVerificationServiceTest {
    private final PendingRegistrationRepository pending = mock(PendingRegistrationRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final UserSessionRepository sessionRepo = mock(UserSessionRepository.class);
    private final EmailVerificationSender mail = mock(EmailVerificationSender.class);
    private final Instant now = Instant.parse("2026-11-24T00:00:00Z");
    private final AuthService auth = new AuthService(userRepo, sessionRepo, Clock.fixed(now, ZoneOffset.UTC));
    private final RegistrationVerificationService registration =
            new RegistrationVerificationService(pending, auth, mail, Clock.fixed(now, ZoneOffset.UTC), "test-secret");

    private PendingRegistration entry;
    private String code;

    @BeforeEach
    void startRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("EasySport Test");
        request.setPhone("0901234567");
        request.setEmail("test@example.com");
        request.setAddress("Hà Nội");
        request.setDateOfBirth("2001-01-01");
        request.setPassword("Example123");
        var challenge = registration.start(request);

        var saved = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pending).saveAndFlush(saved.capture());
        entry = saved.getValue();
        var sent = ArgumentCaptor.forClass(String.class);
        verify(mail).sendRegistrationCode(eq(entry.email), sent.capture());
        code = sent.getValue();
        when(pending.findByChallengeId(challenge.challengeId())).thenReturn(Optional.of(entry));
        when(pending.findByEmail(entry.email)).thenReturn(Optional.of(entry));
        assertTrue(code.matches("[0-9]{6}"));
        assertNotEquals(code, entry.codeDigest);
        assertNotEquals(request.getPassword(), entry.passwordHash);
        verify(userRepo, never()).save(any());
    }

    @Test
    void createsAccountOnlyAfterCorrectCode() {
        var response = registration.verify(entry.challengeId, code);
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(entry.email, response.getUser().getEmail());
        verify(userRepo).save(any(com.easysport.entity.User.class));
        verify(sessionRepo).save(any(com.easysport.entity.UserSession.class));
        verify(pending).delete(entry);
    }

    @Test
    void refusesCorrectCodeAfterFiveIncorrectAttempts() {
        String wrong = "000000".equals(code) ? "111111" : "000000";
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(IllegalArgumentException.class, () -> registration.verify(entry.challengeId, wrong));
        }
        assertEquals(5, entry.failedAttempts);
        assertThrows(IllegalArgumentException.class, () -> registration.verify(entry.challengeId, code));
        verify(pending, never()).delete(any());
    }

    @Test
    void refusesExpiredCode() {
        entry.expiresAt = now;
        assertThrows(IllegalArgumentException.class, () -> registration.verify(entry.challengeId, code));
        verify(pending, never()).delete(any());
    }

    @Test
    void refusesImmediateResend() {
        RegisterRequest request = new RegisterRequest();
        request.setPhone(entry.phone);
        request.setEmail(entry.email);
        request.setAddress(entry.address);
        request.setDateOfBirth(entry.dateOfBirth.toString());
        var error = assertThrows(ResponseStatusException.class, () -> registration.start(request));
        assertEquals(429, error.getStatusCode().value());
        verifyNoMoreInteractions(mail);
    }
}
