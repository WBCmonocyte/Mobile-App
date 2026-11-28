-- ========================================================
-- MIGRATION V5: Tích hợp Chức năng Chủ Sân (Owner), Nhân Viên (Staff) & Điểm Bán Quầy (POS)
-- Dự án: EasySport (MySQL 8.x / MariaDB trên Railway / Local)
-- An toàn, có thể chạy lại nhiều lần (idempotent), không phá vỡ dữ liệu cũ
-- ========================================================

-- 1. Chọn schema đích trong SQL editor trước khi chạy toàn bộ file.
-- Kiểm tra từng cột và index riêng để có thể tiếp tục nếu lần chạy trước bị gián đoạn.
SET @v5_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courts' AND COLUMN_NAME = 'owner_id'
), 'SELECT 1', 'ALTER TABLE `courts` ADD COLUMN `owner_id` VARCHAR(50) NULL');
PREPARE v5_stmt FROM @v5_sql;
EXECUTE v5_stmt;
DEALLOCATE PREPARE v5_stmt;

SET @v5_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'service_note'
), 'SELECT 1', 'ALTER TABLE `bookings` ADD COLUMN `service_note` VARCHAR(255) NULL');
PREPARE v5_stmt FROM @v5_sql;
EXECUTE v5_stmt;
DEALLOCATE PREPARE v5_stmt;

SET @v5_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'service_amount'
), 'SELECT 1', 'ALTER TABLE `bookings` ADD COLUMN `service_amount` INT DEFAULT 0');
PREPARE v5_stmt FROM @v5_sql;
EXECUTE v5_stmt;
DEALLOCATE PREPARE v5_stmt;

SET @v5_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'is_locked'
), 'SELECT 1', 'ALTER TABLE `users` ADD COLUMN `is_locked` TINYINT(1) DEFAULT 0');
PREPARE v5_stmt FROM @v5_sql;
EXECUTE v5_stmt;
DEALLOCATE PREPARE v5_stmt;

SET @v5_sql = IF(EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courts' AND INDEX_NAME = 'idx_courts_owner'
), 'SELECT 1', 'CREATE INDEX `idx_courts_owner` ON `courts` (`owner_id`)');
PREPARE v5_stmt FROM @v5_sql;
EXECUTE v5_stmt;
DEALLOCATE PREPARE v5_stmt;

-- ========================================================
-- 2. Tạo bảng staff_shifts (Lịch phân ca nhân viên trực sân)
-- ========================================================
CREATE TABLE IF NOT EXISTS `staff_shifts` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `staff_id` VARCHAR(50) NOT NULL,
    `staff_name` VARCHAR(100) NOT NULL,
    `staff_phone` VARCHAR(20) DEFAULT '',
    `court_id` VARCHAR(50) DEFAULT 'court_thanh_phat',
    `court_name` VARCHAR(150) DEFAULT 'Sân bóng Thành Phát',
    `shift_date` VARCHAR(50) NOT NULL,
    `shift_slot` VARCHAR(80) NOT NULL,
    `duty` VARCHAR(150) DEFAULT 'Trực lễ tân & Thu ngân',
    `status` VARCHAR(30) NOT NULL DEFAULT 'UPCOMING',
    `check_in_time` VARCHAR(30) NULL,
    `check_out_time` VARCHAR(30) NULL,
    `late_minutes` INT NULL,
    `notes` VARCHAR(255) DEFAULT '',
    `created_at` DATETIME NOT NULL,
    INDEX `idx_staff_shifts_staff` (`staff_id`),
    INDEX `idx_staff_shifts_date` (`shift_date`),
    INDEX `idx_staff_shifts_court` (`court_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 3. Tạo bảng shift_reports (Báo cáo bàn giao két tiền cuối ca)
-- ========================================================
CREATE TABLE IF NOT EXISTS `shift_reports` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `court_id` VARCHAR(50) DEFAULT 'court_thanh_phat',
    `court_name` VARCHAR(150) DEFAULT 'Sân bóng Thành Phát',
    `staff_id` VARCHAR(50) NOT NULL,
    `staff_name` VARCHAR(100) NOT NULL,
    `shift_date` VARCHAR(80) NOT NULL,
    `cash_amount` INT NOT NULL DEFAULT 0,
    `qr_amount` INT NOT NULL DEFAULT 0,
    `total_orders` INT NOT NULL DEFAULT 0,
    `handover_note` VARCHAR(500) DEFAULT '',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `confirmed_by` VARCHAR(100) NULL,
    `shift_id` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL,
    INDEX `idx_shift_reports_court` (`court_id`),
    INDEX `idx_shift_reports_staff` (`staff_id`),
    INDEX `idx_shift_reports_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 4. Tạo bảng pos_sales (Doanh thu bán lẻ nước, đồ uống, phụ kiện quầy)
-- ========================================================
CREATE TABLE IF NOT EXISTS `pos_sales` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `court_id` VARCHAR(50) DEFAULT 'court_thanh_phat',
    `court_name` VARCHAR(150) DEFAULT 'Sân bóng Thành Phát',
    `staff_id` VARCHAR(50) NOT NULL,
    `staff_name` VARCHAR(100) NOT NULL,
    `shift_id` VARCHAR(50) NULL,
    `items_note` VARCHAR(500) DEFAULT '',
    `amount` INT NOT NULL DEFAULT 0,
    `payment_method` VARCHAR(30) NOT NULL DEFAULT 'CASH',
    `sale_date` VARCHAR(20) NULL,
    `settlement_booking_id` VARCHAR(50) NULL,
    `created_at` DATETIME NOT NULL,
    INDEX `idx_pos_sales_staff_date` (`staff_id`, `sale_date`),
    INDEX `idx_pos_sales_shift` (`shift_id`),
    INDEX `idx_pos_sales_court` (`court_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 5. HƯỚNG DẪN THỦ TỤC CẤP QUYỀN OWNER CHO TÀI KHOẢN VÀ GÁN SÂN THỬ NGHIỆM
-- ========================================================
-- Giả sử bạn muốn phân quyền cho một người dùng đã đăng ký (ví dụ SĐT '0987654321' hoặc email 'owner@easysport.vn')
-- làm Chủ sân của cụm sân 'court_thanh_phat'.

-- BƯỚC 5.1: Kiểm tra trạng thái người dùng & sân trước khi cập nhật
-- SELECT id, email, phone, full_name, role FROM users WHERE email = 'owner@easysport.vn' OR phone = '0987654321';
-- SELECT id, name, owner_id FROM courts WHERE id = 'court_thanh_phat';

-- BƯỚC 5.2: Cập nhật quyền OWNER và gán sân (Thay <USER_ID> bằng ID thực tế của user, ví dụ 'user_2')
-- UPDATE users SET role = 'OWNER' WHERE email = 'owner@easysport.vn' OR phone = '0987654321';
-- UPDATE courts SET owner_id = (SELECT id FROM users WHERE email = 'owner@easysport.vn' OR phone = '0987654321' LIMIT 1) WHERE id = 'court_thanh_phat';

-- BƯỚC 5.3: Kiểm tra đối soát sau khi cập nhật
-- SELECT u.id AS user_id, u.full_name, u.role, c.id AS court_id, c.name AS court_name
-- FROM users u
-- JOIN courts c ON c.owner_id = u.id
-- WHERE u.role = 'OWNER';
