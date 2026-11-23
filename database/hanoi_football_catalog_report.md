# BÁO CÁO THU THẬP & TẠO DANH MỤC SÂN BÓNG ĐÁ HÀ NỘI (EASYSPORT)

> **Dự án:** EasySport - Nền tảng đặt sân và tra cứu thể thao đa nền tảng  
> **Phạm vi dữ liệu:** Địa điểm sân bóng đá thực tế đang hoạt động tại Hà Nội (Vành đai 1, 2, 3 và phụ cận)  
> **Ngày thực hiện:** 20/11/2026  
> **Tài liệu bàn giao kèm theo:**
> 1. `database/hanoi_football_catalog_import.sql` (File nạp SQL 70 địa điểm bóng đá vật lý)
> 2. `database/hanoi_football_catalog_sources.csv` (File đối soát 75 địa điểm: 70 hợp lệ nạp SQL, 5 danh sách chờ/bảo trì)

---

## 1. Tổng quan & Kết quả thu thập

- **Mục tiêu đề ra:** Thu thập 50–80 địa điểm có sân bóng đá thực tế tại Hà Nội, tập trung trong và ven Vành đai 1, 2, 3; đếm theo địa điểm vật lý (mỗi cụm sân nhiều sân con 5, 7, 11 chỉ tính 1 địa điểm); xuất file SQL độc lập không chứa lệnh `USE` để người dùng chủ động nạp lên Railway MySQL.
- **Kết quả thực tế:**
  - **70 địa điểm vật lý đạt chuẩn** được chọn lọc, chuẩn hóa và đóng gói vào file SQL `database/hanoi_football_catalog_import.sql`.
  - **75 địa điểm được theo dõi** trong file CSV `database/hanoi_football_catalog_sources.csv` (gồm 70 địa điểm `IN_SQL` và 5 địa điểm đưa vào danh sách chờ `PENDING` do đang trong diện bảo trì mặt cỏ hoặc giải tỏa).
  - Tất cả các địa điểm đều có tọa độ GPS (WGS84) xác thực, địa chỉ chi tiết theo phường/quận, khoảng giá thực tế tham khảo và liên kết nguồn gốc rõ ràng.

---

## 2. Quy ước phân loại Vành đai & Phân bố địa lý

### 2.1. Quy ước xác định Vành đai
Hệ thống giao thông và không gian đô thị Hà Nội được phân định theo 3 tuyến vành đai chính:
1. **Vành đai 1 (VĐ1 - Lõi nội đô lịch sử):**
   - *Tuyến bao:* Trục Đê La Thành - Giảng Võ - Hoàng Cầu - Xã Đàn - Đại Cồ Việt - Trần Khát Chân - Bờ sông Hồng (Phúc Xá/Bạch Đằng).
   - *Đặc điểm:* Quỹ đất hạn chế, tập trung các sân vận động trung tâm, sân của các trường đại học lớn lâu đời (Bách Khoa, Thủy Lợi, ĐH Y, Công Đoàn) và một số sân khu dân cư trung tâm (Hoàng Cầu, Đội Cấn, Cát Linh).
2. **Vành đai 2 (VĐ2 - Nội đô mở rộng & VĐ2.5):**
   - *Tuyến bao:* Cầu Nhật Tân - Võ Chí Công - Bưởi - Cầu Giấy - Đường Láng - Trường Chinh - Đại La - Minh Khai - Cầu Vĩnh Tuy; kết hợp hành lang Vành đai 2.5 (Kim Đồng - Định Công - Đầm Hồng - Hoàng Văn Thái).
   - *Đặc điểm:* Mật độ sân bóng phong trào cao, gắn liền với các khu liên hợp quân đội (Phòng Không Không Quân, Thượng Đình) và trường đại học (Giao Thông Vận Tải, Sư Phạm).
3. **Vành đai 3 (VĐ3 - Trục vành đai đô thị phát triển):**
   - *Tuyến bao:* Cầu Thăng Long - Phạm Văn Đồng - Phạm Hùng - Khuất Duy Tiến - Nguyễn Xiển - Nghiêm Xuân Yêm - Hoàng Liệt - Pháp Vân.
   - *Đặc điểm:* Tập trung các tổ hợp sân thể thao cỏ nhân tạo quy mô lớn nhất thủ đô (Thành Lâm Green Park, Nam Trung Yên, Cường Quốc, Bộ Công An, Bán đảo Linh Đàm, Gamuda).
