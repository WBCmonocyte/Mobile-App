package com.easysport.config;

import com.easysport.entity.Booking;
import com.easysport.entity.Court;
import com.easysport.entity.CourtField;
import com.easysport.entity.CourtResource;
import com.easysport.entity.User;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.CourtFieldRepository;
import com.easysport.repository.CourtRepository;
import com.easysport.repository.CourtResourceRepository;
import com.easysport.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final CourtFieldRepository courtFieldRepository;
    private final CourtResourceRepository courtResourceRepository;
    private final BookingRepository bookingRepository;

    public DataInitializer(UserRepository userRepository,
                           CourtRepository courtRepository,
                           CourtFieldRepository courtFieldRepository,
                           CourtResourceRepository courtResourceRepository,
                           BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.courtRepository = courtRepository;
        this.courtFieldRepository = courtFieldRepository;
        this.courtResourceRepository = courtResourceRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void run(String... args) {
        // 1. Seed Demo Users if empty
        if (userRepository.count() == 0) {
            userRepository.save(new User(
                    "user_1",
                    "0901234567",
                    com.easysport.util.PasswordHasher.hash("123456"),
                    "Nguyễn Văn A",
                    "nguyenvana@gmail.com",
                    "Quận Tân Bình, TP. HCM",
                    "USER"
            ));

            userRepository.save(new User(
                    "user_2",
                    "0987654321",
                    com.easysport.util.PasswordHasher.hash("123456"),
                    "Trần Văn B",
                    "tranvanb@gmail.com",
                    "Quận 3, TP. HCM",
                    "OWNER"
            ));
            System.out.println(">>> Đã khởi tạo dữ liệu mẫu cho bảng Users (Tài khoản: 0901234567 / 123456, mật khẩu đã băm PBKDF2)");
        } else {
            // Nâng cấp mật khẩu cho các tài khoản cũ nếu chưa băm
            List<User> existingUsers = userRepository.findAll();
            for (User u : existingUsers) {
                if (u.getPassword() != null && !u.getPassword().contains("$")) {
                    u.setPassword(com.easysport.util.PasswordHasher.hash(u.getPassword()));
                    userRepository.save(u);
                }
            }
        }

        // 2. Seed Courts and Fields if empty
        if (courtRepository.count() == 0) {
            // Sân 1: Thành Phát
            Court court1 = new Court(
                    "court_thanh_phat",
                    "Sân bóng Thành Phát",
                    "12 Hoàng Hoa Thám, P.13, Q. Tân Bình, TP. HCM",
                    4.8, 128, 1.2,
                    300000, 150000, 250000, 300000,
                    "court_thanh_phat",
                    10.8016, 106.6508, "Bóng đá", false
            );
            List<CourtField> fields1 = new ArrayList<>();
            fields1.add(new CourtField("field_tp_1", court1, "Sân 7", "Sân 7 người tiêu chuẩn FIFA cỏ nhân tạo mới", "7_VS_7", "court_thanh_phat:g1"));
            fields1.add(new CourtField("field_tp_2", court1, "Sân 11", "Sân 11 người kích thước chuẩn thi đấu có khán đài", "11_VS_11", "court_thanh_phat:g1,court_thanh_phat:g2"));
            court1.setFields(fields1);
            courtRepository.save(court1);

            // Sân 2: Hoàng Gia
            Court court2 = new Court(
                    "court_hoang_gia",
                    "Sân bóng Hoàng Gia",
                    "18D Phan Văn Trị, P.10, Gò Vấp, TP. HCM",
                    4.7, 96, 2.5,
                    280000, 140000, 220000, 280000,
                    "court_hoang_gia",
                    10.8322, 106.6715, "Bóng đá", false
            );
            List<CourtField> fields2 = new ArrayList<>();
            fields2.add(new CourtField("field_hg_1", court2, "Sân 5A", "Sân 5 mini cỏ mềm cao cấp", "5_VS_5", "court_hoang_gia:g1"));
            fields2.add(new CourtField("field_hg_2", court2, "Sân 5B", "Sân 5 mini có mái che", "5_VS_5", "court_hoang_gia:g2"));
            fields2.add(new CourtField("field_hg_3", court2, "Sân 7", "Sân 7 người dàn đèn chiếu sáng hiện đại", "7_VS_7", "court_hoang_gia:g1,court_hoang_gia:g2"));
            court2.setFields(fields2);
            courtRepository.save(court2);

            // Sân 3: Cầu lông Tân Bình
            Court court3 = new Court(
                    "court_cau_long_tan_binh",
                    "CLB Cầu Lông Tân Bình",
                    "18 Cộng Hòa, P.4, Q. Tân Bình, TP. HCM",
                    4.9, 210, 1.8,
                    120000, 80000, 100000, 120000,
                    "court_cau_long",
                    10.8002, 106.6558, "Cầu lông", true
            );
            List<CourtField> fields3 = new ArrayList<>();
            fields3.add(new CourtField("field_cl_1", court3, "Sân số 1", "Thảm Yonex chuyên dụng, chống trượt", "BADMINTON", "court_cau_long_tan_binh:g1"));
            fields3.add(new CourtField("field_cl_2", court3, "Sân số 2", "Thảm Yonex chuyên dụng", "BADMINTON", "court_cau_long_tan_binh:g2"));
            fields3.add(new CourtField("field_cl_3", court3, "Sân số 3", "Thảm cao su cao cấp", "BADMINTON", "court_cau_long_tan_binh:g3"));
            court3.setFields(fields3);
            courtRepository.save(court3);

            // Sân 4: Pickleball Việt Nhật
            Court court4 = new Court(
                    "court_pickleball_viet_nhat",
                    "Sân Pickleball Việt Nhật",
                    "55 Thăng Long, P.4, Q. Tân Bình, TP. HCM",
                    4.9, 85, 3.0,
                    180000, 120000, 150000, 180000,
                    "court_pickleball",
                    10.8051, 106.6582, "Pickleball", true
            );
            List<CourtField> fields4 = new ArrayList<>();
            fields4.add(new CourtField("field_pb_1", court4, "Sân P1 Ngoài trời", "Mặt sân Acrylic tiêu chuẩn Mỹ", "PICKLEBALL", "court_pickleball_viet_nhat:g1"));
            fields4.add(new CourtField("field_pb_2", court4, "Sân P2 Trong nhà", "Có máy lạnh và đèn chống lóa", "PICKLEBALL", "court_pickleball_viet_nhat:g2"));
            court4.setFields(fields4);
            courtRepository.save(court4);

            // Sân 5: Chảo Lửa
            Court court5 = new Court(
                    "court_chao_lua",
                    "Sân bóng Chảo Lửa",
                    "30 Phan Thúc Duyện, P.4, Tân Bình, TP. HCM",
                    4.9, 214, 3.1,
                    400000, 200000, 300000, 400000,
                    "court_chao_lua",
                    10.8065, 106.6617, "Bóng đá", false
            );
            List<CourtField> fields5 = new ArrayList<>();
            fields5.add(new CourtField("field_clua_1", court5, "Sân A", "Sân cỏ nhân tạo 7 người", "7_VS_7", "court_chao_lua:g1"));
            fields5.add(new CourtField("field_clua_2", court5, "Sân B", "Sân cỏ nhân tạo 5 người", "5_VS_5", "court_chao_lua:g2"));
            court5.setFields(fields5);
            courtRepository.save(court5);

            System.out.println(">>> Đã khởi tạo dữ liệu mẫu cho bảng Courts & CourtFields");
        }

        // 3. Seed Physical Court Resources if empty
        if (courtResourceRepository.count() == 0) {
            List<CourtResource> resources = Arrays.asList(
                    new CourtResource("court_thanh_phat:g1", "court_thanh_phat", "Mặt sân 1 (Thành Phát)", "Mặt cỏ 1"),
                    new CourtResource("court_thanh_phat:g2", "court_thanh_phat", "Mặt sân 2 (Thành Phát)", "Mặt cỏ 2"),
                    new CourtResource("court_hoang_gia:g1", "court_hoang_gia", "Mặt sân 1 (Hoàng Gia)", "Mặt cỏ 5A"),
                    new CourtResource("court_hoang_gia:g2", "court_hoang_gia", "Mặt sân 2 (Hoàng Gia)", "Mặt cỏ 5B"),
                    new CourtResource("court_cau_long_tan_binh:g1", "court_cau_long_tan_binh", "Thảm 1 Tân Bình", "Thảm số 1"),
                    new CourtResource("court_cau_long_tan_binh:g2", "court_cau_long_tan_binh", "Thảm 2 Tân Bình", "Thảm số 2"),
                    new CourtResource("court_cau_long_tan_binh:g3", "court_cau_long_tan_binh", "Thảm 3 Tân Bình", "Thảm số 3"),
                    new CourtResource("court_pickleball_viet_nhat:g1", "court_pickleball_viet_nhat", "Sân P1 Ngoài trời", "Acrylic 1"),
                    new CourtResource("court_pickleball_viet_nhat:g2", "court_pickleball_viet_nhat", "Sân P2 Trong nhà", "Acrylic 2"),
                    new CourtResource("court_chao_lua:g1", "court_chao_lua", "Mặt sân A (Chảo Lửa)", "Mặt cỏ 7A"),
                    new CourtResource("court_chao_lua:g2", "court_chao_lua", "Mặt sân B (Chảo Lửa)", "Mặt cỏ 5B")
            );
            courtResourceRepository.saveAll(resources);
            System.out.println(">>> Đã khởi tạo bảng CourtResources để khóa tài nguyên vật lý chống đặt trùng");
        }

        // 4. Ensure CourtField resourceKeys are populated
        List<CourtField> allFields = courtFieldRepository.findAll();
        boolean needUpdate = false;
        for (CourtField f : allFields) {
            if (f.getResourceKeys() == null || f.getResourceKeys().isBlank()) {
                if (f.getId().equals("field_tp_1")) f.setResourceKeys("court_thanh_phat:g1");
                else if (f.getId().equals("field_tp_2")) f.setResourceKeys("court_thanh_phat:g1,court_thanh_phat:g2");
                else if (f.getId().equals("field_hg_1")) f.setResourceKeys("court_hoang_gia:g1");
                else if (f.getId().equals("field_hg_2")) f.setResourceKeys("court_hoang_gia:g2");
                else if (f.getId().equals("field_hg_3")) f.setResourceKeys("court_hoang_gia:g1,court_hoang_gia:g2");
                else f.setResourceKeys(f.getCourt().getId() + ":" + f.getId());
                needUpdate = true;
            }
        }
        if (needUpdate) {
            courtFieldRepository.saveAll(allFields);
        }

        // 5. Seed demo booking if empty
        if (bookingRepository.count() == 0) {
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Booking sampleBooking = new Booking(
                    "BK" + (System.currentTimeMillis() % 1000000),
                    "user_1",
                    "court_thanh_phat",
                    "Sân bóng Thành Phát",
                    "Sân 7 người tiêu chuẩn FIFA cỏ nhân tạo mới",
                    "18:00 - 19:30",
                    "Hôm nay, " + todayStr,
                    450000,
                    135000,
                    315000,
                    "QR_TRANSFER",
                    "PAID",
                    "Nguyễn Văn A",
                    "0901234567",
                    "12 Hoàng Hoa Thám, P.13, Q. Tân Bình, TP. HCM",
                    LocalDateTime.now().plusYears(1)
            );
            bookingRepository.save(sampleBooking);
            System.out.println(">>> Đã khởi tạo đơn đặt sân mẫu (PAID)");
        }
    }
}
