package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.entity.User;
import com.easysport.entity.WalletTransaction;
import com.easysport.service.AuthService;
import com.easysport.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final AuthService authService;
    private final WalletService walletService;

    public WalletController(AuthService authService, WalletService walletService) {
        this.authService = authService;
        this.walletService = walletService;
    }

    private User requireUser(String authHeader) {
        User user = authService.authenticate(authHeader);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn hoặc không hợp lệ.");
        }
        return user;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyWallet(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String requestedUserId
    ) {
        User user = requireUser(authHeader);
        if (requestedUserId != null && !requestedUserId.isBlank() && !user.getId().equals(requestedUserId.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem Ví EasySport của người khác.");
        }
        return ResponseEntity.ok(ApiResponse.success("Thông tin Ví EasySport", walletService.getWalletSummary(user.getId())));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransaction>>> getMyWalletTransactions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String requestedUserId
    ) {
        User user = requireUser(authHeader);
        if (requestedUserId != null && !requestedUserId.isBlank() && !user.getId().equals(requestedUserId.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem lịch sử Ví EasySport của người khác.");
        }
        return ResponseEntity.ok(ApiResponse.success("Lịch sử giao dịch ví", walletService.getTransactions(user.getId())));
    }
}
