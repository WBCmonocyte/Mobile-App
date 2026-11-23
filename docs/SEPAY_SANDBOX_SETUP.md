# Hướng Dẫn Tích Hợp & Kiểm Thử Cổng Thanh Toán SePay Sandbox (EasySport)

Tài liệu này hướng dẫn chi tiết cách cấu hình, vận hành và kiểm thử luồng thanh toán đặt sân thể thao trên hệ thống **EasySport** kết hợp với **Cổng thanh toán SePay Sandbox**.

---

## 1. Tổng Quan Kiến Trúc & Nghiệp Vụ

### 1.1. Luồng nghiệp vụ
1. **Phương thức thanh toán:**
   - `QR_TRANSFER` (Chuyển khoản QR): Người dùng thanh toán **100% tổng tiền sân** online qua SePay. Tiền còn lại tại sân = 0 đ.
   - `CASH_ON_SITE` (Thanh toán tại sân): Người dùng thanh toán **tiền cọc 30%** online qua SePay. Phần 70% còn lại thanh toán trực tiếp tại sân khi đến nhận sân.
2. **Nguyên tắc bảo mật giá (Server-side Calculation):**
   - Giá tiền gửi từ Android client **chỉ mang tính chất hiển thị tham khảo**.
   - Khi nhận yêu cầu `POST /api/bookings`, backend tra cứu giá thực tế của sân trong database (theo ca Sáng/Chiều/Tối hoặc giá mỗi giờ), tự động tính toán chính xác tổng tiền sân và số tiền cọc 30%.
3. **Cơ chế giữ chỗ & Hết hạn (15 phút hold period):**
   - Khi tạo booking, đơn ở trạng thái `PENDING` và được giữ chỗ trong **15 phút**.
   - Các yêu cầu đặt trùng `(courtId, fieldDetail, dateLabel, timeRange)` trong thời gian này sẽ bị từ chối với mã lỗi `409 Conflict`.
   - Nếu quá 15 phút người dùng chưa hoàn tất thanh toán, trạng thái sẽ chuyển thành `EXPIRED` và slot sân được giải phóng cho người khác đặt.
   - Nếu tiền về qua IPN sau khi đã hết hạn và khung giờ đã bị người khác đặt trước, hệ thống đánh dấu đơn là `REVIEW_REQUIRED` (cần đối soát hoàn tiền), không bao giờ ghi đè lịch của khách hàng khác.
4. **Bảo mật tuyệt đối (Zero Secret in Client):**
   - Không chứa bất kỳ thông tin `SEPAY_SECRET_KEY` hay `SEPAY_IPN_SECRET` nào trong ứng dụng Android (APK).
   - Backend sinh token checkout tạm thời (`/payments/{token}/checkout`) và phục vụ form HTML tự động submit kèm chữ ký HMAC-SHA256 Base64 sang SePay Checkout.
   - Webhook IPN (`POST /api/payments/sepay/ipn`) được xác thực bằng Header `X-Secret-Key`.

---

## 2. Thông Tin Cấu Hình SePay Sandbox

