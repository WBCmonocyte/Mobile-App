package com.easysport.config;

import com.easysport.entity.Booking;
import com.easysport.entity.Court;
import com.easysport.entity.CourtField;
import com.easysport.entity.CourtResource;
import com.easysport.entity.InventoryItem;
import com.easysport.entity.InventoryTransaction;
import com.easysport.entity.ShiftReport;
import com.easysport.entity.StaffShift;
import com.easysport.entity.User;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.CourtFieldRepository;
import com.easysport.repository.CourtRepository;
import com.easysport.repository.CourtResourceRepository;
import com.easysport.repository.InventoryItemRepository;
import com.easysport.repository.InventoryTransactionRepository;
import com.easysport.repository.ShiftReportRepository;
import com.easysport.repository.StaffShiftRepository;
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
    private final StaffShiftRepository staffShiftRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public DataInitializer(UserRepository userRepository,
                           CourtRepository courtRepository,
                           CourtFieldRepository courtFieldRepository,
                           CourtResourceRepository courtResourceRepository,
                           BookingRepository bookingRepository,
                           StaffShiftRepository staffShiftRepository,
                           ShiftReportRepository shiftReportRepository,
                           InventoryItemRepository inventoryItemRepository,
                           InventoryTransactionRepository inventoryTransactionRepository) {
        this.userRepository = userRepository;
        this.courtRepository = courtRepository;
        this.courtFieldRepository = courtFieldRepository;
        this.courtResourceRepository = courtResourceRepository;
        this.bookingRepository = bookingRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.shiftReportRepository = shiftReportRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
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
            court1.setCity("TP. Hồ Chí Minh");
            court1.setDistrict("Tân Bình");
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
            court2.setCity("TP. Hồ Chí Minh");
            court2.setDistrict("Gò Vấp");
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
            court3.setCity("TP. Hồ Chí Minh");
            court3.setDistrict("Tân Bình");
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
            court4.setCity("TP. Hồ Chí Minh");
            court4.setDistrict("Tân Bình");
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
            court5.setCity("TP. Hồ Chí Minh");
            court5.setDistrict("Tân Bình");
            courtRepository.save(court5);

            System.out.println(">>> Đã khởi tạo dữ liệu mẫu cho bảng Courts & CourtFields");
        } else {
            List<Court> existingCourts = courtRepository.findAll();
            for (Court c : existingCourts) {
                boolean changed = false;
                if (c.getCity() == null && c.getAddress() != null) {
                    if (c.getAddress().contains("Hà Nội")) {
                        c.setCity("Hà Nội");
                        changed = true;
                    } else if (c.getAddress().contains("TP. HCM") || c.getAddress().contains("Hồ Chí Minh")) {
                        c.setCity("TP. Hồ Chí Minh");
                        if (c.getAddress().contains("Tân Bình")) c.setDistrict("Tân Bình");
                        else if (c.getAddress().contains("Gò Vấp")) c.setDistrict("Gò Vấp");
                        changed = true;
                    }
                }
                if (changed) courtRepository.save(c);
            }
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

        // 6. Ensure STAFF & OWNER demo accounts exist
        if (!userRepository.existsByEmail("owner@easysport.vn")) {
            userRepository.save(new User(
                    "user_owner_demo",
                    "0988776655",
                    com.easysport.util.PasswordHasher.hash("123456"),
                    "Trần Chủ Sân",
                    "owner@easysport.vn",
                    "Quận 3, TP. HCM",
                    "OWNER"
            ));
        }
        if (!userRepository.existsByEmail("staff@easysport.vn")) {
            userRepository.save(new User(
                    "user_staff_1",
                    "0911223344",
                    com.easysport.util.PasswordHasher.hash("123456"),
                    "Trần Thị Nhân Viên",
                    "staff@easysport.vn",
                    "Quận Tân Bình, TP. HCM",
                    "STAFF"
            ));
        }
        if (!userRepository.existsByEmail("trucsan@easysport.vn")) {
            userRepository.save(new User(
                    "user_staff_2",
                    "0977112233",
                    com.easysport.util.PasswordHasher.hash("123456"),
                    "Nguyễn Văn Trực Sân",
                    "trucsan@easysport.vn",
                    "Quận Tân Bình, TP. HCM",
                    "STAFF"
            ));
        }
        if (!userRepository.existsByEmail("thungan@easysport.vn")) {
            userRepository.save(new User(
                    "user_staff_3",
                    "0966554433",
                    com.easysport.util.PasswordHasher.hash("123456"),
                    "Lê Minh Thu Ngân",
                    "thungan@easysport.vn",
                    "Quận Tân Bình, TP. HCM",
                    "STAFF"
            ));
        }

        // 7. Seed Staff Shifts if empty
        if (staffShiftRepository.count() == 0) {
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String tomorrowStr = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            staffShiftRepository.saveAll(Arrays.asList(
                    new StaffShift("sh_1", "user_staff_1", "Trần Thị Nhân Viên", "0911223344",
                            "court_thanh_phat", "Sân bóng Thành Phát", todayStr, "Ca Chiều (12:00 - 17:00)",
                            "Lễ tân & Thu ngân", "ACTIVE", "11:58", null,
                            "Đón khách đặt online và khách vãng lai buổi chiều."),
                    new StaffShift("sh_2", "user_staff_1", "Trần Thị Nhân Viên", "0911223344",
                            "court_thanh_phat", "Sân bóng Thành Phát", tomorrowStr, "Ca Sáng (06:00 - 12:00)",
                            "Lễ tân & Thu ngân", "UPCOMING", null, null,
                            "Ca sáng đón các đội đá sớm."),
                    new StaffShift("sh_3", "user_staff_2", "Nguyễn Văn Trực Sân", "0977112233",
                            "court_thanh_phat", "Sân bóng Thành Phát", todayStr, "Ca Sáng (06:00 - 12:00)",
                            "Trực sân & POS", "COMPLETED", "05:55", "12:02",
                            "Kiểm tra đèn, lưới và bơm bóng trước giờ mở sân."),
                    new StaffShift("sh_4", "user_staff_2", "Nguyễn Văn Trực Sân", "0977112233",
                            "court_thanh_phat", "Sân bóng Thành Phát", tomorrowStr, "Ca Tối (17:00 - 23:00)",
                            "Trực sân & POS", "UPCOMING", null, null,
                            "Trực khung giờ cao điểm buổi tối."),
                    new StaffShift("sh_5", "user_staff_3", "Lê Minh Thu Ngân", "0966554433",
                            "court_thanh_phat", "Sân bóng Thành Phát", todayStr, "Ca Tối (17:00 - 23:00)",
                            "Lễ tân & Thu ngân", "UPCOMING", null, null,
                            "Thu ngân quầy POS và đối soát cuối ngày."),
                    new StaffShift("sh_6", "user_staff_3", "Lê Minh Thu Ngân", "0966554433",
                            "court_thanh_phat", "Sân bóng Thành Phát", tomorrowStr, "Ca Chiều (12:00 - 17:00)",
                            "Lễ tân & Thu ngân", "UPCOMING", null, null,
                            "Trực quầy lễ tân buổi chiều.")
            ));
        } else {
            // Chuẩn hóa khung giờ ca cũ (06-14 / 14-22) nếu tồn tại trong DB từ bản trước
            List<StaffShift> allShifts = staffShiftRepository.findAll();
            for (StaffShift sh : allShifts) {
                if ("Ca Sáng (06:00 - 14:00)".equals(sh.getShiftSlot())) {
                    sh.setShiftSlot("Ca Sáng (06:00 - 12:00)");
                    staffShiftRepository.save(sh);
                } else if ("Ca Chiều (14:00 - 22:00)".equals(sh.getShiftSlot())) {
                    sh.setShiftSlot("Ca Chiều (12:00 - 17:00)");
                    staffShiftRepository.save(sh);
                }
            }
        }

        // 8. Seed Shift Report if empty
        if (shiftReportRepository.count() == 0) {
            String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            shiftReportRepository.save(new ShiftReport(
                    "rep_1",
                    "court_thanh_phat",
                    "Sân bóng Thành Phát",
                    "user_staff_2",
                    "Nguyễn Văn Trực Sân",
                    todayStr + " (Ca Sáng 06:00 - 12:00)",
                    450000,
                    300000,
                    3,
                    "Đã kiểm đếm đủ 450.000 đ tiền mặt bàn giao trong két. Lưới sân 2 đã kiểm tra chắc chắn, bóng thi đấu đủ 6 quả.",
                    "PENDING",
                    null
            ));
        }

        // 9. Seed Inventory Items if empty
        if (inventoryItemRepository.count() == 0) {
            List<InventoryItem> items = Arrays.asList(
                    new InventoryItem("inv_tp_revive", "court_thanh_phat", "Sân bóng Thành Phát",
                            "Revive chanh muối", "DRINK", "chai", 8500, 15000, 48, 12),
                    new InventoryItem("inv_tp_water", "court_thanh_phat", "Sân bóng Thành Phát",
                            "Nước khoáng 500ml", "DRINK", "chai", 4000, 10000, 72, 15),
                    new InventoryItem("inv_tp_bib", "court_thanh_phat", "Sân bóng Thành Phát",
                            "Thuê bộ áo bib (14c)", "EQUIPMENT", "bộ", 150000, 50000, 8, 2),
                    new InventoryItem("inv_tp_redbull", "court_thanh_phat", "Sân bóng Thành Phát",
                            "Redbull tăng lực", "DRINK", "lon", 11000, 20000, 9, 10),
                    new InventoryItem("inv_tp_ball", "court_thanh_phat", "Sân bóng Thành Phát",
                            "Bóng đá số 5 Động Lực", "EQUIPMENT", "quả", 320000, 30000, 10, 3),
                    new InventoryItem("inv_tp_socks", "court_thanh_phat", "Sân bóng Thành Phát",
                            "Tất bóng đá chống trơn", "ACCESSORY", "đôi", 25000, 45000, 20, 5),

                    new InventoryItem("inv_hg_revive", "court_hoang_gia", "Sân bóng Hoàng Gia",
                            "Revive chanh muối", "DRINK", "chai", 8500, 15000, 36, 10),
                    new InventoryItem("inv_hg_water", "court_hoang_gia", "Sân bóng Hoàng Gia",
                            "Nước khoáng 500ml", "DRINK", "chai", 4000, 10000, 60, 15),
                    new InventoryItem("inv_hg_bib", "court_hoang_gia", "Sân bóng Hoàng Gia",
                            "Thuê bộ áo bib (14c)", "EQUIPMENT", "bộ", 150000, 50000, 6, 2),
                    new InventoryItem("inv_hg_redbull", "court_hoang_gia", "Sân bóng Hoàng Gia",
                            "Redbull tăng lực", "DRINK", "lon", 11000, 20000, 24, 8)
            );
            applyInventoryMetadata(items);
            inventoryItemRepository.saveAll(items);

            inventoryTransactionRepository.saveAll(Arrays.asList(
                    new InventoryTransaction("tx_seed_1", "inv_tp_revive", "Revive chanh muối", "court_thanh_phat",
                            "IMPORT", 48, 48, 8500, "Nhập kho đầu kỳ (2 thùng)", "Trần Chủ Sân"),
                    new InventoryTransaction("tx_seed_2", "inv_tp_water", "Nước khoáng 500ml", "court_thanh_phat",
                            "IMPORT", 72, 72, 4000, "Nhập kho đầu kỳ (3 thùng)", "Trần Chủ Sân"),
                    new InventoryTransaction("tx_seed_3", "inv_tp_redbull", "Redbull tăng lực", "court_thanh_phat",
                            "IMPORT", 9, 9, 11000, "Tồn đầu kỳ (sắp hết, cần nhập thêm)", "Trần Chủ Sân")
            ));
            System.out.println(">>> Đã khởi tạo dữ liệu mẫu Kho hàng (InventoryItems & InventoryTransactions)");
        } else {
            List<InventoryItem> existingItems = inventoryItemRepository.findAll();
            boolean changed = applyInventoryMetadata(existingItems);
            if (changed) {
                inventoryItemRepository.saveAll(existingItems);
            }
        }

        if (inventoryTransactionRepository.count() <= 3) {
            inventoryTransactionRepository.saveAll(Arrays.asList(
                    new InventoryTransaction("tx_seed_sale_1", "inv_tp_revive", "Revive chanh muối", "court_thanh_phat",
                            "SALE", -12, 48, 15000, "Bán lẻ tại quầy POS (khách đặt sân)", "Trần Thị Nhân Viên"),
                    new InventoryTransaction("tx_seed_sale_2", "inv_tp_water", "Nước khoáng 500ml", "court_thanh_phat",
                            "SALE", -18, 72, 10000, "Bán lẻ tại quầy POS", "Trần Thị Nhân Viên"),
                    new InventoryTransaction("tx_seed_sale_3", "inv_tp_redbull", "Redbull tăng lực", "court_thanh_phat",
                            "SALE", -6, 9, 20000, "Gắn vào đơn sân Sân 2", "Trần Thị Nhân Viên"),
                    new InventoryTransaction("tx_seed_sale_4", "inv_hg_revive", "Revive chanh muối", "court_hoang_gia",
                            "SALE", -8, 36, 15000, "Bán lẻ tại quầy POS", "Trần Thị Nhân Viên"),
                    new InventoryTransaction("tx_seed_dmg_1", "inv_tp_water", "Nước khoáng 500ml", "court_thanh_phat",
                            "DAMAGE", -2, 70, 4000, "Bẹp vỏ chai khi vận chuyển", "Trần Chủ Sân")
            ));
        }
    }

    private boolean applyInventoryMetadata(List<InventoryItem> items) {
        boolean updated = false;
        for (InventoryItem item : items) {
            if (item.getSku() != null && !item.getSku().isBlank()) continue;
            String n = item.getName() != null ? item.getName().toLowerCase() : "";
            if (n.contains("revive")) {
                item.setSku("REV-330");
                item.setBarcode("8934588012112");
                item.setSupplierName("Đại lý Nước Minh Anh");
            } else if (n.contains("nước khoáng")) {
                item.setSku("WTR-500");
                item.setBarcode("8935049500112");
                item.setSupplierName("Đại lý Nước Minh Anh");
            } else if (n.contains("bib")) {
                item.setSku("BIB-14");
                item.setBarcode("8936001100014");
                item.setSupplierName("Động Lực Sport");
            } else if (n.contains("redbull")) {
                item.setSku("RDB-250");
                item.setBarcode("8850228000105");
                item.setSupplierName("Đại lý Nước Minh Anh");
            } else if (n.contains("bóng đá")) {
                item.setSku("BALL-05");
                item.setBarcode("8936001100055");
                item.setSupplierName("Động Lực Sport");
            } else if (n.contains("tất")) {
                item.setSku("SCK-01");
                item.setBarcode("8936001100099");
                item.setSupplierName("Động Lực Sport");
            } else {
                item.setSku("SKU-" + item.getId().substring(0, Math.min(6, item.getId().length())).toUpperCase());
                item.setBarcode("");
                item.setSupplierName("Nhà cung cấp EasySport");
            }
            updated = true;
        }
        return updated;
    }
}
