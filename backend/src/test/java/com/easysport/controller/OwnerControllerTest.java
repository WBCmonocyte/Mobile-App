package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.entity.Booking;
import com.easysport.entity.Court;
import com.easysport.entity.InventoryItem;
import com.easysport.entity.InventoryTransaction;
import com.easysport.entity.PosSale;
import com.easysport.entity.ShiftReport;
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

class OwnerControllerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T03:00:00Z"), ZONE);

    private UserRepository userRepository;
    private UserSessionRepository userSessionRepository;
    private BookingRepository bookingRepository;
    private CourtRepository courtRepository;
    private StaffShiftRepository staffShiftRepository;
    private ShiftReportRepository shiftReportRepository;
    private PosSaleRepository posSaleRepository;
    private InventoryItemRepository inventoryItemRepository;
    private InventoryTransactionRepository inventoryTransactionRepository;
    private OwnerController controller;

    private User owner;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userSessionRepository = mock(UserSessionRepository.class);
        bookingRepository = mock(BookingRepository.class);
        courtRepository = mock(CourtRepository.class);
        staffShiftRepository = mock(StaffShiftRepository.class);
        shiftReportRepository = mock(ShiftReportRepository.class);
        posSaleRepository = mock(PosSaleRepository.class);
        inventoryItemRepository = mock(InventoryItemRepository.class);
        inventoryTransactionRepository = mock(InventoryTransactionRepository.class);

        AuthService authService = new AuthService(userRepository, userSessionRepository, clock);
        ShiftPlanningService shiftPlanningService = new ShiftPlanningService(staffShiftRepository, userRepository, clock, false);

        controller = new OwnerController(
                authService,
                bookingRepository,
                courtRepository,
                staffShiftRepository,
                shiftReportRepository,
                posSaleRepository,
                userRepository,
                shiftPlanningService,
                inventoryItemRepository,
                inventoryTransactionRepository,
                clock
        );

        owner = new User("own1", "0988776655", "hash", "Trần Chủ Sân", "owner@es.vn", "HCM", "OWNER");
        UserSession session = new UserSession(
                "tok_owner",
                "own1",
                LocalDateTime.now(clock).minusHours(1),
                LocalDateTime.now(clock).plusHours(5)
        );
        when(userSessionRepository.findByToken("tok_owner")).thenReturn(Optional.of(session));
        when(userRepository.findById("own1")).thenReturn(Optional.of(owner));
        when(courtRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void updateCourtPricingUpdatesPricesCleanly() {
        Court court = new Court("c1", "Sân Thành Phát", "HCM", 4.8, 10, 1.0, 300000, 150000, 250000, 300000, "img");
        when(courtRepository.findById("c1")).thenReturn(Optional.of(court));

        ResponseEntity<ApiResponse<Court>> res = controller.updateCourtPricing(
                "c1",
                "Bearer tok_owner",
                Map.of("pricePerHour", 320000, "morningPrice", 180000, "afternoonPrice", 260000, "eveningPrice", 350000)
        );

        assertEquals(200, res.getStatusCode().value());
        Court updated = res.getBody().getData();
        assertEquals(320000, updated.getPricePerHour());
        assertEquals(180000, updated.getMorningPrice());
        assertEquals(260000, updated.getAfternoonPrice());
        assertEquals(350000, updated.getEveningPrice());
    }

    @Test
    void createStaffAccountHashesPasswordAndRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("new@es.vn")).thenReturn(false);
        when(userRepository.existsByPhone("0912345678")).thenReturn(false);

        ResponseEntity<ApiResponse<Map<String, Object>>> ok = controller.createStaffAccount(
                "Bearer tok_owner",
                Map.of("fullName", "NV Mới", "email", "new@es.vn", "phone", "0912345678", "password", "123456")
        );
        assertEquals(200, ok.getStatusCode().value());
        assertEquals("NV Mới", ok.getBody().getData().get("name"));

        when(userRepository.existsByEmail("dup@es.vn")).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.createStaffAccount(
                        "Bearer tok_owner",
                        Map.of("fullName", "NV Trùng", "email", "dup@es.vn", "phone", "0999888777", "password", "123456")
                )
        );
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void toggleStaffLockFlipsLockedState() {
        User staff = new User("s1", "0911", "hash", "NV A", "s1@es.vn", "HCM", "STAFF");
        staff.setLocked(false);
        when(userRepository.findById("s1")).thenReturn(Optional.of(staff));
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of());

        ResponseEntity<ApiResponse<Map<String, Object>>> locked = controller.toggleStaffLock("s1", "Bearer tok_owner");
        assertEquals(true, locked.getBody().getData().get("locked"));

        ResponseEntity<ApiResponse<Map<String, Object>>> unlocked = controller.toggleStaffLock("s1", "Bearer tok_owner");
        assertEquals(false, unlocked.getBody().getData().get("locked"));
    }

    @Test
    void rejectShiftReportMarksDiscrepancy() {
        ShiftReport report = new ShiftReport(
                "rep1", "c1", "Sân", "s1", "NV A", "24/11/2026", 200000, 0, 1, "Bàn giao", "PENDING", null
        );
        when(shiftReportRepository.findById("rep1")).thenReturn(Optional.of(report));

        ResponseEntity<ApiResponse<ShiftReport>> res = controller.rejectShiftReport(
                "rep1", "Bearer tok_owner", Map.of("note", "Thiếu 20k tiền mặt")
        );
        assertEquals("DISCREPANCY", res.getBody().getData().getStatus());
        assertEquals("Trần Chủ Sân", res.getBody().getData().getConfirmedBy());
        assertTrue(res.getBody().getData().getHandoverNote().contains("Thiếu 20k tiền mặt"));
    }

    @Test
    void overviewFiltersByTodayPeriodAndSplitsMixedChannelRevenue() {
        Court court = new Court("c1", "Sân Thành Phát", "HCM", 4.8, 10, 1.0, 300000, 150000, 250000, 300000, "img");
        Booking todaySplit = new Booking(
                "b_today", "u1", "c1", "Sân Thành Phát", "Sân 5",
                "08:00 - 09:00", "24/11/2026", 300000, 90000, 0, "QR_TRANSFER", "COMPLETED",
                "Khách A", "0901", "HCM", LocalDateTime.now(clock).plusHours(1)
        );
        Booking oldBooking = new Booking(
                "b_old", "u1", "c1", "Sân Thành Phát", "Sân 5",
                "08:00 - 09:00", "20/11/2026", 500000, 500000, 0, "CASH_ON_SITE", "COMPLETED",
                "Khách B", "0902", "HCM", LocalDateTime.now(clock).minusDays(4)
        );
        PosSale cashSettlement = new PosSale(
                "pos1", "c1", "Sân Thành Phát", "s1", "NV", "sh1",
                "Quyết toán #b_today", 210000, "CASH", "24/11/2026"
        );
        cashSettlement.setSettlementBookingId("b_today");

        when(courtRepository.findAll()).thenReturn(List.of(court));
        when(bookingRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(todaySplit, oldBooking));
        when(posSaleRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(cashSettlement));
        when(shiftReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        ResponseEntity<ApiResponse<Map<String, Object>>> todayRes = controller.getOverview("Bearer tok_owner", "TODAY");
        Map<String, Object> data = todayRes.getBody().getData();
        assertEquals(300000L, ((Number) data.get("totalRevenue")).longValue());
        assertEquals(90000L, ((Number) data.get("qrRevenue")).longValue());
        assertEquals(210000L, ((Number) data.get("cashRevenue")).longValue());
        assertEquals(1, ((Number) data.get("totalBookings")).intValue());
        assertEquals(300000L, ((Number) data.get("courtRevenue")).longValue());
        assertEquals(0L, ((Number) data.get("posRevenue")).longValue());

        List<?> fields = (List<?>) data.get("fieldRevenues");
        assertEquals(1, fields.size());
        List<?> slots = (List<?>) data.get("timeSlotRevenues");
        assertEquals(3, slots.size());
    }

    @Test
    void createCourtPersistsCourtWithSubFields() {
        ResponseEntity<ApiResponse<Court>> res = controller.createCourt(
                "Bearer tok_owner",
                Map.of(
                        "name", "Sân Mới Q7",
                        "address", "Quận 7, TP. HCM",
                        "sportType", "Cầu lông",
                        "fieldCount", 4,
                        "pricePerHour", 180000,
                        "morningPrice", 120000,
                        "afternoonPrice", 150000,
                        "eveningPrice", 180000
                )
        );
        assertEquals(200, res.getStatusCode().value());
        Court created = res.getBody().getData();
        assertEquals("Sân Mới Q7", created.getName());
        assertEquals(4, created.getFields().size());
        assertEquals("Sân 1", created.getFields().get(0).getLabel());
    }

    @Test
    void createCourtSavesCoordinatesAndFieldDetails() {
        ResponseEntity<ApiResponse<Court>> res = controller.createCourt(
                "Bearer tok_owner",
                Map.ofEntries(
                        Map.entry("name", "Sân Thảo Điền"),
                        Map.entry("address", "Thủ Đức, TP. HCM"),
                        Map.entry("sportType", "Bóng đá"),
                        Map.entry("fieldTypeLabel", "Sân 7 người"),
                        Map.entry("indoor", true),
                        Map.entry("latitude", 10.8055),
                        Map.entry("longitude", 106.7312),
                        Map.entry("fieldCount", 2),
                        Map.entry("pricePerHour", 400000),
                        Map.entry("morningPrice", 250000),
                        Map.entry("afternoonPrice", 350000),
                        Map.entry("eveningPrice", 450000)
                )
        );
        Court created = res.getBody().getData();
        assertEquals(10.8055, created.getLatitude());
        assertEquals(106.7312, created.getLongitude());
        assertTrue(created.getIndoor());
        assertEquals("Sân 7 người", created.getFields().get(0).getDetail());
        assertEquals("7_VS_7", created.getFields().get(0).getFieldType());
    }

    @Test
    void addAndDeleteCourtFieldWorksAndProtectsLastField() {
        Court court = new Court("c1", "Sân A", "HCM", 4.8, 10, 1.0, 300000, 150000, 250000, 300000, "img");
        when(courtRepository.findById("c1")).thenReturn(Optional.of(court));

        controller.addCourtField("c1", "Bearer tok_owner", Map.of("label", "Sân 1", "detail", "Sân 5 người"));
        controller.addCourtField("c1", "Bearer tok_owner", Map.of("label", "Sân 2", "detail", "Sân 5 người"));
        assertEquals(2, court.getFields().size());

        String firstFieldId = court.getFields().get(0).getId();
        String secondFieldId = court.getFields().get(1).getId();
        controller.deleteCourtField("c1", firstFieldId, "Bearer tok_owner");
        assertEquals(1, court.getFields().size());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.deleteCourtField("c1", secondFieldId, "Bearer tok_owner")
        );
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void updateStaffProfileAndResetPasswordWorks() {
        User staff = new User("s1", "0911000111", "oldhash", "NV Cũ", "s1@es.vn", "Chi nhánh cũ", "STAFF");
        when(userRepository.findById("s1")).thenReturn(Optional.of(staff));
        when(userRepository.existsByPhone("0999888777")).thenReturn(false);
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("s1")).thenReturn(List.of());

        ResponseEntity<ApiResponse<Map<String, Object>>> res = controller.updateStaffAccount(
                "s1",
                "Bearer tok_owner",
                Map.of("fullName", "NV Mới", "phone", "0999888777", "address", "Sân Thành Phát", "newPassword", "abcdef")
        );
        Map<String, Object> data = res.getBody().getData();
        assertEquals("NV Mới", data.get("name"));
        assertEquals("0999888777", data.get("phone"));
        assertEquals("Sân Thành Phát", data.get("address"));
        assertNotEquals("oldhash", staff.getPassword());
    }

    @Test
    void restockAndAdjustInventoryUpdatesStockAndLogsTransaction() {
        InventoryItem item = new InventoryItem(
                "inv_1", "c1", "Sân Thành Phát", "Revive", "DRINK", "chai", 8000, 15000, 10, 12
        );
        when(inventoryItemRepository.findById("inv_1")).thenReturn(Optional.of(item));

        ResponseEntity<ApiResponse<Map<String, Object>>> restockRes = controller.restockInventoryItem(
                "inv_1",
                "Bearer tok_owner",
                Map.of("quantity", 24, "unitCost", 8500, "note", "Nhập 1 thùng")
        );
        assertEquals(34, restockRes.getBody().getData().get("stockQuantity"));
        assertEquals(8500, item.getImportPrice());
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));

        ResponseEntity<ApiResponse<Map<String, Object>>> adjustRes = controller.adjustInventoryItem(
                "inv_1",
                "Bearer tok_owner",
                Map.of("newStock", 30, "note", "Kiểm kê cuối ngày (hỏng 4 chai)")
        );
        assertEquals(30, adjustRes.getBody().getData().get("stockQuantity"));
        verify(inventoryTransactionRepository, times(2)).save(any(InventoryTransaction.class));
    }

    @Test
    void wastageInventoryItemDeductsStockAndLogsTransaction() {
        InventoryItem item = new InventoryItem(
                "inv_w", "c1", "Sân Thành Phát", "Nước suối", "DRINK", "chai", 4000, 10000, 15, 5
        );
        when(inventoryItemRepository.findById("inv_w")).thenReturn(Optional.of(item));

        ResponseEntity<ApiResponse<Map<String, Object>>> okRes = controller.wastageInventoryItem(
                "inv_w",
                "Bearer tok_owner",
                Map.of("quantity", 3, "reasonType", "DAMAGE", "note", "Rơi vỡ 3 chai")
        );
        assertEquals(200, okRes.getStatusCode().value());
        assertEquals(12, okRes.getBody().getData().get("stockQuantity"));
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));

        ResponseEntity<ApiResponse<Map<String, Object>>> badRes = controller.wastageInventoryItem(
                "inv_w",
                "Bearer tok_owner",
                Map.of("quantity", 20, "reasonType", "EXPIRED")
        );
        assertEquals(400, badRes.getStatusCode().value());
    }

    @Test
    void transferInventoryItemMovesStockAcrossBranches() {
        InventoryItem src = new InventoryItem(
                "inv_src", "c1", "Sân Thành Phát", "Revive", "DRINK", "chai", 8500, 15000, 40, 10
        );
        src.setSku("REV-330");
        Court destCourt = new Court("c2", "Sân Hoàng Gia", "HCM", 4.9, 10, 1.0, 300000, 150000, 250000, 300000, "img");

        when(inventoryItemRepository.findById("inv_src")).thenReturn(Optional.of(src));
        when(courtRepository.findById("c2")).thenReturn(Optional.of(destCourt));
        when(inventoryItemRepository.findByCourtIdAndActiveTrue("c2")).thenReturn(List.of());
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<ApiResponse<Map<String, Object>>> sameBranchRes = controller.transferInventoryItem(
                "inv_src",
                "Bearer tok_owner",
                Map.of("targetCourtId", "c1", "quantity", 5)
        );
        assertEquals(400, sameBranchRes.getStatusCode().value());

        ResponseEntity<ApiResponse<Map<String, Object>>> okRes = controller.transferInventoryItem(
                "inv_src",
                "Bearer tok_owner",
                Map.of("targetCourtId", "c2", "quantity", 12, "note", "Chuyển hỗ trợ giải")
        );
        assertEquals(200, okRes.getStatusCode().value());
        assertEquals(28, src.getStockQuantity());
        verify(inventoryItemRepository, times(2)).save(any(InventoryItem.class));
        verify(inventoryTransactionRepository, times(2)).save(any(InventoryTransaction.class));
    }

    @Test
    void getInventoryStatsComputesRevenueProfitWastageAndSuggestions() {
        InventoryItem item1 = new InventoryItem(
                "inv_1", "c1", "Sân Thành Phát", "Revive", "DRINK", "chai", 8500, 15000, 40, 10
        );
        InventoryItem item2 = new InventoryItem(
                "inv_2", "c1", "Sân Thành Phát", "Redbull", "DRINK", "lon", 11000, 20000, 5, 10
        );
        InventoryTransaction saleTx = new InventoryTransaction(
                "tx_s1", "inv_1", "Revive", "c1", "SALE", -10, 40, 15000, "Bán POS", "NV"
        );
        saleTx.setCreatedAt(LocalDateTime.now(clock));
        InventoryTransaction dmgTx = new InventoryTransaction(
                "tx_d1", "inv_2", "Redbull", "c1", "DAMAGE", -2, 5, 11000, "Móp lon", "Owner"
        );
        dmgTx.setCreatedAt(LocalDateTime.now(clock));
        InventoryTransaction impTx = new InventoryTransaction(
                "tx_i1", "inv_1", "Revive", "c1", "IMPORT", 20, 50, 8500, "Nhập kho", "Owner"
        );
        impTx.setCreatedAt(LocalDateTime.now(clock));

        when(inventoryItemRepository.findAll()).thenReturn(List.of(item1, item2));
        when(inventoryItemRepository.findByActiveTrueOrderByCourtIdAscNameAsc()).thenReturn(List.of(item1, item2));
        when(inventoryTransactionRepository.findByOrderByCreatedAtDesc()).thenReturn(List.of(saleTx, dmgTx, impTx));

        ResponseEntity<ApiResponse<Map<String, Object>>> res = controller.getInventoryStats("Bearer tok_owner", null, "ALL");
        assertEquals(200, res.getStatusCode().value());
        Map<String, Object> stats = res.getBody().getData();
        assertEquals(150000L, ((Number) stats.get("totalRevenue")).longValue());
        assertEquals(85000L, ((Number) stats.get("totalCogs")).longValue());
        assertEquals(65000L, ((Number) stats.get("grossProfit")).longValue());
        assertEquals(170000L, ((Number) stats.get("totalImportSpend")).longValue());
        assertEquals(22000L, ((Number) stats.get("totalWastageCost")).longValue());
        assertEquals(1, ((Number) stats.get("lowStockCount")).intValue());

        List<?> suggestions = (List<?>) stats.get("reorderSuggestions");
        assertEquals(1, suggestions.size());
        List<?> topSelling = (List<?>) stats.get("topSelling");
        assertEquals(1, topSelling.size());
    }
}
