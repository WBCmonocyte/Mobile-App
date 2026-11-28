package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.dto.BookingPaymentStatusResponse;
import com.easysport.dto.RecurringDtos;
import com.easysport.dto.SlotConflictException;
import com.easysport.entity.User;
import com.easysport.service.AuthService;
import com.easysport.service.RecurringBookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RecurringController {

    private final AuthService authService;
    private final RecurringBookingService recurringService;

    public RecurringController(AuthService authService, RecurringBookingService recurringService) {
        this.authService = authService;
        this.recurringService = recurringService;
    }

    private User requireCustomerOrUser(String authHeader) {
        User user = authService.authenticate(authHeader);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để thực hiện thao tác.");
        }
        return user;
    }

    @PostMapping("/recurring-packages/preview")
    public ResponseEntity<ApiResponse<RecurringDtos.PackagePreviewResponse>> previewPackage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RecurringDtos.PreviewRequest req
    ) {
        User user = authService.authenticate(authHeader);
        String userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(
                "Xem trước lịch cố định 12 buổi",
                recurringService.previewPackage(req, userId)
        ));
    }

    @PostMapping("/recurring-packages/checkout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkoutPackage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RecurringDtos.RecurringCheckoutRequest req
    ) {
        User user = requireCustomerOrUser(authHeader);
        Map<String, Object> result = recurringService.startRecurringCheckout(req, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo gói lịch cố định thành công", result));
    }

    @GetMapping("/recurring-packages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyPackages(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireCustomerOrUser(authHeader);
        return ResponseEntity.ok(ApiResponse.success(
                "Danh sách lịch cố định của tôi",
                recurringService.getMyPackages(user.getId())
        ));
    }

    @GetMapping("/recurring-packages/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPackageDetail(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireCustomerOrUser(authHeader);
        return ResponseEntity.ok(ApiResponse.success(
                "Chi tiết gói lịch cố định",
                recurringService.getPackageDetail(id, user.getId())
        ));
    }

    @GetMapping("/recurring-packages/{id}/payment-status")
    public ResponseEntity<ApiResponse<BookingPaymentStatusResponse>> getPackagePaymentStatus(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireCustomerOrUser(authHeader);
        return ResponseEntity.ok(ApiResponse.success(
                "Trạng thái thanh toán gói lịch cố định",
                recurringService.getPackagePaymentStatus(id, user.getId())
        ));
    }

    @PostMapping("/recurring-packages/{id}/release")
    public ResponseEntity<ApiResponse<Void>> releasePackageHold(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireCustomerOrUser(authHeader);
        recurringService.releasePendingPackageHold(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã giải phóng giữ chỗ 12 buổi", null));
    }

    @GetMapping("/recurring-sessions/{id}/cancel-preview")
    public ResponseEntity<ApiResponse<RecurringDtos.CancelPreviewResponse>> previewSessionAction(
            @PathVariable String id,
            @RequestParam(value = "reasonType", required = false, defaultValue = "PERSONAL") String reasonType,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User user = requireCustomerOrUser(authHeader);
        return ResponseEntity.ok(ApiResponse.success(
                "Xem trước phương án xử lý buổi",
                recurringService.previewSessionAction(id, reasonType, user.getId())
        ));
    }

    @PostMapping("/recurring-sessions/{id}/cancel-or-postpone")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelOrPostponeSession(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RecurringDtos.CancelOrPostponeRequest req
    ) {
        User user = requireCustomerOrUser(authHeader);
        Map<String, Object> result = recurringService.cancelOrPostponeSession(id, req, user.getId());
        return ResponseEntity.ok(ApiResponse.success(String.valueOf(result.get("message")), result));
    }

    @PostMapping("/recurring-credits/{creditId}/redeem")
    public ResponseEntity<ApiResponse<Map<String, Object>>> redeemCredit(
            @PathVariable String creditId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RecurringDtos.RedeemCreditRequest req
    ) {
        User user = requireCustomerOrUser(authHeader);
        Map<String, Object> result = recurringService.redeemCredit(creditId, req, user.getId());
        return ResponseEntity.ok(ApiResponse.success(String.valueOf(result.get("message")), result));
    }

    @ExceptionHandler(SlotConflictException.class)
    public ResponseEntity<Map<String, Object>> handleSlotConflict(SlotConflictException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("errorCode", ex.getErrorCode());
        body.put("message", ex.getMessage());
        body.put("conflictingSlots", ex.getConflictingSlots());
        body.put("serverNow", ex.getServerNow());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
