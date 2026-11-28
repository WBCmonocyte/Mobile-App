package com.easysport.service;

import com.easysport.dto.AuthResponse;
import com.easysport.dto.LoginRequest;
import com.easysport.dto.RegisterRequest;
import com.easysport.dto.UpdateProfileRequest;
import com.easysport.entity.User;
import com.easysport.entity.UserSession;
import com.easysport.repository.UserRepository;
import com.easysport.repository.UserSessionRepository;
import com.easysport.util.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       Clock clock) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmailOrPhone(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));

        if (Boolean.TRUE.equals(user.getLocked())) {
            throw new IllegalStateException("Tài khoản đã bị khóa hoặc không tồn tại.");
        }

        if (!PasswordHasher.verify(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu không chính xác.");
        }

        // Tự động nâng cấp mật khẩu từ plain text sang băm PBKDF2 nếu cần
        if (!user.getPassword().contains("$")) {
            user.setPassword(PasswordHasher.hash(request.getPassword()));
            userRepository.save(user);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusDays(30);
        String token = "t_" + UUID.randomUUID().toString().replace("-", "");

        UserSession session = new UserSession(token, user.getId(), now, expiresAt);
        userSessionRepository.save(session);

        User safeUser = sanitizeUser(user);
        return new AuthResponse(token, safeUser, expiresAt);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        LocalDate dob = null;
        try {
            if (request.getDateOfBirth() != null && !request.getDateOfBirth().isBlank()) {
                dob = LocalDate.parse(request.getDateOfBirth().trim());
            }
        } catch (Exception ignored) {}

        return createVerifiedAccount(email, phone, request.getFullName().trim(),
                request.getAddress() != null ? request.getAddress().trim() : "", dob,
                PasswordHasher.hash(request.getPassword()));
    }

    void ensureAvailable(String email, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được đăng ký.");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký.");
        }

    }

    AuthResponse createVerifiedAccount(String email, String phone, String fullName, String address,
                                       LocalDate dob, String passwordHash) {
        ensureAvailable(email, phone);
        User newUser = new User(
                "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                email, passwordHash, fullName, phone, address, dob, "USER", false
        );
        userRepository.save(newUser);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusDays(30);
        String token = "t_" + UUID.randomUUID().toString().replace("-", "");

        UserSession session = new UserSession(token, newUser.getId(), now, expiresAt);
        userSessionRepository.save(session);

        User safeUser = sanitizeUser(newUser);
        return new AuthResponse(token, safeUser, expiresAt);
    }

    @Transactional(readOnly = true)
    public User authenticate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        LocalDateTime now = LocalDateTime.now(clock);

        Optional<UserSession> sessionOpt = userSessionRepository.findByToken(cleanToken);
        if (sessionOpt.isEmpty() || !sessionOpt.get().isValid(now)) {
            return null;
        }

        return userRepository.findById(sessionOpt.get().getUserId()).filter(u -> !Boolean.TRUE.equals(u.getLocked())).orElse(null);
    }

    @Transactional(readOnly = true)
    public User getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + userId));
        return sanitizeUser(user);
    }

    @Transactional
    public User updateProfile(String userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + userId));

        user.setFullName(req.getFullName().trim());
        user.setPhone(req.getPhone().trim());
        if (req.getAddress() != null) {
            user.setAddress(req.getAddress().trim());
        }
        if (req.getDateOfBirth() != null && !req.getDateOfBirth().isBlank()) {
            try {
                user.setDateOfBirth(LocalDate.parse(req.getDateOfBirth().trim()));
            } catch (Exception ignored) {}
        }

        userRepository.save(user);
        return sanitizeUser(user);
    }

    @Transactional
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
            userSessionRepository.findByToken(cleanToken).ifPresent(s -> {
                s.setActive(false);
                userSessionRepository.save(s);
            });
        }
    }

    public static User sanitizeUser(User user) {
        return new User(
                user.getId(),
                user.getEmail(),
                "", // Mật khẩu luôn để trống khi trả về
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getDateOfBirth(),
                user.getRole(),
                user.getLocked()
        );
    }
}

