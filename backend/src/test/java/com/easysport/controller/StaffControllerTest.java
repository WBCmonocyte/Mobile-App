package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.entity.Booking;
import com.easysport.entity.InventoryItem;
import com.easysport.entity.InventoryTransaction;
import com.easysport.entity.PosSale;
import com.easysport.entity.ShiftReport;
import com.easysport.entity.StaffShift;
import com.easysport.entity.User;
import com.easysport.entity.UserSession;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.CourtRepository;
import com.easysport.repository.InventoryItemRepository;
import com.easysport.repository.InventoryTransactionRepository;
import com.easysport.repository.PosSaleRepository;
import com.easysport.repository.ShiftReportRepository;
import com.easysport.repository.StaffShiftRepository;
import com.easysport.repository.UserRepository;
import com.easysport.repository.UserSessionRepository;
import com.easysport.service.AuthService;
import com.easysport.service.ShiftPlanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaffControllerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    // 24/11/2026 10:00 giờ VN (trong khung Ca Sáng 06:00 - 12:00)
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T03:00:00Z"), ZONE);

    private UserRepository userRepository;
    private UserSessionRepository userSessionRepository;
    private BookingRepository bookingRepository;
    private CourtRepository courtRepository;
    private StaffShiftRepository staffShiftRepository;
    private ShiftReportRepository shiftReportRepository;
    private PosSaleRepository posSaleRepository;
    private StaffController controller;

    private User staff;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userSessionRepository = mock(UserSessionRepository.class);
        bookingRepository = mock(BookingRepository.class);
        courtRepository = mock(CourtRepository.class);
        staffShiftRepository = mock(StaffShiftRepository.class);
        shiftReportRepository = mock(ShiftReportRepository.class);
        posSaleRepository = mock(PosSaleRepository.class);

        AuthService authService = new AuthService(userRepository, userSessionRepository, clock);
        ShiftPlanningService shiftPlanningService = new ShiftPlanningService(staffShiftRepository, userRepository, clock, false);

        controller = new StaffController(
                authService,
                bookingRepository,
                courtRepository,
                staffShiftRepository,
                shiftReportRepository,
                posSaleRepository,
                shiftPlanningService,
                clock
        );

        staff = new User("s1", "0911223344", "hash", "Trần Thị NV", "staff@es.vn", "HCM", "STAFF");
        UserSession session = new UserSession(
                "tok",
                "s1",
                LocalDateTime.now(clock).minusHours(1),
                LocalDateTime.now(clock).plusHours(5)
        );
        when(userSessionRepository.findByToken("tok")).thenReturn(Optional.of(session));
        when(userRepository.findById("s1")).thenReturn(Optional.of(staff));
        when(staffShiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(posSaleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void submitReportWithoutActiveOrCompletedShiftThrows409() {
        StaffShift upcoming = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "UPCOMING", null, null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(upcoming));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.submitShiftReport("Bearer tok", Map.of("cashAmount", 100000))
        );
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void submitReportOnActiveShiftSucceedsSetsShiftIdAndClocksOut() {
        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findById("sh1")).thenReturn(Optional.of(active));
        when(shiftReportRepository.existsByShiftId("sh1")).thenReturn(false);

        ResponseEntity<ApiResponse<ShiftReport>> res = controller.submitShiftReport(
                "Bearer tok",
                Map.of("shiftId", "sh1", "cashAmount", 300000, "qrAmount", 150000, "totalOrders", 2)
        );

        assertEquals(200, res.getStatusCode().value());
        ShiftReport body = res.getBody().getData();
        assertNotNull(body);
        assertEquals("sh1", body.getShiftId());
        assertEquals("court_thanh_phat", body.getCourtId());
        assertEquals("COMPLETED", active.getStatus());
        assertEquals("10:00", active.getCheckOutTime());
        verify(shiftReportRepository, times(1)).save(any(ShiftReport.class));
    }

    @Test
    void submitDuplicateReportForSameShiftThrows409() {
        StaffShift completed = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "COMPLETED", "06:00", "12:00", ""
        );
        when(staffShiftRepository.findById("sh1")).thenReturn(Optional.of(completed));
        when(shiftReportRepository.existsByShiftId("sh1")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.submitShiftReport("Bearer tok", Map.of("shiftId", "sh1", "cashAmount", 100000))
        );
        assertEquals(409, ex.getStatusCode().value());
        verify(shiftReportRepository, never()).save(any());
    }

    @Test
    void getBookingsScopedToShiftFiltersByCourtDateAndOverlappingTime() {
        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(active));

        Booking matchedMorning = new Booking(
                "b_match", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 5",
                "08:00 - 09:00", "24/11/2026", 300000, 300000, 0, "CASH_ON_SITE", "PAID",
                "Khách A", "0901", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        Booking matchedWalkInPrefix = new Booking(
                "b_walkin", "s1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 5",
                "10:00 - 11:30 (Đá ngay)", "Hôm nay, 24/11/2026", 300000, 300000, 0, "CASH_ON_SITE", "CHECKED_IN",
                "Khách B", "0902", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        Booking wrongTimeEvening = new Booking(
                "b_evening", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 5",
                "18:00 - 19:00", "24/11/2026", 300000, 300000, 0, "CASH_ON_SITE", "PAID",
                "Khách C", "0903", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        Booking wrongCourt = new Booking(
                "b_court", "u1", "court_other", "Sân Khác", "Sân 1",
                "08:00 - 09:00", "24/11/2026", 300000, 300000, 0, "CASH_ON_SITE", "PAID",
                "Khách D", "0904", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        Booking wrongDate = new Booking(
                "b_date", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 5",
                "08:00 - 09:00", "25/11/2026", 300000, 300000, 0, "CASH_ON_SITE", "PAID",
                "Khách E", "0905", "HCM", LocalDateTime.now(clock).plusHours(1)
        );

        when(bookingRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(matchedMorning, matchedWalkInPrefix, wrongTimeEvening, wrongCourt, wrongDate));

        ResponseEntity<ApiResponse<List<Booking>>> res = controller.getAllBookingsForStaff("Bearer tok", "SHIFT");
        List<Booking> result = res.getBody().getData();

        assertEquals(2, result.size());
        assertEquals(List.of("b_match", "b_walkin"), result.stream().map(Booking::getId).toList());
    }

    @Test
    void checkInPreservesRemainingAmountForDepositBookings() {
        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(active));

        Booking depositBooking = new Booking(
                "b_dep", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 7",
                "10:00 - 11:30", "24/11/2026", 300000, 90000, 210000, "QR_TRANSFER", "DEPOSIT_PAID",
                "Khách Cọc", "0909", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        when(bookingRepository.findById("b_dep")).thenReturn(Optional.of(depositBooking));

        ResponseEntity<ApiResponse<Booking>> res = controller.checkInBooking("b_dep", "Bearer tok");
        assertEquals(200, res.getStatusCode().value());
        assertEquals("CHECKED_IN", res.getBody().getData().getStatus());
        assertEquals(210000, res.getBody().getData().getRemainingAmount());
    }

    @Test
    void checkInOutsideShiftThrowsForbidden() {
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of());
        Booking waiting = new Booking(
                "b_wait", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 7",
                "10:00 - 11:30", "24/11/2026", 300000, 90000, 210000, "QR_TRANSFER", "DEPOSIT_PAID",
                "Khách", "0909", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        when(bookingRepository.findById("b_wait")).thenReturn(Optional.of(waiting));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.checkInBooking("b_wait", "Bearer tok")
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void walkInDoubleBookingSameCourtFieldAndOverlappingTimeThrows409() {
        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(active));

        Booking activeBooking = new Booking(
                "b_act", "s1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 7",
                "10:00 - 11:30 (Đá ngay)", "Hôm nay, 24/11/2026", 300000, 300000, 0, "CASH_ON_SITE", "CHECKED_IN",
                "Khách 1", "0901", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        when(bookingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(activeBooking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.createWalkInBooking("Bearer tok", Map.of(
                        "courtId", "court_thanh_phat",
                        "fieldDetail", "Sân 7",
                        "timeRange", "10:30 - 12:00",
                        "totalPrice", 300000
                ))
        );
        assertEquals(409, ex.getStatusCode().value());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void walkInBookingOutsideShiftThrowsForbidden() {
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.createWalkInBooking("Bearer tok", Map.of(
                        "courtId", "court_thanh_phat",
                        "fieldDetail", "Sân 7",
                        "timeRange", "10:30 - 12:00",
                        "totalPrice", 300000
                ))
        );
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("chưa vào ca trực"));
    }

    @Test
    void clockOutEarlyWithoutReasonThrowsBadRequestWhenEnforced() {
        ShiftPlanningService enforcedService = new ShiftPlanningService(staffShiftRepository, userRepository, clock, true);
        AuthService authService = new AuthService(userRepository, userSessionRepository, clock);
        StaffController enforcedController = new StaffController(
                authService, bookingRepository, courtRepository, staffShiftRepository,
                shiftReportRepository, posSaleRepository, enforcedService, clock
        );

        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findById("sh1")).thenReturn(Optional.of(active));

        // clock is 10:00, slot ends at 12:00 -> early by 120 minutes (> 15 mins) without reason
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                enforcedController.clockOutShift("sh1", "Bearer tok", Map.of())
        );
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("kết thúc ca sớm"));
    }

    @Test
    void clockOutEarlyWithReasonAppendsEarlyMinutesToNotes() {
        ShiftPlanningService enforcedService = new ShiftPlanningService(staffShiftRepository, userRepository, clock, true);
        AuthService authService = new AuthService(userRepository, userSessionRepository, clock);
        StaffController enforcedController = new StaffController(
                authService, bookingRepository, courtRepository, staffShiftRepository,
                shiftReportRepository, posSaleRepository, enforcedService, clock
        );

        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findById("sh1")).thenReturn(Optional.of(active));

        ResponseEntity<ApiResponse<StaffShift>> res = enforcedController.clockOutShift(
                "sh1", "Bearer tok", Map.of("reason", "Có việc gia đình gấp")
        );
        assertEquals(200, res.getStatusCode().value());
        StaffShift updated = res.getBody().getData();
        assertEquals("COMPLETED", updated.getStatus());
        assertEquals("10:00", updated.getCheckOutTime());
        assertTrue(updated.getNotes().contains("Về sớm 120 phút: Có việc gia đình gấp"));
    }

    @Test
    void createRetailPosSalePersistsSale() {
        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(active));

        ResponseEntity<ApiResponse<PosSale>> res = controller.createRetailPosSale(
                "Bearer tok",
                Map.of("itemsNote", "2x Revive", "amount", 30000, "paymentMethod", "Tiền mặt")
        );
        assertEquals(200, res.getStatusCode().value());
        PosSale saved = res.getBody().getData();
        assertNotNull(saved);
        assertEquals(30000, saved.getAmount());
        assertEquals("CASH", saved.getPaymentMethod());
        assertEquals("sh1", saved.getShiftId());
        verify(posSaleRepository, times(1)).save(any(PosSale.class));
    }

    @Test
    void cancelBookingSucceedsForWaitingAndRejectsCheckedIn() {
        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(active));

        Booking waiting = new Booking(
                "b_wait", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 7",
                "10:00 - 11:30", "24/11/2026", 300000, 90000, 210000, "QR_TRANSFER", "DEPOSIT_PAID",
                "Khách A", "0901", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        Booking playing = new Booking(
                "b_play", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 7",
                "10:00 - 11:30", "24/11/2026", 300000, 300000, 0, "CASH_ON_SITE", "CHECKED_IN",
                "Khách B", "0902", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        when(bookingRepository.findById("b_wait")).thenReturn(Optional.of(waiting));
        when(bookingRepository.findById("b_play")).thenReturn(Optional.of(playing));

        ResponseEntity<ApiResponse<Booking>> ok = controller.cancelBooking("b_wait", "Bearer tok", Map.of("reason", "No-show"));
        assertEquals("CANCELLED", ok.getBody().getData().getStatus());
        assertEquals(0, ok.getBody().getData().getRemainingAmount());

        ResponseStatusException err = assertThrows(ResponseStatusException.class, () ->
                controller.cancelBooking("b_play", "Bearer tok", Map.of())
        );
        assertEquals(409, err.getStatusCode().value());
    }

    @Test
    void checkOutMixedChannelCreatesPosSaleSettlementAndSplitsCashQrAccurately() {
        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(active));

        Booking splitBooking = new Booking(
                "b_split", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 7",
                "09:00 - 10:30", "24/11/2026", 300000, 90000, 210000, "QR_TRANSFER", "CHECKED_IN",
                "Khách Split", "0901", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        when(bookingRepository.findById("b_split")).thenReturn(Optional.of(splitBooking));

        controller.checkOutBooking("b_split", "Bearer tok", Map.of("paymentMethod", "CASH"));

        PosSale settlement = new PosSale(
                "pos_s", "court_thanh_phat", "Sân bóng Thành Phát", "s1", "Trần Thị NV", "sh1",
                "Quyết toán #b_split", 210000, "CASH", "24/11/2026"
        );
        settlement.setSettlementBookingId("b_split");
        when(posSaleRepository.findByShiftIdOrderByCreatedAtDesc("sh1")).thenReturn(List.of(settlement));
        when(posSaleRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(settlement));
        when(bookingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(splitBooking));

        Map<String, Object> summary = controller.getShiftSummary("Bearer tok").getBody().getData();
        assertEquals(90000, summary.get("totalQr"));
        assertEquals(210000, summary.get("totalCash"));
    }

    @Test
    void extendTimeOverlappingNextBookingThrows409() {
        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(active));

        Booking current = new Booking(
                "b1", "u1", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 7",
                "09:00 - 10:00", "24/11/2026", 300000, 300000, 0, "CASH_ON_SITE", "CHECKED_IN",
                "Khách 1", "0901", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        Booking next = new Booking(
                "b2", "u2", "court_thanh_phat", "Sân bóng Thành Phát", "Sân 7",
                "10:15 - 11:15", "24/11/2026", 300000, 300000, 0, "QR_TRANSFER", "PAID",
                "Khách 2", "0902", "HCM", LocalDateTime.now(clock).plusHours(2)
        );
        when(bookingRepository.findById("b1")).thenReturn(Optional.of(current));
        when(bookingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(current, next));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.addPosServices("b1", "Bearer tok", Map.of("serviceNote", "Gia hạn thêm 30 phút", "serviceAmount", 150000))
        );
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void resubmitDiscrepancyReportResetsStatusToPending() {
        ShiftReport report = new ShiftReport(
                "rep1", "court_thanh_phat", "Sân bóng Thành Phát", "s1", "Trần Thị NV",
                "24/11/2026", 200000, 0, 1, "Bàn giao · [Chủ sân: Thiếu 20k]", "DISCREPANCY", "Trần Chủ Sân"
        );
        when(shiftReportRepository.findById("rep1")).thenReturn(Optional.of(report));
        when(shiftReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<ApiResponse<ShiftReport>> res = controller.resubmitShiftReport(
                "rep1", "Bearer tok", Map.of("cashAmount", 220000, "handoverNote", "Đã bù đủ 20k tiền lẻ")
        );
        assertEquals(200, res.getStatusCode().value());
        ShiftReport updated = res.getBody().getData();
        assertEquals("PENDING", updated.getStatus());
        assertNull(updated.getConfirmedBy());
        assertEquals(220000, updated.getCashAmount());
        assertTrue(updated.getHandoverNote().contains("NV giải trình: Đã bù đủ 20k tiền lẻ"));
    }

    @Test
    void retailPosDeductsInventoryStockAndRejectsInsufficientOrCrossCourtItems() {
        InventoryItemRepository invRepo = mock(InventoryItemRepository.class);
        InventoryTransactionRepository txRepo = mock(InventoryTransactionRepository.class);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "inventoryItemRepository", invRepo);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "inventoryTransactionRepository", txRepo);

        StaffShift active = new StaffShift(
                "sh1", "s1", "Trần Thị NV", "0911223344", "court_thanh_phat", "Sân bóng Thành Phát",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", "06:00", null, ""
        );
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(active));

        InventoryItem revive = new InventoryItem(
                "inv_rev", "court_thanh_phat", "Sân bóng Thành Phát", "Revive", "DRINK", "chai", 8500, 15000, 5, 10
        );
        when(invRepo.findByCourtIdAndActiveTrueOrderByNameAsc("court_thanh_phat")).thenReturn(List.of(revive));
        when(invRepo.findById("inv_rev")).thenReturn(Optional.of(revive));
        when(invRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 1. Staff queries their own court inventory cleanly, blocked on foreign court
        ResponseEntity<ApiResponse<List<Map<String, Object>>>> invList = controller.getStaffInventory("Bearer tok", null);
        assertEquals(200, invList.getStatusCode().value());
        assertEquals(1, invList.getBody().getData().size());

        ResponseStatusException crossCourt = assertThrows(ResponseStatusException.class, () ->
                controller.getStaffInventory("Bearer tok", "court_hoang_gia"));
        assertEquals(403, crossCourt.getStatusCode().value());

        // 2. Retail POS sale of 2 Revive deducts stock from 5 -> 3 and logs SALE transaction
        ResponseEntity<ApiResponse<PosSale>> saleRes = controller.createRetailPosSale(
                "Bearer tok",
                Map.of(
                        "itemsNote", "2x Revive",
                        "amount", 30000,
                        "paymentMethod", "CASH",
                        "items", List.of(Map.of("itemId", "inv_rev", "quantity", 2))
                )
        );
        assertEquals(200, saleRes.getStatusCode().value());
        assertEquals(3, revive.getStockQuantity());
        verify(txRepo, times(1)).save(any(InventoryTransaction.class));

        // 3. Selling 4 when only 3 left throws 400 and does NOT mutate stock
        ResponseStatusException lowStockEx = assertThrows(ResponseStatusException.class, () ->
                controller.createRetailPosSale(
                        "Bearer tok",
                        Map.of(
                                "itemsNote", "4x Revive",
                                "amount", 60000,
                                "paymentMethod", "CASH",
                                "items", List.of(Map.of("itemId", "inv_rev", "quantity", 4))
                        )
                )
        );
        assertEquals(400, lowStockEx.getStatusCode().value());
        assertEquals(3, revive.getStockQuantity());
    }
}
