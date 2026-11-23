-- ========================================================
-- MIGRATION V2: Thêm bảng user_sessions và thời hạn xác minh 1 phút
-- Áp dụng an toàn cho database easysport hiện có, bảo toàn dữ liệu
-- ========================================================

USE `easysport`;

-- 1. Tạo bảng user_sessions nếu chưa có
CREATE TABLE IF NOT EXISTS `user_sessions` (
    `token` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(50) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at` DATETIME NOT NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX `idx_user_sessions_user` (`user_id`),
    INDEX `idx_user_sessions_expires` (`expires_at`),
    CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bổ sung các cột thời hạn xác minh cho court_holds nếu chưa có
ALTER TABLE `court_holds` 
    ADD COLUMN IF NOT EXISTS `payment_deadline` DATETIME NULL AFTER `expires_at`,
    ADD COLUMN IF NOT EXISTS `verification_deadline` DATETIME NULL AFTER `payment_deadline`;

-- Cập nhật dữ liệu cho các dòng cũ
UPDATE `court_holds` 
SET `payment_deadline` = `expires_at`, 
    `verification_deadline` = DATE_ADD(`expires_at`, INTERVAL 1 MINUTE)
WHERE `payment_deadline` IS NULL;

-- 3. Bổ sung index cho verification_deadline
CREATE INDEX IF NOT EXISTS `idx_court_holds_verify_deadline` ON `court_holds` (`verification_deadline`);

-- 4. Bổ sung cột remaining_amount và hold_expires_at cho bookings nếu chưa có
ALTER TABLE `bookings`
    ADD COLUMN IF NOT EXISTS `remaining_amount` INT NOT NULL DEFAULT 0 AFTER `deposit_amount`,
    ADD COLUMN IF NOT EXISTS `hold_expires_at` DATETIME NULL AFTER `created_at`;

