package com.easysport.controller;

import com.easysport.config.SepayConfig;
import com.easysport.dto.SlotConflictException;
import com.easysport.dto.StartCheckoutRequest;
import com.easysport.entity.*;
import com.easysport.repository.*;
import com.easysport.service.AuthService;
import com.easysport.service.CourtHoldService;
import com.easysport.service.CourtHoldTransactionService;
import com.easysport.service.SepayService;
import com.easysport.service.ShiftPlanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WalkInOnlineHoldSyncTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T03:00:00Z"), ZONE); // 10:00 AM VN

    private UserRepository userRepository;
    private UserSessionRepository userSessionRepository;
    private BookingRepository bookingRepository;
    private CourtRepository courtRepository;
    private StaffShiftRepository staffShiftRepository;
    private ShiftReportRepository shiftReportRepository;
    private PosSaleRepository posSaleRepository;
    private CourtHoldSlotRepository courtHoldSlotRepository;
    private CourtResourceRepository courtResourceRepository;
    private CourtFieldRepository courtFieldRepository;
    private CourtHoldRepository courtHoldRepository;
    private PaymentRepository paymentRepository;
    private SepayService sepayService;
    private CourtHoldTransactionService courtHoldTransactionService;
    private StaffController staffController;

    private Court court;
    private CourtField field;
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
        courtHoldSlotRepository = mock(CourtHoldSlotRepository.class);
        courtResourceRepository = mock(CourtResourceRepository.class);
        courtFieldRepository = mock(CourtFieldRepository.class);
        courtHoldRepository = mock(CourtHoldRepository.class);
        paymentRepository = mock(PaymentRepository.class);

        court = new Court("court1", "Sân Test", "123 Đường Test", 4.5, 1, 1.0, 300000, 150000, 250000, 300000, "img");
        field = new CourtField("f1", court, "Sân 5 người", "Sân 5A", "5_VS_5", "court1:f1");
        court.setFields(List.of(field));

        when(courtRepository.findById("court1")).thenReturn(Optional.of(court));
        when(courtRepository.lockById("court1")).thenReturn(Optional.of(court));
        when(courtResourceRepository.existsById("court1:f1")).thenReturn(true);

        sepayService = new SepayService(new SepayConfig(), paymentRepository, bookingRepository, courtRepository, courtHoldRepository, courtHoldSlotRepository, clock);

        courtHoldTransactionService = new CourtHoldTransactionService(
                courtRepository,
                courtFieldRepository,
                courtResourceRepository,
                courtHoldRepository,
                courtHoldSlotRepository,
                bookingRepository,
                paymentRepository,
                sepayService,
                clock
        );

        AuthService authService = new AuthService(userRepository, userSessionRepository, clock);
        ShiftPlanningService shiftPlanningService = new ShiftPlanningService(staffShiftRepository, userRepository, clock, false);

        staffController = new StaffController(
                authService,
                bookingRepository,
                courtRepository,
                staffShiftRepository,
                shiftReportRepository,
                posSaleRepository,
                shiftPlanningService,
                clock,
                courtHoldSlotRepository,
                courtResourceRepository,
                courtFieldRepository,
                courtHoldTransactionService
        );

        staff = new User("s1", "0911223344", "hash", "Nhân Viên Test", "nv@test.vn", "HN", "STAFF");
        UserSession session = new UserSession("tok", "s1", LocalDateTime.now(clock).minusHours(1), LocalDateTime.now(clock).plusHours(5));
        when(userSessionRepository.findByToken("tok")).thenReturn(Optional.of(session));
        when(userRepository.findById("s1")).thenReturn(Optional.of(staff));

        StaffShift activeShift = new StaffShift("sh1", "s1", "Nhân Viên Test", "0911223344", "court1", "Sân Test",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", null, null, "");
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of(activeShift));

        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(courtHoldSlotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void walkInBookingFailsWhenActiveOnlineHoldSlotExists() {
        // Giả lập đã có online hold slot 18:00 - 19:30 (1080 - 1170)
        CourtHoldSlot activeSlot = new CourtHoldSlot(
                "slot1", "hold1", "court1", "court1:f1", "24/11/2026",
                1080, 1170, "18:00 - 19:30", "AWAITING_PAYMENT", LocalDateTime.now(clock).plusMinutes(5)
        );
        when(courtHoldSlotRepository.findOverlappingActiveSlots(anyList(), eq("24/11/2026"), eq(1080), eq(1170), any()))
                .thenReturn(List.of(activeSlot));

        Map<String, Object> body = Map.of(
                "courtId", "court1",
                "fieldDetail", "Sân 5 người",
                "timeRange", "18:00 - 19:30",
                "totalPrice", 300000
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                staffController.createWalkInBooking("Bearer tok", body)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("đang có khách giữ chỗ hoặc đặt online"));
    }

    @Test
    void walkInBookingSucceedsAndRegistersBookedHoldSlot() {
        when(courtHoldSlotRepository.findOverlappingActiveSlots(anyList(), anyString(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());
        when(bookingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        Map<String, Object> body = Map.of(
                "courtId", "court1",
                "fieldDetail", "Sân 5 người",
                "timeRange", "18:00 - 19:30",
                "totalPrice", 300000
        );

        var response = staffController.createWalkInBooking("Bearer tok", body);
        assertNotNull(response.getBody());
        assertEquals("CHECKED_IN", response.getBody().getData().getStatus());

        // Verify that courtResource was locked
        verify(courtResourceRepository).lockResourcesForUpdate(List.of("court1:f1"));

        // Verify that a BOOKED hold slot was saved to prevent online booking
        verify(courtHoldSlotRepository).save(argThat(slot ->
                "BOOKED".equals(slot.getStatus()) &&
                "court1:f1".equals(slot.getResourceId()) &&
                slot.getStartMinute() == 1080 &&
                slot.getEndMinute() == 1170
        ));
    }

    @Test
    void onlineHoldFailsWhenWalkInBookingExistsInBookingRepository() {
        Booking walkIn = new Booking(
                "WALK-1", "s1", "court1", "Sân Test", "Sân 5 người",
                "19:00 - 20:30", "Hôm nay, 24/11/2026", 300000, 300000, 0,
                "CASH_ON_SITE", "CHECKED_IN", "Khách tại quầy", "0900000000", "123 Đường Test",
                LocalDateTime.now(clock).plusHours(2)
        );
        when(bookingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(walkIn));
        when(courtHoldSlotRepository.findOverlappingActiveSlots(anyList(), anyString(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        StartCheckoutRequest req = new StartCheckoutRequest();
        req.setCourtId("court1");
        req.setFieldDetail("Sân 5 người");
        req.setDateLabel("24/11/2026");
        req.setSlots(List.of("19:00 - 20:30"));
        req.setCustomerName("Online User");
        req.setCustomerPhone("0912345678");
        req.setPaymentMethod("QR_TRANSFER");
        req.setIdempotencyKey("idem1");

        assertThrows(SlotConflictException.class, () ->
                courtHoldTransactionService.acquireHoldTransaction(req, "u1")
        );
    }
}
