-- ========================================================
-- MIGRATION V7: Quản lý kho hàng & lịch sử biến động kho (Inventory)
-- Tương thích: MySQL 8.4 trên Railway và Local
-- Không dùng DELIMITER hay cú pháp MariaDB để chạy trực tiếp trong mọi trình soạn thảo SQL
-- ========================================================

-- 1. Tạo bảng inventory_items (mặt hàng tại từng chi nhánh sân)
CREATE TABLE IF NOT EXISTS `inventory_items` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `court_id` VARCHAR(50) NOT NULL,
    `court_name` VARCHAR(150) NOT NULL,
    `name` VARCHAR(150) NOT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'DRINK',
    `unit` VARCHAR(30) NOT NULL DEFAULT 'chai',
    `import_price` INT NOT NULL DEFAULT 0,
    `sell_price` INT NOT NULL DEFAULT 0,
    `stock_quantity` INT NOT NULL DEFAULT 0,
    `min_alert_threshold` INT NOT NULL DEFAULT 10,
    `sku` VARCHAR(50) NULL,
    `barcode` VARCHAR(80) NULL,
    `supplier_name` VARCHAR(150) NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_inventory_court_active` (`court_id`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tạo bảng inventory_transactions (lịch sử nhập/xuất/bán POS/kiểm kê/chuyển kho)
CREATE TABLE IF NOT EXISTS `inventory_transactions` (
    `id` VARCHAR(50) NOT NULL PRIMARY KEY,
    `item_id` VARCHAR(50) NOT NULL,
    `item_name` VARCHAR(150) NOT NULL,
    `court_id` VARCHAR(50) NOT NULL,
    `type` VARCHAR(30) NOT NULL,
    `quantity_delta` INT NOT NULL,
    `stock_after` INT NOT NULL,
    `unit_cost` INT NULL DEFAULT 0,
    `note` VARCHAR(300) NULL DEFAULT '',
    `created_by` VARCHAR(100) NULL,
    `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_inv_tx_court_time` (`court_id`, `created_at`),
    INDEX `idx_inv_tx_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
