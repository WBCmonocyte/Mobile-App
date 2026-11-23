package com.easysport;

import com.easysport.dto.AuthResponse;
import com.easysport.dto.LoginRequest;
import com.easysport.dto.RegisterRequest;
import com.easysport.dto.UpdateProfileRequest;
import com.easysport.entity.User;
import com.easysport.entity.UserSession;
import com.easysport.repository.UserRepository;
import com.easysport.repository.UserSessionRepository;
import com.easysport.service.AuthService;
import com.easysport.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private UserSessionRepository userSessionRepository;
    private AuthService authService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userSessionRepository = mock(UserSessionRepository.class);
        clock = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneId.of("UTC"));
        authService = new AuthService(userRepository, userSessionRepository, clock);
    }

    @Test
    @DisplayName("Đăng ký thành công: mật khẩu được băm bằng PBKDF2 và tạo phiên token")
    void testRegister_success_hashesPasswordAndGeneratesToken() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Lê Văn C");
        req.setEmail("levanc@gmail.com");
        req.setPhone("0933445566");
        req.setPassword("SecretPass123");
        req.setAddress("TP. Hồ Chí Minh");
        req.setDateOfBirth("1995-10-15");

        when(userRepository.existsByEmail("levanc@gmail.com")).thenReturn(false);
        when(userRepository.existsByPhone("0933445566")).thenReturn(false);

        AuthResponse resp = authService.register(req);

        assertNotNull(resp.getToken());
        assertTrue(resp.getToken().startsWith("t_"));
        assertNotNull(resp.getUser());
        assertEquals("Lê Văn C", resp.getUser().getFullName());
        assertEquals("", resp.getUser().getPassword(), "Mật khẩu an toàn phải được ẩn");

        verify(userRepository).save(argThat(u -> {
            assertNotNull(u.getPassword());
            assertTrue(u.getPassword().contains("$"), "Mật khẩu lưu trong DB phải ở dạng băm PBKDF2 salt$hash");
            assertTrue(PasswordHasher.verify("SecretPass123", u.getPassword()));
            return true;
        }));

        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Đăng nhập thành công với mật khẩu đã băm và trả về token")
    void testLogin_success_withHashedPassword() {
        String hashed = PasswordHasher.hash("MyPassword123");
        User user = new User("u_100", "user@test.com", hashed, "Test User", "0900112233", "HCM", LocalDate.of(1990, 1, 1), "USER", false);

        when(userRepository.findByEmailOrPhone("user@test.com", "user@test.com")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("MyPassword123");

        AuthResponse resp = authService.login(req);

        assertNotNull(resp.getToken());
        assertEquals("Test User", resp.getUser().getFullName());
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi mật khẩu sai")
    void testLogin_wrongPassword() {
        String hashed = PasswordHasher.hash("CorrectPass");
        User user = new User("u_100", "user@test.com", hashed, "Test User", "0900112233", "HCM", null, "USER", false);

        when(userRepository.findByEmailOrPhone("user@test.com", "user@test.com")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("WrongPass");

        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Xác thực token thành công trả về User tương ứng")
    void testAuthenticate_validToken() {
        LocalDateTime now = LocalDateTime.now(clock);
        UserSession session = new UserSession("t_valid123", "u_100", now, now.plusDays(10));
        User user = new User("u_100", "user@test.com", "hash", "User 100", "0900112233", "HCM", null, "USER", false);

        when(userSessionRepository.findByToken("t_valid123")).thenReturn(Optional.of(session));
        when(userRepository.findById("u_100")).thenReturn(Optional.of(user));

        User authenticated = authService.authenticate("Bearer t_valid123");
        assertNotNull(authenticated);
        assertEquals("u_100", authenticated.getId());
    }

    @Test
    @DisplayName("Xác thực token thất bại khi token hết hạn hoặc không tồn tại")
    void testAuthenticate_expiredOrInvalidToken() {
        LocalDateTime now = LocalDateTime.now(clock);
        UserSession expiredSession = new UserSession("t_expired", "u_100", now.minusDays(5), now.minusDays(1));

        when(userSessionRepository.findByToken("t_expired")).thenReturn(Optional.of(expiredSession));

        User authenticated = authService.authenticate("Bearer t_expired");
        assertNull(authenticated, "Token đã hết hạn phải trả về null");

        User notFound = authService.authenticate("Bearer t_non_existent");
        assertNull(notFound);
    }

    @Test
    @DisplayName("Cập nhật hồ sơ người dùng thành công")
    void testUpdateProfile_success() {
        User user = new User("u_100", "user@test.com", "hash", "Old Name", "0900000000", "Old Address", LocalDate.of(1995, 5, 5), "USER", false);
        when(userRepository.findById("u_100")).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest("New Full Name", "0988776655", "New Address", "1996-06-06");
        User updated = authService.updateProfile("u_100", req);

        assertEquals("New Full Name", updated.getFullName());
        assertEquals("0988776655", updated.getPhone());
        assertEquals("New Address", updated.getAddress());
        assertEquals(LocalDate.of(1996, 6, 6), updated.getDateOfBirth());
        verify(userRepository).save(user);
    }
}

