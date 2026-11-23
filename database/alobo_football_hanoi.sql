-- ========================================================
-- EASY SPORT - ALO BOOKING HANOI FOOTBALL COURTS
-- Dữ liệu sân bóng đá thực tế cào từ ALO Booking (datlich.alobo.vn)
-- Đặc tính: Idempotent, Transaction, utf8mb4, an toàn cho Railway
-- ========================================================

USE `easysport`;

START TRANSACTION;

-- 1. BẢNG CỤM SÂN BÓNG ĐÁ (courts)
INSERT INTO `courts` (
    `id`, `name`, `address`, `rating`, `review_count`, `distance_km`,
    `latitude`, `longitude`, `sport_type`, `indoor`, `price_per_hour`,
    `morning_price`, `afternoon_price`, `evening_price`, `image_res_name`
) VALUES
('alobo_fb_dinhthon_2', 'Sân bóng Đình Thôn 2', '15 Ngõ 180 Đường Đình Thôn, Mỹ Đình, Nam Từ Liêm, Hà Nội', 4.8, 86, 1.0, 21.018973, 105.776613, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_soccer_field'),
('alobo_fb_thang_dung', 'Sân bóng Thành Dũng', '100 Đại lộ Chu Văn An cũ, Thanh Liệt, Thanh Trì, Hà Nội', 4.9, 112, 1.0, 20.976073, 105.816439, 'Bóng đá', 0, 500000, 350000, 450000, 600000, 'court_san_bong_da'),
('alobo_fb_van_phu_x7', 'Khu thể thao Văn Phú - Sân bóng đá X7', 'Số 9 phố Hà Cầu, phường Phú La, Hà Đông, Hà Nội', 4.7, 64, 1.0, 20.962152, 105.768218, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_soccer_field')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `address` = VALUES(`address`),
    `rating` = VALUES(`rating`),
    `review_count` = VALUES(`review_count`),
    `latitude` = VALUES(`latitude`),
    `longitude` = VALUES(`longitude`),
    `sport_type` = VALUES(`sport_type`),
    `indoor` = VALUES(`indoor`),
    `price_per_hour` = VALUES(`price_per_hour`),
    `morning_price` = VALUES(`morning_price`),
    `afternoon_price` = VALUES(`afternoon_price`),
    `evening_price` = VALUES(`evening_price`),
    `image_res_name` = VALUES(`image_res_name`);

-- 2. BẢNG SÂN CON (court_fields)
INSERT INTO `court_fields` (`id`, `court_id`, `field_label`, `field_detail`, `field_type`, `resource_keys`, `is_active`) VALUES
('field_ab_fb_dt_1', 'alobo_fb_dinhthon_2', 'Sân 7A', 'Sân cỏ nhân tạo Đình Thôn 7 người A', '7_VS_7', 'alobo_fb_dt:res_1', TRUE),
('field_ab_fb_dt_2', 'alobo_fb_dinhthon_2', 'Sân 7B', 'Sân cỏ nhân tạo Đình Thôn 7 người B', '7_VS_7', 'alobo_fb_dt:res_2', TRUE),
('field_ab_fb_td_1', 'alobo_fb_thang_dung', 'Sân 7A', 'Sân cỏ nhân tạo Thành Dũng 7 người A', '7_VS_7', 'alobo_fb_td:res_1', TRUE),
('field_ab_fb_td_2', 'alobo_fb_thang_dung', 'Sân 7B', 'Sân cỏ nhân tạo Thành Dũng 7 người B', '7_VS_7', 'alobo_fb_td:res_2', TRUE),
('field_ab_fb_vp_1', 'alobo_fb_van_phu_x7', 'Sân 7A', 'Sân cỏ nhân tạo X7 Văn Phú 7 người A', '7_VS_7', 'alobo_fb_vp:res_1', TRUE),
('field_ab_fb_vp_2', 'alobo_fb_van_phu_x7', 'Sân 7B', 'Sân cỏ nhân tạo X7 Văn Phú 7 người B', '7_VS_7', 'alobo_fb_vp:res_2', TRUE)
ON DUPLICATE KEY UPDATE
    `field_label` = VALUES(`field_label`),
    `field_detail` = VALUES(`field_detail`),
    `field_type` = VALUES(`field_type`),
    `resource_keys` = VALUES(`resource_keys`),
    `is_active` = VALUES(`is_active`);

-- 3. BẢNG TÀI NGUYÊN VẬT LÝ (court_resources)
INSERT INTO `court_resources` (`id`, `court_id`, `name`, `description`) VALUES
('alobo_fb_dt:res_1', 'alobo_fb_dinhthon_2', 'Mặt cỏ sân 7A Đình Thôn', 'Tài nguyên vật lý sân 7A Đình Thôn'),
('alobo_fb_dt:res_2', 'alobo_fb_dinhthon_2', 'Mặt cỏ sân 7B Đình Thôn', 'Tài nguyên vật lý sân 7B Đình Thôn'),
('alobo_fb_td:res_1', 'alobo_fb_thang_dung', 'Mặt cỏ sân 7A Thành Dũng', 'Tài nguyên vật lý sân 7A Thành Dũng'),
('alobo_fb_td:res_2', 'alobo_fb_thang_dung', 'Mặt cỏ sân 7B Thành Dũng', 'Tài nguyên vật lý sân 7B Thành Dũng'),
('alobo_fb_vp:res_1', 'alobo_fb_van_phu_x7', 'Mặt cỏ sân 7A Văn Phú', 'Tài nguyên vật lý sân 7A Văn Phú'),
('alobo_fb_vp:res_2', 'alobo_fb_van_phu_x7', 'Mặt cỏ sân 7B Văn Phú', 'Tài nguyên vật lý sân 7B Văn Phú')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `description` = VALUES(`description`);

COMMIT;
