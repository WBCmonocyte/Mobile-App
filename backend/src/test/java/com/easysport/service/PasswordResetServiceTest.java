package com.easysport.service;

import com.easysport.entity.User;
import com.easysport.repository.UserRepository;
import com.easysport.repository.UserSessionRepository;
import com.easysport.util.PasswordHasher;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserSessionRepository sessions = mock(UserSessionRepository.class);
    private final MailjetSender mail = mock(MailjetSender.class);
    private final PasswordResetService service = new PasswordResetService(users, sessions, mail);
    private final User user = new User();
    private final String email = "demo@example.com";

    private String requestCode() {
        user.setId("test-id");
        user.setEmail(email);
        when(users.lockByEmail(email)).thenReturn(Optional.of(user));
        service.request(email);
        var code = ArgumentCaptor.forClass(String.class);
        verify(mail).sendResetCode(eq(email), code.capture());
        return code.getValue();
    }

    @Test void resetsOnceAndRevokesSessions() {
        String code = requestCode();
        service.request(email);
        verify(mail, times(1)).sendResetCode(eq(email), anyString());
        assertTrue(service.confirm(email, code, "NewPassword123"));
        assertTrue(PasswordHasher.verify("NewPassword123", user.getPassword()));
        assertFalse(service.confirm(email, code, "OtherPass123"));
        verify(sessions).deactivateAllByUserId("test-id");
    }

    @Test void rejectsCorrectCodeAfterFiveFailures() {
        String code = requestCode();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) assertFalse(service.confirm(email, wrong, "NewPassword123"));
        assertFalse(service.confirm(email, code, "NewPassword123"));
        verifyNoInteractions(sessions);
    }

    @Test void rejectsExpiredCode() {
        String code = requestCode();
        user.resetExpires = Instant.now().minusSeconds(1);
        assertFalse(service.confirm(email, code, "NewPassword123"));
        verifyNoInteractions(sessions);
    }
}
