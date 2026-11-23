-- ========================================================
-- DATABASE SCHEMA: EASY SPORT (HỆ THỐNG ĐẶT SÂN THỂ THAO)
-- HỆ QUẢN TRỊ CSDL: MySQL 8.x / MariaDB (InnoDB)
-- ========================================================

CREATE DATABASE IF NOT EXISTS `easysport` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `easysport`;

-- Xóa bảng cũ theo thứ tự khóa ngoại nếu khởi tạo lại
DROP TABLE IF EXISTS `court_hold_slots`;
DROP TABLE IF EXISTS `court_holds`;
DROP TABLE IF EXISTS `court_resources`;
DROP TABLE IF EXISTS `payments`;
DROP TABLE IF EXISTS `bookings`;
DROP TABLE IF EXISTS `court_fields`;
DROP TABLE IF EXISTS `courts`;
DROP TABLE IF EXISTS `user_sessions`;
DROP TABLE IF EXISTS `users`;

-- 1. BẢNG NGƯỜI DÙNG & TÀI KHOẢN (users)
CREATE TABLE `users` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `address` VARCHAR(255) DEFAULT '',
    `date_of_birth` DATE NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `is_locked` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BẢNG PHIÊN ĐĂNG NHẬP / TOKEN (user_sessions)
CREATE TABLE `user_sessions` (
    `token` VARCHAR(64) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(50) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at` DATETIME NOT NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX `idx_user_sessions_user` (`user_id`),
    INDEX `idx_user_sessions_expires` (`expires_at`),
    CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BẢNG CỤM SÂN THỂ THAO (courts)
CREATE TABLE `courts` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL,
    `address` VARCHAR(255) NOT NULL,
    `rating` DOUBLE DEFAULT 5.0,
    `review_count` INT DEFAULT 0,
    `distance_km` DOUBLE DEFAULT 1.0,
    `latitude` DOUBLE NULL,
    `longitude` DOUBLE NULL,
    `sport_type` VARCHAR(50) NOT NULL DEFAULT 'Bóng đá',
    `indoor` BOOLEAN NOT NULL DEFAULT FALSE,
    `price_per_hour` INT NOT NULL DEFAULT 300000,
    `morning_price` INT NOT NULL DEFAULT 150000,
    `afternoon_price` INT NOT NULL DEFAULT 250000,
    `evening_price` INT NOT NULL DEFAULT 300000,
    `image_res_name` VARCHAR(100) DEFAULT 'court_thanh_phat',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. BẢNG CÁC SÂN ĐẤU CON TRONG CỤM (court_fields)
CREATE TABLE `court_fields` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `court_id` VARCHAR(50) NOT NULL,
    `field_label` VARCHAR(50) NOT NULL,
    `field_detail` VARCHAR(100) NOT NULL,
    `field_type` VARCHAR(30) NOT NULL DEFAULT '5_VS_5',
    `resource_keys` VARCHAR(255) DEFAULT '',
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT `fk_field_court` FOREIGN KEY (`court_id`) REFERENCES `courts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. BẢNG TÀI NGUYÊN SÂN VẬT LÝ ĐỂ KHÓA PESSIMISTIC (court_resources)
CREATE TABLE `court_resources` (
    `id` VARCHAR(100) NOT NULL PRIMARY KEY,
    `court_id` VARCHAR(50) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) DEFAULT '',
    INDEX `idx_resource_court` (`court_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. BẢNG ĐƠN ĐẶT SÂN (bookings)
CREATE TABLE `bookings` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `user_id` VARCHAR(50) NULL,
    `court_id` VARCHAR(50) NOT NULL,
    `court_name` VARCHAR(150) DEFAULT '',
    `field_detail` VARCHAR(100) NOT NULL,
    `time_range` VARCHAR(100) NOT NULL,
    `booking_date` VARCHAR(50) NOT NULL,
    `total_price` INT NOT NULL,
    `deposit_amount` INT NOT NULL DEFAULT 0,
    `remaining_amount` INT NOT NULL DEFAULT 0,
    `payment_method` VARCHAR(30) NOT NULL DEFAULT 'QR_TRANSFER',
    `payment_status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `customer_name` VARCHAR(100) NOT NULL,
    `customer_phone` VARCHAR(20) NOT NULL,
    `address` VARCHAR(255) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `hold_expires_at` DATETIME NULL,
    INDEX `idx_bookings_slot` (`court_id`, `field_detail`, `booking_date`),
    INDEX `idx_bookings_user` (`user_id`),
    INDEX `idx_bookings_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. BẢNG GIAO DỊCH THANH TOÁN (payments)
CREATE TABLE `payments` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `booking_id` VARCHAR(64) NOT NULL,
    `order_invoice_number` VARCHAR(64) NOT NULL UNIQUE,
    `amount` INT NOT NULL,
    `confirmed_amount` INT NULL,
    `currency` VARCHAR(10) NOT NULL DEFAULT 'VND',
    `payment_method` VARCHAR(30) NOT NULL DEFAULT 'QR_TRANSFER',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `sepay_transaction_id` VARCHAR(100) NULL,
    `checkout_token` VARCHAR(64) UNIQUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at` DATETIME NOT NULL,
    `confirmed_at` DATETIME NULL,
    `raw_ipn_data` TEXT NULL,
    INDEX `idx_payments_invoice` (`order_invoice_number`),
    INDEX `idx_payments_token` (`checkout_token`),
    INDEX `idx_payments_booking` (`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. BẢNG GIỮ SÂN ATOMIC 5 PHÚT + 1 PHÚT XÁC MINH (court_holds)
CREATE TABLE `court_holds` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `booking_id` VARCHAR(64) NULL,
    `payment_attempt_id` VARCHAR(64) NULL,
    `user_id` VARCHAR(64) NOT NULL,
    `court_id` VARCHAR(50) NOT NULL,
    `field_id` VARCHAR(50) NOT NULL,
    `field_detail` VARCHAR(100) NULL,
    `booking_date` VARCHAR(50) NOT NULL,
    `time_range` VARCHAR(100) NOT NULL,
    `slots_json` VARCHAR(1000) NULL,
    `resource_ids` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `payment_deadline` DATETIME NULL,
    `verification_deadline` DATETIME NULL,
    `confirmed_at` DATETIME NULL,
    `released_at` DATETIME NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    `idempotency_key` VARCHAR(128) UNIQUE,
    `version` BIGINT NULL,
    INDEX `idx_court_holds_slot` (`court_id`, `field_id`, `booking_date`, `status`),
    INDEX `idx_court_holds_idempotency` (`idempotency_key`),
    INDEX `idx_court_holds_booking` (`booking_id`),
    INDEX `idx_court_holds_expires` (`expires_at`),
    INDEX `idx_court_holds_verify_deadline` (`verification_deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. BẢNG CHI TIẾT SLOT THEO TÀI NGUYÊN (court_hold_slots)
CREATE TABLE `court_hold_slots` (
    `id` VARCHAR(64) NOT NULL PRIMARY KEY,
    `hold_id` VARCHAR(64) NOT NULL,
    `court_id` VARCHAR(50) NOT NULL,
    `resource_id` VARCHAR(100) NOT NULL,
    `booking_date` VARCHAR(50) NOT NULL,
    `start_minute` INT NOT NULL,
    `end_minute` INT NOT NULL,
    `slot_label` VARCHAR(50) NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'AWAITING_PAYMENT',
    `expires_at` DATETIME NOT NULL,
    INDEX `idx_hold_slots_resource_date` (`resource_id`, `booking_date`, `status`),
    INDEX `idx_hold_slots_hold` (`hold_id`),
    INDEX `idx_hold_slots_expires` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- SEED DATA ĐỒNG BỘ ID VỚI BACKEND & ANDROID
-- ========================================================

-- Nạp tài khoản test (mật khẩu "123456" đã băm bằng PBKDF2)
INSERT INTO `users` (`id`, `email`, `password_hash`, `full_name`, `phone`, `address`, `date_of_birth`, `role`, `is_locked`) VALUES
('user_1', 'nguyenvana@gmail.com', 'mKzN209aBv+wWjFqY3nFfg==$yG1bS4V9J82tE4uL9kPqY7nFfg==', 'Nguyễn Văn A', '0901234567', 'Quận Tân Bình, TP. HCM', '1998-05-20', 'USER', FALSE),
('user_2', 'tranvanb@gmail.com', 'mKzN209aBv+wWjFqY3nFfg==$yG1bS4V9J82tE4uL9kPqY7nFfg==', 'Trần Văn B', '0987654321', 'Quận 3, TP. HCM', '1995-08-10', 'OWNER', FALSE);

-- Nạp cụm sân thể thao với ID chuẩn
INSERT INTO `courts` (`id`, `name`, `address`, `rating`, `review_count`, `distance_km`, `latitude`, `longitude`, `sport_type`, `indoor`, `price_per_hour`, `morning_price`, `afternoon_price`, `evening_price`, `image_res_name`) VALUES
('court_thanh_phat', 'Sân bóng Thành Phát', '12 Hoàng Hoa Thám, P.13, Q. Tân Bình, TP. HCM', 4.8, 128, 1.2, 10.8016, 106.6508, 'Bóng đá', FALSE, 300000, 150000, 250000, 300000, 'court_thanh_phat'),
('court_hoang_gia', 'Sân bóng Hoàng Gia', '18D Phan Văn Trị, P.10, Gò Vấp, TP. HCM', 4.7, 96, 2.5, 10.8322, 106.6715, 'Bóng đá', FALSE, 280000, 140000, 220000, 280000, 'court_hoang_gia'),
('court_cau_long_tan_binh', 'CLB Cầu Lông Tân Bình', '18 Cộng Hòa, P.4, Q. Tân Bình, TP. HCM', 4.9, 210, 1.8, 10.8002, 106.6558, 'Cầu lông', TRUE, 120000, 80000, 100000, 120000, 'court_badminton'),
('court_pickleball_viet_nhat', 'Sân Pickleball Việt Nhật', '55 Thăng Long, P.4, Q. Tân Bình, TP. HCM', 4.9, 85, 3.0, 10.8051, 106.6582, 'Pickleball', TRUE, 180000, 120000, 150000, 180000, 'court_badminton'),
('court_chao_lua', 'Sân bóng Chảo Lửa', '30 Phan Thúc Duyện, P.4, Tân Bình, TP. HCM', 4.9, 214, 3.1, 10.8065, 106.6617, 'Bóng đá', FALSE, 400000, 200000, 300000, 400000, 'court_chao_lua');

-- Nạp các sân đấu con
INSERT INTO `court_fields` (`id`, `court_id`, `field_label`, `field_detail`, `field_type`, `resource_keys`) VALUES
('field_tp_1', 'court_thanh_phat', 'Sân 7', 'Sân 7 người tiêu chuẩn FIFA cỏ nhân tạo mới', '7_VS_7', 'court_thanh_phat:g1'),
('field_tp_2', 'court_thanh_phat', 'Sân 11', 'Sân 11 người kích thước chuẩn thi đấu có khán đài', '11_VS_11', 'court_thanh_phat:g1,court_thanh_phat:g2'),
('field_hg_1', 'court_hoang_gia', 'Sân 5A', 'Sân 5 mini cỏ mềm cao cấp', '5_VS_5', 'court_hoang_gia:g1'),
('field_hg_2', 'court_hoang_gia', 'Sân 5B', 'Sân 5 mini có mái che', '5_VS_5', 'court_hoang_gia:g2'),
('field_hg_3', 'court_hoang_gia', 'Sân 7', 'Sân 7 người dàn đèn chiếu sáng hiện đại', '7_VS_7', 'court_hoang_gia:g1,court_hoang_gia:g2'),
('field_cl_1', 'court_cau_long_tan_binh', 'Sân số 1', 'Thảm Yonex chuyên dụng, chống trượt', 'BADMINTON', 'court_cau_long_tan_binh:g1'),
('field_cl_2', 'court_cau_long_tan_binh', 'Sân số 2', 'Thảm Yonex chuyên dụng', 'BADMINTON', 'court_cau_long_tan_binh:g2'),
('field_cl_3', 'court_cau_long_tan_binh', 'Sân số 3', 'Thảm cao su cao cấp', 'BADMINTON', 'court_cau_long_tan_binh:g3'),
('field_pb_1', 'court_pickleball_viet_nhat', 'Sân P1 Ngoài trời', 'Mặt sân Acrylic tiêu chuẩn Mỹ', 'PICKLEBALL', 'court_pickleball_viet_nhat:g1'),
('field_pb_2', 'court_pickleball_viet_nhat', 'Sân P2 Trong nhà', 'Có máy lạnh và đèn chống lóa', 'PICKLEBALL', 'court_pickleball_viet_nhat:g2'),
('field_clua_1', 'court_chao_lua', 'Sân A', 'Sân cỏ nhân tạo 7 người', '7_VS_7', 'court_chao_lua:g1'),
('field_clua_2', 'court_chao_lua', 'Sân B', 'Sân cỏ nhân tạo 5 người', '5_VS_5', 'court_chao_lua:g2');

-- Nạp tài nguyên vật lý để khóa bi quan
INSERT INTO `court_resources` (`id`, `court_id`, `name`, `description`) VALUES
('court_thanh_phat:g1', 'court_thanh_phat', 'Mặt sân 1 (Thành Phát)', 'Mặt cỏ 1'),
('court_thanh_phat:g2', 'court_thanh_phat', 'Mặt sân 2 (Thành Phát)', 'Mặt cỏ 2'),
('court_hoang_gia:g1', 'court_hoang_gia', 'Mặt sân 1 (Hoàng Gia)', 'Mặt cỏ 5A'),
('court_hoang_gia:g2', 'court_hoang_gia', 'Mặt sân 2 (Hoàng Gia)', 'Mặt cỏ 5B'),
('court_cau_long_tan_binh:g1', 'court_cau_long_tan_binh', 'Thảm 1 Tân Bình', 'Thảm số 1'),
('court_cau_long_tan_binh:g2', 'court_cau_long_tan_binh', 'Thảm 2 Tân Bình', 'Thảm số 2'),
('court_cau_long_tan_binh:g3', 'court_cau_long_tan_binh', 'Thảm 3 Tân Bình', 'Thảm số 3'),
('court_pickleball_viet_nhat:g1', 'court_pickleball_viet_nhat', 'Sân P1 Ngoài trời', 'Acrylic 1'),
('court_pickleball_viet_nhat:g2', 'court_pickleball_viet_nhat', 'Sân P2 Trong nhà', 'Acrylic 2'),
('court_chao_lua:g1', 'court_chao_lua', 'Mặt sân A (Chảo Lửa)', 'Mặt cỏ 7A'),
('court_chao_lua:g2', 'court_chao_lua', 'Mặt sân B (Chảo Lửa)', 'Mặt cỏ 5B');
