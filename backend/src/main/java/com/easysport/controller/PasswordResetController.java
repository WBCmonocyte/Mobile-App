package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {
    private final PasswordResetService resets;

    public PasswordResetController(PasswordResetService resets) { this.resets = resets; }

    public record Request(@NotBlank @Email @Size(max=100) String email) {}
    public record Confirm(@NotBlank @Email @Size(max=100) String email,
                          @NotBlank @Pattern(regexp="[0-9]{6}") String code,
                          @NotBlank @Size(min=8,max=128) @Pattern(regexp="(?s).*[0-9].*") String password) {}

    @PostMapping
    public ApiResponse<Void> request(@Valid @RequestBody Request request) {
        resets.request(request.email());
        return ApiResponse.success("Nếu email đã đăng ký, bạn sẽ nhận được mã xác minh.", null);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirm(@Valid @RequestBody Confirm request) {
        if (!resets.confirm(request.email(), request.code(), request.password()))
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã không hợp lệ hoặc đã hết hạn."));
        return ResponseEntity.ok(ApiResponse.success("Đã đặt lại mật khẩu.", null));
    }
}
