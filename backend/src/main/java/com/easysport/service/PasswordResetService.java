package com.easysport.service;

import com.easysport.repository.UserRepository;
import com.easysport.repository.UserSessionRepository;
import com.easysport.util.PasswordHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final MailjetSender sender;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users, UserSessionRepository sessions, MailjetSender sender) {
        this.users = users;
        this.sessions = sessions;
        this.sender = sender;
    }

    @Transactional
    public void request(String email) {
        var user = users.lockByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || Boolean.TRUE.equals(user.getLocked())) return;
        var now = Instant.now();
        if (user.resetExpires != null && user.resetExpires.isAfter(now.plusSeconds(240))) return;
        var code = String.format(Locale.ROOT, "%06d", random.nextInt(1000000));
        user.resetHash = PasswordHasher.hash(code);
        user.resetExpires = now.plusSeconds(300);
        user.resetAttempts = 0;
        sender.sendResetCode(user.getEmail(), code);
    }

    @Transactional
    public boolean confirm(String email, String code, String password) {
        var user = users.lockByEmail(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null || Boolean.TRUE.equals(user.getLocked()) || user.resetHash == null
                || user.resetExpires == null || !user.resetExpires.isAfter(Instant.now())
                || user.resetAttempts == null || user.resetAttempts >= 5) return false;
        user.resetAttempts++;
        if (!PasswordHasher.verify(code, user.resetHash)) return false;
        user.setPassword(PasswordHasher.hash(password));
        user.resetHash = null;
        user.resetExpires = null;
        user.resetAttempts = null;
        sessions.deactivateAllByUserId(user.getId());
        return true;
    }
}