4. **Gần Vành đai 3 (Phụ cận VĐ3 trong bán kính 1 - 4 km):**
   - *Khu vực bao gồm:* Các khu đô thị mở rộng liền kề phía ngoài vành đai 3 như Hà Đông (Văn Quán, Mỗ Lao, Vạn Phúc, Hà Cầu), Nam Từ Liêm (Đại Mỗ, Tây Mỗ, Phú Đô, Mễ Trì), Bắc Từ Liêm (Cổ Nhuế, Minh Khai, ĐH Công Nghiệp), và dải Long Biên (Bồ Đề, Ngọc Thụy, Thạch Bàn).

### 2.2. Quy ước xử lý chồng lấn Đại Mỗ và Nam Từ Liêm
- *Vấn đề:* Đại Mỗ về mặt hành chính là một phường trực thuộc Quận Nam Từ Liêm. Nếu tách riêng nhóm "Đại Mỗ" và "Nam Từ Liêm" sẽ dẫn đến nguy cơ đếm trùng hoặc chồng chéo khu vực.
- *Quy ước thống nhất:*
  - Cột `district` trong SQL và CSV được gán thống nhất là **`Nam Từ Liêm`**.
  - Đối với các sân thuộc phường Đại Mỗ (ví dụ: Sân bóng FLC Đại Mỗ, Sân bóng Cầu Đôi Đại Mỗ, Sân bóng Hữu Hưng Đại Mỗ), cột `notes` được ghi rõ: `Khu vực Phường Đại Mỗ (thuộc Nam Từ Liêm)`.
  - Đảm bảo **mỗi địa điểm vật lý chỉ tính duy nhất vào một nhóm khu vực khi thống kê**.

### 2.3. Bảng thống kê phân bố 70 sân trong file SQL

| Vành đai | Số lượng sân | Tỷ lệ (%) | Các quận / huyện tiêu biểu |
| :--- | :---: | :---: | :--- |
| **Vành đai 1** | 15 | 21.4% | Đống Đa (8), Ba Đình (4), Hai Bà Trưng (3) |
| **Vành đai 2** | 18 | 25.7% | Thanh Xuân (6), Đống Đa (2), Cầu Giấy (4), Hoàng Mai (3), Tây Hồ (3) |
| **Vành đai 3** | 22 | 31.4% | Cầu Giấy (4), Nam Từ Liêm (6), Hoàng Mai (7), Thanh Xuân (3), Thanh Trì (1), Hai Bà Trưng (1) |
| **Gần Vành đai 3** | 15 | 21.4% | Nam Từ Liêm / Đại Mỗ (5), Hà Đông (3), Bắc Từ Liêm (4), Long Biên (2), Hai Bà Trưng (1) |
| **Tổng cộng** | **70** | **100%** | **11 quận / huyện trên địa bàn Hà Nội** |

---

## 3. Nguồn dữ liệu, Chuẩn hóa & Cơ chế tra cứu (Lookup-Only)

### 3.1. Nguồn thu thập và xác minh
- Dữ liệu được trích xuất và đối soát chéo từ các danh bạ thể thao công khai uy tín của Hà Nội: `vecgroup.vn` (VEC Group - đơn vị thi công cỏ nhân tạo lớn nhất VN), `sport9.vn`, `thegioithethao.vn`, `dongduongsport.com`, `sanbong.vn`, kết hợp trang chính thức của các Trung tâm TDTT Đại học (Bách Khoa, Thủy Lợi, ĐH Y, Sư Phạm, ĐH Giao Thông Vận Tải, ĐH Công Nghiệp).
- **Ngày kiểm tra / xác thực:** 20/11/2026.
- **Quy tắc tính giá:** Giá thu thập là giá theo giờ (VNĐ/giờ) của sân 7 người phổ biến. Biểu phí theo khung giờ được nội suy tương ứng:
  - Khung sáng (Morning: 06:00 - 16:00): ~70% giá tiêu chuẩn.
  - Khung chiều (Afternoon: 16:00 - 17:30): ~90% giá tiêu chuẩn.
  - Khung tối vàng (Evening: 17:30 - 22:30): ~120% giá tiêu chuẩn.
