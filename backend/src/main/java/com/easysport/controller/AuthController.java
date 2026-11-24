package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.dto.AuthResponse;
import com.easysport.dto.LoginRequest;
import com.easysport.dto.RegisterRequest;
import com.easysport.dto.UpdateProfileRequest;
import com.easysport.entity.User;
import com.easysport.service.AuthService;
import com.easysport.service.RegistrationVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RegistrationVerificationService registration;

    public AuthController(AuthService authService, RegistrationVerificationService registration) {
        this.registration = registration;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationVerificationService.Challenge>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.success("Đã gửi mã xác nhận", registration.start(request)));
    }

    public record VerifyRegistrationRequest(
            @NotBlank @Pattern(regexp = "[0-9a-f-]{36}") String challengeId,
            @NotBlank @Pattern(regexp = "[0-9]{6}", message = "Mã phải có 6 chữ số.") String code) {}

    @PostMapping("/register/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistration(
            @Valid @RequestBody VerifyRegistrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công",
                registration.verify(request.challengeId(), request.code())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.authenticate(authHeader);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Phiên đăng nhập đã hết hạn hoặc không hợp lệ."));
        }
        return ResponseEntity.ok(ApiResponse.success("Thông tin người dùng", AuthService.sanitizeUser(user)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User user = authService.authenticate(authHeader);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Phiên đăng nhập đã hết hạn hoặc không hợp lệ."));
        }
        User updated = authService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", updated));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }
}
