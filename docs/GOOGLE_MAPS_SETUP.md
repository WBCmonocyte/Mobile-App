# Hướng Dẫn Cấu Hình Google Maps SDK Cho EasySport

Tài liệu này hướng dẫn cách cấu hình Google Maps Platform để kích hoạt bản đồ thật và tính năng định vị sân thể thao trong dự án EasySport.

---

## 1. Tổng Quan Kiến Trúc Bản Đồ

- **Bản đồ hiển thị**: Sử dụng thư viện chính thức `com.google.maps.android:maps-compose:6.4.1` kết hợp với `com.google.android.gms:play-services-maps:19.1.0`.
- **Dịch vụ định vị**: Sử dụng `com.google.android.gms:play-services-location:21.3.0` (Fused Location Provider) để lấy tọa độ thực của thiết bị người dùng.
- **Tính toán khoảng cách**: Sử dụng công thức Haversine tính khoảng cách địa lý đường chim bay từ vị trí người dùng đến tọa độ thực của từng sân trong hệ thống.
- **Bảo mật API Key**: API Key được nạp thông qua `local.properties` (file cục bộ không commit Git) hoặc biến môi trường `MAPS_API_KEY`, sau đó được Gradle inject vào `BuildConfig` và `AndroidManifest.xml`. Không hardcode key vào mã nguồn.
- **Xử lý khi chưa có API Key**: Ứng dụng không bị crash và hiển thị thông báo rõ ràng yêu cầu bổ sung khóa, đồng thời vẫn cho phép xem danh sách sân, bộ lọc và thẻ thông tin sân.

---

## 2. Các Bước Tạo Và Cấu Hình Google Maps API Key

### Bước 2.1: Tạo Project trên Google Cloud Console
1. Truy cập [Google Cloud Console](https://console.cloud.google.com/).
2. Đăng nhập tài khoản Google và tạo Project mới (ví dụ: `EasySport-App`).

### Bước 2.2: Kích hoạt dịch vụ "Maps SDK for Android"
1. Trong thanh tìm kiếm của Google Cloud Console, gõ **Maps SDK for Android**.
2. Chọn dịch vụ và nhấn **ENABLE** (Bật).
> [!NOTE]
> Google Cloud yêu cầu tài khoản phải liên kết thông tin thanh toán (Billing Account) để kích hoạt Maps SDK. Google tặng hạn mức miễn phí \$200 mỗi tháng, đủ để phát triển và chạy thử nghiệm mà không phát sinh chi phí thực tế.

### Bước 2.3: Tạo API Key
1. Vào mục **APIs & Services** -> **Credentials** (Thông tin xác thực).
2. Nhấn **Create Credentials** -> Chọn **API Key**.
3. Một khóa API dạng `AIzaSy...` sẽ được tạo.

### Bước 2.4: Giới hạn bảo mật API Key (Bắt buộc)
Để ngăn chặn việc API key bị đánh cắp hoặc sử dụng trái phép, cấu hình các hạn chế sau:
1. Nhấp vào tên API Key vừa tạo để chỉnh sửa.
2. **Set an application restriction** -> Chọn **Android apps**.
3. Nhấn **ADD AN ITEM**:
   - **Package name**: `com.example.easysport`
   - **SHA-1 certificate fingerprint**: Lấy từ debug keystore trên máy của bạn bằng lệnh:
     ```bash
     keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
     ```
     *(Hoặc chạy lệnh Gradle: `./gradlew signingReport`)*
4. **API restrictions** -> Chọn **Restrict key** -> Tích chọn duy nhất **Maps SDK for Android**.
5. Nhấn **Save**.

---

## 3. Cấu Hình Khóa Trong Dự Án (Cho Lập Trình Viên)

### Cách 1: Thêm vào `local.properties` (Khuyến nghị)
Mở file `local.properties` ở thư mục gốc dự án (file này nằm trong `.gitignore`, không bao giờ bị commit) và thêm dòng:
```properties
MAPS_API_KEY=AIzaSyYourActualKeyHere
```

### Cách 2: Thiết lập qua Biến Môi Trường (CI/CD hoặc Terminal)
- **Windows (PowerShell):**
  ```powershell
  $env:MAPS_API_KEY="AIzaSyYourActualKeyHere"
  ```
- **Linux/macOS:**
  ```bash
  export MAPS_API_KEY="AIzaSyYourActualKeyHere"
  ```

---

## 4. Tọa Độ Các Cụm Sân Trong Hệ Thống EasySport

Các sân thể thao đã đăng ký trong EasySport được định vị theo tọa độ thực tế tại TP. Hồ Chí Minh:

| ID Sân | Tên Sân | Địa Chỉ | Tọa Độ (Lat, Lng) | Môn Thể Thao | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `thanh-phat` | Sân bóng Thành Phát | 123 Cộng Hòa, P.12, Q. Tân Bình | `10.8016, 106.6508` | Bóng đá | Đã xác minh |
| `hoang-gia` | Sân bóng Hoàng Gia | 18D Phan Văn Trị, P.10, Q. Gò Vấp | `10.8322, 106.6715` | Bóng đá | Đã xác minh |
| `chao-lua` | Sân bóng Chảo Lửa | 30 Phan Thúc Duyện, P.4, Q. Tân Bình | `10.8065, 106.6617` | Bóng đá | Đã xác minh |
| `cau-long-thanh-phat` | CLB Cầu Lông Tân Bình / Thành Phát | 18 Cộng Hòa, P.4, Q. Tân Bình | `10.8002, 106.6558` | Cầu lông | Đã xác minh |
| `pickleball-tan-binh` | Sân Pickleball Tân Bình / Việt Nhật | 45 Hoàng Hoa Thám / 55 Thăng Long, P.4, Tân Bình | `10.8051, 106.6582` | Pickleball | Đã xác minh |

> [!IMPORTANT]
> - Nếu một sân mới được thêm vào hệ thống mà **chưa có tọa độ** (`latitude = null`, `longitude = null`), hệ thống sẽ **loại trừ** sân đó khỏi phép tính khoảng cách địa lý và thông báo cho người dùng chỉ đường theo địa chỉ text, tuyệt đối không tạo marker giả mạo hoặc gán tọa độ ngẫu nhiên.
> - Bán kính tìm kiếm mặc định được định nghĩa tại `CourtDistanceConstants.DEFAULT_SEARCH_RADIUS_KM = 5.0` (km).

