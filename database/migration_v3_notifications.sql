-- ========================================================
-- MIGRATION V3: Thêm bảng user_notifications cho EasySport
-- Áp dụng an toàn cho database easysport hiện có trên Railway / Local
-- ========================================================

-- 1. Tạo bảng user_notifications nếu chưa có
CREATE TABLE IF NOT EXISTS `user_notifications` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(50) NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `body` TEXT NOT NULL,
    `booking_id` VARCHAR(64) NULL,
    `dedup_key` VARCHAR(200) NULL UNIQUE,
    `created_at` DATETIME NOT NULL,
    `read_at` DATETIME NULL,
    INDEX `idx_notif_user` (`user_id`),
    INDEX `idx_notif_booking` (`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
