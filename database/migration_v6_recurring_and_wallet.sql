-- ========================================================
-- MIGRATION V6: Đặt lịch cố định (Recurring Bookings) & Ví EasySport (Wallet)
-- Tương thích: MySQL 8.4 trên Railway và Local
-- Không dùng DELIMITER hay cú pháp MariaDB để chạy trực tiếp trong mọi trình soạn thảo SQL
-- ========================================================

-- 1. Tạo bảng user_wallets (mỗi khách hàng một ví VND nguyên, balance >= 0)
CREATE TABLE IF NOT EXISTS `user_wallets` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(50) NOT NULL,
    `balance` INT NOT NULL DEFAULT 0,
    `currency` VARCHAR(10) NOT NULL DEFAULT 'VND',
    `version` BIGINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_wallets_user_id` (`user_id`),
    CONSTRAINT `chk_user_wallets_non_negative` CHECK (`balance` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tạo bảng wallet_transactions (lịch sử biến động số dư, chặn hoàn trùng bằng dedup_key)
CREATE TABLE IF NOT EXISTS `wallet_transactions` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `wallet_id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `type` VARCHAR(40) NOT NULL,
    `direction` VARCHAR(10) NOT NULL,
    `amount` INT NOT NULL,
    `balance_after` INT NOT NULL,
    `reason` VARCHAR(500) NOT NULL,
    `reference_booking_id` VARCHAR(64) NULL,
    `reference_package_id` VARCHAR(64) NULL,
    `reference_session_id` VARCHAR(64) NULL,
    `dedup_key` VARCHAR(128) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_wallet_tx_dedup` (`dedup_key`),
    INDEX `idx_wallet_tx_user` (`user_id`, `created_at`),
    INDEX `idx_wallet_tx_wallet` (`wallet_id`, `created_at`),
    CONSTRAINT `chk_wallet_tx_amount_pos` CHECK (`amount` > 0),
    CONSTRAINT `chk_wallet_tx_balance_non_neg` CHECK (`balance_after` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tạo bảng recurring_packages (gói đặt lịch cố định theo tuần)
CREATE TABLE IF NOT EXISTS `recurring_packages` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(50) NOT NULL,
    `court_id` VARCHAR(50) NOT NULL,
    `court_name` VARCHAR(150) NOT NULL,
    `court_address` VARCHAR(255) DEFAULT '',
    `field_id` VARCHAR(50) NOT NULL,
    `field_detail` VARCHAR(100) NOT NULL,
    `package_type` VARCHAR(40) NOT NULL DEFAULT 'WEEKLY_12',
    `package_title` VARCHAR(120) NOT NULL DEFAULT '3 tháng · 12 buổi · 1 buổi/tuần',
    `start_date` VARCHAR(20) NOT NULL,
    `weekday_label` VARCHAR(30) NOT NULL,
    `time_range` VARCHAR(100) NOT NULL,
    `slots_csv` VARCHAR(500) NOT NULL,
    `slot_count` INT NOT NULL DEFAULT 1,
    `total_sessions` INT NOT NULL DEFAULT 12,
    `unit_price` INT NOT NULL,
    `total_price` INT NOT NULL,
    `total_paid` INT NOT NULL DEFAULT 0,
    `total_refunded` INT NOT NULL DEFAULT 0,
    `payment_method` VARCHAR(30) NOT NULL DEFAULT 'QR_TRANSFER',
    `status` VARCHAR(30) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    `hold_id` VARCHAR(64) NULL,
    `payment_id` VARCHAR(64) NULL,
    `customer_name` VARCHAR(100) NOT NULL,
    `customer_phone` VARCHAR(20) NOT NULL,
    `idempotency_key` VARCHAR(128) NULL,
    `version` BIGINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_rec_pkg_idempotency` (`idempotency_key`),
    INDEX `idx_rec_pkg_user` (`user_id`, `created_at`),
    INDEX `idx_rec_pkg_court` (`court_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tạo bảng recurring_sessions (từng buổi trong gói & liên kết kết quả xử lý)
CREATE TABLE IF NOT EXISTS `recurring_sessions` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `package_id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `court_id` VARCHAR(50) NOT NULL,
    `field_id` VARCHAR(50) NOT NULL,
    `field_detail` VARCHAR(100) NOT NULL,
    `session_index` INT NOT NULL,
    `session_date` VARCHAR(20) NOT NULL,
    `time_range` VARCHAR(100) NOT NULL,
    `unit_price_snapshot` INT NOT NULL,
    `paid_amount_snapshot` INT NOT NULL DEFAULT 0,
    `refunded_amount` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    `booking_id` VARCHAR(64) NULL,
    `rescheduled_to_date` VARCHAR(20) NULL,
    `replacement_booking_id` VARCHAR(64) NULL,
    `credit_id` VARCHAR(64) NULL,
    `wallet_transaction_id` VARCHAR(64) NULL,
    `resolution_type` VARCHAR(40) NULL,
    `resolution_note` VARCHAR(500) NULL,
    `resolved_at` DATETIME NULL,
    `version` BIGINT NOT NULL DEFAULT 0,
    INDEX `idx_rec_sess_pkg` (`package_id`, `session_index`),
    INDEX `idx_rec_sess_booking` (`booking_id`),
    INDEX `idx_rec_sess_court_date` (`court_id`, `session_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Tạo bảng recurring_credits (quyền đặt 1 buổi còn dư; mỗi buổi gốc tối đa 1 quyền)
CREATE TABLE IF NOT EXISTS `recurring_credits` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `package_id` VARCHAR(64) NOT NULL,
    `source_session_id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `court_id` VARCHAR(50) NOT NULL,
    `court_name` VARCHAR(150) NOT NULL,
    `field_id` VARCHAR(50) NOT NULL,
    `field_detail` VARCHAR(100) NOT NULL,
    `slot_count` INT NOT NULL DEFAULT 1,
    `original_price_snapshot` INT NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    `redeemed_booking_id` VARCHAR(64) NULL,
    `redeemed_date` VARCHAR(20) NULL,
    `redeemed_time_range` VARCHAR(100) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `used_at` DATETIME NULL,
    `version` BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY `uk_rec_credit_source_session` (`source_session_id`),
    INDEX `idx_rec_credit_pkg_status` (`package_id`, `status`),
    INDEX `idx_rec_credit_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Tạo bảng session_change_requests (yêu cầu hoãn thời tiết / hủy muộn / hoàn tiền & đếm hủy chủ động)
CREATE TABLE IF NOT EXISTS `session_change_requests` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `package_id` VARCHAR(64) NOT NULL,
    `session_id` VARCHAR(64) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `customer_name` VARCHAR(100) DEFAULT '',
    `customer_phone` VARCHAR(20) DEFAULT '',
    `court_id` VARCHAR(50) NOT NULL,
    `court_name` VARCHAR(150) DEFAULT '',
    `session_date` VARCHAR(20) NOT NULL,
    `time_range` VARCHAR(100) NOT NULL,
    `request_type` VARCHAR(40) NOT NULL,
    `requested_action` VARCHAR(40) NULL,
    `reason_note` VARCHAR(500) DEFAULT '',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `counts_toward_cancel_limit` BOOLEAN NOT NULL DEFAULT FALSE,
    `max_refundable_amount` INT NOT NULL DEFAULT 0,
    `approved_refund_amount` INT NULL,
    `resolved_outcome` VARCHAR(40) NULL,
    `owner_note` VARCHAR(500) NULL,
    `resolved_by` VARCHAR(100) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `resolved_at` DATETIME NULL,
    `version` BIGINT NOT NULL DEFAULT 0,
    INDEX `idx_scr_court_status` (`court_id`, `status`),
    INDEX `idx_scr_user_counts` (`user_id`, `counts_toward_cancel_limit`, `created_at`),
    INDEX `idx_scr_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Bổ sung cột liên kết trên bảng bookings, payments, court_holds (an toàn không dùng DELIMITER)
SET @b_pkg = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'recurring_package_id');
SET @sql_b_pkg = IF(@b_pkg = 0, 'ALTER TABLE `bookings` ADD COLUMN `recurring_package_id` VARCHAR(64) NULL, ADD INDEX `idx_bookings_recurring_pkg` (`recurring_package_id`)', 'SELECT 1');
PREPARE stmt_b_pkg FROM @sql_b_pkg;
EXECUTE stmt_b_pkg;
DEALLOCATE PREPARE stmt_b_pkg;

SET @b_sess = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'recurring_session_id');
SET @sql_b_sess = IF(@b_sess = 0, 'ALTER TABLE `bookings` ADD COLUMN `recurring_session_id` VARCHAR(64) NULL, ADD INDEX `idx_bookings_recurring_sess` (`recurring_session_id`)', 'SELECT 1');
PREPARE stmt_b_sess FROM @sql_b_sess;
EXECUTE stmt_b_sess;
DEALLOCATE PREPARE stmt_b_sess;

