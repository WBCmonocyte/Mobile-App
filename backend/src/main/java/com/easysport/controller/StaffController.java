package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.entity.Booking;
import com.easysport.entity.Court;
import com.easysport.entity.CourtField;
import com.easysport.entity.CourtHoldSlot;
import com.easysport.entity.CourtResource;
import com.easysport.entity.InventoryItem;
import com.easysport.entity.InventoryTransaction;
import com.easysport.entity.PosSale;
import com.easysport.entity.ShiftReport;
import com.easysport.entity.StaffShift;
import com.easysport.entity.User;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.CourtFieldRepository;
import com.easysport.repository.CourtHoldSlotRepository;
import com.easysport.repository.CourtRepository;
import com.easysport.repository.CourtResourceRepository;
import com.easysport.repository.InventoryItemRepository;
import com.easysport.repository.InventoryTransactionRepository;
import com.easysport.repository.PosSaleRepository;
import com.easysport.repository.ShiftReportRepository;
import com.easysport.repository.StaffShiftRepository;
import com.easysport.service.AuthService;
import com.easysport.service.CourtHoldTransactionService;
import com.easysport.service.ShiftPlanningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}:\\d{2})");
    private static final Pattern EXTEND_PATTERN = Pattern.compile("Gia hạn thêm (\\d+) phút", Pattern.CASE_INSENSITIVE);

    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final PosSaleRepository posSaleRepository;
    private final ShiftPlanningService shiftPlanningService;
    private final Clock clock;
    private final CourtHoldSlotRepository courtHoldSlotRepository;
    private final CourtResourceRepository courtResourceRepository;
    private final CourtFieldRepository courtFieldRepository;
    private final CourtHoldTransactionService courtHoldTransactionService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private InventoryItemRepository inventoryItemRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private InventoryTransactionRepository inventoryTransactionRepository;

    public StaffController(AuthService authService,
                           BookingRepository bookingRepository,
                           CourtRepository courtRepository,
                           StaffShiftRepository staffShiftRepository,
                           ShiftReportRepository shiftReportRepository,
                           PosSaleRepository posSaleRepository,
                           ShiftPlanningService shiftPlanningService,
                           Clock clock) {
        this(authService, bookingRepository, courtRepository, staffShiftRepository,
                shiftReportRepository, posSaleRepository, shiftPlanningService, clock,
                null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public StaffController(AuthService authService,
                           BookingRepository bookingRepository,
                           CourtRepository courtRepository,
                           StaffShiftRepository staffShiftRepository,
                           ShiftReportRepository shiftReportRepository,
                           PosSaleRepository posSaleRepository,
                           ShiftPlanningService shiftPlanningService,
                           Clock clock,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) CourtHoldSlotRepository courtHoldSlotRepository,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) CourtResourceRepository courtResourceRepository,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) CourtFieldRepository courtFieldRepository,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) CourtHoldTransactionService courtHoldTransactionService) {
        this.authService = authService;
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.shiftReportRepository = shiftReportRepository;
        this.posSaleRepository = posSaleRepository;
        this.shiftPlanningService = shiftPlanningService;
        this.clock = clock;
        this.courtHoldSlotRepository = courtHoldSlotRepository;
        this.courtResourceRepository = courtResourceRepository;
        this.courtFieldRepository = courtFieldRepository;
        this.courtHoldTransactionService = courtHoldTransactionService;
    }

    private User requireStaffOrOwner(String authHeader) {
        User user = authService.authenticate(authHeader);
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn.");
        }
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "USER";
        if (!role.equals("STAFF") && !role.equals("OWNER")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Tài khoản không có quyền Nhân viên.");
        }
        return user;
    }

    private void requireActiveShiftIfStaff(User actor, String courtId) {
        if (actor == null || !"STAFF".equalsIgnoreCase(actor.getRole())) {
            return;
        }
        String todayStr = LocalDate.now(clock).format(DATE_FORMAT);
        List<StaffShift> myShifts = staffShiftRepository.findByStaffIdOrderByCreatedAtDesc(actor.getId());
        boolean hasActiveShift = myShifts.stream()
                .anyMatch(s -> "ACTIVE".equalsIgnoreCase(s.getStatus())
                        && todayStr.equals(s.getShiftDate())
                        && (courtId == null || courtId.isBlank()
                        || s.getCourtId() == null || s.getCourtId().isBlank()
                        || s.getCourtId().equals(courtId)));
        if (!hasActiveShift) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn chưa vào ca trực (hoặc ca trực đã kết thúc). Vui lòng chấm công vào ca để thao tác tại quầy."
            );
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookingsForStaff(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "scope", required = false, defaultValue = "ALL") String scope
    ) {
        User staff = requireStaffOrOwner(authHeader);
        List<Booking> list = bookingRepository.findAllByOrderByCreatedAtDesc();
        if ("SHIFT".equalsIgnoreCase(scope)) {
            Optional<StaffShift> currentShift = resolveCurrentOrTodayShift(staff.getId());
            if (currentShift.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("Hôm nay bạn chưa có ca trực", List.of()));
            }
            StaffShift shift = currentShift.get();
            List<Booking> filtered = list.stream()
                    .filter(b -> bookingMatchesShift(b, shift))
                    .toList();
            return ResponseEntity.ok(ApiResponse.success("Danh sách đơn đặt sân trong ca", filtered));
        }
        return ResponseEntity.ok(ApiResponse.success("Danh sách đơn đặt sân", list));
    }

    public List<String> resolveResourceIds(String courtId, String fieldDetail) {
        if (courtHoldTransactionService != null) {
            try {
                Court court = courtHoldTransactionService.resolveCourt(courtId);
                CourtField field = courtHoldTransactionService.resolveField(court, fieldDetail);
                return courtHoldTransactionService.resolvePhysicalResourceIds(court, field);
            } catch (Exception ignored) {
            }
        }
        Optional<Court> courtOpt = courtRepository.findById(courtId);
        if (courtOpt.isPresent()) {
            Court c = courtOpt.get();
            if (c.getFields() != null) {
                for (CourtField f : c.getFields()) {
                    if (f.getId().equalsIgnoreCase(fieldDetail) || f.getDetail().equalsIgnoreCase(fieldDetail) || f.getLabel().equalsIgnoreCase(fieldDetail)) {
                        if (f.getResourceKeys() != null && !f.getResourceKeys().isBlank()) {
                            return Arrays.stream(f.getResourceKeys().split(","))
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .collect(Collectors.toList());
                        }
                        return List.of(courtId + ":" + f.getId());
                    }
                }
            }
        }
        return List.of(courtId + ":" + fieldDetail);
    }

    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PostMapping("/bookings/walk-in")
    public ResponseEntity<ApiResponse<Booking>> createWalkInBooking(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User staff = requireStaffOrOwner(authHeader);
        String courtId = String.valueOf(body.getOrDefault("courtId", "court_thanh_phat"));
        requireActiveShiftIfStaff(staff, courtId);

        String fieldDetail = String.valueOf(body.getOrDefault("fieldDetail", "Sân 5 người"));
        String timeRange = String.valueOf(body.getOrDefault("timeRange", "18:00 - 19:30 (Đá ngay)"));
        String customerName = String.valueOf(body.getOrDefault("customerName", "Khách vãng lai")).trim();
        if (customerName.isEmpty()) customerName = "Khách vãng lai";
        String customerPhone = String.valueOf(body.getOrDefault("customerPhone", "0900000000")).trim();
        if (customerPhone.isEmpty()) customerPhone = "0900000000";
        int totalPrice = ((Number) body.getOrDefault("totalPrice", 300000)).intValue();
        if (totalPrice <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá thuê sân phải lớn hơn 0.");
        }
        String paymentMethod = String.valueOf(body.getOrDefault("paymentMethod", "CASH_ON_SITE"));
        int depositAmount = body.containsKey("depositAmount")
                ? ((Number) body.get("depositAmount")).intValue()
                : ("PAY_LATER".equalsIgnoreCase(paymentMethod) ? 0 : totalPrice);
        int remainingAmount = body.containsKey("remainingAmount")
                ? ((Number) body.get("remainingAmount")).intValue()
                : Math.max(0, totalPrice - depositAmount);

        String todayStr = LocalDate.now(clock).format(DATE_FORMAT);
        LocalTime[] requestedTimes = extractTimeInterval(timeRange);

        List<String> resourceIds = new ArrayList<>(resolveResourceIds(courtId, fieldDetail));
        Collections.sort(resourceIds);
        if (courtResourceRepository != null) {
            for (String resId : resourceIds) {
                if (!courtResourceRepository.existsById(resId)) {
                    courtResourceRepository.save(new CourtResource(resId, courtId, "Resource " + resId, "Auto-created"));
                }
            }
            courtResourceRepository.lockResourcesForUpdate(resourceIds);
        }

        int reqStartMin = requestedTimes != null ? requestedTimes[0].getHour() * 60 + requestedTimes[0].getMinute() : 0;
        int reqEndMin = requestedTimes != null ? requestedTimes[1].getHour() * 60 + requestedTimes[1].getMinute() : 1440;

        if (courtHoldSlotRepository != null) {
            List<CourtHoldSlot> activeHolds = courtHoldSlotRepository.findOverlappingActiveSlots(
                    resourceIds,
                    todayStr,
                    reqStartMin,
                    reqEndMin,
                    LocalDateTime.now(clock)
            );
            if (!activeHolds.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Sân " + fieldDetail + " hiện đang có khách giữ chỗ hoặc đặt online trong khung giờ này!"
                );
            }
        }

        List<Booking> existingBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        for (Booking existing : existingBookings) {
            if (!Objects.equals(courtId, existing.getCourtId())) continue;
            String st = existing.getStatus() != null ? existing.getStatus().toUpperCase() : "";
            if (!st.equals("PAID") && !st.equals("DEPOSIT_PAID") && !st.equals("CHECKED_IN") && !st.equals("PENDING")) {
                continue;
            }
            String existingDate = extractDate(existing.getDateLabel());
            if (!todayStr.equals(existingDate)) continue;
            if (!subFieldsOverlap(fieldDetail, existing.getFieldDetail())) continue;
            LocalTime[] existingTimes = extractTimeInterval(existing.getTimeRange());
            if (requestedTimes != null && existingTimes != null) {
                if (requestedTimes[0].isBefore(existingTimes[1]) && requestedTimes[1].isAfter(existingTimes[0])) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Sân " + fieldDetail + " đã có khách đặt hoặc đang đá trong khung giờ này!"
                    );
                }
            }
        }

        Optional<Court> courtOpt = courtRepository.findById(courtId);
        String courtName = courtOpt.map(Court::getName).orElse("Sân bóng Thành Phát");
        String address = courtOpt.map(Court::getAddress).orElse("12 Hoàng Hoa Thám, P.13, Q. Tân Bình, TP. HCM");

        String id = "WALK-" + (System.currentTimeMillis() % 1000000);

        Booking walkIn = new Booking(
                id,
                staff.getId(),
                courtId,
                courtName,
                fieldDetail,
                timeRange,
                "Hôm nay, " + todayStr,
                totalPrice,
                depositAmount,
                remainingAmount,
                paymentMethod,
                "CHECKED_IN",
                customerName,
                customerPhone,
                address,
                LocalDateTime.now(clock).plusHours(2)
        );
        bookingRepository.save(walkIn);

        if (courtHoldSlotRepository != null) {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime slotExpiry = now.plusDays(1);
            for (String resId : resourceIds) {
                CourtHoldSlot bookedSlot = new CourtHoldSlot(
                        UUID.randomUUID().toString().replace("-", ""),
                        "WALK-" + walkIn.getId(),
                        courtId,
                        resId,
                        todayStr,
                        reqStartMin,
                        reqEndMin,
                        timeRange,
                        "BOOKED",
                        slotExpiry
                );
                courtHoldSlotRepository.save(bookedSlot);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Đã tạo lượt thuê trực tiếp tại quầy", walkIn));
    }

    @PostMapping("/bookings/{id}/check-in")
    public ResponseEntity<ApiResponse<Booking>> checkInBooking(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User staff = requireStaffOrOwner(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn: " + id));
        requireActiveShiftIfStaff(staff, booking.getCourtId());
        String st = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "";
        if ("COMPLETED".equals(st) || "CANCELLED".equals(st)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã hoàn tất hoặc đã hủy, không thể nhận sân.");
        }
        booking.setStatus("CHECKED_IN");
        bookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.success("Check-in thành công", booking));
    }

    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    @PostMapping("/bookings/{id}/services")
    public ResponseEntity<ApiResponse<Booking>> addPosServices(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User staff = requireStaffOrOwner(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn: " + id));
        requireActiveShiftIfStaff(staff, booking.getCourtId());

        String note = String.valueOf(body.getOrDefault("serviceNote", "")).trim();
        int amount = ((Number) body.getOrDefault("serviceAmount", 0)).intValue();
        if (amount <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Số tiền dịch vụ phải lớn hơn 0."));
        }

        Matcher extMatcher = EXTEND_PATTERN.matcher(note);
        if (extMatcher.find()) {
            int extraMinutes = Integer.parseInt(extMatcher.group(1));
            if (extraMinutes > 0) {
                LocalTime[] bookingTimes = extractTimeInterval(booking.getTimeRange());
                String bookingDate = extractDate(booking.getDateLabel());
                if (bookingTimes != null && bookingDate != null) {
                    LocalTime oldEnd = bookingTimes[1];
                    LocalTime newEnd = oldEnd.plusMinutes(extraMinutes);
                    int extStartMin = oldEnd.getHour() * 60 + oldEnd.getMinute();
                    int extEndMin = newEnd.getHour() * 60 + newEnd.getMinute();
                    List<String> resIds = new ArrayList<>(resolveResourceIds(booking.getCourtId(), booking.getFieldDetail()));
                    Collections.sort(resIds);

                    if (courtResourceRepository != null) {
                        for (String rId : resIds) {
                            if (!courtResourceRepository.existsById(rId)) {
                                courtResourceRepository.save(new CourtResource(rId, booking.getCourtId(), "Resource " + rId, "Auto-created"));
                            }
                        }
                        courtResourceRepository.lockResourcesForUpdate(resIds);
                    }

                    if (courtHoldSlotRepository != null) {
                        List<CourtHoldSlot> activeHolds = courtHoldSlotRepository.findOverlappingActiveSlots(
                                resIds,
                                bookingDate,
                                extStartMin,
                                extEndMin,
                                LocalDateTime.now(clock)
                        );
                        if (!activeHolds.isEmpty()) {
                            throw new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "Không thể gia hạn vì khung giờ kế tiếp đã có khách giữ chỗ hoặc đặt online."
                            );
                        }
                    }

                    List<Booking> existingBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
                    for (Booking other : existingBookings) {
                        if (Objects.equals(booking.getId(), other.getId())) continue;
                        if (!Objects.equals(booking.getCourtId(), other.getCourtId())) continue;
                        String st = other.getStatus() != null ? other.getStatus().toUpperCase() : "";
                        if (!st.equals("PAID") && !st.equals("DEPOSIT_PAID") && !st.equals("CHECKED_IN") && !st.equals("PENDING")) {
                            continue;
                        }
                        if (!bookingDate.equals(extractDate(other.getDateLabel()))) continue;
                        if (!subFieldsOverlap(booking.getFieldDetail(), other.getFieldDetail())) continue;
                        LocalTime[] otherTimes = extractTimeInterval(other.getTimeRange());
                        if (otherTimes != null && otherTimes[0].isBefore(newEnd) && otherTimes[1].isAfter(oldEnd)) {
                            throw new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "Không thể gia hạn vì khung giờ kế tiếp đã có khách đặt sân này."
                            );
                        }
                    }
                    booking.setTimeRange(bookingTimes[0].format(TIME_FORMAT) + " - " + newEnd.format(TIME_FORMAT));

                    if (courtHoldSlotRepository != null) {
                        LocalDateTime now = LocalDateTime.now(clock);
                        for (String rId : resIds) {
                            CourtHoldSlot extSlot = new CourtHoldSlot(
                                    UUID.randomUUID().toString().replace("-", ""),
                                    "EXT-" + booking.getId(),
                                    booking.getCourtId(),
                                    rId,
                                    bookingDate,
                                    extStartMin,
                                    extEndMin,
                                    oldEnd.format(TIME_FORMAT) + " - " + newEnd.format(TIME_FORMAT),
                                    "BOOKED",
                                    now.plusDays(1)
                            );
                            courtHoldSlotRepository.save(extSlot);
                        }
                    }
                }
            }
        }

        deductInventoryForPos(body.get("items"), staff.getFullName(), "Gắn vào đơn sân " + booking.getId());

        String updatedNote = booking.getServiceNote().isEmpty()
                ? note
                : booking.getServiceNote() + ", " + note;
        booking.setServiceNote(updatedNote);
        booking.setServiceAmount(booking.getServiceAmount() + amount);
        booking.setRemainingAmount((booking.getRemainingAmount() != null ? booking.getRemainingAmount() : 0) + amount);
        bookingRepository.save(booking);

        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận dịch vụ POS vào sân", booking));
    }

    @PostMapping("/bookings/{id}/check-out")
    public ResponseEntity<ApiResponse<Booking>> checkOutBooking(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        User staff = requireStaffOrOwner(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn: " + id));
        requireActiveShiftIfStaff(staff, booking.getCourtId());
        String st = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "";
        if ("COMPLETED".equals(st) || "CANCELLED".equals(st)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn này đã hoàn tất hoặc đã hủy.");
        }
        String checkoutMethod = body != null && body.get("paymentMethod") != null
                ? String.valueOf(body.get("paymentMethod"))
                : "CASH";
        int due = booking.getRemainingAmount() != null ? booking.getRemainingAmount() : 0;
        boolean checkoutIsQr = isQrMethod(checkoutMethod);
        boolean origIsQr = isQrMethod(booking.getPaymentMethod());

        if ("PAY_LATER".equalsIgnoreCase(booking.getPaymentMethod())) {
            booking.setPaymentMethod(checkoutIsQr ? "QR_TRANSFER" : "CASH_ON_SITE");
            booking.setDepositAmount(booking.getTotalPrice() != null ? booking.getTotalPrice() : 0);
        } else if (due > 0 && booking.getDepositAmount() != null && booking.getDepositAmount() > 0 && checkoutIsQr != origIsQr) {
            Optional<StaffShift> currentShift = resolveCurrentOrTodayShift(staff.getId());
            PosSale settlement = new PosSale(
                    "pos_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                    booking.getCourtId(),
                    booking.getCourtName(),
                    staff.getId(),
                    staff.getFullName(),
                    currentShift.map(StaffShift::getId).orElse(null),
                    "Quyết toán #" + booking.getId() + " (" + booking.getCustomerName() + ")",
                    due,
                    checkoutIsQr ? "QR_TRANSFER" : "CASH",
                    LocalDate.now(clock).format(DATE_FORMAT)
            );
            settlement.setSettlementBookingId(booking.getId());
            posSaleRepository.save(settlement);
        } else {
            booking.setDepositAmount(booking.getTotalPrice() != null ? booking.getTotalPrice() : 0);
        }
        booking.setRemainingAmount(0);
        booking.setStatus("COMPLETED");
        booking.setPayment("COMPLETED");
        bookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.success("Đã trả sân & hoàn tất thanh toán", booking));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        User staff = requireStaffOrOwner(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đơn: " + id));
        requireActiveShiftIfStaff(staff, booking.getCourtId());
        String st = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "";
        if ("CHECKED_IN".equals(st) || "COMPLETED".equals(st) || "CANCELLED".equals(st)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể hủy đơn đang chờ nhận sân."
            );
        }
        String reason = body != null && body.get("reason") != null
                ? String.valueOf(body.get("reason")).trim()
                : "";
        if (reason.isEmpty()) reason = "Khách không đến (No-show)";
        booking.setStatus("CANCELLED");
        booking.setPayment("CANCELLED");
        booking.setRemainingAmount(0);
        String existingNote = booking.getServiceNote() != null ? booking.getServiceNote().trim() : "";
        booking.setServiceNote(existingNote.isEmpty() ? "Hủy: " + reason : existingNote + " · Hủy: " + reason);
        bookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn đặt sân", booking));
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/pos/retail")
    public ResponseEntity<ApiResponse<PosSale>> createRetailPosSale(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User staff = requireStaffOrOwner(authHeader);
        requireActiveShiftIfStaff(staff, null);
        String itemsNote = String.valueOf(body.getOrDefault("itemsNote", "")).trim();
        int amount = ((Number) body.getOrDefault("amount", 0)).intValue();
        if (amount <= 0 || itemsNote.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Món hàng và số tiền bán lẻ phải hợp lệ."));
        }
        String rawMethod = String.valueOf(body.getOrDefault("paymentMethod", "CASH"));
        String normalizedMethod = isQrMethod(rawMethod) ? "QR_TRANSFER" : "CASH";

        Optional<StaffShift> currentShift = resolveCurrentOrTodayShift(staff.getId());
        String courtId = body.get("courtId") != null && !String.valueOf(body.get("courtId")).isBlank()
                ? String.valueOf(body.get("courtId"))
                : currentShift.map(StaffShift::getCourtId).orElse("court_thanh_phat");
        String courtName = body.get("courtName") != null && !String.valueOf(body.get("courtName")).isBlank()
                ? String.valueOf(body.get("courtName"))
                : currentShift.map(StaffShift::getCourtName).orElse("Sân bóng Thành Phát");

        deductInventoryForPos(body.get("items"), staff.getFullName(), "Bán lẻ tại quầy POS");

        PosSale sale = new PosSale(
                "pos_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                courtId,
                courtName,
                staff.getId(),
                staff.getFullName(),
                currentShift.map(StaffShift::getId).orElse(null),
                itemsNote,
                amount,
                normalizedMethod,
                LocalDate.now(clock).format(DATE_FORMAT)
        );
        posSaleRepository.save(sale);
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận đơn bán lẻ quầy POS", sale));
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStaffInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String courtId
    ) {
        User staff = requireStaffOrOwner(authHeader);
        if (inventoryItemRepository == null) {
            return ResponseEntity.ok(ApiResponse.success("Danh sách kho trống", Collections.emptyList()));
        }
        String resolvedCourtId = (courtId != null && !courtId.isBlank())
                ? courtId.trim()
                : resolveCurrentOrTodayShift(staff.getId()).map(StaffShift::getCourtId).orElse("court_thanh_phat");

        List<InventoryItem> items = inventoryItemRepository.findByCourtIdAndActiveTrueOrderByNameAsc(resolvedCourtId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (InventoryItem item : items) {
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
            result.add(m);
        }
        return ResponseEntity.ok(ApiResponse.success("Kho hàng tại chi nhánh", result));
    }

    @SuppressWarnings("unchecked")
    private void deductInventoryForPos(Object rawItems, String staffName, String notePrefix) {
        if (inventoryItemRepository == null || !(rawItems instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        List<Map.Entry<InventoryItem, Integer>> deductions = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) continue;
            Object idObj = map.get("itemId");
            Object qtyObj = map.get("quantity");
            if (idObj == null || !(qtyObj instanceof Number qtyNum)) continue;
            String itemId = String.valueOf(idObj).trim();
            int qty = qtyNum.intValue();
            if (itemId.isEmpty() || qty <= 0) continue;

            InventoryItem item = inventoryItemRepository.findById(itemId).orElse(null);
            if (item == null || !Boolean.TRUE.equals(item.getActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mặt hàng không tồn tại trong kho: " + itemId);
            }
            if (item.getStockQuantity() < qty) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không đủ tồn kho cho \"" + item.getName() + "\" (chỉ còn " + item.getStockQuantity() + " " + item.getUnit() + ")."
                );
            }
            deductions.add(Map.entry(item, qty));
        }

        for (Map.Entry<InventoryItem, Integer> d : deductions) {
            InventoryItem item = d.getKey();
            int qty = d.getValue();
            int newStock = item.getStockQuantity() - qty;
            item.setStockQuantity(newStock);
            item.setUpdatedAt(LocalDateTime.now(clock));
            inventoryItemRepository.save(item);

            if (inventoryTransactionRepository != null) {
                InventoryTransaction tx = new InventoryTransaction(
                        "tx_" + UUID.randomUUID().toString().substring(0, 8),
                        item.getId(),
                        item.getName(),
                        item.getCourtId(),
                        "SALE",
                        -qty,
                        newStock,
                        item.getSellPrice(),
                        notePrefix,
                        staffName
                );
                inventoryTransactionRepository.save(tx);
            }
        }
    }

    @GetMapping("/pos/sales")
    public ResponseEntity<ApiResponse<List<PosSale>>> getMyRetailSales(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User staff = requireStaffOrOwner(authHeader);
        String todayStr = LocalDate.now(clock).format(DATE_FORMAT);
        List<PosSale> list = posSaleRepository.findByStaffIdAndSaleDateOrderByCreatedAtDesc(staff.getId(), todayStr);
        return ResponseEntity.ok(ApiResponse.success("Danh sách bán lẻ POS hôm nay", list));
    }

    @GetMapping("/shifts/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShiftSummary(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User staff = requireStaffOrOwner(authHeader);
        Optional<StaffShift> shiftOpt = resolveHandoverShift(staff.getId());
        String todayStr = LocalDate.now(clock).format(DATE_FORMAT);

        List<PosSale> retailSales = shiftOpt
                .map(s -> posSaleRepository.findByShiftIdOrderByCreatedAtDesc(s.getId()))
                .orElseGet(() -> posSaleRepository.findByStaffIdAndSaleDateOrderByCreatedAtDesc(staff.getId(), todayStr));
        Set<String> splitSettledIds = new HashSet<>();
        for (PosSale sale : retailSales) {
            if (sale.getSettlementBookingId() != null) {
                splitSettledIds.add(sale.getSettlementBookingId());
            }
        }
        for (PosSale sale : posSaleRepository.findAllByOrderByCreatedAtDesc()) {
            if (sale.getSettlementBookingId() != null) {
                splitSettledIds.add(sale.getSettlementBookingId());
            }
        }

        int cashFromBookings = 0;
        int qrFromBookings = 0;
        int orderCount = 0;
        int unpaidActiveCount = 0;
        int unpaidTotal = 0;

        if (shiftOpt.isPresent()) {
            StaffShift shift = shiftOpt.get();
            List<Booking> allBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
            for (Booking b : allBookings) {
                if (!bookingMatchesShift(b, shift)) continue;
                String st = b.getStatus() != null ? b.getStatus().toUpperCase() : "";
                if ("CANCELLED".equals(st)) continue;
                orderCount++;

                int paidCourt = 0;
                int paidService = 0;
                if ("PAID".equals(st) || "DEPOSIT_PAID".equals(st) || "CHECKED_IN".equals(st) || "COMPLETED".equals(st)) {
                    paidCourt = b.getDepositAmount() != null ? b.getDepositAmount() : 0;
                }
                if ("COMPLETED".equals(st) && !splitSettledIds.contains(b.getId())) {
                    paidService = b.getServiceAmount();
                }
                int collected = paidCourt + paidService;
                if (isQrMethod(b.getPaymentMethod())) {
                    qrFromBookings += collected;
                } else {
                    cashFromBookings += collected;
                }

                if ("CHECKED_IN".equals(st) && b.getRemainingAmount() != null && b.getRemainingAmount() > 0) {
                    unpaidActiveCount++;
                    unpaidTotal += b.getRemainingAmount();
                }
            }
        }

        int retailCash = 0;
        int retailQr = 0;
        for (PosSale sale : retailSales) {
            if (isQrMethod(sale.getPaymentMethod())) {
                retailQr += sale.getAmount();
            } else {
                retailCash += sale.getAmount();
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("shiftId", shiftOpt.map(StaffShift::getId).orElse(null));
        summary.put("cashFromBookings", cashFromBookings);
        summary.put("qrFromBookings", qrFromBookings);
        summary.put("retailCash", retailCash);
        summary.put("retailQr", retailQr);
        summary.put("totalCash", cashFromBookings + retailCash);
        summary.put("totalQr", qrFromBookings + retailQr);
        summary.put("orderCount", orderCount);
        summary.put("retailCount", retailSales.size());
        summary.put("unpaidActiveCount", unpaidActiveCount);
        summary.put("unpaidTotal", unpaidTotal);
        return ResponseEntity.ok(ApiResponse.success("Tổng hợp doanh thu ca trực", summary));
    }

    @GetMapping("/shifts/my")
    public ResponseEntity<ApiResponse<List<StaffShift>>> getMyShifts(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User staff = requireStaffOrOwner(authHeader);
        List<StaffShift> list = new ArrayList<>(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc(staff.getId()));
        list.sort((a, b) -> {
            LocalDate da = ShiftPlanningService.parseDateOrNull(a.getShiftDate());
            LocalDate db = ShiftPlanningService.parseDateOrNull(b.getShiftDate());
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            int cmp = da.compareTo(db);
            if (cmp != 0) return cmp;
            LocalTime[] ta = ShiftPlanningService.timesOf(a.getShiftSlot());
            LocalTime[] tb = ShiftPlanningService.timesOf(b.getShiftSlot());
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return ta[0].compareTo(tb[0]);
        });
        return ResponseEntity.ok(ApiResponse.success("Lịch làm việc cá nhân", list));
    }

    @PostMapping("/shifts/{id}/clock-in")
    public ResponseEntity<ApiResponse<StaffShift>> clockInShift(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User actor = requireStaffOrOwner(authHeader);
        StaffShift shift = shiftPlanningService.clockIn(actor, id);
        return ResponseEntity.ok(ApiResponse.success("Chấm công vào ca thành công", shift));
    }

    @PostMapping("/shifts/{id}/clock-out")
    public ResponseEntity<ApiResponse<StaffShift>> clockOutShift(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        User actor = requireStaffOrOwner(authHeader);
        String reason = body != null && body.get("reason") != null ? String.valueOf(body.get("reason")).trim() : null;
        StaffShift shift = shiftPlanningService.clockOut(actor, id, reason);
        return ResponseEntity.ok(ApiResponse.success("Đã kết thúc ca làm việc", shift));
    }

    @GetMapping("/reports/my")
    public ResponseEntity<ApiResponse<List<ShiftReport>>> getMyReports(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        User staff = requireStaffOrOwner(authHeader);
        return ResponseEntity.ok(ApiResponse.success(
                "Báo cáo bàn giao két của tôi",
                shiftReportRepository.findByStaffIdOrderByCreatedAtDesc(staff.getId())
        ));
    }

    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<ShiftReport>> submitShiftReport(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User staff = requireStaffOrOwner(authHeader);
        String requestedShiftId = body.get("shiftId") != null ? String.valueOf(body.get("shiftId")).trim() : "";
        StaffShift targetShift = resolveTargetShiftForReport(staff, requestedShiftId);

        if (targetShift == null || "UPCOMING".equalsIgnoreCase(targetShift.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bạn cần chấm công vào ca trước khi nộp báo cáo bàn giao két."
            );
        }

        if (shiftReportRepository.existsByShiftId(targetShift.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ca làm việc này đã được nộp báo cáo bàn giao két rồi. Không thể nộp trùng lặp."
            );
        }

        String note = String.valueOf(body.getOrDefault("handoverNote", "")).trim();
        if (note.isEmpty()) {
            note = "Đã kiểm đếm đủ tiền két và bàn giao trang thiết bị sân.";
        }

        if ("ACTIVE".equalsIgnoreCase(targetShift.getStatus())) {
            targetShift = shiftPlanningService.clockOut(staff, targetShift.getId(), note);
        }

        int cashAmount = ((Number) body.getOrDefault("cashAmount", 0)).intValue();
        int qrAmount = ((Number) body.getOrDefault("qrAmount", 0)).intValue();
        int totalOrders = ((Number) body.getOrDefault("totalOrders", 0)).intValue();
        String courtId = targetShift.getCourtId() != null && !targetShift.getCourtId().isBlank()
                ? targetShift.getCourtId()
                : String.valueOf(body.getOrDefault("courtId", "court_thanh_phat"));
        String courtName = targetShift.getCourtName() != null && !targetShift.getCourtName().isBlank()
                ? targetShift.getCourtName()
                : String.valueOf(body.getOrDefault("courtName", "Sân bóng Thành Phát"));

        String dateLabel = targetShift.getShiftDate() + " · " + targetShift.getShiftSlot()
                + " (Nộp lúc " + LocalTime.now(clock).format(TIME_FORMAT) + ")";

        ShiftReport report = new ShiftReport(
                "rep_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                courtId,
                courtName,
                staff.getId(),
                staff.getFullName(),
                dateLabel,
                cashAmount,
                qrAmount,
                totalOrders,
                note,
                "PENDING",
                null
        );
        report.setShiftId(targetShift.getId());
        shiftReportRepository.save(report);
        return ResponseEntity.ok(ApiResponse.success("Đã nộp báo cáo bàn giao két cho Chủ sân", report));
    }

    @PostMapping("/reports/{id}/resubmit")
    public ResponseEntity<ApiResponse<ShiftReport>> resubmitShiftReport(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body
    ) {
        User actor = requireStaffOrOwner(authHeader);
        ShiftReport report = shiftReportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo két"));
        if (!"OWNER".equalsIgnoreCase(actor.getRole()) && !Objects.equals(actor.getId(), report.getStaffId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải người nộp báo cáo này");
        }
        if (!"DISCREPANCY".equalsIgnoreCase(report.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể gửi giải trình lại cho báo cáo đang bị báo lệch.");
        }
        if (body.get("cashAmount") instanceof Number n) {
            report.setCashAmount(n.intValue());
        }
        if (body.get("qrAmount") instanceof Number n) {
            report.setQrAmount(n.intValue());
        }
        String note = String.valueOf(body.getOrDefault("handoverNote", "")).trim();
        if (note.isEmpty()) {
            note = "Đã kiểm đếm và đối soát lại tiền két.";
        }
        String oldNote = report.getHandoverNote() != null ? report.getHandoverNote().trim() : "";
        report.setHandoverNote(oldNote.isEmpty() ? "[NV giải trình: " + note + "]" : oldNote + " · [NV giải trình: " + note + "]");
        report.setStatus("PENDING");
        report.setConfirmedBy(null);
        shiftReportRepository.save(report);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi giải trình đối soát lại cho Chủ sân", report));
    }

    private StaffShift resolveTargetShiftForReport(User actor, String requestedShiftId) {
        if (!requestedShiftId.isEmpty() && !"null".equalsIgnoreCase(requestedShiftId)) {
            StaffShift shift = staffShiftRepository.findById(requestedShiftId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc: " + requestedShiftId));
            boolean isOwner = "OWNER".equalsIgnoreCase(actor.getRole());
            if (!isOwner && !Objects.equals(actor.getId(), shift.getStaffId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Bạn không phải nhân viên được phân ca này");
            }
            return shift;
        }
        String todayStr = LocalDate.now(clock).format(DATE_FORMAT);
        List<StaffShift> myShifts = staffShiftRepository.findByStaffIdOrderByCreatedAtDesc(actor.getId());
        return myShifts.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .findFirst()
                .or(() -> myShifts.stream()
                        .filter(s -> "COMPLETED".equalsIgnoreCase(s.getStatus()) && todayStr.equals(s.getShiftDate()))
                        .findFirst())
                .orElse(null);
    }

    private Optional<StaffShift> resolveCurrentOrTodayShift(String staffId) {
        String todayStr = LocalDate.now(clock).format(DATE_FORMAT);
        List<StaffShift> myShifts = staffShiftRepository.findByStaffIdOrderByCreatedAtDesc(staffId);
        return myShifts.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .findFirst()
                .or(() -> myShifts.stream()
                        .filter(s -> "COMPLETED".equalsIgnoreCase(s.getStatus()) && todayStr.equals(s.getShiftDate()))
                        .findFirst())
                .or(() -> myShifts.stream()
                        .filter(s -> "UPCOMING".equalsIgnoreCase(s.getStatus()) && todayStr.equals(s.getShiftDate()))
                        .findFirst());
    }

    public static boolean bookingMatchesShift(Booking booking, StaffShift shift) {
        if (booking == null || shift == null) return false;
        if (shift.getCourtId() != null && !shift.getCourtId().isBlank()
                && booking.getCourtId() != null && !booking.getCourtId().isBlank()
                && !shift.getCourtId().equals(booking.getCourtId())) {
            return false;
        }
        String bookingDate = extractDate(booking.getDateLabel());
        if (bookingDate == null || !bookingDate.equals(shift.getShiftDate())) {
            return false;
        }
        LocalTime[] shiftTimes = ShiftPlanningService.timesOf(shift.getShiftSlot());
        LocalTime[] bookingTimes = extractTimeInterval(booking.getTimeRange());
        if (shiftTimes != null && bookingTimes != null) {
            return bookingTimes[0].isBefore(shiftTimes[1]) && bookingTimes[1].isAfter(shiftTimes[0]);
        }
        return true;
    }

    public static String extractDate(String dateLabel) {
        if (dateLabel == null || dateLabel.isBlank()) return null;
        Matcher m = DATE_PATTERN.matcher(dateLabel);
        return m.find() ? m.group(1) : null;
    }

    public static LocalTime[] extractTimeInterval(String timeRange) {
        if (timeRange == null || timeRange.isBlank()) return null;
        Matcher m = TIME_PATTERN.matcher(timeRange);
        List<LocalTime> times = new ArrayList<>();
        while (m.find()) {
            try {
                times.add(LocalTime.parse(m.group(1), TIME_FORMAT));
            } catch (Exception ignored) {}
        }
        if (times.size() >= 2) {
            return new LocalTime[]{times.get(0), times.get(times.size() - 1)};
        }
        if (times.size() == 1) {
            return new LocalTime[]{times.get(0), times.get(0).plusHours(1)};
        }
        return null;
    }

    private Optional<StaffShift> resolveHandoverShift(String staffId) {
        String todayStr = LocalDate.now(clock).format(DATE_FORMAT);
        List<StaffShift> myShifts = staffShiftRepository.findByStaffIdOrderByCreatedAtDesc(staffId);
        return myShifts.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .findFirst()
                .or(() -> myShifts.stream()
                        .filter(s -> "COMPLETED".equalsIgnoreCase(s.getStatus()) && todayStr.equals(s.getShiftDate()))
                        .findFirst());
    }

    public static boolean subFieldsOverlap(String fieldA, String fieldB) {
        String a = fieldA != null ? fieldA.trim().toLowerCase() : "";
        String b = fieldB != null ? fieldB.trim().toLowerCase() : "";
        if (a.isEmpty() || b.isEmpty()) return true;
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    public static boolean isQrMethod(String method) {
        if (method == null) return false;
        String m = method.trim().toUpperCase();
        return m.contains("QR") || m.contains("VIETQR") || m.contains("TRANSFER");
    }
}
