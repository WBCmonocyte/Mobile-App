# Báo Cáo Crawl Dữ Liệu Sân Thể Thao Hà Nội Từ ALO Booking (EasySport)

> **Nguồn dữ liệu**: ALO Booking ([datlich.alobo.vn](https://datlich.alobo.vn/))  
> **Khu vực**: Thủ đô Hà Nội (Chỉ riêng Hà Nội)  
> **Các môn thể thao**: Bóng đá, Cầu lông, Pickleball  
> **Tệp xuất**: `database/alobo_hanoi_courts.sql` (file nạp Railway), `database/alobo_hanoi_courts.csv` (file đối chiếu)  
> **Ngày thực hiện**: 20/11/2026

---

## 1. Phương Pháp Thu Thập & Giải Mã Kỹ Thuật

ALO Booking là nền tảng đặt sân thể thao hoạt động theo mô hình ứng dụng Flutter Web (CanvasKit/WebAssembly). Để thu thập dữ liệu tự động, giải pháp reverse-engineering đã được áp dụng trực tiếp lên hệ thống API ngầm:

1. **Giao tiếp REST API Google Cloud Run**:
   - Máy chủ API: `https://user-app-new-vk7r7j5t3q-uc.a.run.app`
2. **Giải mã thuật toán xác thực động `x-user-app`**:
   - ALO Booking kiểm soát truy cập bằng chữ ký động SHA-256. Mọi request cần kèm header:
     - `x-name-app`: `alobo-user`
     - `x-version-app`: `1.0.0`
     - `x-platform`: `web`
     - `x-user-app`: `SHA-256("{UTC_TIMESTAMP}@Alobo-User-Key-2026")` với định dạng thời gian `MM/dd/yyyy, HH:mm` theo giờ chuẩn quốc tế UTC.
3. **Chiến lược quét toàn diện**:
   - Kết hợp quét bán kính GPS (`/api/v1/public/branch/nearby`) qua 13 tâm điểm quận/huyện Hà Nội (Cầu Giấy, Hoàn Kiếm, Nam Từ Liêm, Thanh Xuân, Tây Hồ, Long Biên, Hà Đông, Hoàng Mai, Bắc Từ Liêm, Đông Anh, Gia Lâm, Thanh Trì, Ba Đình).
   - Quét bổ sung theo từ khóa tìm kiếm (`/api/v1/public/branch/search?q=...`).
   - Lấy đầy đủ chi tiết bằng endpoint theo lô `/api/v1/public/branch/by-ids`.

---

## 2. Thống Kê Dữ Liệu Thu Thập Được

### 2.1. Quá trình lọc & khử nhiễu
- **Tổng số bản ghi ban đầu quét được quanh Hà Nội**: 90 bản ghi.
- **Số bản ghi bị loại bỏ (21 bản ghi)**:
  - 10 tài khoản thử nghiệm / trùng lặp (ví dụ: các chi nhánh test HUD Vân Canh `sport_sana_fit_max_hud_cn1, cn2`, `sport_aba_fit_max_hud_cn1, cn2`...).
  - 4 tài khoản cá nhân / shop thử nghiệm (`Shop của Trang`, `Hoàng Cảnh Du`, `Nguyễn Quàng Sơn`, `96 Chùa Hà only me`).
  - 3 môn thể thao ngoài yêu cầu (`SixtyNineTree Corner` - Bóng rổ type 7; `Đại Linh Tennis`, `Đại Tài Tennis` - Tennis; `Giãn cơ thể thao` - Spa/Recovery type 14).
  - 4 địa điểm có tọa độ lỗi hoặc nằm ngoài phạm vi Hà Nội (tọa độ 0, 0 hoặc tọa độ tại TP. Hồ Chí Minh như `shop_cho_trang_1`).
- **Tổng số sân hợp lệ, thực tế và chuẩn xác tại Hà Nội**: **69 địa điểm**.

### 2.2. Bảng phân bổ theo môn thể thao
| Môn thể thao | Mã môn trên ALO Booking | Số lượng thực tế tại Hà Nội | Tỷ lệ (%) | Đặc điểm trên nền tảng ALO Booking |
| :--- | :---: | :---: | :---: | :--- |
| **Pickleball** | `type = 5` | **50 địa điểm** | **72.5%** | Thế mạnh vượt trội của ALO Booking; độ phủ dày đặc khắp Cầu Giấy, Thanh Xuân, Đống Đa, Long Biên, Hà Đông... |
| **Cầu lông** | `type = 2` | **17 địa điểm** | **24.6%** | Các nhà thi đấu và cụm sân thảm PVC tiêu chuẩn (An Dương, Trường Chinh, Tôn Thất Thuyết, Trần Quốc Toản...) |
| **Bóng đá** | `type = 3` | **2 địa điểm** | **2.9%** | ALO Booking hiện tại chỉ có 2 cụm sân bóng đá đăng ký tại Hà Nội (`Sân bóng Đình Thôn 2` - Nam Từ Liêm và `Sân bóng Thành Dũng` - Bắc Từ Liêm) |
| **TỔNG CỘNG** | - | **69 địa điểm** | **100%** | **100% địa điểm thực tế tại Thủ đô Hà Nội** |

---

## 3. Kiến Trúc Dữ Liệu & Tương Thích Với EasySport

1. **Khóa chính an toàn (`id`)**:
   - Sử dụng tiền tố `alobo_` kết hợp với ID gốc của ALO Booking.
   - Xử lý giới hạn độ dài `VARCHAR(50)` nghiêm ngặt bằng thuật toán hash rút gọn đảm bảo 100% các ID có độ dài $\le 50$ ký tự, ngăn ngừa lỗi `Data too long for column 'id'`.
2. **Tọa độ địa lý chuẩn xác (GPS)**:
   - Lưu trữ `latitude` và `longitude` thực tế thu được từ ALO Booking với độ chính xác 6 chữ số thập phân, ghim chính xác từng vị trí lên Google Map của ứng dụng EasySport.
3. **Bảo tồn đường dẫn ảnh gốc từ Firebase Storage**:
   - Các URL ảnh đại diện (`avatar`) và ảnh bìa (`cover`) lưu trữ trên Firebase Storage của ALO Booking được lưu đầy đủ trong tệp CSV `database/alobo_hanoi_courts.csv` để phục vụ hiển thị ảnh thực tế.
4. **Phân cấp Tra cứu (Lookup Only) vs Demo Đặt chỗ**:
   - **66 địa điểm**: Ở chế độ tra cứu thông tin tĩnh an toàn (không có sân con trong bảng `court_fields`), hiển thị đầy đủ trên Map và Tìm kiếm nhưng chặn đặt chỗ ảo.
   - **3 địa điểm đại diện** (1 Pickleball: `alobo_sport_htl_pickleball_ham_tu_quan`, 1 Cầu lông: `alobo_sport_an_duong_badminton`, 1 Bóng đá: `alobo_sport_bongda_dinhthon_2`): Được cấp sẵn 2 sân con (`court_fields`) và tài nguyên vật lý (`court_resources`) để kiểm thử đầy đủ luồng Availability và Giữ chỗ (Hold) 5 phút qua SePay.

---

## 4. Hướng Dẫn Triển Khai Lên Railway MySQL

Tệp `database/alobo_hanoi_courts.sql` được thiết kế theo chuẩn:
- **Idempotent**: Dùng lệnh `INSERT ... ON DUPLICATE KEY UPDATE` cho phép nạp đi nạp lại nhiều lần mà không bị lỗi trùng khóa hoặc nhân bản dòng.
- **Transaction Safe**: Bao bọc trong `START TRANSACTION` và `COMMIT`, tự động rollback nếu gặp sự cố.
- **Bảo toàn dữ liệu hiện có**: Tuyệt đối không dùng các lệnh phá hủy (`DROP TABLE`, `TRUNCATE TABLE`, `REPLACE INTO`), giữ nguyên vẹn 100% các bảng người dùng, đơn đặt sân, giao dịch thanh toán hiện tại trên Railway.

### Các bước thực hiện:

#### Cách 1: Sử dụng Công Cụ Quản Lý Database (DBeaver, TablePlus, DataGrip, MySQL Workbench)
1. Kết nối vào MySQL instance trên **Railway** bằng connection string hoặc thông tin trong tab **Variables / Connect** (Host, Port, User, Password, Database).
2. Mở file `database/alobo_hanoi_courts.sql`.
3. Kiểm tra dòng 13 của file SQL: nếu tên database trên Railway của bạn là `railway` (mặc định của Railway), hãy đổi `USE easysport;` thành `USE railway;` (hoặc comment dòng này lại và chọn database đang active).
4. Nhấn **Execute SQL Script** (hoặc `Ctrl + Enter` / `F5`).

#### Cách 2: Sử dụng dòng lệnh MySQL CLI
```bash
mysql -h <RAILWAY_HOST> -P <RAILWAY_PORT> -u <RAILWAY_USER> -p<RAILWAY_PASSWORD> --default-character-set=utf8mb4 <RAILWAY_DATABASE> < database/alobo_hanoi_courts.sql
```

---

## 5. Câu Lệnh SQL Kiểm Tra Sau Khi Nạp Vào Railway

Sau khi chạy xong file SQL trên Railway, bạn có thể chạy các câu truy vấn sau để nghiệm thu:

```sql
-- 1. Thống kê số lượng sân ALO Booking vừa nạp theo từng môn thể thao
SELECT 
    `sport_type` AS mon_the_thao, 
    COUNT(*) AS so_luong_san,
    ROUND(AVG(`rating`), 1) AS danh_gia_tb,
    MIN(`price_per_hour`) AS gia_thap_nhat,
    MAX(`price_per_hour`) AS gia_cao_nhat
FROM `courts` 
WHERE `id` LIKE 'alobo_%'
GROUP BY `sport_type`;

-- 2. Kiểm tra danh sách 10 sân mẫu hiển thị tiếng Việt và tọa độ GPS
SELECT `id`, `name`, `address`, `sport_type`, `latitude`, `longitude`
FROM `courts` 
WHERE `id` LIKE 'alobo_%'
ORDER BY `id` ASC
LIMIT 10;

-- 3. Kiểm tra cấu hình 3 sân demo sẵn sàng kiểm thử đặt sân
SELECT 
    c.`id` AS court_id,
    c.`name` AS court_name,
    c.`sport_type`,
    f.`id` AS field_id,
    f.`field_label`,
    f.`field_type`
FROM `courts` c
JOIN `court_fields` f ON c.`id` = f.`court_id`
WHERE c.`id` LIKE 'alobo_%';
```

---

## 6. Danh Mục Tệp Bàn Giao

1. [alobo_hanoi_courts.sql](file:///D:/DaiHoc/PTUD_mobileapp/database/alobo_hanoi_courts.sql) — Tệp SQL nạp an toàn lên Railway MySQL (Idempotent, Transaction, utf8mb4, 69 sân).
2. [alobo_hanoi_courts.csv](file:///D:/DaiHoc/PTUD_mobileapp/database/alobo_hanoi_courts.csv) — Tệp CSV chuẩn hóa kèm URL ảnh Firebase gốc và giờ mở/đóng cửa.
3. [alobo_crawl_report.md](file:///D:/DaiHoc/PTUD_mobileapp/database/alobo_crawl_report.md) — Tài liệu báo cáo phân tích, thống kê môn và hướng dẫn triển khai.