SET @p_pkg = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'recurring_package_id');
SET @sql_p_pkg = IF(@p_pkg = 0, 'ALTER TABLE `payments` ADD COLUMN `recurring_package_id` VARCHAR(64) NULL, ADD INDEX `idx_payments_recurring_pkg` (`recurring_package_id`)', 'SELECT 1');
PREPARE stmt_p_pkg FROM @sql_p_pkg;
EXECUTE stmt_p_pkg;
DEALLOCATE PREPARE stmt_p_pkg;

SET @h_pkg = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'court_holds' AND COLUMN_NAME = 'recurring_package_id');
SET @sql_h_pkg = IF(@h_pkg = 0, 'ALTER TABLE `court_holds` ADD COLUMN `recurring_package_id` VARCHAR(64) NULL, ADD INDEX `idx_court_holds_recurring_pkg` (`recurring_package_id`)', 'SELECT 1');
PREPARE stmt_h_pkg FROM @sql_h_pkg;
EXECUTE stmt_h_pkg;
DEALLOCATE PREPARE stmt_h_pkg;

-- 8. Khởi tạo Ví EasySport số dư 0đ cho toàn bộ tài khoản đã tồn tại trước migration
INSERT IGNORE INTO `user_wallets` (`id`, `user_id`, `balance`, `currency`, `version`, `created_at`, `updated_at`)
SELECT CONCAT('wal_', REPLACE(UUID(), '-', '')), `id`, 0, 'VND', 0, NOW(), NOW()
FROM `users`;
