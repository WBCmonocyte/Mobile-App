package com.easysport.service;

import com.easysport.entity.UserWallet;
import com.easysport.entity.WalletTransaction;
import com.easysport.repository.UserWalletRepository;
import com.easysport.repository.WalletTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Dịch vụ Ví EasySport (VND nguyên):
 * - Mỗi khách hàng có duy nhất một ví, số dư khởi tạo 0đ.
 * - Khóa bi quan (PESSIMISTIC_WRITE) khi trừ hoặc cộng tiền để bảo đảm không âm và chống ghi đè.
 * - Chống hoàn tiền trùng lặp qua dedupKey duy nhất.
 */
@Service
public class WalletService {

    private final UserWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final Clock clock;

    public WalletService(UserWalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository,
                         Clock clock) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserWallet getOrCreateWallet(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã người dùng.");
        }
        Optional<UserWallet> existing = walletRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        UserWallet created = new UserWallet("wal_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), userId, 0, now);
        try {
            return walletRepository.saveAndFlush(created);
        } catch (DataIntegrityViolationException ex) {
            return walletRepository.findByUserId(userId).orElseThrow(() -> ex);
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserWallet lockOrCreateWallet(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Thiếu mã người dùng.");
        }
        Optional<UserWallet> locked = walletRepository.lockByUserId(userId);
        if (locked.isPresent()) {
            return locked.get();
        }
        getOrCreateWallet(userId);
        return walletRepository.lockByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Không thể khóa Ví EasySport của người dùng."));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> getWalletSummary(String userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        List<WalletTransaction> history = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("walletId", wallet.getId());
        result.put("userId", wallet.getUserId());
        result.put("balance", wallet.getBalance());
        result.put("currency", wallet.getCurrency());
        result.put("updatedAt", wallet.getUpdatedAt());
        result.put("transactions", history);
        return result;
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> getTransactions(String userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Cộng tiền hoàn vào ví bên trong transaction của nghiệp vụ gọi.
     * Nếu dedupKey đã tồn tại -> từ chối với HTTP 409 để tuyệt đối không hoàn tiền hai lần.
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public WalletTransaction creditRefund(
            String userId,
            int amount,
            String reason,
            String referenceBookingId,
            String referencePackageId,
            String referenceSessionId,
            String dedupKey
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0 đ.");
        }
        if (dedupKey != null && !dedupKey.isBlank() && transactionRepository.existsByDedupKey(dedupKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khoản hoàn tiền này đã được ghi nhận vào ví trước đó.");
        }

        UserWallet wallet = lockOrCreateWallet(userId);
        int nextBalance = Math.addExact(wallet.getBalance(), amount);
        LocalDateTime now = LocalDateTime.now(clock);
        wallet.setBalance(nextBalance);
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction(
                "wtx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                wallet.getId(),
                userId,
                "CREDIT_REFUND",
                "CREDIT",
                amount,
                nextBalance,
                reason != null && !reason.isBlank() ? reason : "Hoàn tiền vào Ví EasySport",
                referenceBookingId,
                referencePackageId,
                referenceSessionId,
                dedupKey,
                now
        );
        return transactionRepository.save(tx);
    }

    /**
     * Trừ tiền trong ví nguyên tử dưới PESSIMISTIC_WRITE.
     * Đảm bảo số dư không bao giờ xuống âm kể cả khi nhiều giao dịch cùng trừ đồng thời.
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public WalletTransaction debitWallet(
            String userId,
            int amount,
            String type,
            String reason,
            String referenceBookingId,
            String referencePackageId,
            String dedupKey
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán bằng ví phải lớn hơn 0 đ.");
        }
        if (dedupKey != null && !dedupKey.isBlank()) {
            Optional<WalletTransaction> existing = transactionRepository.findByDedupKey(dedupKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        UserWallet wallet = lockOrCreateWallet(userId);
        int current = wallet.getBalance();
        if (current < amount) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Số dư Ví EasySport không đủ (%s đ < %s đ). Vui lòng dùng SePay.",
                            String.format("%,d", current).replace(",", "."),
                            String.format("%,d", amount).replace(",", "."))
            );
        }

        int nextBalance = current - amount;
        LocalDateTime now = LocalDateTime.now(clock);
        wallet.setBalance(nextBalance);
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction(
                "wtx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                wallet.getId(),
                userId,
                type != null && !type.isBlank() ? type : "DEBIT_BOOKING",
                "DEBIT",
                amount,
                nextBalance,
                reason != null && !reason.isBlank() ? reason : "Thanh toán đặt sân bằng Ví EasySport",
                referenceBookingId,
                referencePackageId,
                null,
                dedupKey,
                now
        );
        return transactionRepository.save(tx);
    }
}