- **Tài nguyên hình ảnh:** Gán xoay vòng giữa 3 ảnh drawable có sẵn trong mã nguồn Android (`court_thanh_phat`, `court_chao_lua`, `court_hoang_gia`), đảm bảo giao diện app hiển thị phong phú, không bị ảnh trắng hay lỗi FileNotFoundException.

### 3.2. Cơ chế chỉ tra cứu (Lookup-Only Catalog)
Yêu cầu cốt lõi của bài toán là chỉ nạp thông tin để người dùng **tra cứu, tìm đường, xem giá và liên hệ**, tuyệt đối không tạo sân con hoặc lịch đặt ảo dẫn đến rủi ro thanh toán giả.

#### Bằng chứng từ Backend Java Spring Boot:
1. **Kiểm tra sân con khi giữ chỗ (`CourtHoldTransactionService.java`):**
   ```java
   public CourtField resolveField(Court court, String fieldDetail) {
       if (fieldDetail == null || fieldDetail.isBlank()) {
           return court.getFields().stream().filter(f -> Boolean.TRUE.equals(f.getActive())).findFirst()
                   .orElseThrow(() -> new IllegalArgumentException("Sân chưa có sân con đang hoạt động."));
       }
       String query = fieldDetail.trim();
       return court.getFields().stream()
               .filter(f -> Boolean.TRUE.equals(f.getActive()))
               .filter(f -> f.getFieldDetail().equalsIgnoreCase(query) || f.getFieldLabel().equalsIgnoreCase(query))
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sân con hợp lệ: " + fieldDetail));
   }
   ```
   - Khi bảng `court_fields` không có bản ghi nào liên kết với `court_id` (số sân con = 0), `court.getFields()` trả về danh sách rỗng.
   - Mọi nỗ lực gọi API kiểm tra khung giờ (`/api/courts/{id}/availability`) hoặc tạo lượt giữ chỗ thanh toán (`/api/courts/hold`) đều bị chặn ngay lập tức với lỗi `IllegalArgumentException: "Sân chưa có sân con đang hoạt động."`, chuyển thành mã lỗi **HTTP 400 Bad Request**.
   - Không có bản ghi nào bị ghi vào các bảng `court_holds`, `court_hold_slots`, `bookings` hay `payments`.

2. **Hiển thị danh sách sân (`CourtController.java` & `PaymentApiClient.kt`):**
   - API `GET /api/courts` trả về đầy đủ thông tin của 70 sân với `sportType = "Bóng đá"` và `fields: []`.
   - Ứng dụng Android đọc mảng `fields` rỗng một cách an toàn (`fields = emptyList()`), gán tọa độ kinh/vĩ độ để cắm cờ ghim trên Google Map (`MapScreen.kt`) và hiển thị trên bộ lọc môn "Bóng đá" ở màn hình chính (`HomeScreen.kt`).

---

## 4. Kết quả kiểm thử thực tế trên MySQL Local

Đã tiến hành nạp thử nghiệm trực tiếp file `database/hanoi_football_catalog_import.sql` vào cơ sở dữ liệu MySQL 8.x cục bộ (`easysport`):

1. **Kiểm thử nạp lần 1 (Fresh Import):**
   - Lệnh thực thi: `mysql -u root -p123456 easysport < database/hanoi_football_catalog_import.sql`
   - Kết quả: Thành công 100%, thời gian thực thi < 0.2s.
   - Số lượng bản ghi `hanoi_fb_%` được tạo: **Đúng 70 sân**.
   - Số lượng bản ghi sân con trong `court_fields`: **Đúng 0 sân** (bảo đảm an toàn chỉ tra cứu).

2. **Kiểm thử tính Idempotent (Nạp lần 2):**
   - Chạy lại nguyên văn file SQL lần 2 trên cùng schema.
   - Kết quả: Không phát sinh bất kỳ lỗi `Duplicate entry` hay `Foreign key violation`.
   - Số lượng bản ghi sau khi nạp lần 2: **Vẫn đúng 70 sân**, các trường thông tin được giữ nguyên vẹn mà không làm ảnh hưởng đến các sân demo ban đầu (`court_1`, `court_2`, `court_3`).

