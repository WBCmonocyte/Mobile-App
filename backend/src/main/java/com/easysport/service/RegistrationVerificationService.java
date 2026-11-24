package com.easysport.service;

import com.easysport.dto.AuthResponse;
import com.easysport.dto.RegisterRequest;
import com.easysport.entity.PendingRegistration;
import com.easysport.repository.PendingRegistrationRepository;
import com.easysport.util.PasswordHasher;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationVerificationService {
    private final PendingRegistrationRepository pending;
    private final AuthService auth;
    private final EmailVerificationSender mail;
    private final Clock clock;
    private final String secret;
    private final SecureRandom random = new SecureRandom();

    public RegistrationVerificationService(PendingRegistrationRepository pending, AuthService auth,
                                           EmailVerificationSender mail, Clock clock,
                                           @Value("${MAILJET_SECRET_KEY:}") String secret) {
        this.pending = pending;
        this.auth = auth;
        this.mail = mail;
        this.clock = clock;
        this.secret = secret;
    }

    public record Challenge(String challengeId) {}

    @Transactional
    public Challenge start(RegisterRequest request) {
        if (secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Hệ thống chưa cấu hình gửi email.");
        }
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone().replace(" ", "").trim();
        if (!phone.matches("(0|[+]84)[0-9]{9,10}")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        String address = request.getAddress();
        if (address == null || address.isBlank() || address.length() > 255) {
            throw new IllegalArgumentException("Địa chỉ cần có từ 1 đến 255 ký tự.");
        }
        LocalDate dob;
        try {
            dob = LocalDate.parse(request.getDateOfBirth() == null ? "" : request.getDateOfBirth().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
        }
        if (dob.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
        }
        auth.ensureAvailable(email, phone);
        Instant now = clock.instant();
        PendingRegistration entry = pending.findByEmail(email).orElseGet(PendingRegistration::new);
        if (entry.resendAt != null && now.isBefore(entry.resendAt)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng đợi 60 giây giữa hai lần gửi mã.");
        }
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        entry.email = email;
        entry.challengeId = UUID.randomUUID().toString();
        entry.fullName = request.getFullName().trim();
        entry.phone = phone;
        entry.address = address.trim();
        entry.dateOfBirth = dob;
        entry.passwordHash = PasswordHasher.hash(request.getPassword());
        entry.codeDigest = digest(entry.challengeId, code);
        entry.expiresAt = now.plusSeconds(300);
        entry.resendAt = now.plusSeconds(60);
        entry.failedAttempts = 0;
        pending.saveAndFlush(entry);
        mail.sendRegistrationCode(email, code);
        return new Challenge(entry.challengeId);
    }

    @Transactional(noRollbackFor = InvalidCode.class)
    public AuthResponse verify(String challengeId, String code) {
        PendingRegistration entry = pending.findByChallengeId(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Mã đã hết hiệu lực. Vui lòng gửi mã mới."));
        if (!clock.instant().isBefore(entry.expiresAt) || entry.failedAttempts >= 5) {
            throw new IllegalArgumentException("Mã đã hết hạn hoặc hết lượt thử. Vui lòng gửi mã mới.");
        }
        byte[] expected = HexFormat.of().parseHex(entry.codeDigest);
        byte[] actual = HexFormat.of().parseHex(digest(challengeId, code));
        if (!MessageDigest.isEqual(expected, actual)) {
            entry.failedAttempts++;
            throw new InvalidCode();
        }
        AuthResponse response = auth.createVerifiedAccount(entry.email, entry.phone, entry.fullName,
                entry.address, entry.dateOfBirth, entry.passwordHash);
        pending.delete(entry);
        return response;
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cleanExpired() {
        pending.deleteByExpiresAtBefore(clock.instant().minusSeconds(3600));
    }

    private String digest(String challengeId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((challengeId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không thể xử lý mã xác nhận.", e);
        }
    }

    private static class InvalidCode extends IllegalArgumentException {
        InvalidCode() {
            super("Mã xác nhận không đúng.");
        }
    }
}
