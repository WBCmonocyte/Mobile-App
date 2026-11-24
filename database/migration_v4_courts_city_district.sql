-- ========================================================
-- MIGRATION V4: Chuẩn hóa khu vực (city, district) cho bảng courts
-- Dự án: EasySport (MySQL 8.x / MariaDB trên Railway / Local)
-- Chạy được an toàn, có thể chạy lại nhiều lần (idempotent)
-- ========================================================

-- 1. Thêm cột city và district nếu chưa tồn tại
DROP PROCEDURE IF EXISTS AddCourtsCityDistrictColumns;
DELIMITER $$
CREATE PROCEDURE AddCourtsCityDistrictColumns()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courts' AND COLUMN_NAME = 'city'
    ) THEN
        ALTER TABLE `courts` ADD COLUMN `city` VARCHAR(100) NULL AFTER `address`;
    END IF;

    IF NOT EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courts' AND COLUMN_NAME = 'district'
    ) THEN
        ALTER TABLE `courts` ADD COLUMN `district` VARCHAR(100) NULL AFTER `city`;
    END IF;
END$$
DELIMITER ;

CALL AddCourtsCityDistrictColumns();
DROP PROCEDURE IF EXISTS AddCourtsCityDistrictColumns;

START TRANSACTION;

-- 2. Cập nhật khu vực cho 5 sân mẫu demo (TP. Hồ Chí Minh)
UPDATE `courts` SET `city` = 'TP. Hồ Chí Minh', `district` = 'Tân Bình' WHERE `id` = 'court_thanh_phat'; -- Sân bóng Thành Phát
UPDATE `courts` SET `city` = 'TP. Hồ Chí Minh', `district` = 'Gò Vấp' WHERE `id` = 'court_hoang_gia'; -- Sân bóng Hoàng Gia
UPDATE `courts` SET `city` = 'TP. Hồ Chí Minh', `district` = 'Tân Bình' WHERE `id` = 'court_cau_long_tan_binh'; -- CLB Cầu Lông Tân Bình
UPDATE `courts` SET `city` = 'TP. Hồ Chí Minh', `district` = 'Tân Bình' WHERE `id` = 'court_pickleball_viet_nhat'; -- Sân Pickleball Việt Nhật
UPDATE `courts` SET `city` = 'TP. Hồ Chí Minh', `district` = 'Tân Bình' WHERE `id` = 'court_chao_lua'; -- Sân bóng Chảo Lửa

