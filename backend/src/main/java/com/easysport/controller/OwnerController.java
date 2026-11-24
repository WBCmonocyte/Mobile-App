package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.entity.Booking;
import com.easysport.entity.Court;
import com.easysport.entity.CourtField;
import com.easysport.entity.PosSale;
import com.easysport.entity.ShiftReport;
import com.easysport.entity.StaffShift;
import com.easysport.entity.User;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.CourtRepository;
import com.easysport.repository.PosSaleRepository;
import com.easysport.repository.ShiftReportRepository;
import com.easysport.repository.StaffShiftRepository;
import com.easysport.repository.UserRepository;
import com.easysport.service.AuthService;
import com.easysport.service.ShiftPlanningService;
import com.easysport.util.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final PosSaleRepository posSaleRepository;
    private final UserRepository userRepository;
    private final ShiftPlanningService shiftPlanningService;
    private final Clock clock;

    public OwnerController(AuthService authService,
                           BookingRepository bookingRepository,
                           CourtRepository courtRepository,
                           StaffShiftRepository staffShiftRepository,
                           ShiftReportRepository shiftReportRepository,
                           PosSaleRepository posSaleRepository,
                           UserRepository userRepository,
                           ShiftPlanningService shiftPlanningService,
                           Clock clock) {
        this.authService = authService;
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.shiftReportRepository = shiftReportRepository;
        this.posSaleRepository = posSaleRepository;
        this.userRepository = userRepository;
        this.shiftPlanningService = shiftPlanningService;
        this.clock = clock;
    }

    private User requireOwner(String authHeader) {
        User user = authService.authenticate(authHeader);
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn.");
        }
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
        if (!role.equals("OWNER")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Tài khoản không có quyền Chủ sân.");
        }
        return user;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "period", required = false, defaultValue = "ALL") String period
    ) {
        requireOwner(authHeader);
        String normalizedPeriod = period != null ? period.trim().toUpperCase() : "ALL";
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        YearMonth thisMonth = YearMonth.from(today);

        List<Booking> allBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        List<Booking> bookings = allBookings.stream()
                .filter(b -> matchesPeriod(resolveBookingDate(b), normalizedPeriod, today, weekStart, weekEnd, thisMonth))
                .toList();

        List<Court> courts = courtRepository.findAll();
        List<ShiftReport> reports = shiftReportRepository.findAllByOrderByCreatedAtDesc();
        List<PosSale> allPosSales = posSaleRepository.findAllByOrderByCreatedAtDesc();
        List<PosSale> posSales = allPosSales.stream()
                .filter(s -> matchesPeriod(ShiftPlanningService.parseDateOrNull(s.getSaleDate()), normalizedPeriod, today, weekStart, weekEnd, thisMonth))
                .toList();

        Set<String> splitSettledIds = new HashSet<>();
        for (PosSale sale : allPosSales) {
            if (sale.getSettlementBookingId() != null) {
                splitSettledIds.add(sale.getSettlementBookingId());
            }
        }

        long totalRevenue = 0;
        long cashRevenue = 0;
        long qrRevenue = 0;
        int activeCount = 0;

        Map<String, Long> revenueByCourt = new HashMap<>();
        Map<String, Integer> bookingCountByCourt = new HashMap<>();
        Map<String, Integer> activeCountByCourt = new HashMap<>();

        for (Booking b : bookings) {
            String st = b.getStatus() != null ? b.getStatus().toUpperCase() : "";
            String cId = b.getCourtId() != null ? b.getCourtId() : "";
            bookingCountByCourt.merge(cId, 1, Integer::sum);
            if ("CHECKED_IN".equals(st)) {
                activeCount++;
                activeCountByCourt.merge(cId, 1, Integer::sum);
            }
            if (st.equals("PAID") || st.equals("DEPOSIT_PAID") || st.equals("CHECKED_IN") || st.equals("COMPLETED")) {
                long amt;
                if (splitSettledIds.contains(b.getId())) {
                    amt = b.getDepositAmount() != null ? b.getDepositAmount() : 0;
                } else if (st.equals("COMPLETED") || st.equals("PAID")) {
                    amt = (long) (b.getTotalPrice() != null ? b.getTotalPrice() : 0) + b.getServiceAmount();
                } else {
                    amt = b.getDepositAmount() != null ? b.getDepositAmount() : 0;
                }
                totalRevenue += amt;
                revenueByCourt.merge(cId, amt, Long::sum);
                if (StaffController.isQrMethod(b.getPaymentMethod())) {
                    qrRevenue += amt;
                } else {
                    cashRevenue += amt;
                }
            }
        }

        for (PosSale sale : posSales) {
            long amt = sale.getAmount() != null ? sale.getAmount() : 0;
            totalRevenue += amt;
            String cId = sale.getCourtId() != null ? sale.getCourtId() : "";
            revenueByCourt.merge(cId, amt, Long::sum);
            if (StaffController.isQrMethod(sale.getPaymentMethod())) {
                qrRevenue += amt;
            } else {
                cashRevenue += amt;
            }
        }

        List<Map<String, Object>> branchRevenues = new ArrayList<>();
        for (Court c : courts) {
            Map<String, Object> bm = new LinkedHashMap<>();
            bm.put("courtId", c.getId());
            bm.put("courtName", c.getName());
            bm.put("address", c.getAddress());
            bm.put("sportType", c.getSportType());
            bm.put("indoor", Boolean.TRUE.equals(c.getIndoor()));
            bm.put("latitude", c.getLatitude());
            bm.put("longitude", c.getLongitude());
            List<Map<String, String>> fieldMaps = new ArrayList<>();
            if (c.getFields() != null) {
                for (CourtField f : c.getFields()) {
                    Map<String, String> fm = new LinkedHashMap<>();
                    fm.put("id", f.getId());
                    fm.put("label", f.getLabel());
                    fm.put("detail", f.getDetail());
                    fm.put("fieldType", f.getFieldType() != null ? f.getFieldType() : "5_VS_5");
                    fieldMaps.add(fm);
                }
            }
            bm.put("fields", fieldMaps);
            bm.put("fieldCount", fieldMaps.size());
            bm.put("pricePerHour", c.getPricePerHour());
            bm.put("morningPrice", c.getMorningPrice());
            bm.put("afternoonPrice", c.getAfternoonPrice());
            bm.put("eveningPrice", c.getEveningPrice());
            bm.put("bookingCount", bookingCountByCourt.getOrDefault(c.getId(), 0));
            bm.put("activeCount", activeCountByCourt.getOrDefault(c.getId(), 0));
            bm.put("revenue", revenueByCourt.getOrDefault(c.getId(), 0L));
            branchRevenues.add(bm);
        }

        long pendingReports = reports.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("cashRevenue", cashRevenue);
        result.put("qrRevenue", qrRevenue);
        result.put("totalBookings", bookings.size());
        result.put("activeBookings", activeCount);
        result.put("totalCourts", courts.size());
        result.put("pendingReports", pendingReports);
        result.put("bookings", bookings);
        result.put("branchRevenues", branchRevenues);

        return ResponseEntity.ok(ApiResponse.success("Tổng quan chủ sân", result));
    }

    @GetMapping("/shifts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShiftsAndStaff(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        requireOwner(authHeader);
        List<StaffShift> shifts = staffShiftRepository.findAllByOrderByCreatedAtDesc();
        List<User> staffList = userRepository.findByRole("STAFF");
        List<Map<String, String>> sanitizedStaff = new ArrayList<>();
        for (User u : staffList) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getFullName());
            m.put("phone", u.getPhone());
            m.put("email", u.getEmail());
            sanitizedStaff.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shifts", shifts);
        result.put("staffList", sanitizedStaff);
        return ResponseEntity.ok(ApiResponse.success("Danh sách phân ca nhân viên", result));
    }

    @GetMapping("/shifts/schedule")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeekSchedule(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "weekStart", required = false) String weekStartParam,
            @RequestParam(value = "courtId", required = false) String courtId
    ) {
        requireOwner(authHeader);
        LocalDate baseDate = resolveWeekStartDate(weekStartParam);
        return ResponseEntity.ok(ApiResponse.success("Lịch phân ca tuần", shiftPlanningService.buildWeekSchedule(baseDate, courtId)));
    }

    @PostMapping("/shifts/auto-assign")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoAssignWeek(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        requireOwner(authHeader);
        String weekStartParam = body != null && body.get("weekStart") != null
                ? String.valueOf(body.get("weekStart"))
                : null;
        String courtId = body != null && body.get("courtId") != null
                ? String.valueOf(body.get("courtId")).trim()
                : null;
        String courtName = body != null && body.get("courtName") != null
                ? String.valueOf(body.get("courtName")).trim()
                : null;
        if (courtId != null && !courtId.isBlank() && (courtName == null || courtName.isBlank())) {
            courtName = courtRepository.findById(courtId).map(Court::getName).orElse(null);
        }
        LocalDate baseDate = resolveWeekStartDate(weekStartParam);
        Map<String, Object> result = shiftPlanningService.autoAssign(baseDate, courtId, courtName);
        return ResponseEntity.ok(ApiResponse.success(String.valueOf(result.get("message")), result));
    }

    private LocalDate resolveWeekStartDate(String weekStartParam) {
        if (weekStartParam == null || weekStartParam.isBlank()) {
            return LocalDate.now(clock);
        }
        LocalDate parsed = ShiftPlanningService.parseDateOrNull(weekStartParam);
        if (parsed == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Định dạng ngày weekStart phải là dd/MM/yyyy");
        }
        return parsed;
    }

    @PostMapping("/shifts")
    public ResponseEntity<ApiResponse<StaffShift>> assignShift(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        requireOwner(authHeader);
        String staffId = String.valueOf(body.getOrDefault("staffId", ""));
        User staff = userRepository.findById(staffId).orElse(null);
        if (staff == null || !"STAFF".equalsIgnoreCase(staff.getRole())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Nhân viên không tồn tại hoặc không có vai trò STAFF");
        }
        String shiftDateStr = String.valueOf(body.getOrDefault("shiftDate", ""));
        LocalDate shiftDate = ShiftPlanningService.parseDateOrNull(shiftDateStr);
        if (shiftDate == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Ngày làm việc không hợp lệ (định dạng dd/MM/yyyy)");
        }
        String courtId = String.valueOf(body.getOrDefault("courtId", "court_thanh_phat"));
        String courtName = String.valueOf(body.getOrDefault("courtName", "Sân bóng Thành Phát"));
        String shiftSlot = String.valueOf(body.getOrDefault("shiftSlot", "Ca Sáng (06:00 - 12:00)"));
        String duty = String.valueOf(body.getOrDefault("duty", "Lễ tân & Thu ngân"));
        String notes = String.valueOf(body.getOrDefault("notes", ""));

        StaffShift shift = shiftPlanningService.assign(staff, shiftDate, shiftSlot, duty, notes, courtId, courtName);
        return ResponseEntity.ok(ApiResponse.success("Đã phân ca thành công cho " + staff.getFullName(), shift));
    }

    @DeleteMapping("/shifts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteShift(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        requireOwner(authHeader);
        StaffShift shift = staffShiftRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy ca trực"));
        if (!"UPCOMING".equalsIgnoreCase(shift.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Không thể hủy ca đã chấm công");
        }
        staffShiftRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy ca trực", null));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ShiftReport>>> getShiftReports(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        requireOwner(authHeader);
        List<ShiftReport> list = shiftReportRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success("Danh sách bàn giao két", list));
    }

    @PostMapping("/reports/{id}/confirm")
    public ResponseEntity<ApiResponse<ShiftReport>> confirmShiftReport(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User owner = requireOwner(authHeader);
        ShiftReport report = shiftReportRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo két"));
        report.setStatus("CONFIRMED");
        report.setConfirmedBy(owner.getFullName());
        shiftReportRepository.save(report);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thu đủ két tiền", report));
    }

    @PostMapping("/reports/{id}/reject")
    public ResponseEntity<ApiResponse<ShiftReport>> rejectShiftReport(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
        ShiftReport report = shiftReportRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo két"));
        String ownerNote = body != null && body.get("note") != null
                ? String.valueOf(body.get("note")).trim()
                : "";
        if (ownerNote.isEmpty()) ownerNote = "Cần kiểm tra lại chênh lệch tiền két";
        report.setStatus("DISCREPANCY");
        report.setConfirmedBy(owner.getFullName());
        String oldNote = report.getHandoverNote() != null ? report.getHandoverNote().trim() : "";
        report.setHandoverNote(oldNote.isEmpty() ? "[Chủ sân: " + ownerNote + "]" : oldNote + " · [Chủ sân: " + ownerNote + "]");
        shiftReportRepository.save(report);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu báo cáo cần kiểm lại", report));
    }

    @PutMapping("/courts/{id}/pricing")
    public ResponseEntity<ApiResponse<Court>> updateCourtPricing(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        requireOwner(authHeader);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân: " + id));
        int pricePerHour = body.get("pricePerHour") instanceof Number n ? n.intValue() : court.getPricePerHour();
        int morningPrice = body.get("morningPrice") instanceof Number n ? n.intValue() : court.getMorningPrice();
        int afternoonPrice = body.get("afternoonPrice") instanceof Number n ? n.intValue() : court.getAfternoonPrice();
        int eveningPrice = body.get("eveningPrice") instanceof Number n ? n.intValue() : court.getEveningPrice();
        if (pricePerHour < 10000 || morningPrice < 10000 || afternoonPrice < 10000 || eveningPrice < 10000) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Giá thuê sân tối thiểu phải từ 10.000 đ trở lên.");
        }
        court.setPricePerHour(pricePerHour);
        court.setMorningPrice(morningPrice);
        court.setAfternoonPrice(afternoonPrice);
        court.setEveningPrice(eveningPrice);
        courtRepository.save(court);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật bảng giá cho " + court.getName(), court));
    }

    @PostMapping("/courts")
    public ResponseEntity<ApiResponse<Court>> createCourt(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        requireOwner(authHeader);
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        String address = String.valueOf(body.getOrDefault("address", "")).trim();
        String sportType = String.valueOf(body.getOrDefault("sportType", "Bóng đá")).trim();
        if (sportType.isEmpty()) sportType = "Bóng đá";
        String fieldTypeLabel = String.valueOf(body.getOrDefault("fieldTypeLabel", "")).trim();
        if (fieldTypeLabel.isEmpty()) fieldTypeLabel = sportType;
        String fieldTypeCode = resolveFieldTypeCode(fieldTypeLabel);
        boolean indoor = Boolean.TRUE.equals(body.get("indoor"));
        double latitude = body.get("latitude") instanceof Number n ? n.doubleValue() : 10.8016;
        double longitude = body.get("longitude") instanceof Number n ? n.doubleValue() : 106.6508;
        int fieldCount = body.get("fieldCount") instanceof Number n ? Math.max(1, Math.min(10, n.intValue())) : 2;
        int pricePerHour = body.get("pricePerHour") instanceof Number n ? n.intValue() : 300000;
        int morningPrice = body.get("morningPrice") instanceof Number n ? n.intValue() : 150000;
        int afternoonPrice = body.get("afternoonPrice") instanceof Number n ? n.intValue() : 250000;
        int eveningPrice = body.get("eveningPrice") instanceof Number n ? n.intValue() : 300000;

        if (name.isEmpty() || address.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ tên cụm sân và địa chỉ.");
        }
        if (pricePerHour < 10000 || morningPrice < 10000 || afternoonPrice < 10000 || eveningPrice < 10000) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Giá thuê sân tối thiểu phải từ 10.000 đ trở lên.");
        }

        String courtId = "court_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Court court = new Court(
                courtId, name, address, 5.0, 0, 1.5,
                pricePerHour, morningPrice, afternoonPrice, eveningPrice,
                "court_thanh_phat", latitude, longitude, sportType, indoor
        );
        for (int i = 1; i <= fieldCount; i++) {
            court.getFields().add(new CourtField(
                    courtId + "_f" + i,
                    court,
                    "Sân " + i,
                    fieldTypeLabel,
                    fieldTypeCode
            ));
        }
        courtRepository.save(court);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo cụm sân mới " + name, court));
    }

    @PutMapping("/courts/{id}")
    public ResponseEntity<ApiResponse<Court>> updateCourt(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        requireOwner(authHeader);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân: " + id));
        if (body.get("name") != null) {
            String name = String.valueOf(body.get("name")).trim();
            if (!name.isEmpty()) court.setName(name);
        }
        if (body.get("address") != null) {
            String address = String.valueOf(body.get("address")).trim();
            if (!address.isEmpty()) court.setAddress(address);
        }
        if (body.get("sportType") != null) {
            String st = String.valueOf(body.get("sportType")).trim();
            if (!st.isEmpty()) court.setSportType(st);
        }
        if (body.get("indoor") instanceof Boolean ind) {
            court.setIndoor(ind);
        }
        if (body.get("latitude") instanceof Number lat) {
            court.setLatitude(lat.doubleValue());
        }
        if (body.get("longitude") instanceof Number lng) {
            court.setLongitude(lng.doubleValue());
        }
        int pricePerHour = body.get("pricePerHour") instanceof Number n ? n.intValue() : court.getPricePerHour();
        int morningPrice = body.get("morningPrice") instanceof Number n ? n.intValue() : court.getMorningPrice();
        int afternoonPrice = body.get("afternoonPrice") instanceof Number n ? n.intValue() : court.getAfternoonPrice();
        int eveningPrice = body.get("eveningPrice") instanceof Number n ? n.intValue() : court.getEveningPrice();
        if (pricePerHour < 10000 || morningPrice < 10000 || afternoonPrice < 10000 || eveningPrice < 10000) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Giá thuê sân tối thiểu phải từ 10.000 đ trở lên.");
        }
        court.setPricePerHour(pricePerHour);
        court.setMorningPrice(morningPrice);
        court.setAfternoonPrice(afternoonPrice);
        court.setEveningPrice(eveningPrice);
        courtRepository.save(court);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật thông tin " + court.getName(), court));
    }

    @PostMapping("/courts/{id}/fields")
    public ResponseEntity<ApiResponse<Court>> addCourtField(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        requireOwner(authHeader);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân: " + id));
        String label = String.valueOf(body.getOrDefault("label", "")).trim();
        if (label.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Vui lòng nhập tên sân con (ví dụ: Sân 5C).");
        }
        String detail = String.valueOf(body.getOrDefault("detail", court.getSportType())).trim();
        if (detail.isEmpty()) detail = court.getSportType();
        String fieldType = String.valueOf(body.getOrDefault("fieldType", resolveFieldTypeCode(detail))).trim();
        if (fieldType.isEmpty()) fieldType = "5_VS_5";
        String fieldId = id + "_f_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        court.getFields().add(new CourtField(fieldId, court, label, detail, fieldType));
        courtRepository.save(court);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm " + label + " vào " + court.getName(), court));
    }

    @DeleteMapping("/courts/{id}/fields/{fieldId}")
    public ResponseEntity<ApiResponse<Court>> deleteCourtField(
            @PathVariable String id,
            @PathVariable String fieldId,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        requireOwner(authHeader);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân: " + id));
        if (court.getFields().size() <= 1) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Mỗi cụm sân cần giữ lại ít nhất 1 sân con.");
        }
        boolean removed = court.getFields().removeIf(f -> Objects.equals(f.getId(), fieldId));
        if (!removed) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Không tìm thấy sân con: " + fieldId);
        }
        courtRepository.save(court);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sân con khỏi " + court.getName(), court));
    }

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStaffAccounts(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        requireOwner(authHeader);
        List<User> staffList = userRepository.findByRole("STAFF");
        List<StaffShift> allShifts = staffShiftRepository.findAllByOrderByCreatedAtDesc();
        Map<String, List<StaffShift>> shiftsByStaff = new HashMap<>();
        for (StaffShift s : allShifts) {
            shiftsByStaff.computeIfAbsent(s.getStaffId(), k -> new ArrayList<>()).add(s);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : staffList) {
            result.add(buildStaffAccountMap(u, shiftsByStaff.getOrDefault(u.getId(), List.of())));
        }
        return ResponseEntity.ok(ApiResponse.success("Danh sách tài khoản nhân viên", result));
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createStaffAccount(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        requireOwner(authHeader);
        String fullName = String.valueOf(body.getOrDefault("fullName", "")).trim();
        String email = String.valueOf(body.getOrDefault("email", "")).trim().toLowerCase();
        String phone = String.valueOf(body.getOrDefault("phone", "")).trim();
        String password = String.valueOf(body.getOrDefault("password", ""));
        String address = String.valueOf(body.getOrDefault("address", "")).trim();
        if (address.isEmpty()) address = "Chi nhánh EasySport";

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.length() < 6) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Vui lòng nhập đủ Họ tên, Email, SĐT và mật khẩu tối thiểu 6 ký tự.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Email nhân viên đã tồn tại trong hệ thống.");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Số điện thoại nhân viên đã được sử dụng.");
        }

        User newStaff = new User(
                "user_staff_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                phone,
                PasswordHasher.hash(password),
                fullName,
                email,
                address,
                "STAFF"
        );
        userRepository.save(newStaff);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo tài khoản nhân viên " + fullName, buildStaffAccountMap(newStaff, List.of())));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStaffAccount(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        requireOwner(authHeader);
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên: " + id));
        if (!"STAFF".equalsIgnoreCase(staff.getRole())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Chỉ có thể chỉnh sửa tài khoản vai trò STAFF.");
        }
        if (body.get("fullName") != null) {
            String fullName = String.valueOf(body.get("fullName")).trim();
            if (fullName.length() < 2) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Họ tên nhân viên tối thiểu 2 ký tự.");
            }
            staff.setFullName(fullName);
        }
        if (body.get("phone") != null) {
            String phone = String.valueOf(body.get("phone")).trim();
            if (!phone.isEmpty() && !phone.equals(staff.getPhone())) {
                if (userRepository.existsByPhone(phone)) {
                    throw new org.springframework.web.server.ResponseStatusException(
                            HttpStatus.CONFLICT, "Số điện thoại đã được nhân viên khác sử dụng.");
                }
                staff.setPhone(phone);
            }
        }
        if (body.get("address") != null) {
            String address = String.valueOf(body.get("address")).trim();
            if (!address.isEmpty()) staff.setAddress(address);
        }
        if (body.get("newPassword") != null) {
            String newPw = String.valueOf(body.get("newPassword"));
            if (!newPw.isBlank()) {
                if (newPw.length() < 6) {
                    throw new org.springframework.web.server.ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Mật khẩu mới phải có ít nhất 6 ký tự.");
                }
                staff.setPassword(PasswordHasher.hash(newPw));
            }
        }
        userRepository.save(staff);
        List<StaffShift> staffShifts = staffShiftRepository.findByStaffIdOrderByCreatedAtDesc(staff.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật thông tin " + staff.getFullName(), buildStaffAccountMap(staff, staffShifts)));
    }

    @PostMapping("/staff/{id}/toggle-lock")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleStaffLock(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        requireOwner(authHeader);
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên: " + id));
        if (!"STAFF".equalsIgnoreCase(staff.getRole())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Chỉ có thể khóa/mở tài khoản vai trò STAFF.");
        }
        boolean nextLocked = !Boolean.TRUE.equals(staff.getLocked());
        staff.setLocked(nextLocked);
        userRepository.save(staff);
        List<StaffShift> staffShifts = staffShiftRepository.findByStaffIdOrderByCreatedAtDesc(staff.getId());
        String msg = nextLocked ? "Đã khóa tài khoản " + staff.getFullName() : "Đã mở khóa tài khoản " + staff.getFullName();
        return ResponseEntity.ok(ApiResponse.success(msg, buildStaffAccountMap(staff, staffShifts)));
    }

    private Map<String, Object> buildStaffAccountMap(User u, List<StaffShift> staffShifts) {
        int completed = 0;
        int lateCount = 0;
        int earlyCount = 0;
        double totalHours = 0.0;
        for (StaffShift s : staffShifts) {
            totalHours += ShiftPlanningService.hoursOf(s.getShiftSlot());
            if ("COMPLETED".equalsIgnoreCase(s.getStatus())) completed++;
            if (s.getLateMinutes() != null && s.getLateMinutes() > 0) lateCount++;
            if (s.getNotes() != null && s.getNotes().contains("[Về sớm")) earlyCount++;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getFullName());
        m.put("phone", u.getPhone());
        m.put("email", u.getEmail());
        m.put("address", u.getAddress() != null ? u.getAddress() : "");
        m.put("locked", Boolean.TRUE.equals(u.getLocked()));
        m.put("shiftCount", staffShifts.size());
        m.put("completedShifts", completed);
        m.put("lateCount", lateCount);
        m.put("earlyCount", earlyCount);
        m.put("totalHours", totalHours);
        return m;
    }

    private static String resolveFieldTypeCode(String detail) {
        if (detail == null) return "5_VS_5";
        if (detail.contains("7")) return "7_VS_7";
        if (detail.contains("11")) return "11_VS_11";
        return "5_VS_5";
    }

    private static LocalDate resolveBookingDate(Booking b) {
        LocalDate parsed = ShiftPlanningService.parseDateOrNull(StaffController.extractDate(b.getDateLabel()));
        if (parsed != null) return parsed;
        return b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate() : null;
    }

    private static boolean matchesPeriod(
            LocalDate date, String period, LocalDate today, LocalDate weekStart, LocalDate weekEnd, YearMonth thisMonth
    ) {
        if ("ALL".equals(period)) return true;
        if (date == null) return false;
        return switch (period) {
            case "TODAY" -> date.equals(today);
            case "WEEK" -> !date.isBefore(weekStart) && !date.isAfter(weekEnd);
            case "MONTH" -> YearMonth.from(date).equals(thisMonth);
            default -> true;
        };
    }
}