3. **Kiểm thử Bảng mã & Hiển thị Tiếng Việt (`utf8mb4`):**
   - Truy vấn thử nghiệm mẫu:
     ```sql
     SELECT id, name, address, latitude, longitude, price_per_hour, sport_type 
     FROM courts WHERE id LIKE 'hanoi_fb_%' LIMIT 5;
     ```
   - Kết quả: Toàn bộ dấu tiếng Việt (Đội Cấn, Bách Khoa, Hoàng Hoa Thám, Đại Học FPT...) hiển thị chính xác 100%, không bị lỗi font (mojibake) hay dấu hỏi chấm `???`.

4. **Kiểm tra độ dài khóa chính:**
   - Trường `id` tối đa chỉ 42 ký tự (giới hạn cột là `VARCHAR(50)`), tuân thủ chặt chẽ tiền tố `hanoi_fb_`.

---

## 5. Hướng dẫn nạp file lên Railway & Câu lệnh hậu kiểm

### 5.1. Các bước thực hiện trên Railway
1. **Mở giao diện MySQL:** Đăng nhập vào Railway Dashboard hoặc mở kết nối Railway qua MySQL Workbench / DBeaver / Navicat / CLI.
2. **Chọn đúng Schema:** Chọn database của EasySport trên Railway (ví dụ click đúp vào database `railway` hoặc `easysport`).
3. **Thực thi file SQL:**
   - Mở file `database/hanoi_football_catalog_import.sql` và bấm **Execute / Run**.
   - *Lưu ý:* File được cố tình thiết kế **không chứa lệnh `USE`**, do đó script sẽ nạp trực tiếp vào schema hiện hành của Railway mà bạn đã chọn mà không bao giờ báo lỗi `Unknown database 'easysport'`.

### 5.2. Các câu lệnh SQL kiểm tra sau khi nạp trên Railway

```sql
-- 1. Đếm tổng số sân bóng đá Hà Nội vừa nạp (kết quả mong đợi: 70)
SELECT COUNT(*) AS total_hanoi_fb_courts 
FROM courts 
WHERE id LIKE 'hanoi_fb_%';

-- 2. Kiểm tra tính toàn vẹn của cơ chế chỉ tra cứu (kết quả mong đợi: 0)
SELECT COUNT(*) AS should_be_zero 
FROM court_fields 
WHERE court_id LIKE 'hanoi_fb_%';

-- 3. Xem phân bố sân theo các quận nội/ngoại thành
SELECT 
    CASE 
        WHEN address LIKE '%Đống Đa%' THEN 'Đống Đa'
        WHEN address LIKE '%Cầu Giấy%' THEN 'Cầu Giấy'
        WHEN address LIKE '%Thanh Xuân%' THEN 'Thanh Xuân'
        WHEN address LIKE '%Hoàng Mai%' THEN 'Hoàng Mai'
        WHEN address LIKE '%Nam Từ Liêm%' THEN 'Nam Từ Liêm'
        WHEN address LIKE '%Ba Đình%' THEN 'Ba Đình'
        WHEN address LIKE '%Hai Bà Trưng%' THEN 'Hai Bà Trưng'
        WHEN address LIKE '%Tây Hồ%' THEN 'Tây Hồ'
        WHEN address LIKE '%Bắc Từ Liêm%' THEN 'Bắc Từ Liêm'
        WHEN address LIKE '%Hà Đông%' THEN 'Hà Đông'
        WHEN address LIKE '%Thanh Trì%' THEN 'Thanh Trì'
        WHEN address LIKE '%Long Biên%' THEN 'Long Biên'
        ELSE 'Quận khác'
    END AS quan_huyen,
    COUNT(*) AS so_luong_san
FROM courts
WHERE id LIKE 'hanoi_fb_%'
GROUP BY quan_huyen
ORDER BY so_luong_san DESC;

-- 4. Lấy ngẫu nhiên 5 sân bóng để kiểm tra tọa độ hiển thị bản đồ
SELECT id, name, address, latitude, longitude, price_per_hour, sport_type
FROM courts 
WHERE id LIKE 'hanoi_fb_%'
ORDER BY RAND()
LIMIT 5;
```

---
*Báo cáo được hoàn thành tự động với đầy đủ dữ liệu đối soát và kiểm chứng kỹ thuật.*
