package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.dto.RecurringDtos;
import com.easysport.entity.Booking;
import com.easysport.entity.Court;
import com.easysport.entity.CourtField;
import com.easysport.entity.InventoryItem;
import com.easysport.entity.InventoryTransaction;
import com.easysport.entity.PosSale;
import com.easysport.entity.RecurringPackage;
import com.easysport.entity.SessionChangeRequest;
import com.easysport.entity.ShiftReport;
import com.easysport.entity.StaffShift;
import com.easysport.entity.User;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.CourtRepository;
import com.easysport.repository.InventoryItemRepository;
import com.easysport.repository.InventoryTransactionRepository;
import com.easysport.repository.PosSaleRepository;
import com.easysport.repository.RecurringPackageRepository;
import com.easysport.repository.ShiftReportRepository;
import com.easysport.repository.StaffShiftRepository;
import com.easysport.repository.UserRepository;
import com.easysport.service.AuthService;
import com.easysport.service.RecurringBookingService;
import com.easysport.service.ShiftPlanningService;
import com.easysport.util.PasswordHasher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

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
    private final RecurringPackageRepository recurringPackageRepository;
    private final RecurringBookingService recurringBookingService;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public OwnerController(AuthService authService,
                           BookingRepository bookingRepository,
                           CourtRepository courtRepository,
                           StaffShiftRepository staffShiftRepository,
                           ShiftReportRepository shiftReportRepository,
                           PosSaleRepository posSaleRepository,
                           UserRepository userRepository,
                           ShiftPlanningService shiftPlanningService,
                           Clock clock) {
        this(authService, bookingRepository, courtRepository, staffShiftRepository,
                shiftReportRepository, posSaleRepository, userRepository, shiftPlanningService, clock,
                null, null, null, null);
    }

    public OwnerController(AuthService authService,
                           BookingRepository bookingRepository,
                           CourtRepository courtRepository,
                           StaffShiftRepository staffShiftRepository,
                           ShiftReportRepository shiftReportRepository,
                           PosSaleRepository posSaleRepository,
                           UserRepository userRepository,
                           ShiftPlanningService shiftPlanningService,
                           Clock clock,
                           RecurringPackageRepository recurringPackageRepository,
                           RecurringBookingService recurringBookingService) {
        this(authService, bookingRepository, courtRepository, staffShiftRepository,
                shiftReportRepository, posSaleRepository, userRepository, shiftPlanningService, clock,
                recurringPackageRepository, recurringBookingService, null, null);
    }

    public OwnerController(AuthService authService,
                           BookingRepository bookingRepository,
                           CourtRepository courtRepository,
                           StaffShiftRepository staffShiftRepository,
                           ShiftReportRepository shiftReportRepository,
                           PosSaleRepository posSaleRepository,
                           UserRepository userRepository,
                           ShiftPlanningService shiftPlanningService,
                           InventoryItemRepository inventoryItemRepository,
                           InventoryTransactionRepository inventoryTransactionRepository,
                           Clock clock) {
        this(authService, bookingRepository, courtRepository, staffShiftRepository,
                shiftReportRepository, posSaleRepository, userRepository, shiftPlanningService, clock,
                null, null, inventoryItemRepository, inventoryTransactionRepository);
    }

    @Autowired
    public OwnerController(AuthService authService,
                           BookingRepository bookingRepository,
                           CourtRepository courtRepository,
                           StaffShiftRepository staffShiftRepository,
                           ShiftReportRepository shiftReportRepository,
                           PosSaleRepository posSaleRepository,
                           UserRepository userRepository,
                           ShiftPlanningService shiftPlanningService,
                           Clock clock,
                           @Autowired(required = false) RecurringPackageRepository recurringPackageRepository,
                           @Autowired(required = false) RecurringBookingService recurringBookingService,
                           @Autowired(required = false) InventoryItemRepository inventoryItemRepository,
                           @Autowired(required = false) InventoryTransactionRepository inventoryTransactionRepository) {
        this.authService = authService;
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.shiftReportRepository = shiftReportRepository;
        this.posSaleRepository = posSaleRepository;
        this.userRepository = userRepository;
        this.shiftPlanningService = shiftPlanningService;
        this.clock = clock;
        this.recurringPackageRepository = recurringPackageRepository;
        this.recurringBookingService = recurringBookingService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
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

    public List<Court> getCourtsForOwner(User owner) {
        List<Court> allCourts = courtRepository.findAll();
        List<Court> specificallyOwned = allCourts.stream()
                .filter(c -> Objects.equals(c.getOwnerId(), owner.getId()))
                .toList();
        return specificallyOwned.isEmpty()
                ? allCourts.stream().filter(c -> c.getOwnerId() == null).toList()
                : specificallyOwned;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "period", required = false, defaultValue = "ALL") String period
    ) {
        User owner = requireOwner(authHeader);
        String normalizedPeriod = period != null ? period.trim().toUpperCase() : "ALL";
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        YearMonth thisMonth = YearMonth.from(today);

        List<Court> courts = getCourtsForOwner(owner);
        Set<String> myCourtIds = courts.stream().map(Court::getId).collect(java.util.stream.Collectors.toSet());

        List<Booking> allBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        List<Booking> bookings = allBookings.stream()
                .filter(b -> b.getCourtId() != null && myCourtIds.contains(b.getCourtId()))
                .filter(b -> matchesPeriod(resolveBookingDate(b), normalizedPeriod, today, weekStart, weekEnd, thisMonth))
                .toList();

        List<ShiftReport> reports = shiftReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(r -> r.getCourtId() != null && myCourtIds.contains(r.getCourtId()))
                .toList();
        List<PosSale> allPosSales = posSaleRepository.findAllByOrderByCreatedAtDesc();
        List<PosSale> posSales = allPosSales.stream()
                .filter(s -> s.getCourtId() != null && myCourtIds.contains(s.getCourtId()))
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
        long walletRevenue = 0;
        long totalCourtRevenue = 0;
        long totalPosRevenue = 0;
        int activeCount = 0;

        Map<String, Court> courtMap = new HashMap<>();
        for (Court c : courts) {
            courtMap.put(c.getId(), c);
        }

        Map<String, Long> revenueByCourt = new HashMap<>();
        Map<String, Long> courtRevenueByCourt = new HashMap<>();
        Map<String, Long> posRevenueByCourt = new HashMap<>();
        Map<String, Integer> bookingCountByCourt = new HashMap<>();
        Map<String, Integer> activeCountByCourt = new HashMap<>();

        Map<String, Map<String, Object>> fieldStatsMap = new LinkedHashMap<>();
        Map<String, long[]> slotStats = new LinkedHashMap<>();
        slotStats.put("MORNING", new long[]{0L, 0L});
        slotStats.put("AFTERNOON", new long[]{0L, 0L});
        slotStats.put("EVENING", new long[]{0L, 0L});

        for (Booking b : bookings) {
            String st = b.getStatus() != null ? b.getStatus().toUpperCase() : "";
            String cId = b.getCourtId() != null ? b.getCourtId() : "";
            bookingCountByCourt.merge(cId, 1, Integer::sum);
            if ("CHECKED_IN".equals(st)) {
                activeCount++;
                activeCountByCourt.merge(cId, 1, Integer::sum);
            }
            if (b.getRecurringPackageId() != null) {
                // Tiền thuê sân của lịch cố định ghi nhận 1 lần theo RecurringPackage (totalPaid - totalRefunded);
                // ở đây chỉ cộng thêm doanh thu dịch vụ tại quầy nếu có phát sinh khi trả sân.
                if ("COMPLETED".equals(st) && !splitSettledIds.contains(b.getId()) && b.getServiceAmount() > 0) {
                    long svcAmt = b.getServiceAmount();
                    totalRevenue += svcAmt;
                    totalPosRevenue += svcAmt;
                    revenueByCourt.merge(cId, svcAmt, Long::sum);
                    posRevenueByCourt.merge(cId, svcAmt, Long::sum);
                    if (StaffController.isQrMethod(b.getPaymentMethod())) {
                        qrRevenue += svcAmt;
                    } else if (!"WALLET".equalsIgnoreCase(b.getPaymentMethod()) && !"CREDIT".equalsIgnoreCase(b.getPaymentMethod())) {
                        cashRevenue += svcAmt;
                    }
                    Court courtObj = courtMap.get(cId);
                    String fieldLabel = resolveShortFieldName(courtObj, b.getFieldDetail());
                    Map<String, Object> fRow = fieldStatsMap.computeIfAbsent(cId + "|" + fieldLabel, k -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("courtId", cId);
                        m.put("courtName", courtObj != null ? courtObj.getName() : (b.getCourtName() != null ? b.getCourtName() : "Sân bóng"));
                        m.put("fieldName", fieldLabel);
                        m.put("bookingCount", 0);
                        m.put("courtRevenue", 0L);
                        m.put("serviceRevenue", 0L);
                        m.put("totalRevenue", 0L);
                        return m;
                    });
                    fRow.put("serviceRevenue", (Long) fRow.get("serviceRevenue") + svcAmt);
                    fRow.put("totalRevenue", (Long) fRow.get("totalRevenue") + svcAmt);

                    String slotKey = resolveTimeSlotBucket(b.getTimeRange());
                    long[] sArr = slotStats.get(slotKey);
                    if (sArr != null) {
                        sArr[1] += svcAmt;
                    }
                }
                continue;
            }
            if (st.equals("PAID") || st.equals("DEPOSIT_PAID") || st.equals("CHECKED_IN") || st.equals("COMPLETED")) {
                long amt;
                long svcPart = 0;
                if (splitSettledIds.contains(b.getId())) {
                    amt = b.getDepositAmount() != null ? b.getDepositAmount() : 0;
                } else if (st.equals("COMPLETED") || st.equals("PAID")) {
                    int basePrice = b.getTotalPrice() != null ? b.getTotalPrice() : 0;
                    amt = (long) basePrice + b.getServiceAmount();
                    svcPart = Math.min(amt, b.getServiceAmount());
                } else {
                    amt = b.getDepositAmount() != null ? b.getDepositAmount() : 0;
                }
                long courtPart = Math.max(0L, amt - svcPart);
                totalRevenue += amt;
                totalCourtRevenue += courtPart;
                totalPosRevenue += svcPart;
                revenueByCourt.merge(cId, amt, Long::sum);
                courtRevenueByCourt.merge(cId, courtPart, Long::sum);
                posRevenueByCourt.merge(cId, svcPart, Long::sum);
                if (StaffController.isQrMethod(b.getPaymentMethod())) {
                    qrRevenue += amt;
                } else if ("WALLET".equalsIgnoreCase(b.getPaymentMethod())) {
                    walletRevenue += amt;
                } else if (!"CREDIT".equalsIgnoreCase(b.getPaymentMethod())) {
                    cashRevenue += amt;
                }

                Court courtObj = courtMap.get(cId);
                String fieldLabel = resolveShortFieldName(courtObj, b.getFieldDetail());
                String fieldKey = cId + "|" + fieldLabel;
                Map<String, Object> fRow = fieldStatsMap.computeIfAbsent(fieldKey, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("courtId", cId);
                    m.put("courtName", courtObj != null ? courtObj.getName() : (b.getCourtName() != null ? b.getCourtName() : "Sân bóng"));
                    m.put("fieldName", fieldLabel);
                    m.put("bookingCount", 0);
                    m.put("courtRevenue", 0L);
                    m.put("serviceRevenue", 0L);
                    m.put("totalRevenue", 0L);
                    return m;
                });
                fRow.put("bookingCount", (Integer) fRow.get("bookingCount") + 1);
                fRow.put("courtRevenue", (Long) fRow.get("courtRevenue") + courtPart);
                fRow.put("serviceRevenue", (Long) fRow.get("serviceRevenue") + svcPart);
                fRow.put("totalRevenue", (Long) fRow.get("totalRevenue") + amt);

                String slotKey = resolveTimeSlotBucket(b.getTimeRange());
                long[] sArr = slotStats.get(slotKey);
                if (sArr != null) {
                    sArr[0] += 1;
                    sArr[1] += amt;
                }
            }
        }

        for (PosSale sale : posSales) {
            long amt = sale.getAmount() != null ? sale.getAmount() : 0;
            totalRevenue += amt;
            String cId = sale.getCourtId() != null ? sale.getCourtId() : "";
            revenueByCourt.merge(cId, amt, Long::sum);
            if (sale.getSettlementBookingId() != null) {
                totalCourtRevenue += amt;
                courtRevenueByCourt.merge(cId, amt, Long::sum);
            } else {
                totalPosRevenue += amt;
                posRevenueByCourt.merge(cId, amt, Long::sum);
            }
            if (StaffController.isQrMethod(sale.getPaymentMethod())) {
                qrRevenue += amt;
            } else {
                cashRevenue += amt;
            }
        }

        if (recurringPackageRepository != null && !myCourtIds.isEmpty()) {
            for (RecurringPackage pkg : recurringPackageRepository.findByCourtIdInOrderByCreatedAtDesc(new ArrayList<>(myCourtIds))) {
                if (pkg.getTotalPaid() <= 0) continue;
                LocalDate pkgDate = pkg.getCreatedAt() != null ? pkg.getCreatedAt().toLocalDate() : null;
                if (!matchesPeriod(pkgDate, normalizedPeriod, today, weekStart, weekEnd, thisMonth)) continue;
                long netPaid = Math.max(0L, (long) pkg.getTotalPaid() - pkg.getTotalRefunded());
                totalRevenue += netPaid;
                totalCourtRevenue += netPaid;
                String pkgCourtId = pkg.getCourtId() != null ? pkg.getCourtId() : "";
                revenueByCourt.merge(pkgCourtId, netPaid, Long::sum);
                courtRevenueByCourt.merge(pkgCourtId, netPaid, Long::sum);
                if (StaffController.isQrMethod(pkg.getPaymentMethod())) {
                    qrRevenue += netPaid;
                } else if ("WALLET".equalsIgnoreCase(pkg.getPaymentMethod())) {
                    walletRevenue += netPaid;
                }

                if (netPaid > 0) {
                    Court courtObj = courtMap.get(pkgCourtId);
                    String fieldLabel = resolveShortFieldName(courtObj, pkg.getFieldDetail());
                    Map<String, Object> fRow = fieldStatsMap.computeIfAbsent(pkgCourtId + "|" + fieldLabel, k -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("courtId", pkgCourtId);
                        m.put("courtName", courtObj != null ? courtObj.getName() : (pkg.getCourtName() != null ? pkg.getCourtName() : "Sân bóng"));
                        m.put("fieldName", fieldLabel);
                        m.put("bookingCount", 0);
                        m.put("courtRevenue", 0L);
                        m.put("serviceRevenue", 0L);
                        m.put("totalRevenue", 0L);
                        return m;
                    });
                    fRow.put("bookingCount", (Integer) fRow.get("bookingCount") + 1);
                    fRow.put("courtRevenue", (Long) fRow.get("courtRevenue") + netPaid);
                    fRow.put("totalRevenue", (Long) fRow.get("totalRevenue") + netPaid);

                    String slotKey = resolveTimeSlotBucket(pkg.getTimeRange());
                    long[] sArr = slotStats.get(slotKey);
                    if (sArr != null) {
                        sArr[0] += 1;
                        sArr[1] += netPaid;
                    }
                }
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
            bm.put("courtRevenue", courtRevenueByCourt.getOrDefault(c.getId(), 0L));
            bm.put("posRevenue", posRevenueByCourt.getOrDefault(c.getId(), 0L));
            branchRevenues.add(bm);
        }

        List<Map<String, Object>> fieldRevenues = new ArrayList<>(fieldStatsMap.values());
        fieldRevenues.sort((a, b) -> Long.compare((Long) b.get("totalRevenue"), (Long) a.get("totalRevenue")));

        List<Map<String, Object>> timeSlotRevenues = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : slotStats.entrySet()) {
            String slot = entry.getKey();
            String label = switch (slot) {
                case "MORNING" -> "Sáng (05:00 - 12:00)";
                case "AFTERNOON" -> "Chiều (12:00 - 17:00)";
                default -> "Tối (17:00 - 23:00)";
            };
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("slot", slot);
            sm.put("label", label);
            sm.put("bookingCount", (int) entry.getValue()[0]);
            sm.put("revenue", entry.getValue()[1]);
            timeSlotRevenues.add(sm);
        }

        long pendingReports = reports.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();

        long pendingRecurringRequests = recurringBookingService != null && !myCourtIds.isEmpty()
                ? recurringBookingService.getChangeRequestsForOwnerCourts(myCourtIds).stream()
                        .filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count()
                : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("cashRevenue", cashRevenue);
        result.put("qrRevenue", qrRevenue);
        result.put("walletRevenue", walletRevenue);
        result.put("courtRevenue", totalCourtRevenue);
        result.put("posRevenue", totalPosRevenue);
        result.put("totalBookings", bookings.size());
        result.put("activeBookings", activeCount);
        result.put("totalCourts", courts.size());
        result.put("pendingReports", pendingReports);
        result.put("pendingRecurringRequests", pendingRecurringRequests);
        result.put("bookings", bookings);
        result.put("branchRevenues", branchRevenues);
        result.put("fieldRevenues", fieldRevenues);
        result.put("timeSlotRevenues", timeSlotRevenues);

        return ResponseEntity.ok(ApiResponse.success("Tổng quan chủ sân", result));
    }

    private static String resolveShortFieldName(Court court, String fieldDetail) {
        String raw = fieldDetail != null ? fieldDetail.trim() : "";
        if (raw.isEmpty()) return "Sân mặc định";
        if (court != null && court.getFields() != null) {
            for (CourtField f : court.getFields()) {
                if (f.getLabel() != null && !f.getLabel().isBlank()) {
                    if (raw.equalsIgnoreCase(f.getLabel()) || raw.toLowerCase().contains(f.getLabel().toLowerCase())
                            || (f.getDetail() != null && f.getDetail().equalsIgnoreCase(raw))) {
                        return f.getLabel();
                    }
                }
            }
        }
        return raw.length() > 28 ? raw.substring(0, 28) + "..." : raw;
    }

    private static String resolveTimeSlotBucket(String timeRange) {
        LocalTime[] times = StaffController.extractTimeInterval(timeRange);
        if (times == null || times.length == 0) return "EVENING";
        LocalTime start = times[0];
        if (start.isBefore(LocalTime.of(12, 0))) return "MORNING";
        if (start.isBefore(LocalTime.of(17, 0))) return "AFTERNOON";
        return "EVENING";
    }

    @GetMapping("/shifts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShiftsAndStaff(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User owner = requireOwner(authHeader);
        Set<String> myCourtIds = getCourtsForOwner(owner).stream().map(Court::getId).collect(java.util.stream.Collectors.toSet());
        List<StaffShift> shifts = staffShiftRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(s -> s.getCourtId() == null || myCourtIds.contains(s.getCourtId()))
                .toList();
        shiftPlanningService.autoCloseExpiredActiveShifts(shifts);
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
        User owner = requireOwner(authHeader);
        Set<String> myCourtIds = getCourtsForOwner(owner).stream()
                .map(Court::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (courtId != null && !courtId.isBlank() && !"ALL".equalsIgnoreCase(courtId)) {
            Court c = courtRepository.findById(courtId).orElse(null);
            if (c != null && c.getOwnerId() != null && !Objects.equals(c.getOwnerId(), owner.getId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Bạn không có quyền xem lịch phân ca của cụm sân khác.");
            }
        }
        LocalDate baseDate = resolveWeekStartDate(weekStartParam);
        return ResponseEntity.ok(ApiResponse.success("Lịch phân ca tuần", shiftPlanningService.buildWeekSchedule(baseDate, courtId, myCourtIds)));
    }

    @PostMapping("/shifts/auto-assign")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoAssignWeek(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
        String weekStartParam = body != null && body.get("weekStart") != null
                ? String.valueOf(body.get("weekStart"))
                : null;
        String courtId = body != null && body.get("courtId") != null
                ? String.valueOf(body.get("courtId")).trim()
                : null;
        String courtName = body != null && body.get("courtName") != null
                ? String.valueOf(body.get("courtName")).trim()
                : null;
        if (courtId != null && !courtId.isBlank() && !"ALL".equalsIgnoreCase(courtId)) {
            Court c = courtRepository.findById(courtId).orElse(null);
            if (c != null && c.getOwnerId() != null && !Objects.equals(c.getOwnerId(), owner.getId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Bạn không có quyền tự động xếp ca cho cụm sân khác.");
            }
            if (courtName == null || courtName.isBlank()) {
                courtName = c != null ? c.getName() : null;
            }
        } else {
            List<Court> myCourts = getCourtsForOwner(owner);
            if (!myCourts.isEmpty()) {
                courtId = myCourts.get(0).getId();
                if (courtName == null || courtName.isBlank()) {
                    courtName = myCourts.get(0).getName();
                }
            }
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
        User owner = requireOwner(authHeader);
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
        Court targetCourt = courtRepository.findById(courtId).orElse(null);
        if (targetCourt != null && targetCourt.getOwnerId() != null && !Objects.equals(targetCourt.getOwnerId(), owner.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Bạn không có quyền phân ca cho cụm sân của chủ sân khác.");
        }
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
        User owner = requireOwner(authHeader);
        StaffShift shift = staffShiftRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy ca trực"));
        if (!"UPCOMING".equalsIgnoreCase(shift.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.CONFLICT, "Không thể hủy ca đã chấm công");
        }
        if (shift.getCourtId() != null) {
            Court c = courtRepository.findById(shift.getCourtId()).orElse(null);
            if (c != null && c.getOwnerId() != null && !Objects.equals(c.getOwnerId(), owner.getId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Bạn không có quyền hủy ca của cụm sân khác.");
            }
        }
        staffShiftRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy ca trực", null));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ShiftReport>>> getShiftReports(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User owner = requireOwner(authHeader);
        Set<String> myCourtIds = getCourtsForOwner(owner).stream().map(Court::getId).collect(java.util.stream.Collectors.toSet());
        List<ShiftReport> list = shiftReportRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(r -> r.getCourtId() == null || myCourtIds.contains(r.getCourtId()))
                .toList();
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
        if (report.getCourtId() != null) {
            Court c = courtRepository.findById(report.getCourtId()).orElse(null);
            if (c != null && c.getOwnerId() != null && !Objects.equals(c.getOwnerId(), owner.getId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Bạn không có quyền xác nhận báo cáo két của cụm sân khác.");
            }
        }
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
        if (report.getCourtId() != null) {
            Court c = courtRepository.findById(report.getCourtId()).orElse(null);
            if (c != null && c.getOwnerId() != null && !Objects.equals(c.getOwnerId(), owner.getId())) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Bạn không có quyền từ chối báo cáo két của cụm sân khác.");
            }
        }
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
        User owner = requireOwner(authHeader);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân: " + id));
        if (court.getOwnerId() != null && !Objects.equals(court.getOwnerId(), owner.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa bảng giá của cụm sân khác.");
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
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật bảng giá cho " + court.getName(), court));
    }

    @PostMapping("/courts")
    public ResponseEntity<ApiResponse<Court>> createCourt(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
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
        court.setOwnerId(owner.getId());
        String city = body.get("city") != null ? String.valueOf(body.get("city")).trim() : null;
        String district = body.get("district") != null ? String.valueOf(body.get("district")).trim() : null;
        populateAreaFromAddress(court, address, city, district);
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
        User owner = requireOwner(authHeader);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân: " + id));
        if (court.getOwnerId() != null && !Objects.equals(court.getOwnerId(), owner.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Bạn không có quyền chỉnh sửa thông tin của cụm sân khác.");
        }
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
        String explicitCity = body.get("city") != null ? String.valueOf(body.get("city")).trim() : null;
        String explicitDistrict = body.get("district") != null ? String.valueOf(body.get("district")).trim() : null;
        if (explicitCity != null || explicitDistrict != null || body.get("address") != null) {
            populateAreaFromAddress(court, court.getAddress(), explicitCity, explicitDistrict);
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
        User owner = requireOwner(authHeader);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân: " + id));
        if (court.getOwnerId() != null && !Objects.equals(court.getOwnerId(), owner.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Bạn không có quyền thêm sân con cho cụm sân khác.");
        }
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
        User owner = requireOwner(authHeader);
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cụm sân: " + id));
        if (court.getOwnerId() != null && !Objects.equals(court.getOwnerId(), owner.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Bạn không có quyền xóa sân con của cụm sân khác.");
        }
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

    // ==================== QUẢN LÝ KHO HÀNG (INVENTORY) ====================

    private Set<String> requireOwnedCourtIds(User owner) {
        return getCourtsForOwner(owner).stream().map(Court::getId).collect(Collectors.toSet());
    }

    private void assertCourtAccess(Set<String> myCourtIds, String courtId) {
        if (courtId == null || !myCourtIds.contains(courtId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác kho của chi nhánh này.");
        }
    }

    private void requireInventoryReady() {
        if (inventoryItemRepository == null || inventoryTransactionRepository == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ kho hàng chưa sẵn sàng.");
        }
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String courtId
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        String filterCourt = courtId != null && !courtId.isBlank() && !"ALL".equalsIgnoreCase(courtId)
                ? courtId.trim() : null;
        if (filterCourt != null) {
            assertCourtAccess(myCourtIds, filterCourt);
        }

        List<InventoryItem> items = filterCourt != null
                ? inventoryItemRepository.findByCourtIdAndActiveTrueOrderByNameAsc(filterCourt)
                : inventoryItemRepository.findByActiveTrueOrderByCourtIdAscNameAsc().stream()
                        .filter(i -> myCourtIds.contains(i.getCourtId()))
                        .toList();

        List<InventoryTransaction> transactions = filterCourt != null
                ? inventoryTransactionRepository.findTop50ByCourtIdOrderByCreatedAtDesc(filterCourt)
                : inventoryTransactionRepository.findTop50ByOrderByCreatedAtDesc().stream()
                        .filter(tx -> myCourtIds.contains(tx.getCourtId()))
                        .toList();

        long lowStockCount = items.stream().filter(InventoryItem::isLowStock).count();
        long totalValue = items.stream()
                .mapToLong(i -> (long) i.getStockQuantity() * i.getImportPrice())
                .sum();
        int totalUnits = items.stream().mapToInt(InventoryItem::getStockQuantity).sum();

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (InventoryItem item : items) {
            itemMaps.add(toInventoryItemMap(item));
        }
        List<Map<String, Object>> txMaps = new ArrayList<>();
        for (InventoryTransaction tx : transactions) {
            txMaps.add(toInventoryTxMap(tx));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", itemMaps);
        result.put("transactions", txMaps);
        result.put("totalItems", items.size());
        result.put("totalUnits", totalUnits);
        result.put("lowStockCount", lowStockCount);
        result.put("totalStockValue", totalValue);

        return ResponseEntity.ok(ApiResponse.success("Danh sách kho hàng", result));
    }

    @PostMapping("/inventory")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createInventoryItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        String courtId = String.valueOf(body.getOrDefault("courtId", "")).trim();
        if (name.isEmpty() || courtId.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tên mặt hàng và chi nhánh không được để trống."));
        }
        assertCourtAccess(myCourtIds, courtId);

        Court court = courtRepository.findById(courtId).orElse(null);
        if (court == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy chi nhánh: " + courtId));
        }

        String category = String.valueOf(body.getOrDefault("category", "DRINK")).trim().toUpperCase();
        String unit = String.valueOf(body.getOrDefault("unit", "chai")).trim();
        int importPrice = ((Number) body.getOrDefault("importPrice", 0)).intValue();
        int sellPrice = ((Number) body.getOrDefault("sellPrice", 0)).intValue();
        int stockQuantity = ((Number) body.getOrDefault("stockQuantity", 0)).intValue();
        int minAlert = ((Number) body.getOrDefault("minAlertThreshold", 10)).intValue();

        if (sellPrice < 0 || importPrice < 0 || stockQuantity < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Giá và số lượng tồn không được âm."));
        }

        String id = "inv_" + UUID.randomUUID().toString().substring(0, 8);
        InventoryItem item = new InventoryItem(
                id, court.getId(), court.getName(), name, category, unit,
                importPrice, sellPrice, stockQuantity, minAlert
        );
        if (body.get("sku") != null) {
            item.setSku(String.valueOf(body.get("sku")).trim());
        }
        if (body.get("barcode") != null) {
            item.setBarcode(String.valueOf(body.get("barcode")).trim());
        }
        if (body.get("supplierName") != null) {
            item.setSupplierName(String.valueOf(body.get("supplierName")).trim());
        }
        inventoryItemRepository.save(item);

        if (stockQuantity > 0) {
            InventoryTransaction tx = new InventoryTransaction(
                    "tx_" + UUID.randomUUID().toString().substring(0, 8),
                    item.getId(), item.getName(), item.getCourtId(),
                    "IMPORT", stockQuantity, stockQuantity, importPrice,
                    "Khởi tạo mặt hàng mới vào kho", owner.getFullName()
            );
            inventoryTransactionRepository.save(tx);
        }

        return ResponseEntity.ok(ApiResponse.success("Đã thêm mặt hàng vào kho", toInventoryItemMap(item)));
    }

    @PutMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateInventoryItem(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        InventoryItem item = inventoryItemRepository.findById(id).orElse(null);
        if (item == null || !Boolean.TRUE.equals(item.getActive())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy mặt hàng."));
        }
        assertCourtAccess(myCourtIds, item.getCourtId());

        if (body.containsKey("name")) {
            String n = String.valueOf(body.get("name")).trim();
            if (!n.isEmpty()) item.setName(n);
        }
        if (body.containsKey("category")) {
            item.setCategory(String.valueOf(body.get("category")).trim().toUpperCase());
        }
        if (body.containsKey("unit")) {
            item.setUnit(String.valueOf(body.get("unit")).trim());
        }
        if (body.containsKey("importPrice")) {
            item.setImportPrice(Math.max(0, ((Number) body.get("importPrice")).intValue()));
        }
        if (body.containsKey("sellPrice")) {
            item.setSellPrice(Math.max(0, ((Number) body.get("sellPrice")).intValue()));
        }
        if (body.containsKey("minAlertThreshold")) {
            item.setMinAlertThreshold(Math.max(0, ((Number) body.get("minAlertThreshold")).intValue()));
        }
        if (body.containsKey("sku")) {
            item.setSku(body.get("sku") != null ? String.valueOf(body.get("sku")).trim() : "");
        }
        if (body.containsKey("barcode")) {
            item.setBarcode(body.get("barcode") != null ? String.valueOf(body.get("barcode")).trim() : "");
        }
        if (body.containsKey("supplierName")) {
            item.setSupplierName(body.get("supplierName") != null ? String.valueOf(body.get("supplierName")).trim() : "");
        }
        item.setUpdatedAt(java.time.LocalDateTime.now(clock));
        inventoryItemRepository.save(item);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật mặt hàng", toInventoryItemMap(item)));
    }

    @PostMapping("/inventory/{id}/restock")
    public ResponseEntity<ApiResponse<Map<String, Object>>> restockInventoryItem(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        InventoryItem item = inventoryItemRepository.findById(id).orElse(null);
        if (item == null || !Boolean.TRUE.equals(item.getActive())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy mặt hàng."));
        }
        assertCourtAccess(myCourtIds, item.getCourtId());

        int quantity = ((Number) body.getOrDefault("quantity", 0)).intValue();
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số lượng nhập kho phải lớn hơn 0."));
        }
        int unitCost = body.containsKey("unitCost")
                ? Math.max(0, ((Number) body.get("unitCost")).intValue())
                : item.getImportPrice();
        String note = String.valueOf(body.getOrDefault("note", "Nhập thêm hàng vào kho")).trim();
        if (body.get("supplierName") != null) {
            String sup = String.valueOf(body.get("supplierName")).trim();
            if (!sup.isEmpty()) item.setSupplierName(sup);
        }

        item.setStockQuantity(item.getStockQuantity() + quantity);
        if (unitCost > 0) item.setImportPrice(unitCost);
        item.setUpdatedAt(java.time.LocalDateTime.now(clock));
        inventoryItemRepository.save(item);

        String fullNote = note.isEmpty() ? "Nhập thêm hàng" : note;
        if (item.getSupplierName() != null && !item.getSupplierName().isBlank()
                && !fullNote.toLowerCase().contains(item.getSupplierName().toLowerCase())) {
            fullNote = fullNote + " (NCC: " + item.getSupplierName() + ")";
        }

        InventoryTransaction tx = new InventoryTransaction(
                "tx_" + UUID.randomUUID().toString().substring(0, 8),
                item.getId(), item.getName(), item.getCourtId(),
                "IMPORT", quantity, item.getStockQuantity(), unitCost,
                fullNote, owner.getFullName()
        );
        inventoryTransactionRepository.save(tx);

        return ResponseEntity.ok(ApiResponse.success(
                "Đã nhập thêm " + quantity + " " + item.getUnit() + " " + item.getName(),
                toInventoryItemMap(item)
        ));
    }

    @PostMapping("/inventory/{id}/adjust")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adjustInventoryItem(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        InventoryItem item = inventoryItemRepository.findById(id).orElse(null);
        if (item == null || !Boolean.TRUE.equals(item.getActive())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy mặt hàng."));
        }
        assertCourtAccess(myCourtIds, item.getCourtId());

        int newStock = ((Number) body.getOrDefault("newStock", item.getStockQuantity())).intValue();
        if (newStock < 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số lượng tồn mới không được âm."));
        }
        int delta = newStock - item.getStockQuantity();
        String note = String.valueOf(body.getOrDefault("note", "Kiểm kê điều chỉnh tồn kho")).trim();

        item.setStockQuantity(newStock);
        item.setUpdatedAt(java.time.LocalDateTime.now(clock));
        inventoryItemRepository.save(item);

        if (delta != 0) {
            InventoryTransaction tx = new InventoryTransaction(
                    "tx_" + UUID.randomUUID().toString().substring(0, 8),
                    item.getId(), item.getName(), item.getCourtId(),
                    "ADJUST", delta, newStock, item.getImportPrice(),
                    note.isEmpty() ? "Kiểm kê điều chỉnh" : note, owner.getFullName()
            );
            inventoryTransactionRepository.save(tx);
        }

        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật kiểm kê kho", toInventoryItemMap(item)));
    }

    @PostMapping("/inventory/{id}/wastage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> wastageInventoryItem(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        InventoryItem item = inventoryItemRepository.findById(id).orElse(null);
        if (item == null || !Boolean.TRUE.equals(item.getActive())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy mặt hàng."));
        }
        assertCourtAccess(myCourtIds, item.getCourtId());

        int quantity = ((Number) body.getOrDefault("quantity", 0)).intValue();
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số lượng xuất hủy phải lớn hơn 0."));
        }
        if (item.getStockQuantity() < quantity) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Không đủ tồn kho để xuất hủy (chỉ còn " + item.getStockQuantity() + " " + item.getUnit() + ")."
            ));
        }
        String rawType = String.valueOf(body.getOrDefault("reasonType", "DAMAGE")).trim().toUpperCase();
        String reasonType = switch (rawType) {
            case "EXPIRED", "INTERNAL_USE" -> rawType;
            default -> "DAMAGE";
        };
        String defaultNote = switch (reasonType) {
            case "EXPIRED" -> "Hàng hết hạn sử dụng";
            case "INTERNAL_USE" -> "Xuất dùng nội bộ / Tài trợ giải đấu";
            default -> "Hàng hư hỏng / rơi vỡ";
        };
        String note = String.valueOf(body.getOrDefault("note", defaultNote)).trim();
        if (note.isEmpty()) note = defaultNote;

        int newStock = item.getStockQuantity() - quantity;
        item.setStockQuantity(newStock);
        item.setUpdatedAt(java.time.LocalDateTime.now(clock));
        inventoryItemRepository.save(item);

        InventoryTransaction tx = new InventoryTransaction(
                "tx_" + UUID.randomUUID().toString().substring(0, 8),
                item.getId(), item.getName(), item.getCourtId(),
                reasonType, -quantity, newStock, item.getImportPrice(),
                note, owner.getFullName()
        );
        inventoryTransactionRepository.save(tx);

        return ResponseEntity.ok(ApiResponse.success(
                "Đã xuất hủy " + quantity + " " + item.getUnit() + " " + item.getName(),
                toInventoryItemMap(item)
        ));
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/inventory/{id}/transfer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transferInventoryItem(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        InventoryItem source = inventoryItemRepository.findById(id).orElse(null);
        if (source == null || !Boolean.TRUE.equals(source.getActive())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy mặt hàng nguồn."));
        }
        assertCourtAccess(myCourtIds, source.getCourtId());

        String targetCourtId = String.valueOf(body.getOrDefault("targetCourtId", "")).trim();
        if (targetCourtId.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng chọn chi nhánh nhận hàng."));
        }
        if (targetCourtId.equals(source.getCourtId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Chi nhánh nhận phải khác chi nhánh xuất."));
        }
        assertCourtAccess(myCourtIds, targetCourtId);

        Court targetCourt = courtRepository.findById(targetCourtId).orElse(null);
        if (targetCourt == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không tìm thấy chi nhánh nhận: " + targetCourtId));
        }
        int quantity = ((Number) body.getOrDefault("quantity", 0)).intValue();
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số lượng chuyển kho phải lớn hơn 0."));
        }
        if (source.getStockQuantity() < quantity) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "Không đủ tồn kho để điều chuyển (chỉ còn " + source.getStockQuantity() + " " + source.getUnit() + ")."
            ));
        }
        String note = String.valueOf(body.getOrDefault("note", "")).trim();

        int newSourceStock = source.getStockQuantity() - quantity;
        source.setStockQuantity(newSourceStock);
        source.setUpdatedAt(java.time.LocalDateTime.now(clock));
        inventoryItemRepository.save(source);

        String outNote = "Chuyển sang " + targetCourt.getName() + (note.isEmpty() ? "" : " · " + note);
        inventoryTransactionRepository.save(new InventoryTransaction(
                "tx_" + UUID.randomUUID().toString().substring(0, 8),
                source.getId(), source.getName(), source.getCourtId(),
                "TRANSFER_OUT", -quantity, newSourceStock, source.getImportPrice(),
                outNote, owner.getFullName()
        ));

        List<InventoryItem> targetItems = inventoryItemRepository.findByCourtIdAndActiveTrue(targetCourtId);
        InventoryItem dest = null;
        String srcSku = source.getSku() != null ? source.getSku().trim() : "";
        for (InventoryItem candidate : targetItems) {
            if (!srcSku.isEmpty() && candidate.getSku() != null && srcSku.equalsIgnoreCase(candidate.getSku().trim())) {
                dest = candidate;
                break;
            }
            if (candidate.getName() != null && candidate.getName().equalsIgnoreCase(source.getName())) {
                dest = candidate;
                break;
            }
        }
        if (dest == null) {
            dest = new InventoryItem(
                    "inv_" + UUID.randomUUID().toString().substring(0, 8),
                    targetCourt.getId(), targetCourt.getName(),
                    source.getName(), source.getCategory(), source.getUnit(),
                    source.getImportPrice(), source.getSellPrice(),
                    quantity, source.getMinAlertThreshold()
            );
            dest.setSku(source.getSku());
            dest.setBarcode(source.getBarcode());
            dest.setSupplierName(source.getSupplierName());
        } else {
            dest.setStockQuantity(dest.getStockQuantity() + quantity);
        }
        dest.setUpdatedAt(java.time.LocalDateTime.now(clock));
        inventoryItemRepository.save(dest);

        String inNote = "Nhận chuyển từ " + source.getCourtName() + (note.isEmpty() ? "" : " · " + note);
        inventoryTransactionRepository.save(new InventoryTransaction(
                "tx_" + UUID.randomUUID().toString().substring(0, 8),
                dest.getId(), dest.getName(), dest.getCourtId(),
                "TRANSFER_IN", quantity, dest.getStockQuantity(), source.getImportPrice(),
                inNote, owner.getFullName()
        ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", toInventoryItemMap(source));
        result.put("target", toInventoryItemMap(dest));
        return ResponseEntity.ok(ApiResponse.success(
                "Đã điều chuyển " + quantity + " " + source.getUnit() + " " + source.getName() + " sang " + targetCourt.getName(),
                result
        ));
    }

    @GetMapping("/inventory/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventoryStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String courtId,
            @RequestParam(value = "period", required = false, defaultValue = "ALL") String period
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        String filterCourt = courtId != null && !courtId.isBlank() && !"ALL".equalsIgnoreCase(courtId)
                ? courtId.trim() : null;
        if (filterCourt != null) {
            assertCourtAccess(myCourtIds, filterCourt);
        }
        String normalizedPeriod = period != null ? period.trim().toUpperCase() : "ALL";

        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        YearMonth thisMonth = YearMonth.from(today);

        List<InventoryItem> allItems = inventoryItemRepository.findAll().stream()
                .filter(it -> myCourtIds.contains(it.getCourtId()))
                .toList();
        Map<String, InventoryItem> itemById = new HashMap<>();
        for (InventoryItem it : allItems) {
            itemById.put(it.getId(), it);
        }

        List<InventoryItem> activeItems = filterCourt != null
                ? inventoryItemRepository.findByCourtIdAndActiveTrueOrderByNameAsc(filterCourt)
                : inventoryItemRepository.findByActiveTrueOrderByCourtIdAscNameAsc().stream()
                        .filter(it -> myCourtIds.contains(it.getCourtId()))
                        .toList();

        List<InventoryTransaction> rawTxs = filterCourt != null
                ? inventoryTransactionRepository.findByCourtIdOrderByCreatedAtDesc(filterCourt)
                : inventoryTransactionRepository.findByOrderByCreatedAtDesc().stream()
                        .filter(tx -> myCourtIds.contains(tx.getCourtId()))
                        .toList();

        List<InventoryTransaction> txs = rawTxs.stream()
                .filter(tx -> {
                    LocalDate txDate = tx.getCreatedAt() != null ? tx.getCreatedAt().toLocalDate() : today;
                    return matchesPeriod(txDate, normalizedPeriod, today, weekStart, weekEnd, thisMonth);
                })
                .toList();

        long totalRevenue = 0;
        long totalCogs = 0;
        int totalSoldUnits = 0;
        long totalImportSpend = 0;
        int totalImportedUnits = 0;
        long totalWastageCost = 0;
        int totalWastageUnits = 0;
        int totalTransferCount = 0;

        Map<String, Long> soldQtyByItem = new HashMap<>();
        Map<String, Long> revenueByItem = new HashMap<>();
        Map<String, Long> profitByItem = new HashMap<>();
        Map<String, Long> revenueByCategory = new HashMap<>();
        Map<String, Long> soldUnitsByCategory = new HashMap<>();
        Map<String, Long> revenueByCourt = new HashMap<>();
        Map<String, Long> profitByCourt = new HashMap<>();

        for (InventoryTransaction tx : txs) {
            String type = tx.getType() != null ? tx.getType().toUpperCase() : "";
            int absQty = Math.abs(tx.getQuantityDelta() != null ? tx.getQuantityDelta() : 0);
            InventoryItem item = itemById.get(tx.getItemId());
            int importCost = item != null && item.getImportPrice() != null ? item.getImportPrice() : 0;
            String cat = item != null && item.getCategory() != null ? item.getCategory().toUpperCase() : "DRINK";

            switch (type) {
                case "SALE" -> {
                    int sellUnitPrice = tx.getUnitCost() != null && tx.getUnitCost() > 0
                            ? tx.getUnitCost()
                            : (item != null && item.getSellPrice() != null ? item.getSellPrice() : 0);
                    long rev = (long) absQty * sellUnitPrice;
                    // Dụng cụ (EQUIPMENT: bóng, áo bib) là tài sản cho thuê quay vòng, không tiêu hao giá vốn mỗi lượt
                    long cost = "EQUIPMENT".equals(cat) ? 0L : (long) absQty * importCost;
                    long prof = rev - cost;

                    totalRevenue += rev;
                    totalCogs += cost;
                    totalSoldUnits += absQty;

                    soldQtyByItem.merge(tx.getItemId(), (long) absQty, Long::sum);
                    revenueByItem.merge(tx.getItemId(), rev, Long::sum);
                    profitByItem.merge(tx.getItemId(), prof, Long::sum);
                    revenueByCategory.merge(cat, rev, Long::sum);
                    soldUnitsByCategory.merge(cat, (long) absQty, Long::sum);
                    revenueByCourt.merge(tx.getCourtId(), rev, Long::sum);
                    profitByCourt.merge(tx.getCourtId(), prof, Long::sum);
                }
                case "IMPORT" -> {
                    int unitCost = tx.getUnitCost() != null && tx.getUnitCost() > 0 ? tx.getUnitCost() : importCost;
                    totalImportSpend += (long) absQty * unitCost;
                    totalImportedUnits += absQty;
                }
                case "DAMAGE", "EXPIRED", "INTERNAL_USE" -> {
                    int unitCost = tx.getUnitCost() != null && tx.getUnitCost() > 0 ? tx.getUnitCost() : importCost;
                    totalWastageCost += (long) absQty * unitCost;
                    totalWastageUnits += absQty;
                }
                case "TRANSFER_OUT" -> totalTransferCount++;
                default -> {}
            }
        }

        long grossProfit = totalRevenue - totalCogs;
        double profitMarginPercent = totalRevenue > 0
                ? Math.round((grossProfit * 1000.0) / totalRevenue) / 10.0
                : 0.0;

        long totalStockValue = 0;
        long totalPotentialRevenue = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        Map<String, int[]> catStockCounts = new LinkedHashMap<>();
        catStockCounts.put("DRINK", new int[]{0, 0});
        catStockCounts.put("EQUIPMENT", new int[]{0, 0});
        catStockCounts.put("ACCESSORY", new int[]{0, 0});
        Map<String, Long> catStockValue = new HashMap<>();

        Map<String, Map<String, Object>> branchMap = new LinkedHashMap<>();
        List<Map<String, Object>> reorderSuggestions = new ArrayList<>();

        for (InventoryItem item : activeItems) {
            int qty = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
            int imp = item.getImportPrice() != null ? item.getImportPrice() : 0;
            int sell = item.getSellPrice() != null ? item.getSellPrice() : 0;
            long val = (long) qty * imp;
            totalStockValue += val;
            totalPotentialRevenue += (long) qty * sell;
            if (qty <= 0) outOfStockCount++;
            if (item.isLowStock()) lowStockCount++;

            String cat = item.getCategory() != null ? item.getCategory().toUpperCase() : "DRINK";
            int[] counts = catStockCounts.computeIfAbsent(cat, k -> new int[]{0, 0});
            counts[0] += 1;
            counts[1] += qty;
            catStockValue.merge(cat, val, Long::sum);

            Map<String, Object> bm = branchMap.computeIfAbsent(item.getCourtId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("courtId", item.getCourtId());
                m.put("courtName", item.getCourtName());
                m.put("itemCount", 0);
                m.put("stockUnits", 0);
                m.put("stockValue", 0L);
                m.put("lowStockCount", 0);
                m.put("revenue", revenueByCourt.getOrDefault(item.getCourtId(), 0L));
                m.put("profit", profitByCourt.getOrDefault(item.getCourtId(), 0L));
                return m;
            });
            bm.put("itemCount", (Integer) bm.get("itemCount") + 1);
            bm.put("stockUnits", (Integer) bm.get("stockUnits") + qty);
            bm.put("stockValue", (Long) bm.get("stockValue") + val);
            if (item.isLowStock()) {
                bm.put("lowStockCount", (Integer) bm.get("lowStockCount") + 1);
            }

            if (item.isLowStock() || qty <= 0) {
                int minAlert = item.getMinAlertThreshold() != null ? item.getMinAlertThreshold() : 10;
                int suggestedQty = Math.max(minAlert * 3 - qty, 12);
                Map<String, Object> sug = new LinkedHashMap<>();
                sug.put("itemId", item.getId());
                sug.put("name", item.getName());
                sug.put("courtId", item.getCourtId());
                sug.put("courtName", item.getCourtName());
                sug.put("unit", item.getUnit());
                sug.put("currentStock", qty);
                sug.put("minAlertThreshold", minAlert);
                sug.put("suggestedQty", suggestedQty);
                sug.put("estimatedCost", (long) suggestedQty * imp);
                reorderSuggestions.add(sug);
            }
        }

        reorderSuggestions.sort(Comparator.comparingInt(m -> (Integer) m.get("currentStock")));

        List<Map<String, Object>> topSelling = new ArrayList<>();
        List<Map.Entry<String, Long>> sortedSales = new ArrayList<>(revenueByItem.entrySet());
        sortedSales.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(5, sortedSales.size()); i++) {
            String itemId = sortedSales.get(i).getKey();
            InventoryItem item = itemById.get(itemId);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", itemId);
            row.put("itemName", item != null ? item.getName() : itemId);
            row.put("courtName", item != null ? item.getCourtName() : "");
            row.put("unit", item != null ? item.getUnit() : "sp");
            row.put("soldUnits", soldQtyByItem.getOrDefault(itemId, 0L));
            row.put("revenue", sortedSales.get(i).getValue());
            row.put("profit", profitByItem.getOrDefault(itemId, 0L));
            row.put("currentStock", item != null && item.getStockQuantity() != null ? item.getStockQuantity() : 0);
            topSelling.add(row);
        }

        List<Map<String, Object>> categoryBreakdown = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : catStockCounts.entrySet()) {
            String cat = entry.getKey();
            String label = switch (cat) {
                case "EQUIPMENT" -> "Dụng cụ";
                case "ACCESSORY" -> "Phụ kiện";
                default -> "Đồ uống";
            };
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("category", cat);
            cm.put("label", label);
            cm.put("itemCount", entry.getValue()[0]);
            cm.put("stockUnits", entry.getValue()[1]);
            cm.put("stockValue", catStockValue.getOrDefault(cat, 0L));
            cm.put("soldUnits", soldUnitsByCategory.getOrDefault(cat, 0L));
            cm.put("revenue", revenueByCategory.getOrDefault(cat, 0L));
            categoryBreakdown.add(cm);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("period", normalizedPeriod);
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalCogs", totalCogs);
        stats.put("grossProfit", grossProfit);
        stats.put("profitMarginPercent", profitMarginPercent);
        stats.put("totalSoldUnits", totalSoldUnits);
        stats.put("totalImportSpend", totalImportSpend);
        stats.put("totalImportedUnits", totalImportedUnits);
        stats.put("totalWastageCost", totalWastageCost);
        stats.put("totalWastageUnits", totalWastageUnits);
        stats.put("totalTransferCount", totalTransferCount);
        stats.put("totalStockValue", totalStockValue);
        stats.put("totalPotentialRevenue", totalPotentialRevenue);
        stats.put("lowStockCount", lowStockCount);
        stats.put("outOfStockCount", outOfStockCount);
        stats.put("topSelling", topSelling);
        stats.put("categoryBreakdown", categoryBreakdown);
        stats.put("branchBreakdown", new ArrayList<>(branchMap.values()));
        stats.put("reorderSuggestions", reorderSuggestions);

        return ResponseEntity.ok(ApiResponse.success("Thống kê kho hàng", stats));
    }

    @DeleteMapping("/inventory/{id}")
    public ResponseEntity<ApiResponse<String>> deleteInventoryItem(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User owner = requireOwner(authHeader);
        requireInventoryReady();
        Set<String> myCourtIds = requireOwnedCourtIds(owner);

        InventoryItem item = inventoryItemRepository.findById(id).orElse(null);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy mặt hàng."));
        }
        assertCourtAccess(myCourtIds, item.getCourtId());
        item.setActive(false);
        item.setUpdatedAt(java.time.LocalDateTime.now(clock));
        inventoryItemRepository.save(item);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa mặt hàng khỏi kho", id));
    }

    private static Map<String, Object> toInventoryItemMap(InventoryItem item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", item.getId());
        m.put("courtId", item.getCourtId());
        m.put("courtName", item.getCourtName());
        m.put("name", item.getName());
        m.put("category", item.getCategory());
        m.put("unit", item.getUnit());
        m.put("importPrice", item.getImportPrice());
        m.put("sellPrice", item.getSellPrice());
        m.put("stockQuantity", item.getStockQuantity());
        m.put("minAlertThreshold", item.getMinAlertThreshold());
        m.put("lowStock", item.isLowStock());
        m.put("sku", item.getSku() != null ? item.getSku() : "");
        m.put("barcode", item.getBarcode() != null ? item.getBarcode() : "");
        m.put("supplierName", item.getSupplierName() != null ? item.getSupplierName() : "");
        m.put("updatedAt", item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : "");
        return m;
    }

    private static Map<String, Object> toInventoryTxMap(InventoryTransaction tx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", tx.getId());
        m.put("itemId", tx.getItemId());
        m.put("itemName", tx.getItemName());
        m.put("courtId", tx.getCourtId());
        m.put("type", tx.getType());
        m.put("quantityDelta", tx.getQuantityDelta());
        m.put("stockAfter", tx.getStockAfter());
        m.put("unitCost", tx.getUnitCost());
        m.put("note", tx.getNote() != null ? tx.getNote() : "");
        m.put("createdBy", tx.getCreatedBy() != null ? tx.getCreatedBy() : "");
        m.put("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "");
        return m;
    }

    private static void populateAreaFromAddress(Court court, String address, String explicitCity, String explicitDistrict) {
        String city = explicitCity != null && !explicitCity.isEmpty() ? explicitCity : null;
        String district = explicitDistrict != null && !explicitDistrict.isEmpty() ? explicitDistrict : null;
        if (address != null && address.contains(",")) {
            String[] parts = address.split(",");
            if (city == null && parts.length >= 1) {
                String last = parts[parts.length - 1].trim().toLowerCase().replaceAll("[.\\s]", "");
                if (last.contains("hanoi") || last.contains("hànội")) city = "Hà Nội";
                else if (last.contains("hcm") || last.contains("tphcm") || last.contains("hochiminh") || last.contains("hồchíminh")) city = "TP. Hồ Chí Minh";
            }
            if (district == null && parts.length >= 2) {
                String distRaw = parts[parts.length - 2].trim();
                String stripped = distRaw.replaceFirst("(?i)^(?:quận\\s+|huyện\\s+|thị xã\\s+|thành phố\\s+|tp\\.?\\s*|[qh]\\.\\s*)", "").trim();
                if (!stripped.isEmpty()) {
                    district = stripped.chars().allMatch(Character::isDigit) ? "Quận " + stripped : stripped;
                }
            }
        }
        if (city != null) court.setCity(city);
        if (district != null) court.setDistrict(district);
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

    // =========================================================================
    // QUẢN LÝ YÊU CẦU LỊCH CỐ ĐỊNH (THỜI TIẾT / HOÀN VÍ / DUYỆT NGOẠI LỆ)
    // =========================================================================

    @GetMapping("/recurring-requests")
    public ResponseEntity<ApiResponse<List<SessionChangeRequest>>> getRecurringRequests(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User owner = requireOwner(authHeader);
        if (recurringBookingService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ lịch cố định chưa sẵn sàng.");
        }
        Set<String> myCourtIds = getCourtsForOwner(owner).stream().map(Court::getId).collect(Collectors.toSet());
        return ResponseEntity.ok(ApiResponse.success(
                "Danh sách yêu cầu lịch cố định",
                recurringBookingService.getChangeRequestsForOwnerCourts(myCourtIds)
        ));
    }

    @GetMapping("/recurring-sessions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecurringSessions(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User owner = requireOwner(authHeader);
        if (recurringBookingService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ lịch cố định chưa sẵn sàng.");
        }
        Set<String> myCourtIds = getCourtsForOwner(owner).stream().map(Court::getId).collect(Collectors.toSet());
        return ResponseEntity.ok(ApiResponse.success(
                "Danh sách buổi lịch cố định sắp diễn ra",
                recurringBookingService.getActiveRecurringSessionsForOwnerCourts(myCourtIds)
        ));
    }

    @PostMapping("/recurring-requests/{id}/resolve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveRecurringRequest(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RecurringDtos.OwnerResolveChangeRequest request
    ) {
        User owner = requireOwner(authHeader);
        if (recurringBookingService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ lịch cố định chưa sẵn sàng.");
        }
        Set<String> myCourtIds = getCourtsForOwner(owner).stream().map(Court::getId).collect(Collectors.toSet());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xử lý yêu cầu lịch cố định",
                recurringBookingService.ownerResolveChangeRequest(id, request, owner, myCourtIds)
        ));
    }

    @PostMapping("/recurring-sessions/{id}/weather-postpone")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ownerWeatherPostpone(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body
    ) {
        User owner = requireOwner(authHeader);
        if (recurringBookingService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ lịch cố định chưa sẵn sàng.");
        }
        Set<String> myCourtIds = getCourtsForOwner(owner).stream().map(Court::getId).collect(Collectors.toSet());
        String note = body != null ? body.get("ownerNote") : null;
        return ResponseEntity.ok(ApiResponse.success(
                "Đã hoãn buổi cố định do thời tiết",
                recurringBookingService.ownerProactiveWeatherPostpone(id, note, owner, myCourtIds)
        ));
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