-- 3. Cập nhật khu vực cho 70 sân bóng đá Hà Nội (nguồn hanoi_football_catalog_sources.csv - IN_SQL)
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_hoang_cau'; -- Sân bóng đá Hoàng Cầu
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_thuy_loi'; -- Sân bóng đá Đại học Thủy Lợi
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_y_ha_noi'; -- Sân bóng Đại học Y Hà Nội
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_cong_doan'; -- Sân bóng Đại học Công Đoàn
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Ba Đình' WHERE `id` = 'hanoi_fb_phuc_xa'; -- Sân bóng đá Phúc Xá
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hai Bà Trưng' WHERE `id` = 'hanoi_fb_bach_khoa'; -- Sân vận động Bách Khoa
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hai Bà Trưng' WHERE `id` = 'hanoi_fb_kinh_te_quoc_dan'; -- Sân bóng Đại học Kinh Tế Quốc Dân
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hai Bà Trưng' WHERE `id` = 'hanoi_fb_vo_thi_sau'; -- Sân bóng đá Công viên Tuổi Trẻ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hai Bà Trưng' WHERE `id` = 'hanoi_fb_nguyen_khoai'; -- Sân bóng đá Hồng Hà Nguyễn Khoái
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Ba Đình' WHERE `id` = 'hanoi_fb_ha_noi_water_khu_lien_hop'; -- Sân bóng Công ty Cấp nước Hà Nội
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_dong_da_dang_tien_dong'; -- Sân bóng Đặng Tiến Đông
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_dong_da_hao_nam'; -- Sân bóng Hào Nam
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_dong_da_hao_nam_cat_linh'; -- Sân bóng Cát Linh
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Ba Đình' WHERE `id` = 'hanoi_fb_ba_dinh_doi_can'; -- Sân bóng Đội Cấn
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_dong_da_chua_boc'; -- Sân bóng Học viện Ngân Hàng
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_phong_khong_khong_quan'; -- Sân bóng Phòng Không Không Quân
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_khuong_dinh'; -- Sân bóng đá Khương Đình
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_thuong_dinh'; -- Sân bóng Thượng Đình
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_ha_dinh'; -- Sân bóng Hạ Đình
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_quan_he_giao_thong'; -- Sân bóng Đại học Giao Thông Vận Tải
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'hanoi_fb_chua_lang'; -- Sân bóng Chùa Láng
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'hanoi_fb_su_pham'; -- Sân vận động Đại học Sư Phạm Hà Nội
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Ba Đình' WHERE `id` = 'hanoi_fb_nghia_do'; -- Sân bóng đá Nghĩa Đô
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Ba Đình' WHERE `id` = 'hanoi_fb_quan_ngua'; -- Sân bóng Cung thể thao Quần Ngựa
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Tây Hồ' WHERE `id` = 'hanoi_fb_nhat_tan_an_duong_vuong'; -- Sân bóng Nhật Tân
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Tây Hồ' WHERE `id` = 'hanoi_fb_xuan_la'; -- Sân bóng Xuân La
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_thanh_xuan_khuong_trung'; -- Sân bóng Đầm Hồng 1 Khương Trung
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Tây Hồ' WHERE `id` = 'hanoi_fb_tay_ho_an_duong_nghi_tam'; -- Sân bóng Nghi Tàm An Dương
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_hoang_mai_kim_luy_tan_mai'; -- Sân bóng Tân Mai
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_hoang_mai_den_lu'; -- Sân bóng Công viên Đền Lừ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_hoang_mai_vinh_tuy_linh_nam'; -- Sân bóng Vĩnh Hưng Lĩnh Nam
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hai Bà Trưng' WHERE `id` = 'hanoi_fb_hai_ba_trung_vinh_tuy_time_city'; -- Sân bóng Times City Minh Khai
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Ba Đình' WHERE `id` = 'hanoi_fb_ba_dinh_hoang_hoa_tham'; -- Sân bóng Hoàng Hoa Thám
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'hanoi_fb_thanh_lam_green_park'; -- Sân bóng Thành Lâm Green Park
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'hanoi_fb_mai_dich'; -- Sân bóng Trường Múa Mai Dịch
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'hanoi_fb_trung_kinh_ha'; -- Sân bóng Trung Kính Hạ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'hanoi_fb_nguyen_chanh'; -- Sân bóng đá Nguyễn Chánh
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'hanoi_fb_yen_hoa_nam_trung_yen'; -- Sân bóng Yên Hòa Nam Trung Yên
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'hanoi_fb_duy_tan'; -- Sân bóng Duy Tân
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'hanoi_fb_cuong_quoc'; -- Sân bóng đá Cường Quốc
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_thanh_phat_hoang_mai'; -- Sân bóng đá Thanh Phát Hoàng Mai
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_bo_cong_an_nguyen_xien'; -- Sân bóng Trung tâm TDTT Bộ Công An
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_dinh_cong'; -- Sân bóng Định Công
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_linh_dam'; -- Sân bóng Bán đảo Linh Đàm
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_gamuda'; -- Sân bóng Công viên Gamuda
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_phap_van'; -- Sân bóng Khu đô thị Pháp Vân
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'hanoi_fb_hoang_liet'; -- Sân bóng Hoàng Liệt
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Trì' WHERE `id` = 'hanoi_fb_tan_trieu'; -- Sân bóng Tổng cục 5 Tân Triều
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_nguyen_xien_ha_dinh'; -- Sân bóng Nguyễn Xiển - Hạ Đình
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_kim_giang'; -- Sân bóng Kim Giang
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_khuat_duy_tien'; -- Sân bóng Khuất Duy Tiến
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'hanoi_fb_nguyen_tuan'; -- Sân bóng số 9 Lê Văn Thiêm - Nguyễn Tuân
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_mic_nguyen_trai'; -- Sân bóng MIC Nguyễn Trãi
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_pham_hung_keangnam'; -- Sân bóng Keangnam Phạm Hùng
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'hanoi_fb_cau_giay_yen_hoa_fpt'; -- Sân bóng Đại học FPT Cầu Giấy
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_my_dinh_le_quang_dao'; -- Sân bóng Mỹ Đình Lê Quang Đạo
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_my_dinh_chau_au'; -- Sân bóng Châu Âu Mỹ Đình
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_me_tri_thuong'; -- Sân bóng Mễ Trì Thượng
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_dong_do_trung_van'; -- Sân bóng Đông Đô Trung Văn
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_flc_dai_mo'; -- Sân bóng FLC Garden City Đại Mỗ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_huu_hung_dai_mo'; -- Sân bóng Hữu Hưng Đại Mỗ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_ngoc_truc_dai_mo'; -- Sân bóng Ngọc Trục Đại Mỗ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_tay_mo_viglacera'; -- Sân bóng Viglacera Tây Mỗ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'hanoi_fb_phuong_canh'; -- Sân bóng Phương Canh
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Bắc Từ Liêm' WHERE `id` = 'hanoi_fb_co_nhue_pham_van_dong'; -- Sân bóng Cổ Nhuế
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Bắc Từ Liêm' WHERE `id` = 'hanoi_fb_xuan_dinh'; -- Sân bóng Xuân Đỉnh
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Bắc Từ Liêm' WHERE `id` = 'hanoi_fb_dai_hoc_cong_nghiep'; -- Sân bóng Đại học Công Nghiệp Hà Nội
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Bắc Từ Liêm' WHERE `id` = 'hanoi_fb_phuc_dien'; -- Sân bóng Phúc Diễn
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'hanoi_fb_ha_dong_van_phuc'; -- Sân bóng Vạn Phúc
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'hanoi_fb_ha_dong_mo_lao'; -- Sân bóng Làng Việt Kiều Châu Âu Mỗ Lao