### 2.1. Endpoints chính thức của SePay Sandbox
- **Form Checkout Sandbox:** `https://pay-sandbox.sepay.vn/v1/checkout/init`
- **API tra cứu Sandbox:** `https://pgapi-sandbox.sepay.vn`
- **Tài liệu tham khảo:**
  - Sandbox: [https://developer.sepay.vn/vi/cong-thanh-toan/sandbox](https://developer.sepay.vn/vi/cong-thanh-toan/sandbox)
  - Form thanh toán: [https://developer.sepay.vn/vi/cong-thanh-toan/API/don-hang/form-thanh-toan](https://developer.sepay.vn/vi/cong-thanh-toan/API/don-hang/form-thanh-toan)
  - IPN Webhook: [https://developer.sepay.vn/vi/cong-thanh-toan/IPN](https://developer.sepay.vn/vi/cong-thanh-toan/IPN)

### 2.2. Các biến môi trường Backend

| Tên biến môi trường | Mô tả | Giá trị mẫu Sandbox |
|---|---|---|
| `SEPAY_MERCHANT_ID` | Mã định danh Merchant trên SePay Sandbox | `SP-SANDBOX-xxxx` |
| `SEPAY_SECRET_KEY` | Khóa bí mật dùng để ký HMAC-SHA256 form thanh toán | `sec_test_xxxxxxxxxxxxxxxx` |
| `SEPAY_IPN_SECRET` | Khóa bí mật đối soát Webhook IPN (`X-Secret-Key`) | `ipn_test_xxxxxxxxxxxxxxxx` |
| `APP_PUBLIC_URL` | URL công khai của Backend để SePay redirect người dùng về | `https://xxxx.ngrok-free.app` hoặc `http://10.0.2.2:8080` |

> [!IMPORTANT]
> Dự án đã cung cấp sẵn file cấu hình mẫu tại `backend/application-sepay.properties.example`.
> Tuyệt đối không commit khóa bí mật lên Git.

---

## 3. Hướng Dẫn Cấu Hình Webhook Tunneling (ngrok)

Do SePay Sandbox gửi thông báo kết quả thanh toán qua Webhook IPN từ Internet vào Backend của bạn, bạn cần một URL HTTPS công khai (public URL) trỏ về port `8080` của máy tính.

### Bước 1: Khởi động ngrok
Nếu bạn đã cài ngrok:
```bash
ngrok http 8080
```
Bạn sẽ nhận được Forwarding URL dạng: `https://a1b2-xxx-xxx.ngrok-free.app`

### Bước 2: Thiết lập biến môi trường
Trước khi chạy Backend, thiết lập các biến môi trường:

**Trên PowerShell (Windows):**
```powershell
$env:SEPAY_MERCHANT_ID="SP-SANDBOX-YOUR-MERCHANT-ID"
$env:SEPAY_SECRET_KEY="YOUR-SECRET-KEY"
$env:SEPAY_IPN_SECRET="YOUR-IPN-SECRET"
$env:APP_PUBLIC_URL="https://a1b2-xxx-xxx.ngrok-free.app"
```

**Trên CMD (Windows):**
```cmd
set SEPAY_MERCHANT_ID=SP-SANDBOX-YOUR-MERCHANT-ID
set SEPAY_SECRET_KEY=YOUR-SECRET-KEY
set SEPAY_IPN_SECRET=YOUR-IPN-SECRET
set APP_PUBLIC_URL=https://a1b2-xxx-xxx.ngrok-free.app
```

### Bước 3: Đăng ký Webhook URL trên SePay Dashboard
Vào trang quản trị SePay Sandbox -> Cài đặt Webhook/IPN -> Điền URL:
```
https://a1b2-xxx-xxx.ngrok-free.app/api/payments/sepay/ipn
```

---

## 4. Khởi Chạy Backend & Kiểm Tra Kết Nối

### 4.1. Chạy Backend Spring Boot
Từ thư mục gốc dự án:
```powershell
cd backend
mvn spring-boot:run
```
Backend sẽ khởi chạy tại cổng `8080`.

### 4.2. Chạy bộ kiểm thử tự động (Unit Tests)
Backend đã tích hợp sẵn 9 kịch bản test chuyên sâu cho dịch vụ SePay:
```powershell
mvn test -f backend/pom.xml
```
Kết quả mong đợi: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).

---

## 5. Hướng Dẫn Cấu Hình Ứng Dụng Android

1. **Android Emulator:**
   - Android Emulator kết nối trực tiếp vào máy chủ backend tại máy host thông qua địa chỉ IP: `http://10.0.2.2:8080`.
   - `PaymentApiClient.kt` đã được cài đặt mặc định trỏ về `http://10.0.2.2:8080`.
2. **Thiết bị thật (Physical Device qua Wi-Fi):**
   - Nếu test app trên điện thoại thật cắm cáp hoặc chung mạng Wi-Fi, hãy thay đổi `PaymentApiClient.baseUrl` sang IP máy tính của bạn:
     ```kotlin
     PaymentApiClient.baseUrl = "http://192.168.1.50:8080"
     ```
3. **Trải nghiệm người dùng trên Android:**
   - Tại màn hình **Chi tiết sân**, người dùng chọn sân, ca giờ và hình thức thanh toán (`QR_TRANSFER` hoặc `CASH_ON_SITE`).
   - Nhấn **Thanh toán**: Màn hình **PaymentScreen** hiển thị bảng kê chi tiết số tiền phải thanh toán online qua SePay và số tiền còn lại tại sân.
   - Nhấn **Thanh toán qua SePay**: Ứng dụng tự động khởi tạo đơn trên backend và mở cổng SePay Sandbox an toàn bằng **Android Custom Tabs** (`androidx.browser:browser`).
   - Sau khi hoàn tất thanh toán trên SePay Sandbox, người dùng quay lại ứng dụng -> Ứng dụng tự động lắng nghe sự kiện `ON_RESUME`, kích hoạt polling kiểm tra trạng thái từ backend và tự động điều hướng sang màn hình **Chi tiết đặt sân**.

---

## 6. Kịch Bản Kiểm Thử Chi Tiết (Test Scenarios)

### Kịch bản 1: Thanh toán 100% (`QR_TRANSFER`)
1. Trên ứng dụng, chọn Sân 7, khung giờ `18:00 - 19:30`.
2. Chọn phương thức: **Chuyển khoản QR**.
3. Tại màn hình thanh toán, kiểm tra:
   - Tổng tiền: 450.000 đ
   - Cần thanh toán SePay: 450.000 đ
   - Còn lại tại sân: 0 đ
4. Nhấn **Thanh toán qua SePay**. Cổng SePay Sandbox mở ra.
5. Bấm nút giả lập thanh toán thành công trên trang SePay.
6. Quay lại app EasySport: Trạng thái hiển thị **Đã thanh toán đủ (100%)** với dấu tích xanh, tự động chuyển về trang chi tiết đơn đặt sân.

---

### Kịch bản 2: Đặt cọc 30% (`CASH_ON_SITE`)
1. Chọn khung giờ chiều `16:00 - 17:30` (tổng tiền 300.000 đ).
2. Chọn phương thức: **Thanh toán tại sân (Cọc 30%)**.
3. Tại màn hình thanh toán, kiểm tra:
   - Tổng tiền: 300.000 đ
   - Tiền cọc thanh toán SePay (30%): 90.000 đ
   - Còn lại thanh toán tại sân: 210.000 đ
4. Nhấn **Thanh toán qua SePay** -> Hoàn tất trên Sandbox.
5. Quay lại app: Trạng thái hiển thị **Đã đặt cọc 30% thành công**, badge hiển thị rõ ràng trên màn hình chi tiết đặt sân.

---

### Kịch bản 3: Giả lập IPN Webhook trực tiếp bằng PowerShell/cURL
Bạn có thể giả lập Webhook của SePay gửi vào backend bất kỳ lúc nào:

**PowerShell Script:**
```powershell
$headers = @{
    "Content-Type" = "application/json"
    "X-Secret-Key" = "YOUR-IPN-SECRET"
}

$body = @{
    notification_type = "ORDER_PAID"
    order = @{
        order_invoice_number = "ES-YOUR-INVOICE-NO"
        order_amount = 135000
        order_currency = "VND"
        order_status = "CAPTURED"
    }
    transaction = @{
        id = "TXN_SANDBOX_99999"
        transaction_status = "APPROVED"
        payment_method = "VIETQR"
    }
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/payments/sepay/ipn" -Method Post -Headers $headers -Body $body
```
Kết quả phản hồi từ backend: `{"success": true}` với HTTP status `200 OK`.

---

### Kịch bản 4: Kiểm tra tính Idempotent (Chống trùng lặp Webhook)
- Gửi lại request IPN ở Kịch bản 3 lần thứ hai với cùng `transaction.id`.
- Backend nhận diện giao dịch đã được xử lý trước đó, không lặp lại ghi nhận hay tạo giao dịch thừa, vẫn trả về `{"success": true}` để báo SePay dừng gửi lại.

---

### Kịch bản 5: Giữ chỗ 15 phút & Xử lý hết hạn (`EXPIRED`)
- Khi tạo booking, backend đặt `holdExpiresAt = now + 15 minutes`.
- Trong thời gian 15 phút, người khác đặt cùng sân/ngày/giờ sẽ nhận mã lỗi `409 Conflict`.
- Nếu quá hạn 15 phút mà chưa nhận được thanh toán:
  - Khi tra cứu `/api/bookings/{id}/payment-status`, backend trả về `expired: true` và `bookingStatus: EXPIRED`.
  - Trên app EasySport, giao diện hiển thị cảnh báo đỏ **"Lịch giữ chỗ đã hết hạn (15 phút)"** và cung cấp nút **"Khung giờ hết hạn - Đặt lại"**.

---

## 7. Xử Lý Các Sự Cố Thường Gặp (Troubleshooting)

1. **Lỗi `403 Forbidden` khi SePay gửi Webhook:**
   - Nguyên nhân: Header `X-Secret-Key` trong Webhook không khớp với biến `SEPAY_IPN_SECRET` của backend.
   - Khắc phục: Kiểm tra lại cấu hình Secret Key trong SePay Dashboard và file môi trường backend.
2. **Lỗi `Signature verification failed` trên trang Checkout SePay:**
   - Nguyên nhân: Chuỗi ký HMAC-SHA256 sai thứ tự trường hoặc sai `SEPAY_SECRET_KEY`.
   - Khắc phục: Backend EasySport đã cài đặt chính xác 11 trường theo tài liệu SePay:
     `order_amount`, `merchant`, `currency`, `operation`, `order_description`, `order_invoice_number`, `customer_id`, `payment_method`, `success_url`, `error_url`, `cancel_url`.
3. **Android Emulator báo lỗi `Không thể kết nối đến máy chủ backend`:**
   - Nguyên nhân: Backend chưa bật hoặc firewall chặn cổng 8080.
   - Khắc phục: Đảm bảo Spring Boot đang chạy tại cổng 8080. Kiểm tra truy cập `http://localhost:8080/api/courts` từ trình duyệt máy tính.

