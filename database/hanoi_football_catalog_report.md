# Danh mục sân bóng Hà Nội cho EasySport

Nguồn đầu vào là hanoi_football_catalog_sources.csv của dự án. Có 75 dòng, gồm 70 địa điểm IN_SQL và 5 địa điểm PENDING. Hai file nhập dùng cùng ID với CSV. Địa chỉ, tọa độ, số sân và giá là thông tin trong bộ nguồn, cần đối soát với chủ sân trước khi nhận đặt thật.

## Nhập bằng MySQL Workbench

Chọn schema MySQL của Railway làm mặc định và kiểm tra bằng `SELECT DATABASE();`. Mở file bằng File → Open SQL Script và thực thi toàn bộ file. Kiểm tra Action Output sau mỗi lần chạy.

1. hanoi_football_catalog_import.sql thêm hoặc cập nhật 70 địa điểm. Điểm và lượt đánh giá được tạo cho demo. Giá cơ bản lấy từ cột price_per_hour. Ba giá theo buổi lần lượt dùng cận dưới, giá cơ bản, cận trên từ CSV để thử giao diện và tính tiền. Ảnh và khoảng cách của địa điểm mới là NULL. Android có thể hiện ảnh hoặc khoảng cách mặc định ở các màn hình dùng giá trị dự phòng.
2. hanoi_football_booking_demo.sql là phần tùy chọn cho môi trường thử nghiệm SePay. File tạo 274 mặt sân tương ứng tổng sub_court_count và 281 lựa chọn đặt sân. Mỗi mặt sân có một lựa chọn sân 7. Tại 7 cụm có ghi sân 7 và 11, lựa chọn sân 11 dùng chung toàn bộ khóa của cụm để tránh đặt chồng khi demo. Số thứ tự, cấu hình ghép và tình trạng mở đặt đều là giả định phục vụ demo.

Khi nhập lại, danh mục cập nhật tên, địa chỉ, tọa độ, môn, trong/ngoài trời và giá. Ảnh, điểm, lượt đánh giá và khoảng cách đã có được giữ nguyên. Gói demo giữ trạng thái kích hoạt hiện tại của sân con nhưng cập nhật mô tả và cách ghép. Khi đã tự cấu hình mặt sân, hãy rà lại trước khi nhập lại gói demo. Cả hai file dùng khóa chính để nhận diện bản ghi. Địa điểm cùng tên từ bộ Alobo có ID khác vẫn có thể xuất hiện hai lần.

Giờ mở cửa, khu vực, đường dẫn nguồn và ghi chú giá theo từng sân có trong CSV. Schema courts hiện tại chưa có các cột này. Giờ hiển thị và ca được phép đặt trong app cần được kiểm tra riêng khi áp dụng cho sân thật.