-- 4. Cập nhật khu vực cho 69 sân Alobo Hà Nội (nguồn alobo_hanoi_courts.csv)
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàn Kiếm' WHERE `id` = 'alobo_sport_htl_pickleball_ham_tu_quan'; -- HTL Pickleball Hàm Tử Quan
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đống Đa' WHERE `id` = 'alobo_sport_3t_pickleball'; -- 3T Pickleball
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Tây Hồ' WHERE `id` = 'alobo_sport_an_duong_badminton'; -- Sân cầu lông An Dương
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'alobo_sport_canh_ho_badminton'; -- Sân cầu lông 173B Trường Trinh
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_ninex_pickleball_club'; -- NineX Pickleball Club
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'alobo_sport_pickleba_vu_dai_kim'; -- CLB Pickleball Vũ Đại Kim
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'alobo_sport_luxury'; -- Luxury
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'alobo_sport_pickleball_345_khuong_trung'; -- Pickleball 345 Khương Trung
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'alobo_sport_pickleball_dam_hong_2'; -- Pickleball Đầm Hồng 2 (có mái che)
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Tây Hồ' WHERE `id` = 'alobo_sport_sixtyninetree_pickleball'; -- SixtyNineTree Pickleball
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'alobo_sport_hcci_stadium'; -- HCCI STADIUM
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_gia_thuy_pick'; -- Sân Gia Thuỵ Pickleball
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_forheads_pickleball_club'; -- Forheads Pickleball Club
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'alobo_sport_dai_linh_pickleball_cn2'; -- Cầu Lông Trường Đại học TDTT Đà Nẵng
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'alobo_sport_pickleball_yen_hoa'; -- Pickleball Yên Hòa
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'alobo_sport_san_cau_long_ton_that_thuyet_cn2'; -- Pickleball Yên Hòa
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_tttt_z133_pickleball'; -- Trung tâm thể thao Z133
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'alobo_sport_pick_vinh_hoang'; -- Sân Pick VĨNH HOÀNG
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'alobo_sport_ton_that_thuyet_badminton'; -- Sân cầu lông Tôn Thất Thuyết
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'alobo_sport_pick360_dai_tu'; -- Sân Pick360 Đại Từ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'alobo_sport_vnpickleball_khanh_duy'; -- VNPickleball Khánh Duy
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Xuân' WHERE `id` = 'alobo_sport_swin_214_nguyen_xien_badminton'; -- Sân cầu lông SWIN 214 Nguyễn Xiển
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Tây Hồ' WHERE `id` = 'alobo_sport_hippi_club_badminton'; -- Hippi Club
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_pickleball_kings_club'; -- Kings Pickleball Club
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'alobo_sport_le_nguyen_cn3'; -- Số 1 Trần Bình
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_sky_pickle_club'; -- Sky-Pickle Club
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_songxanh_pickleball'; -- Sóng Xanh Pickleball
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'alobo_sport_pickleball_cong_vien_bac_linh_dam_nhat'; -- Sân Pickleball Công Viên Bắc Linh Đàm
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoàng Mai' WHERE `id` = 'alobo_sport_pickleball_cong_vien_bac_linh_dam_'; -- Pickleball Công Viên Bắc Linh Đàm
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_pickleball_182_dinhthon_cum3'; -- Pickleball 182 Đình Thôn Cụm 3 Sân
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_bongda_dinhthon_2'; -- Sân bóng  Đình Thôn 2
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_pickleball_182_dinhthon_cum4'; -- Pickleball 182 Đình Thôn Cụm 4 Sân
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Trì' WHERE `id` = 'alobo_sport_clb_pickleball_189_pham_tu'; -- CLB Pickleball 189 Phạm Tu
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Tây Hồ' WHERE `id` = 'alobo_sport_tay_ho_pickleball'; -- SPORT Tây Hồ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_win_pickleball_club'; -- Win Pickleball Club
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_win_pickleball_club_test'; -- Win Pickleball Club Test
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_win_pickleball_club_ha_noi'; -- Win Pickleball Club
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Trì' WHERE `id` = 'alobo_sport_san_bong_thang_dung'; -- Sân bóng Thành Dũng
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_the_pickleball_lounge_2'; -- The Pickleball Lounge 2
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_the_pickleball_lounge'; -- The Pickleball Lounge
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Cầu Giấy' WHERE `id` = 'alobo_sport_linh_dong_pickleball_cn2'; -- Linh Đông Pickleball
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_pickleball_the_pickleball_lounge'; -- The Pickleball Lounge
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Trì' WHERE `id` = 'alobo_sport_tuktuk_pickleball'; -- Tuktuk Pickleball
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_aiko_pickleball_phu_do'; -- Aiko Pickleball Phú Đô
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Gia Lâm' WHERE `id` = 'alobo_sport_drop_shots'; -- DROP SHOTS PICKLEBALL
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Bắc Từ Liêm' WHERE `id` = 'alobo_sport_nfs_arena_dong_ngac'; -- NFS Arena Đông Ngạc
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'alobo_sport_pickleball_ha_dong_cn1'; -- SÂN CẦU LÔNG VB - VẠN PHÚC
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'alobo_sport_vnpickleball_truong_ban_mai__6a21d0300'; -- Sân PickleBall trường Ban Mai Vạn Phúc Hà Đông
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Gia Lâm' WHERE `id` = 'alobo_sport_nam_hai_phong_cn_2'; -- Sân cầu lông Nam Hải Phong
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Gia Lâm' WHERE `id` = 'alobo_sport_lotus_pickleball_gia_lam'; -- Lotus Pickleball Gia Lâm
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Thanh Trì' WHERE `id` = 'alobo_sport_trang_trang_arena_pickleball_club'; -- Trang Trang Arena Pickleball Club
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_sport_tm_coma5_sport_badminton'; -- Tm  COMA5 Sport
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Gia Lâm' WHERE `id` = 'alobo_sport_nam_hai_phong_cn_1'; -- Sân cầu lông Nam Hải Phong
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Gia Lâm' WHERE `id` = 'alobo_sport_dinh_le_hoc_vien_nong_nghiep_viet_nam'; -- Sân cầu lông Đĩnh Lê - Học Viện Nông Nghiệp Việt Nam
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Gia Lâm' WHERE `id` = 'alobo_sport_nam_hai_phong_badminton'; -- sport_nam_hai_phong_badminton
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Gia Lâm' WHERE `id` = 'alobo_tam_luu_sports'; -- Tam Lưu Sports
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Long Biên' WHERE `id` = 'alobo_sport_lam_hong_hai'; -- Lam Hồng Hải
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'alobo_sport_khu_the_thao_van_phu_san_pic_a45c47276'; -- KHU THỂ THAO VĂN PHÚ,  SÂN PICKLEBALL X7 VĂN PHÚ, SÂN BÓNG ĐÁ X7 VĂN PHÚ
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Nam Từ Liêm' WHERE `id` = 'alobo_pickleball_pho_mieu'; -- Pickle Ball Phố Miêu
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'alobo_sport_pickleball_ha_dong_cn2'; -- SÂN CẦU LÔNG TQT - DƯƠNG NỘI
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'alobo_sport_pickleball_la_khe'; -- Pickleball La Khê
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoài Đức' WHERE `id` = 'alobo_sport_aba_fit_max_hud'; -- ABA FIT MAX HUD
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Gia Lâm' WHERE `id` = 'alobo_sport_pickleball_vietgangz'; -- Vietgangz Pickleball
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'alobo_sport_pickball_khanh_hoi'; -- Pickleball Khánh Hội
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hà Đông' WHERE `id` = 'alobo_sport_clb_cau_long_44_bala'; -- CLB Cầu Lông 44 Bala
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Hoài Đức' WHERE `id` = 'alobo_sport_an_khanh_batminton'; -- An Khánh Badminton
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Đông Anh' WHERE `id` = 'alobo_sport_cm_pickleball'; -- CM Pickleball
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Mê Linh' WHERE `id` = 'alobo_sport_urban_pickleball'; -- URBAN PICKLEBALL 01
UPDATE `courts` SET `city` = 'Hà Nội', `district` = 'Sơn Tây' WHERE `id` = 'alobo_sport_puzolan_pickleball'; -- PUZOLAN CAFE & PICK

COMMIT;

-- ========================================================
-- 5. CÂU LỆNH KIỂM TRA ĐỐI CHIẾU DỮ LIỆU SAU MIGRATION
-- ========================================================

-- Truy vấn 1: Thống kê số lượng sân theo Thành phố và Quận/Huyện
SELECT
    COALESCE(city, 'Chưa có TP') AS city,
    COALESCE(district, 'Chưa có Quận/Huyện') AS district,
    COUNT(*) AS total_courts
FROM `courts`
GROUP BY `city`, `district`
ORDER BY `city`, `district`;

-- Truy vấn 2: Danh sách các sân còn thiếu thông tin khu vực (kỳ vọng: 0 dòng)
SELECT
    `id`,
    `name`,
    `address`,
    `sport_type`
FROM `courts`
WHERE `city` IS NULL OR `district` IS NULL OR `city` = '' OR `district` = '';
