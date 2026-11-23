-- ============================================================================
-- EASYSPORT CATALOG IMPORT: 70 PHYSICAL FOOTBALL VENUES IN HANOI
-- Target: Supplement catalog for Hanoi Football (Vành đai 1, 2, 3 và phụ cận)
-- Engine: MySQL 8.x / MariaDB (Railway & Local)
-- Encoding: UTF-8 (utf8mb4)
-- Mode: PURE LOOKUP CATALOG (courts table only, no speculative sub-courts)
-- NOTE: NO `USE` statement included - select your Railway schema before running.
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

START TRANSACTION;

-- ----------------------------------------------------------------------------
-- Upsert 70 verified football venues into `courts` table
-- Idempotent: ON DUPLICATE KEY UPDATE avoids duplicating or breaking rows
-- ----------------------------------------------------------------------------

INSERT INTO `courts` (
    `id`, `name`, `address`, `rating`, `review_count`, `distance_km`,
    `latitude`, `longitude`, `sport_type`, `indoor`,
    `price_per_hour`, `morning_price`, `afternoon_price`, `evening_price`, `image_res_name`
) VALUES
  ('hanoi_fb_hoang_cau', 'Sân bóng đá Hoàng Cầu', 'Số 69 Phố Hoàng Cầu, Phường Ô Chợ Dừa, Quận Đống Đa, Hà Nội', 4.8, 12, 1.0, 21.0189000, 105.8236000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_thanh_phat'),
  ('hanoi_fb_thuy_loi', 'Sân bóng đá Đại học Thủy Lợi', 'Số 175 Phố Tây Sơn, Phường Trung Liệt, Quận Đống Đa, Hà Nội', 4.9, 15, 1.4, 21.0076000, 105.8247000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_chao_lua'),
  ('hanoi_fb_y_ha_noi', 'Sân bóng Đại học Y Hà Nội', 'Số 1 Phố Tôn Thất Tùng, Phường Kim Liên, Quận Đống Đa, Hà Nội', 4.7, 18, 1.8, 21.0033000, 105.8318000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_hoang_gia'),
  ('hanoi_fb_cong_doan', 'Sân bóng Đại học Công Đoàn', 'Số 169 Phố Tây Sơn, Phường Quang Trung, Quận Đống Đa, Hà Nội', 4.8, 21, 2.2, 21.0102000, 105.8239000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_phuc_xa', 'Sân bóng đá Phúc Xá', 'Khu thể thao bãi Phúc Xá, Phường Phúc Xá, Quận Ba Đình, Hà Nội', 4.9, 24, 2.6, 21.0468000, 105.8485000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_chao_lua'),
  ('hanoi_fb_bach_khoa', 'Sân vận động Bách Khoa', 'Số 1 Phố Tạ Quang Bửu, Phường Bách Khoa, Quận Hai Bà Trưng, Hà Nội', 4.7, 27, 3.0, 21.0049000, 105.8447000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_hoang_gia'),
  ('hanoi_fb_kinh_te_quoc_dan', 'Sân bóng Đại học Kinh Tế Quốc Dân', 'Số 207 Đường Giải Phóng, Phường Đồng Tâm, Quận Hai Bà Trưng, Hà Nội', 4.8, 30, 3.4, 21.0012000, 105.8425000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_vo_thi_sau', 'Sân bóng đá Công viên Tuổi Trẻ', 'Số 99 Phố Võ Thị Sáu, Phường Thanh Nhàn, Quận Hai Bà Trưng, Hà Nội', 4.9, 33, 3.8, 21.0084000, 105.8529000, 'Bóng đá', 0, 500000, 300000, 400000, 600000, 'court_chao_lua'),
  ('hanoi_fb_nguyen_khoai', 'Sân bóng đá Hồng Hà Nguyễn Khoái', 'Số 463 Đường Nguyễn Khoái, Phường Vĩnh Tuy, Quận Hai Bà Trưng, Hà Nội', 4.7, 36, 4.2, 21.0019000, 105.8715000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_hoang_gia'),
  ('hanoi_fb_ha_noi_water_khu_lien_hop', 'Sân bóng Công ty Cấp nước Hà Nội', 'Số 44 Phố Yên Phụ, Phường Trúc Bạch, Quận Ba Đình, Hà Nội', 4.8, 39, 4.6, 21.0492000, 105.8421000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_dong_da_dang_tien_dong', 'Sân bóng Đặng Tiến Đông', 'Ngõ 178 Phố Đặng Tiến Đông, Phường Trung Liệt, Quận Đống Đa, Hà Nội', 4.9, 42, 1.0, 21.0125000, 105.8214000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_dong_da_hao_nam', 'Sân bóng Hào Nam', 'Ngõ 8A Phố Vũ Thạnh, Phường Ô Chợ Dừa, Quận Đống Đa, Hà Nội', 4.7, 45, 1.4, 21.0264000, 105.8276000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_hoang_gia'),
  ('hanoi_fb_dong_da_hao_nam_cat_linh', 'Sân bóng Cát Linh', 'Ngõ 33 Phố Cát Linh, Phường Cát Linh, Quận Đống Đa, Hà Nội', 4.8, 48, 1.8, 21.0312000, 105.8315000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_ba_dinh_doi_can', 'Sân bóng Đội Cấn', 'Ngõ 279 Phố Đội Cấn, Phường Liễu Giai, Quận Ba Đình, Hà Nội', 4.9, 51, 2.2, 21.0365000, 105.8184000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_chao_lua'),
  ('hanoi_fb_dong_da_chua_boc', 'Sân bóng Học viện Ngân Hàng', 'Số 12 Phố Chùa Bộc, Phường Quang Trung, Quận Đống Đa, Hà Nội', 4.7, 54, 2.6, 21.0089000, 105.8294000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_hoang_gia'),
  ('hanoi_fb_phong_khong_khong_quan', 'Sân bóng Phòng Không Không Quân', 'Số 3 Đường Lê Trọng Tấn, Phường Khương Mai, Quận Thanh Xuân, Hà Nội', 4.8, 12, 3.0, 20.9997000, 105.8336000, 'Bóng đá', 0, 600000, 400000, 500000, 700000, 'court_thanh_phat'),
  ('hanoi_fb_khuong_dinh', 'Sân bóng đá Khương Đình', 'Ngõ 460 Đường Khương Đình, Phường Hạ Đình, Quận Thanh Xuân, Hà Nội', 4.9, 15, 3.4, 20.9882000, 105.8153000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_thuong_dinh', 'Sân bóng Thượng Đình', 'Số 129 Phố Nguyễn Trãi, Phường Thượng Đình, Quận Thanh Xuân, Hà Nội', 4.7, 18, 3.8, 21.0008000, 105.8164000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_hoang_gia'),
  ('hanoi_fb_ha_dinh', 'Sân bóng Hạ Đình', 'Ngõ 342 Đường Khương Đình, Phường Hạ Đình, Quận Thanh Xuân, Hà Nội', 4.8, 21, 4.2, 20.9912000, 105.8128000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_quan_he_giao_thong', 'Sân bóng Đại học Giao Thông Vận Tải', 'Số 3 Phố Cầu Giấy, Phường Láng Thượng, Quận Đống Đa, Hà Nội', 4.9, 24, 4.6, 21.0284000, 105.8028000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_chua_lang', 'Sân bóng Chùa Láng', 'Số 112 Phố Chùa Láng, Phường Láng Thượng, Quận Đống Đa, Hà Nội', 4.7, 27, 1.0, 21.0232000, 105.8016000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_hoang_gia'),
  ('hanoi_fb_su_pham', 'Sân vận động Đại học Sư Phạm Hà Nội', 'Số 136 Đường Xuân Thủy, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Hà Nội', 4.8, 30, 1.4, 21.0374000, 105.7836000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_thanh_phat'),
  ('hanoi_fb_nghia_do', 'Sân bóng đá Nghĩa Đô', 'Số 100 Phố Nghĩa Dũng, Phường Phúc Xá, Quận Ba Đình, Hà Nội', 4.9, 33, 1.8, 21.0487000, 105.8456000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_chao_lua'),
  ('hanoi_fb_quan_ngua', 'Sân bóng Cung thể thao Quần Ngựa', 'Số 30 Phố Văn Cao, Phường Liễu Giai, Quận Ba Đình, Hà Nội', 4.7, 36, 2.2, 21.0396000, 105.8174000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_hoang_gia'),
  ('hanoi_fb_nhat_tan_an_duong_vuong', 'Sân bóng Nhật Tân', 'Ngõ 464 Đường Âu Cơ, Phường Nhật Tân, Quận Tây Hồ, Hà Nội', 4.8, 39, 2.6, 21.0821000, 105.8239000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_xuan_la', 'Sân bóng Xuân La', 'Ngõ 36 Phố Xuân La, Phường Xuân La, Quận Tây Hồ, Hà Nội', 4.9, 42, 3.0, 21.0573000, 105.8032000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_chao_lua'),
  ('hanoi_fb_thanh_xuan_khuong_trung', 'Sân bóng Đầm Hồng 1 Khương Trung', 'Ngõ 93 Phố Hoàng Văn Thái, Phường Khương Trung, Quận Thanh Xuân, Hà Nội', 4.7, 45, 3.4, 20.9908000, 105.8236000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_hoang_gia'),
  ('hanoi_fb_tay_ho_an_duong_nghi_tam', 'Sân bóng Nghi Tàm An Dương', 'Ngách 35 Ngõ 76 Đường An Dương, Phường Yên Phụ, Quận Tây Hồ, Hà Nội', 4.8, 48, 3.8, 21.0558000, 105.8398000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_hoang_mai_kim_luy_tan_mai', 'Sân bóng Tân Mai', 'Đường Tân Mai, Phường Tân Mai, Quận Hoàng Mai, Hà Nội', 4.9, 51, 4.2, 20.9882000, 105.8524000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_hoang_mai_den_lu', 'Sân bóng Công viên Đền Lừ', 'Khu công viên Đền Lừ, Phường Hoàng Văn Thụ, Quận Hoàng Mai, Hà Nội', 4.7, 54, 4.6, 20.9859000, 105.8582000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_hoang_gia'),
  ('hanoi_fb_hoang_mai_vinh_tuy_linh_nam', 'Sân bóng Vĩnh Hưng Lĩnh Nam', 'Số 126 Phố Vĩnh Hưng, Phường Vĩnh Hưng, Quận Hoàng Mai, Hà Nội', 4.8, 12, 1.0, 20.9935000, 105.8778000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_thanh_phat'),
  ('hanoi_fb_hai_ba_trung_vinh_tuy_time_city', 'Sân bóng Times City Minh Khai', 'Số 458 Đường Minh Khai, Phường Vĩnh Tuy, Quận Hai Bà Trưng, Hà Nội', 4.9, 15, 1.4, 20.9961000, 105.8672000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_chao_lua'),
  ('hanoi_fb_ba_dinh_hoang_hoa_tham', 'Sân bóng Hoàng Hoa Thám', 'Số 262 Phố Hoàng Hoa Thám, Phường Thụy Khuê, Quận Ba Đình, Hà Nội', 4.7, 18, 1.8, 21.0421000, 105.8198000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_hoang_gia'),
  ('hanoi_fb_thanh_lam_green_park', 'Sân bóng Thành Lâm Green Park', 'Lô C4 Khu đô thị mới Dịch Vọng, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội', 4.8, 21, 2.2, 21.0269000, 105.7925000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_thanh_phat'),
  ('hanoi_fb_mai_dich', 'Sân bóng Trường Múa Mai Dịch', 'Khu văn hóa nghệ thuật Mai Dịch, Đường Hồ Tùng Mậu, Quận Cầu Giấy, Hà Nội', 4.9, 24, 2.6, 21.0392000, 105.7761000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_trung_kinh_ha', 'Sân bóng Trung Kính Hạ', 'Ngõ 43 Đường Trung Kính, Phường Trung Hòa, Quận Cầu Giấy, Hà Nội', 4.7, 27, 3.0, 21.0183000, 105.7972000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_hoang_gia'),
  ('hanoi_fb_nguyen_chanh', 'Sân bóng đá Nguyễn Chánh', 'Lô A4 Phố Nguyễn Chánh, Phường Trung Hòa, Quận Cầu Giấy, Hà Nội', 4.8, 30, 3.4, 21.0118000, 105.7938000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_thanh_phat'),
  ('hanoi_fb_yen_hoa_nam_trung_yen', 'Sân bóng Yên Hòa Nam Trung Yên', 'Khu đô thị Nam Trung Yên, Phường Yên Hòa, Quận Cầu Giấy, Hà Nội', 4.9, 33, 3.8, 21.0163000, 105.7891000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_chao_lua'),
  ('hanoi_fb_duy_tan', 'Sân bóng Duy Tân', 'Số 78 Phố Duy Tân, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Hà Nội', 4.7, 36, 4.2, 21.0315000, 105.7828000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_hoang_gia'),
  ('hanoi_fb_cuong_quoc', 'Sân bóng đá Cường Quốc', 'Khu đô thị Kiến Hưng, Phường Kiến Hưng, Quận Hà Đông, Hà Nội', 4.8, 39, 4.6, 20.9572000, 105.7869000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_thanh_phat'),
  ('hanoi_fb_thanh_phat_hoang_mai', 'Sân bóng đá Thanh Phát Hoàng Mai', 'Số 2 Đường Kim Giang, Phường Đại Kim, Quận Hoàng Mai, Hà Nội', 4.9, 42, 1.0, 20.9825000, 105.8196000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_chao_lua'),
  ('hanoi_fb_bo_cong_an_nguyen_xien', 'Sân bóng Trung tâm TDTT Bộ Công An', 'Đường Nguyễn Xiển, Phường Đại Kim, Quận Hoàng Mai, Hà Nội', 4.7, 45, 1.4, 20.9784000, 105.8126000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_hoang_gia'),
  ('hanoi_fb_dinh_cong', 'Sân bóng Định Công', 'Khu đô thị mới Định Công, Phường Định Công, Quận Hoàng Mai, Hà Nội', 4.8, 48, 1.8, 20.9854000, 105.8341000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_linh_dam', 'Sân bóng Bán đảo Linh Đàm', 'Khu đô thị Bán đảo Linh Đàm, Phường Hoàng Liệt, Quận Hoàng Mai, Hà Nội', 4.9, 51, 2.2, 20.9678000, 105.8285000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_gamuda', 'Sân bóng Công viên Gamuda', 'Khu đô thị Gamuda Gardens, Phường Trần Phú, Quận Hoàng Mai, Hà Nội', 4.7, 54, 2.6, 20.9734000, 105.8647000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_hoang_gia'),
  ('hanoi_fb_phap_van', 'Sân bóng Khu đô thị Pháp Vân', 'Khu đô thị Pháp Vân - Tứ Hiệp, Phường Hoàng Liệt, Quận Hoàng Mai, Hà Nội', 4.8, 12, 3.0, 20.9632000, 105.8458000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_hoang_liet', 'Sân bóng Hoàng Liệt', 'Số 1 Đường Bằng Liệt, Phường Hoàng Liệt, Quận Hoàng Mai, Hà Nội', 4.9, 15, 3.4, 20.9658000, 105.8234000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_chao_lua'),
  ('hanoi_fb_tan_trieu', 'Sân bóng Tổng cục 5 Tân Triều', 'Khu đô thị Tổng cục 5, Xã Tân Triều, Huyện Thanh Trì, Hà Nội (sát VĐ3)', 4.7, 18, 3.8, 20.9731000, 105.8042000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_hoang_gia'),
  ('hanoi_fb_nguyen_xien_ha_dinh', 'Sân bóng Nguyễn Xiển - Hạ Đình', 'Ngõ 286 Đường Nguyễn Xiển, Phường Hạ Đình, Quận Thanh Xuân, Hà Nội', 4.8, 21, 4.2, 20.9875000, 105.8078000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_kim_giang', 'Sân bóng Kim Giang', 'Số 64 Phố Kim Giang, Phường Kim Giang, Quận Thanh Xuân, Hà Nội', 4.9, 24, 4.6, 20.9897000, 105.8169000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_khuat_duy_tien', 'Sân bóng Khuất Duy Tiến', 'Số 166 Đường Khuất Duy Tiến, Phường Nhân Chính, Quận Thanh Xuân, Hà Nội', 4.7, 27, 1.0, 20.9972000, 105.7978000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_hoang_gia'),
  ('hanoi_fb_nguyen_tuan', 'Sân bóng số 9 Lê Văn Thiêm - Nguyễn Tuân', 'Số 9 Phố Lê Văn Thiêm, Phường Nhân Chính, Quận Thanh Xuân, Hà Nội', 4.8, 30, 1.4, 21.0028000, 105.8009000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_thanh_phat'),
  ('hanoi_fb_mic_nguyen_trai', 'Sân bóng MIC Nguyễn Trãi', 'Số 1 Đường Lương Thế Vinh, Phường Trung Văn, Quận Nam Từ Liêm, Hà Nội (ngã tư Nguyễn Trãi)', 4.9, 33, 1.8, 20.9958000, 105.7936000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_chao_lua'),
  ('hanoi_fb_pham_hung_keangnam', 'Sân bóng Keangnam Phạm Hùng', 'Lô E6 Đường Phạm Hùng, Phường Mễ Trì, Quận Nam Từ Liêm, Hà Nội (sau Keangnam)', 4.7, 36, 2.2, 21.0185000, 105.7821000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_hoang_gia'),
  ('hanoi_fb_cau_giay_yen_hoa_fpt', 'Sân bóng Đại học FPT Cầu Giấy', 'Số 8 Đường Tôn Thất Thuyết, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Hà Nội', 4.8, 39, 2.6, 21.0289000, 105.7865000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_thanh_phat'),
  ('hanoi_fb_my_dinh_le_quang_dao', 'Sân bóng Mỹ Đình Lê Quang Đạo', 'Số 1 Đường Lê Quang Đạo, Phường Phú Đô, Quận Nam Từ Liêm, Hà Nội', 4.9, 42, 3.0, 21.0128000, 105.7686000, 'Bóng đá', 0, 550000, 350000, 450000, 650000, 'court_chao_lua'),
  ('hanoi_fb_my_dinh_chau_au', 'Sân bóng Châu Âu Mỹ Đình', 'Đường Lê Đức Thọ, Phường Mỹ Đình 2, Quận Nam Từ Liêm, Hà Nội', 4.7, 45, 3.4, 21.0286000, 105.7694000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_hoang_gia'),
  ('hanoi_fb_me_tri_thuong', 'Sân bóng Mễ Trì Thượng', 'Đường Mễ Trì Thượng, Phường Mễ Trì, Quận Nam Từ Liêm, Hà Nội', 4.8, 48, 3.8, 21.0112000, 105.7745000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_thanh_phat'),
  ('hanoi_fb_dong_do_trung_van', 'Sân bóng Đông Đô Trung Văn', 'Đường Trung Văn, Phường Trung Văn, Quận Nam Từ Liêm, Hà Nội', 4.9, 51, 4.2, 20.9964000, 105.7825000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_flc_dai_mo', 'Sân bóng FLC Garden City Đại Mỗ', 'Khu đô thị FLC Garden City, Phường Đại Mỗ, Quận Nam Từ Liêm, Hà Nội', 4.7, 54, 4.6, 20.9986000, 105.7582000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_hoang_gia'),
  ('hanoi_fb_huu_hung_dai_mo', 'Sân bóng Hữu Hưng Đại Mỗ', 'Số 120 Đường Hữu Hưng, Phường Đại Mỗ, Quận Nam Từ Liêm, Hà Nội', 4.8, 12, 1.0, 20.9951000, 105.7516000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_thanh_phat'),
  ('hanoi_fb_ngoc_truc_dai_mo', 'Sân bóng Ngọc Trục Đại Mỗ', 'Khu dân cư Ngọc Trục, Phường Đại Mỗ, Quận Nam Từ Liêm, Hà Nội', 4.9, 15, 1.4, 21.0021000, 105.7634000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_chao_lua'),
  ('hanoi_fb_tay_mo_viglacera', 'Sân bóng Viglacera Tây Mỗ', 'Khu đô thị chức năng Tây Mỗ, Phường Tây Mỗ, Quận Nam Từ Liêm, Hà Nội', 4.7, 18, 1.8, 21.0075000, 105.7482000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_hoang_gia'),
  ('hanoi_fb_phuong_canh', 'Sân bóng Phương Canh', 'Đường Xuân Phương, Phường Phương Canh, Quận Nam Từ Liêm, Hà Nội', 4.8, 21, 2.2, 21.0368000, 105.7429000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_thanh_phat'),
  ('hanoi_fb_co_nhue_pham_van_dong', 'Sân bóng Cổ Nhuế', 'Ngõ 521 Đường Cổ Nhuế, Phường Cổ Nhuế 2, Quận Bắc Từ Liêm, Hà Nội', 4.9, 24, 2.6, 21.0664000, 105.7725000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_chao_lua'),
  ('hanoi_fb_xuan_dinh', 'Sân bóng Xuân Đỉnh', 'Ngõ 355 Đường Xuân Đỉnh, Phường Xuân Đỉnh, Quận Bắc Từ Liêm, Hà Nội', 4.7, 27, 3.0, 21.0692000, 105.7894000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_hoang_gia'),
  ('hanoi_fb_dai_hoc_cong_nghiep', 'Sân bóng Đại học Công Nghiệp Hà Nội', 'Số 298 Đường Cầu Diễn, Phường Minh Khai, Quận Bắc Từ Liêm, Hà Nội', 4.8, 30, 3.4, 21.0538000, 105.7348000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_thanh_phat'),
  ('hanoi_fb_phuc_dien', 'Sân bóng Phúc Diễn', 'Đường Phúc Diễn, Phường Phúc Diễn, Quận Bắc Từ Liêm, Hà Nội', 4.9, 33, 3.8, 21.0481000, 105.7492000, 'Bóng đá', 0, 400000, 250000, 350000, 500000, 'court_chao_lua'),
  ('hanoi_fb_ha_dong_van_phuc', 'Sân bóng Vạn Phúc', 'Phố Vạn Phúc, Phường Vạn Phúc, Quận Hà Đông, Hà Nội', 4.7, 36, 4.2, 20.9782000, 105.7724000, 'Bóng đá', 0, 450000, 300000, 400000, 550000, 'court_hoang_gia'),
  ('hanoi_fb_ha_dong_mo_lao', 'Sân bóng Làng Việt Kiều Châu Âu Mỗ Lao', 'Khu đô thị Mỗ Lao, Phường Mộ Lao, Quận Hà Đông, Hà Nội', 4.8, 39, 4.6, 20.9856000, 105.7862000, 'Bóng đá', 0, 500000, 300000, 450000, 600000, 'court_thanh_phat')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `address` = VALUES(`address`),
    `rating` = VALUES(`rating`),
    `review_count` = VALUES(`review_count`),
    `distance_km` = VALUES(`distance_km`),
    `latitude` = VALUES(`latitude`),
    `longitude` = VALUES(`longitude`),
    `sport_type` = VALUES(`sport_type`),
    `indoor` = VALUES(`indoor`),
    `price_per_hour` = VALUES(`price_per_hour`),
    `morning_price` = VALUES(`morning_price`),
    `afternoon_price` = VALUES(`afternoon_price`),
    `evening_price` = VALUES(`evening_price`),
    `image_res_name` = VALUES(`image_res_name`);

COMMIT;
