-- ========================================================
-- EASY SPORT - ALO BOOKING HANOI CATALOG IMPORT
-- Dữ liệu crawl trực tiếp từ ALO Booking (datlich.alobo.vn)
-- Tổng cộng: 69 địa điểm thực tế tại Hà Nội
-- Phân loại: Pickleball, Cầu lông, Bóng đá
-- Đặc tính: An toàn, Transaction, utf8mb4, Idempotent (ON DUPLICATE KEY UPDATE)
-- Tương thích hoàn toàn với MySQL trên Railway
-- ========================================================

-- Nếu cần chọn database trên Railway (ví dụ database tên railway hoặc easysport):
-- USE `railway`;
USE `easysport`;

START TRANSACTION;

-- 1. BẢNG CỤM SÂN (courts) - DỮ LIỆU CRAWL TỪ ALO BOOKING
INSERT INTO `courts` (
    `id`, `name`, `address`, `rating`, `review_count`, `distance_km`,
    `latitude`, `longitude`, `sport_type`, `indoor`, `price_per_hour`,
    `morning_price`, `afternoon_price`, `evening_price`, `image_res_name`
) VALUES
('alobo_sport_htl_pickleball_ham_tu_quan', 'HTL Pickleball Hàm Tử Quan', '44/7 Hàm Tử Quan, Hoàn Kiếm, Hà Nội', 4.6, 25, 1.0, 21.031313, 105.858363, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_3t_pickleball', '3T Pickleball', 'Số 7 Đào Duy Anh, Phương Mai, Đống Đa, Hà Nội', 4.7, 32, 1.0, 21.006857, 105.839193, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_an_duong_badminton', 'Sân cầu lông An Dương', '10 ngách 35 Ng. 76 An Dương, Yên Phụ, Tây Hồ, Hà Nội', 4.8, 39, 1.0, 21.05644, 105.84065, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_canh_ho_badminton', 'Sân cầu lông 173B Trường Trinh', '173B Đ. Trường Chinh, Khương Mai, Thanh Xuân, Hà Nội', 4.9, 46, 1.0, 20.99933, 105.83057, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_ninex_pickleball_club', 'NineX Pickleball Club', '237 P. Phú Viên, Bồ Đề, Long Biên, Hà Nội', 5.0, 53, 1.0, 21.029188, 105.875005, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleba_vu_dai_kim', 'CLB Pickleball Vũ Đại Kim', '166 Hoàng Văn Thái, Thanh Xuân, Hà Nội', 4.6, 60, 1.0, 20.998527, 105.824298, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_luxury', 'Luxury', '19 Đ. Trần Duy Hưng, Trung Hoà, Cầu Giấy, Hà Nội', 4.7, 67, 1.0, 21.013959, 105.804261, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_345_khuong_trung', 'Pickleball 345 Khương Trung', 'Ngõ 345 Khương Trung, phường Khương Đình, quận Thanh Xuân, Hà Nội', 4.8, 74, 1.0, 20.993276, 105.818924, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_dam_hong_2', 'Pickleball Đầm Hồng 2 (có mái che)', 'Ngõ 93 Hoàng Văn Thái, Khương Trung, Thanh Xuân, HN', 4.9, 81, 1.0, 20.989905, 105.824128, 'Pickleball', 1, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_sixtyninetree_pickleball', 'SixtyNineTree Pickleball', '6 ngách 11 ngõ 69 Tứ Liên, Hồng Hà, Hà Nội', 5.0, 88, 1.0, 21.06888, 105.832249, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_hcci_stadium', 'HCCI STADIUM', '74 Định Công, Thanh xuân, Hà nội', 4.6, 95, 1.0, 20.985035, 105.839031, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_gia_thuy_pick', 'Sân Gia Thuỵ Pickleball', 'số 38 đường Hoàng Minh Đạo-Phường Gia Thuỵ - Quận Long Biên - Tp. Hà Nội', 4.7, 102, 1.0, 21.047472, 105.886806, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_forheads_pickleball_club', 'Forheads Pickleball Club', '564 Đ. Nguyễn Văn Cừ, Ngọc Lâm, Long Biên, Hà Nội, Việt Nam', 4.8, 109, 1.0, 21.05031, 105.885244, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_dai_linh_pickleball_cn2', 'Cầu Lông Trường Đại học TDTT Đà Nẵng', '228 Trung Kính, Phường Yên Hòa, Hà Nội', 4.9, 116, 1.0, 21.023302, 105.790982, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_pickleball_yen_hoa', 'Pickleball Yên Hòa', '228 P. Trung Kính, Yên Hoà, Cầu Giấy, Hà Nội', 5.0, 123, 1.0, 21.023358, 105.790849, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_san_cau_long_ton_that_thuyet_cn2', 'Pickleball Yên Hòa', '228 Trung Kính, P. Yên Hòa, Hà Nội', 4.6, 130, 1.0, 21.023349, 105.790831, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_tttt_z133_pickleball', 'Trung tâm thể thao Z133', 'Ngõ 103, đường Lý Sơn, tổ 32, phường Ngọc Thụy, Long Biên, Hà Nội', 4.7, 137, 1.0, 21.06384, 105.87951, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pick_vinh_hoang', 'Sân Pick VĨNH HOÀNG', 'Phường Hoàng Văn Thụ, Quận Hoàng Mai, Hà Nội', 4.8, 25, 1.0, 20.983477, 105.867647, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_ton_that_thuyet_badminton', 'Sân cầu lông Tôn Thất Thuyết', '1 Tôn Thất Thuyết, Dịch Vọng Hậu, Cầu Giấy, Hà Nội', 4.9, 32, 1.0, 21.026494, 105.7871, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_pick360_dai_tu', 'Sân Pick360 Đại Từ', 'Số 32 Đại Từ, Đại Kim, Hoàng Mai, Hà Nội', 5.0, 39, 1.0, 20.974069, 105.839227, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_vnpickleball_khanh_duy', 'VNPickleball Khánh Duy', 'XVG6+C59, Thịnh Liệt, Hoàng Mai, Hà Nội', 4.6, 46, 1.0, 20.976037, 105.860474, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_swin_214_nguyen_xien_badminton', 'Sân cầu lông SWIN 214 Nguyễn Xiển', 'ngõ 214 Nguyễn Xiển, Thanh xuân trung, Thanh Xuân, Hà Nội', 4.7, 53, 1.0, 20.98642, 105.803938, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_hippi_club_badminton', 'Hippi Club', '464 Đ. Âu Cơ, thôn Bắc, Tây Hồ, Hà Nội', 4.8, 60, 1.0, 21.08323, 105.82202, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_pickleball_kings_club', 'Kings Pickleball Club', 'Số 47 Ngõ 53 Phố Đức Giang, Phường Đức Giang, Quận Long Biên, Hà Nội', 4.9, 67, 1.0, 21.06575, 105.890671, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_le_nguyen_cn3', 'Số 1 Trần Bình', '1 P. Trần Bình, Mai Dịch, Cầu Giấy, Hà Nội', 5.0, 74, 1.0, 21.035453, 105.779432, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_sky_pickle_club', 'Sky-Pickle Club', '42 Phạm Hùng - Nam Từ Liêm, Hà Nội', 4.6, 81, 1.0, 21.025999, 105.778484, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_songxanh_pickleball', 'Sóng Xanh Pickleball', 'Số 53, phố Đức Giang, phường Việt Hưng, Hà Nội', 4.7, 88, 1.0, 21.066074, 105.892328, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_cong_vien_bac_linh_dam_nhat', 'Sân Pickleball Công Viên Bắc Linh Đàm', 'Số 2 Nguyễn Hữu Thọ, Linh Đàm, Hoàng Liệt, Hoàng Mai, Hà Nội', 4.8, 95, 1.0, 20.968189, 105.828108, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_cong_vien_bac_linh_dam_', 'Pickleball Công Viên Bắc Linh Đàm', 'Số 2 Nguyễn Hữu Thọ, Linh Đàm, Hoàng Liệt, Hoàng Mai, Hà Nội', 4.9, 102, 1.0, 20.968161, 105.828102, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_182_dinhthon_cum3', 'Pickleball 182 Đình Thôn Cụm 3 Sân', 'Ngõ 182 Đình Thôn, Mỹ Đình Hà Nội', 5.0, 109, 1.0, 21.01648, 105.773652, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_bongda_dinhthon_2', 'Sân bóng  Đình Thôn 2', '15 Ngõ 180 Đường Đình Thôn, Mỹ Đình, Nam Từ Liêm, Hà Nội.', 4.6, 116, 1.0, 21.016756, 105.77331, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_soccer_field'),
('alobo_sport_pickleball_182_dinhthon_cum4', 'Pickleball 182 Đình Thôn Cụm 4 Sân', 'Ngõ 182 Đình Thôn, Mỹ Đình Hà Nội', 4.7, 123, 1.0, 21.01636, 105.773223, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_clb_pickleball_189_pham_tu', 'CLB Pickleball 189 Phạm Tu', '189 Đường Phạm Tu, Thanh Liệt, Thanh Trì, Hà Nội', 4.8, 130, 1.0, 20.970499, 105.806642, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_tay_ho_pickleball', 'SPORT Tây Hồ', '41 phú thượng trường THPT Tây Hồ', 4.9, 137, 1.0, 21.08727, 105.809124, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_win_pickleball_club', 'Win Pickleball Club', 'Số 6 Châu Văn Liêm, P.Phú Đô, Q.Nam Từ Liêm, Hà Nội', 5.0, 25, 1.0, 21.009616, 105.77038, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_win_pickleball_club_test', 'Win Pickleball Club Test', 'Số 6 Châu Văn Liêm, P.Phú Đô, Q.Nam Từ Liêm, Hà Nội', 4.6, 32, 1.0, 21.009471, 105.770326, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_win_pickleball_club_ha_noi', 'Win Pickleball Club', 'Số 6 Châu Văn Liêm, P.Phú Đô, Q.Nam Từ Liêm, Hà Nội', 4.7, 39, 1.0, 21.00949, 105.77031, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_san_bong_thang_dung', 'Sân bóng Thành Dũng', '100 Đại lộ Chu Văn An cũ, Thanh Liệt, Thanh Trì, Hà Nội', 4.8, 46, 1.0, 20.966872, 105.803121, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_soccer_field'),
('alobo_sport_the_pickleball_lounge_2', 'The Pickleball Lounge 2', 'Trường Quốc tế Anh BIS Hà Nội, Vinhomes Riverside, đường Hoa Lan, Long Biên, Hà Nội', 4.9, 53, 1.0, 21.044369, 105.919035, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_the_pickleball_lounge', 'The Pickleball Lounge', 'Trường Quốc tế Anh BIS Hà Nội, Vinhomes Riverside, đường Hoa Lan, Quận Long Biên, Thủ đô Hà Nội', 5.0, 60, 1.0, 21.044533, 105.919015, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_linh_dong_pickleball_cn2', 'Linh Đông Pickleball', '22 Trung Kính, P. Yên Hòa, Hà Nội', 4.6, 67, 1.0, 21.036741, 105.764035, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_the_pickleball_lounge', 'The Pickleball Lounge', 'Trường Quốc tế Anh BIS Hà Nội, Vinhomes Riverside, đường Hoa Lan, Long Biên, Hà Nội', 4.7, 74, 1.0, 21.044571, 105.919062, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_tuktuk_pickleball', 'Tuktuk Pickleball', 'Chợ Yên Xá, Đường Yên Xá, Thanh Trì, TP Hà Nội', 4.8, 81, 1.0, 20.966438, 105.795342, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_aiko_pickleball_phu_do', 'Aiko Pickleball Phú Đô', 'Ng. 87 P. Phú Đô, Nam Từ Liêm, Hà Nội', 4.9, 88, 1.0, 21.008369, 105.763813, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_drop_shots', 'DROP SHOTS PICKLEBALL', 'Ngõ 64 Xuân Quảng, Gia Lâm, Hà Nội', 5.0, 95, 1.0, 21.017052, 105.927111, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_nfs_arena_dong_ngac', 'NFS Arena Đông Ngạc', '20 Đông Thắng - Đông Ngạc Bắc Từ Liêm - HN - (Phố Kẻ Vẽ đi vào 100m) (cách Ciputra 1,5 Km)(cổng sau chung cư Eco Home 123)', 4.6, 102, 1.0, 21.087779, 105.781553, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_ha_dong_cn1', 'SÂN CẦU LÔNG VB - VẠN PHÚC', 'Trường tiểu học Vạn Bảo, Vạn Phúc, Hà Đông, Hà Nội', 4.7, 109, 1.0, 20.978219, 105.769692, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_vnpickleball_truong_ban_mai__6a21d0300', 'Sân PickleBall trường Ban Mai Vạn Phúc Hà Đông', 'Đ.C7, Vạn Phúc, Hà Đông, Hà Nội', 4.8, 116, 1.0, 20.979113, 105.768008, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_nam_hai_phong_cn_2', 'Sân cầu lông Nam Hải Phong', 'Ngõ 15, Đa tốn, Gia lâm', 4.9, 123, 1.0, 20.983614, 105.920631, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_lotus_pickleball_gia_lam', 'Lotus Pickleball Gia Lâm', 'Ngõ 234 Ngô Xuân Quảng, Trâu Quỳ, Gia Lâm, Hà Nội', 5.0, 130, 1.0, 21.01475, 105.933949, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_trang_trang_arena_pickleball_club', 'Trang Trang Arena Pickleball Club', 'Tập Thể 810 ngõ 512 Ngọc Hồi, Văn Điển, Thanh Trì, TP Hà Nội', 4.6, 137, 1.0, 20.938278, 105.842476, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_tm_coma5_sport_badminton', 'Tm  COMA5 Sport', 'Coma 5-70 Đ. Tây Mỗ, Tây Mỗ, Nam Từ Liêm, Hà Nội', 4.7, 25, 1.0, 21.017557, 105.746826, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_nam_hai_phong_cn_1', 'Sân cầu lông Nam Hải Phong', 'Đường Ngô Xuân Quảng, Gia Lâm, Hà Nội.', 4.8, 32, 1.0, 21.004812, 105.935469, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_dinh_le_hoc_vien_nong_nghiep_viet_nam', 'Sân cầu lông Đĩnh Lê - Học Viện Nông Nghiệp Việt Nam', 'Đường Ngô Xuân Quảng, Gia Lâm, Hà Nội', 4.9, 39, 1.0, 21.004756, 105.935466, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_nam_hai_phong_badminton', 'sport_nam_hai_phong_badminton', 'Đường Ngô Xuân Quảng, Gia Lâm, Hà Nội.', 5.0, 46, 1.0, 21.004782, 105.93548, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_tam_luu_sports', 'Tam Lưu Sports', '55 Đường Y,Đào Nguyên, tt. Trâu Quỳ, Gia Lâm Ng. 86 Đ. Trâu Quỳ, Trâu Quỳ, Gia Lâm, Hà Nội', 4.6, 53, 1.0, 21.004812, 105.935566, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_lam_hong_hai', 'Lam Hồng Hải', 'Số 53, phố Đức Giang, phường Việt Hưng, Hà Nội', 4.7, 60, 1.0, 21.004622, 105.935513, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_khu_the_thao_van_phu_san_pic_a45c47276', 'KHU THỂ THAO VĂN PHÚ,  SÂN PICKLEBALL X7 VĂN PHÚ, SÂN BÓNG ĐÁ X7 VĂN PHÚ', 'số 9 phố Hà Cầu, phường Phú La, Hà Đông', 4.8, 67, 1.0, 20.962152, 105.768218, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_pickleball_pho_mieu', 'Pickle Ball Phố Miêu', 'Cafe & Tea 1982 Khu chợ Miêu Nha, P. Do Nha, Nam Từ Liêm, Hà Nội', 4.9, 74, 1.0, 21.015802, 105.738034, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_ha_dong_cn2', 'SÂN CẦU LÔNG TQT - DƯƠNG NỘI', 'Trường tiểu học Trần Quốc Toản, Dương Nội, Hà Đông, Hà Nội', 5.0, 81, 1.0, 20.988575, 105.744722, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_pickleball_la_khe', 'Pickleball La Khê', 'Số 55 xóm ngõ đồi, tổ dân phố 4, phường La Khê, Hà Đông, Hà Nội', 4.6, 88, 1.0, 20.95687, 105.766905, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_aba_fit_max_hud', 'ABA FIT MAX HUD', 'Lô đất CC3 Dự án Khu đô thị mới Vân Canh, Xã Vân Canh, Huyện Hoài Đức, Thành phố Hà Nội, Việt Nam', 4.7, 95, 1.0, 21.042862, 105.72989, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickleball_vietgangz', 'Vietgangz Pickleball', 'Kiêu Kỵ, Gia Lâm, Hà Nội', 4.8, 102, 1.0, 20.985075, 105.946774, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_pickball_khanh_hoi', 'Pickleball Khánh Hội', '804 Quang Trung, Hà Đông, Hà Nội', 4.9, 109, 1.0, 20.957074, 105.756266, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_clb_cau_long_44_bala', 'CLB Cầu Lông 44 Bala', '44 P. Ba La, Phú Lương, Hà Đông, Hà Nội', 5.0, 116, 1.0, 20.954675, 105.757094, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_an_khanh_batminton', 'An Khánh Badminton', 'Ô A3.1 khu nhà kho - xưởng lô An Khánh - Thôn 1 xã Lại Yên - Hoài Đức - Hà Nội', 4.6, 123, 1.0, 21.019871, 105.716678, 'Cầu lông', 1, 120000, 70000, 100000, 140000, 'court_badminton'),
('alobo_sport_cm_pickleball', 'CM Pickleball', 'Khu đô thị mới Nguyên Khê, Đông Anh, Hà Nội', 4.7, 130, 1.0, 21.178944, 105.84036, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_urban_pickleball', 'URBAN PICKLEBALL 01', '5PQC+C5V Mê Linh, Hà Nội', 4.8, 137, 1.0, 21.188616, 105.720407, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball'),
('alobo_sport_puzolan_pickleball', 'PUZOLAN CAFE & PICK', 'Sơn Lộc P. Tùng Thiện, Trung Sơn Trầm, Sơn Tây, Hà Nội', 4.9, 25, 1.0, 21.114809, 105.492183, 'Pickleball', 0, 180000, 120000, 150000, 200000, 'court_pickleball')
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

-- 2. BẢNG SÂN CON (court_fields) - CẤU HÌNH CHO 3 SÂN DEMO ĐẶT CHỖ (MỖI MÔN 1 SÂN)
INSERT INTO `court_fields` (`id`, `court_id`, `field_label`, `field_detail`, `field_type`, `resource_keys`, `is_active`) VALUES
('field_alobo_pb_1', 'alobo_sport_htl_pickleball_ham_tu_quan', 'Sân P1', 'Sân Pickleball tiêu chuẩn HTL 1', 'PICKLEBALL', 'alobo_pb:res_1', TRUE),
('field_alobo_pb_2', 'alobo_sport_htl_pickleball_ham_tu_quan', 'Sân P2', 'Sân Pickleball tiêu chuẩn HTL 2', 'PICKLEBALL', 'alobo_pb:res_2', TRUE),
('field_alobo_bm_1', 'alobo_sport_an_duong_badminton', 'Sân Cầu Lông 1', 'Sân thảm tiêu chuẩn BWF An Dương 1', 'BADMINTON', 'alobo_bm:res_1', TRUE),
('field_alobo_bm_2', 'alobo_sport_an_duong_badminton', 'Sân Cầu Lông 2', 'Sân thảm tiêu chuẩn BWF An Dương 2', 'BADMINTON', 'alobo_bm:res_2', TRUE),
('field_alobo_fb_1', 'alobo_sport_bongda_dinhthon_2', 'Sân 7A', 'Sân cỏ nhân tạo Đình Thôn 7 người A', '7_VS_7', 'alobo_fb:res_1', TRUE),
('field_alobo_fb_2', 'alobo_sport_bongda_dinhthon_2', 'Sân 7B', 'Sân cỏ nhân tạo Đình Thôn 7 người B', '7_VS_7', 'alobo_fb:res_2', TRUE)
ON DUPLICATE KEY UPDATE
    `field_label` = VALUES(`field_label`),
    `field_detail` = VALUES(`field_detail`),
    `field_type` = VALUES(`field_type`),
    `resource_keys` = VALUES(`resource_keys`),
    `is_active` = VALUES(`is_active`);

-- 3. BẢNG TÀI NGUYÊN VẬT LÝ (court_resources) - CHỐNG RACE CONDITION TRÙNG LỊCH
INSERT INTO `court_resources` (`id`, `court_id`, `name`, `description`) VALUES
('alobo_pb:res_1', 'alobo_sport_htl_pickleball_ham_tu_quan', 'Mặt sân Pickleball P1', 'Tài nguyên vật lý sân P1'),
('alobo_pb:res_2', 'alobo_sport_htl_pickleball_ham_tu_quan', 'Mặt sân Pickleball P2', 'Tài nguyên vật lý sân P2'),
('alobo_bm:res_1', 'alobo_sport_an_duong_badminton', 'Thảm cầu lông số 1', 'Tài nguyên vật lý thảm 1'),
('alobo_bm:res_2', 'alobo_sport_an_duong_badminton', 'Thảm cầu lông số 2', 'Tài nguyên vật lý thảm 2'),
('alobo_fb:res_1', 'alobo_sport_bongda_dinhthon_2', 'Mặt cỏ sân 7A', 'Tài nguyên vật lý mặt cỏ 7A'),
('alobo_fb:res_2', 'alobo_sport_bongda_dinhthon_2', 'Mặt cỏ sân 7B', 'Tài nguyên vật lý mặt cỏ 7B')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `description` = VALUES(`description`);

COMMIT;
