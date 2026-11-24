package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.dto.NotificationDto;
import com.easysport.entity.User;
import com.easysport.entity.UserNotification;
import com.easysport.service.AuthService;
import com.easysport.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User requireAuth(String authHeader) {
        User user = authService.authenticate(authHeader);
        if (user == null) throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập lại.");
        return user;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * GET /api/notifications
     * Trả danh sách tối đa 50 thông báo mới nhất của user hiện tại.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireAuth(authHeader);
        List<NotificationDto> dtos = notificationService.getNotifications(user.getId())
                .stream().map(NotificationDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * PUT /api/notifications/{id}/read
     * Đánh dấu một thông báo đã đọc.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireAuth(authHeader);
        boolean found = notificationService.markRead(id, user.getId());
        if (!found) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy thông báo."));
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * PUT /api/notifications/read-all
     * Đánh dấu tất cả thông báo của user đã đọc.
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireAuth(authHeader);
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * GET /api/notifications/unread-count
     * Trả số thông báo chưa đọc.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireAuth(authHeader);
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
