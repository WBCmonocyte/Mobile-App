package com.easysport;

import com.easysport.config.BookingPolicyProperties;
import com.easysport.config.SepayConfig;
import com.easysport.controller.OwnerController;
import com.easysport.controller.StaffController;
import com.easysport.dto.RecurringDtos;
import com.easysport.dto.SlotConflictException;
import com.easysport.dto.StartCheckoutRequest;
import com.easysport.entity.*;
import com.easysport.repository.*;
import com.easysport.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecurringAndWalletTest {

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant initial, ZoneId zone) {
            this.instant = initial;
            this.zone = zone;
        }

        void setInstant(Instant next) {
            this.instant = next;
        }

        void plusMinutes(long mins) {
            this.instant = this.instant.plusSeconds(mins * 60);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private MutableClock clock;
    private BookingPolicyProperties policy;
    private SepayConfig sepayConfig;

    private final Map<String, Court> courtStore = new ConcurrentHashMap<>();
    private final Map<String, CourtResource> resourceStore = new ConcurrentHashMap<>();
    private final Map<String, CourtHold> holdStore = new ConcurrentHashMap<>();
    private final Map<String, CourtHoldSlot> slotStore = new ConcurrentHashMap<>();
    private final Map<String, Booking> bookingStore = new ConcurrentHashMap<>();
    private final Map<String, Payment> paymentStore = new ConcurrentHashMap<>();
    private final Map<String, RecurringPackage> pkgStore = new ConcurrentHashMap<>();
    private final Map<String, RecurringSession> sessionStore = new ConcurrentHashMap<>();
    private final Map<String, RecurringCredit> creditStore = new ConcurrentHashMap<>();
    private final Map<String, SessionChangeRequest> reqStore = new ConcurrentHashMap<>();
    private final Map<String, UserWallet> walletStore = new ConcurrentHashMap<>();
    private final List<WalletTransaction> txStore = Collections.synchronizedList(new ArrayList<>());

    private CourtRepository courtRepository;
    private CourtFieldRepository fieldRepository;
    private CourtResourceRepository resourceRepository;
    private CourtHoldRepository holdRepository;
    private CourtHoldSlotRepository slotRepository;
    private BookingRepository bookingRepository;
    private PaymentRepository paymentRepository;
    private RecurringPackageRepository packageRepository;
    private RecurringSessionRepository sessionRepository;
    private RecurringCreditRepository creditRepository;
    private SessionChangeRequestRepository changeRequestRepository;
    private UserRepository userRepository;
    private UserWalletRepository walletRepository;
    private WalletTransactionRepository txRepository;
    private StaffShiftRepository staffShiftRepository;
    private ShiftReportRepository shiftReportRepository;
    private PosSaleRepository posSaleRepository;

    private NotificationService notificationService;
    private WalletService walletService;
    private SepayService sepayService;
    private CourtHoldTransactionService holdTransactionService;
    private CourtHoldService holdService;
    private RecurringBookingService recurringService;
    private OwnerController ownerController;
    private StaffController staffController;

    private Court court;
    private User customer;
    private User owner;
    private User staff;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(LocalDateTime.of(2026, 11, 24, 10, 0).atZone(BookingRules.ZONE).toInstant(), BookingRules.ZONE);
        policy = new BookingPolicyProperties();

        sepayConfig = new SepayConfig();
        sepayConfig.setMerchantId("TEST_MERCHANT");
        sepayConfig.setSecretKey("TEST_SECRET");
        sepayConfig.setIpnSecret("IPN_SECRET_KEY");

        courtStore.clear(); resourceStore.clear(); holdStore.clear(); slotStore.clear();
        bookingStore.clear(); paymentStore.clear(); pkgStore.clear(); sessionStore.clear();
        creditStore.clear(); reqStore.clear(); walletStore.clear(); txStore.clear();

        court = new Court("court1", "Sân Thành Phát", "123 Lê Lợi, Quận 1, TP. HCM", 4.8, 25, 1.2, 200000, 150000, 200000, 250000, "img");
        court.setOwnerId("owner1");
        CourtField f5a = new CourtField("f5a", court, "Sân 5A", "Sân 5A", "5_VS_5", "r5a");
        CourtField f5b = new CourtField("f5b", court, "Sân 5B", "Sân 5B", "5_VS_5", "r5b");
        CourtField f7a = new CourtField("f7a", court, "Sân 7A", "Sân 7A", "7_VS_7", "r5a,r5b");
        court.setFields(List.of(f5a, f5b, f7a));
        courtStore.put("court1", court);

        customer = new User();
        customer.setId("u1");
        customer.setFullName("Nguyễn Minh");
        customer.setPhone("0901111111");
        customer.setRole("USER");

        owner = new User();
        owner.setId("owner1");
        owner.setFullName("Chủ Sân 1");
        owner.setPhone("0909999999");
        owner.setRole("OWNER");

        staff = new User();
        staff.setId("staff1");
        staff.setFullName("Nhân Viên Quầy");
        staff.setPhone("0908888888");
        staff.setRole("STAFF");

        courtRepository = mock(CourtRepository.class);
        fieldRepository = mock(CourtFieldRepository.class);
        resourceRepository = mock(CourtResourceRepository.class);
        holdRepository = mock(CourtHoldRepository.class);
        slotRepository = mock(CourtHoldSlotRepository.class);
        bookingRepository = mock(BookingRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        packageRepository = mock(RecurringPackageRepository.class);
        sessionRepository = mock(RecurringSessionRepository.class);
        creditRepository = mock(RecurringCreditRepository.class);
        changeRequestRepository = mock(SessionChangeRequestRepository.class);
        userRepository = mock(UserRepository.class);
        walletRepository = mock(UserWalletRepository.class);
        txRepository = mock(WalletTransactionRepository.class);
        staffShiftRepository = mock(StaffShiftRepository.class);
        shiftReportRepository = mock(ShiftReportRepository.class);
        posSaleRepository = mock(PosSaleRepository.class);
        notificationService = mock(NotificationService.class);

        wireInMemoryRepos();

        walletService = new WalletService(walletRepository, txRepository, clock);
        sepayService = new SepayService(sepayConfig, paymentRepository, bookingRepository, courtRepository,
                holdRepository, slotRepository, clock, notificationService, packageRepository, sessionRepository);
        holdTransactionService = new CourtHoldTransactionService(
                courtRepository, fieldRepository, resourceRepository, holdRepository, slotRepository,
                bookingRepository, paymentRepository, sepayService, clock,
                walletService, packageRepository, sessionRepository, notificationService
        );
        holdService = new CourtHoldService(holdTransactionService, slotRepository, sepayConfig, clock);
        recurringService = new RecurringBookingService(
                courtRepository, resourceRepository, holdRepository, slotRepository, bookingRepository,
                paymentRepository, packageRepository, sessionRepository, creditRepository, changeRequestRepository,
                userRepository, holdTransactionService, sepayService, walletService, notificationService, policy, clock
        );

        AuthService authService = new AuthService(userRepository, mock(UserSessionRepository.class), clock) {
            @Override
            public User authenticate(String authHeader) {
                if ("Bearer owner1".equals(authHeader)) return owner;
                if ("Bearer staff1".equals(authHeader)) return staff;
                if ("Bearer u1".equals(authHeader)) return customer;
                return null;
            }
        };
        ShiftPlanningService planning = new ShiftPlanningService(staffShiftRepository, userRepository, clock, false);
        ownerController = new OwnerController(
                authService, bookingRepository, courtRepository, staffShiftRepository, shiftReportRepository,
                posSaleRepository, userRepository, planning, clock, packageRepository, recurringService
        );
        staffController = new StaffController(
                authService, bookingRepository, courtRepository, staffShiftRepository, shiftReportRepository,
                posSaleRepository, planning, clock, slotRepository, resourceRepository, fieldRepository,
                holdTransactionService, sessionRepository
        );
    }

    @SuppressWarnings("unchecked")
    private void wireInMemoryRepos() {
        when(courtRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(courtStore.get(inv.getArgument(0))));
        when(courtRepository.lockById(anyString())).thenAnswer(inv -> Optional.ofNullable(courtStore.get(inv.getArgument(0))));
        when(courtRepository.findAll()).thenAnswer(inv -> new ArrayList<>(courtStore.values()));

        when(resourceRepository.existsById(anyString())).thenAnswer(inv -> resourceStore.containsKey(inv.getArgument(0)));
        when(resourceRepository.save(any())).thenAnswer(inv -> {
            CourtResource r = inv.getArgument(0);
            resourceStore.put(r.getId(), r);
            return r;
        });

        when(holdRepository.save(any())).thenAnswer(inv -> {
            CourtHold h = inv.getArgument(0);
            holdStore.put(h.getId(), h);
            return h;
        });
        when(holdRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(holdStore.get(inv.getArgument(0))));
        when(holdRepository.findByIdWithLock(anyString())).thenAnswer(inv -> Optional.ofNullable(holdStore.get(inv.getArgument(0))));
        when(holdRepository.findByIdempotencyKey(anyString())).thenAnswer(inv ->
                holdStore.values().stream().filter(h -> Objects.equals(h.getIdempotencyKey(), inv.getArgument(0))).findFirst());
        when(holdRepository.findByBookingId(anyString())).thenAnswer(inv ->
                holdStore.values().stream().filter(h -> Objects.equals(h.getBookingId(), inv.getArgument(0))).findFirst());
        when(holdRepository.findByRecurringPackageId(anyString())).thenAnswer(inv ->
                holdStore.values().stream().filter(h -> Objects.equals(h.getRecurringPackageId(), inv.getArgument(0))).findFirst());
        when(holdRepository.findExpiredHolds(any())).thenAnswer(inv -> {
            LocalDateTime now = inv.getArgument(0);
            return holdStore.values().stream()
                    .filter(h -> List.of("HELD", "AWAITING_PAYMENT", "VERIFYING_PAYMENT").contains(h.getStatus()))
                    .filter(h -> (h.getVerificationDeadline() != null && !h.getVerificationDeadline().isAfter(now))
                            || (h.getVerificationDeadline() == null && !h.getExpiresAt().isAfter(now)))
                    .collect(Collectors.toList());
        });

        when(slotRepository.save(any())).thenAnswer(inv -> {
            CourtHoldSlot s = inv.getArgument(0);
            slotStore.put(s.getId(), s);
            return s;
        });
        when(slotRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            Iterable<CourtHoldSlot> it = inv.getArgument(0);
            List<CourtHoldSlot> out = new ArrayList<>();
            for (CourtHoldSlot s : it) {
                slotStore.put(s.getId(), s);
                out.add(s);
            }
            return out;
        });
        when(slotRepository.findByHoldId(anyString())).thenAnswer(inv ->
                slotStore.values().stream().filter(s -> Objects.equals(s.getHoldId(), inv.getArgument(0))).collect(Collectors.toList()));
        when(slotRepository.findByHoldIdAndDateLabel(anyString(), anyString())).thenAnswer(inv ->
                slotStore.values().stream()
                        .filter(s -> Objects.equals(s.getHoldId(), inv.getArgument(0)) && Objects.equals(s.getDateLabel(), inv.getArgument(1)))
                        .collect(Collectors.toList()));
        when(slotRepository.findOverlappingActiveSlots(anyList(), anyString(), anyInt(), anyInt(), any())).thenAnswer(inv -> {
            List<String> resIds = inv.getArgument(0);
            String date = inv.getArgument(1);
            int sMin = inv.getArgument(2);
            int eMin = inv.getArgument(3);
            LocalDateTime now = inv.getArgument(4);
            return slotStore.values().stream()
                    .filter(s -> resIds.contains(s.getResourceId()) && Objects.equals(date, s.getDateLabel()))
                    .filter(s -> "BOOKED".equals(s.getStatus()) || "CONFIRMED".equals(s.getStatus())
                            || (List.of("HELD", "AWAITING_PAYMENT", "VERIFYING_PAYMENT").contains(s.getStatus()) && s.getExpiresAt().isAfter(now)))
                    .filter(s -> sMin < s.getEndMinute() && s.getStartMinute() < eMin)
                    .collect(Collectors.toList());
        });

        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            bookingStore.put(b.getId(), b);
            return b;
        });
        when(bookingRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            List<Booking> out = new ArrayList<>();
            for (Booking b : (Iterable<Booking>) inv.getArgument(0)) {
                bookingStore.put(b.getId(), b);
                out.add(b);
            }
            return out;
        });
        when(bookingRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(bookingStore.get(inv.getArgument(0))));
        when(bookingRepository.findAllByOrderByCreatedAtDesc()).thenAnswer(inv -> new ArrayList<>(bookingStore.values()));
        when(bookingRepository.findByRecurringPackageId(anyString())).thenAnswer(inv ->
                bookingStore.values().stream().filter(b -> Objects.equals(b.getRecurringPackageId(), inv.getArgument(0))).collect(Collectors.toList()));

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            paymentStore.put(p.getId(), p);
            return p;
        });
        when(paymentRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(paymentStore.get(inv.getArgument(0))));
        when(paymentRepository.lockById(anyString())).thenAnswer(inv -> Optional.ofNullable(paymentStore.get(inv.getArgument(0))));
        when(paymentRepository.findByOrderInvoiceNumber(anyString())).thenAnswer(inv ->
                paymentStore.values().stream().filter(p -> Objects.equals(p.getOrderInvoiceNumber(), inv.getArgument(0))).findFirst());
        when(paymentRepository.findByCheckoutToken(anyString())).thenAnswer(inv ->
                paymentStore.values().stream().filter(p -> Objects.equals(p.getCheckoutToken(), inv.getArgument(0))).findFirst());
        when(paymentRepository.existsBySepayTransactionId(anyString())).thenAnswer(inv ->
                paymentStore.values().stream().anyMatch(p -> Objects.equals(p.getSepayTransactionId(), inv.getArgument(0))));
        when(paymentRepository.findByRecurringPackageIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv ->
                paymentStore.values().stream().filter(p -> Objects.equals(p.getRecurringPackageId(), inv.getArgument(0))).collect(Collectors.toList()));

        when(packageRepository.save(any())).thenAnswer(inv -> {
            RecurringPackage p = inv.getArgument(0);
            pkgStore.put(p.getId(), p);
            return p;
        });
        when(packageRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(pkgStore.get(inv.getArgument(0))));
        when(packageRepository.lockById(anyString())).thenAnswer(inv -> Optional.ofNullable(pkgStore.get(inv.getArgument(0))));
        when(packageRepository.findByIdempotencyKey(anyString())).thenAnswer(inv ->
                pkgStore.values().stream().filter(p -> Objects.equals(p.getIdempotencyKey(), inv.getArgument(0))).findFirst());
        when(packageRepository.findByUserIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv ->
                pkgStore.values().stream().filter(p -> Objects.equals(p.getUserId(), inv.getArgument(0))).collect(Collectors.toList()));
        when(packageRepository.findByCourtIdInOrderByCreatedAtDesc(anyList())).thenAnswer(inv -> {
            List<String> ids = inv.getArgument(0);
            return pkgStore.values().stream().filter(p -> ids.contains(p.getCourtId())).collect(Collectors.toList());
        });

        when(sessionRepository.save(any())).thenAnswer(inv -> {
            RecurringSession s = inv.getArgument(0);
            sessionStore.put(s.getId(), s);
            return s;
        });
        when(sessionRepository.saveAll(anyIterable())).thenAnswer(inv -> {
            List<RecurringSession> out = new ArrayList<>();
            for (RecurringSession s : (Iterable<RecurringSession>) inv.getArgument(0)) {
                sessionStore.put(s.getId(), s);
                out.add(s);
            }
            return out;
        });
        when(sessionRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(sessionStore.get(inv.getArgument(0))));
        when(sessionRepository.lockById(anyString())).thenAnswer(inv -> Optional.ofNullable(sessionStore.get(inv.getArgument(0))));
        when(sessionRepository.findByPackageIdOrderBySessionIndexAsc(anyString())).thenAnswer(inv ->
                sessionStore.values().stream()
                        .filter(s -> Objects.equals(s.getPackageId(), inv.getArgument(0)))
                        .sorted(Comparator.comparingInt(RecurringSession::getSessionIndex))
                        .collect(Collectors.toList()));
        when(sessionRepository.findByCourtIdInOrderBySessionIndexAsc(anyList())).thenAnswer(inv -> {
            List<String> ids = inv.getArgument(0);
            return sessionStore.values().stream()
                    .filter(s -> ids.contains(s.getCourtId()))
                    .sorted(Comparator.comparingInt(RecurringSession::getSessionIndex))
                    .collect(Collectors.toList());
        });

        when(creditRepository.save(any())).thenAnswer(inv -> {
            RecurringCredit c = inv.getArgument(0);
            creditStore.put(c.getId(), c);
            return c;
        });
        when(creditRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(creditStore.get(inv.getArgument(0))));
        when(creditRepository.lockById(anyString())).thenAnswer(inv -> Optional.ofNullable(creditStore.get(inv.getArgument(0))));
        when(creditRepository.findByPackageIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv ->
                creditStore.values().stream().filter(c -> Objects.equals(c.getPackageId(), inv.getArgument(0))).collect(Collectors.toList()));
        when(creditRepository.findByUserIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv ->
                creditStore.values().stream().filter(c -> Objects.equals(c.getUserId(), inv.getArgument(0))).collect(Collectors.toList()));

        when(changeRequestRepository.save(any())).thenAnswer(inv -> {
            SessionChangeRequest r = inv.getArgument(0);
            reqStore.put(r.getId(), r);
            return r;
        });
        when(changeRequestRepository.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(reqStore.get(inv.getArgument(0))));
        when(changeRequestRepository.lockById(anyString())).thenAnswer(inv -> Optional.ofNullable(reqStore.get(inv.getArgument(0))));
        when(changeRequestRepository.findByCourtIdInOrderByCreatedAtDesc(anyList())).thenAnswer(inv -> {
            List<String> ids = inv.getArgument(0);
            return reqStore.values().stream().filter(r -> ids.contains(r.getCourtId())).collect(Collectors.toList());
        });
        when(changeRequestRepository.findBySessionIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv ->
                reqStore.values().stream().filter(r -> Objects.equals(r.getSessionId(), inv.getArgument(0))).collect(Collectors.toList()));
        when(changeRequestRepository.findByPackageIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv ->
                reqStore.values().stream().filter(r -> Objects.equals(r.getPackageId(), inv.getArgument(0))).collect(Collectors.toList()));
        when(changeRequestRepository.countSelfCancellationsSince(anyString(), any())).thenAnswer(inv -> {
            String uid = inv.getArgument(0);
            LocalDateTime since = inv.getArgument(1);
            return reqStore.values().stream()
                    .filter(r -> Objects.equals(uid, r.getUserId())
                            && Boolean.TRUE.equals(r.getCountsTowardCancelLimit())
                            && !r.getCreatedAt().isBefore(since))
                    .count();
        });

        when(walletRepository.findByUserId(anyString())).thenAnswer(inv -> Optional.ofNullable(walletStore.get(inv.getArgument(0))));
        when(walletRepository.lockByUserId(anyString())).thenAnswer(inv -> Optional.ofNullable(walletStore.get(inv.getArgument(0))));
        when(walletRepository.save(any())).thenAnswer(inv -> {
            UserWallet w = inv.getArgument(0);
            walletStore.put(w.getUserId(), w);
            return w;
        });
        when(walletRepository.saveAndFlush(any())).thenAnswer(inv -> {
            UserWallet w = inv.getArgument(0);
            walletStore.put(w.getUserId(), w);
            return w;
        });

        when(txRepository.save(any())).thenAnswer(inv -> {
            WalletTransaction t = inv.getArgument(0);
            txStore.add(t);
            return t;
        });
        when(txRepository.findByUserIdOrderByCreatedAtDesc(anyString())).thenAnswer(inv ->
                txStore.stream().filter(t -> Objects.equals(t.getUserId(), inv.getArgument(0))).collect(Collectors.toList()));
        when(txRepository.existsByDedupKey(anyString())).thenAnswer(inv ->
                txStore.stream().anyMatch(t -> Objects.equals(t.getDedupKey(), inv.getArgument(0))));

        when(userRepository.findById("u1")).thenReturn(Optional.of(customer));
        when(staffShiftRepository.findByStaffIdOrderByCreatedAtDesc("staff1")).thenAnswer(inv -> {
            String today = LocalDate.now(clock).format(BookingRules.DATE);
            return List.of(new StaffShift(
                    "sh1", "staff1", "Nhân Viên Quầy", "0908888888",
                    "court1", "Sân Thành Phát", today,
                    "Ca Sáng (06:00 - 12:00)", "Lễ tân", "ACTIVE", null, null, ""
            ));
        });
        when(staffShiftRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(shiftReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(posSaleRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(posSaleRepository.findByStaffIdAndSaleDateOrderByCreatedAtDesc(anyString(), anyString())).thenReturn(List.of());
    }

    private Map<String, Object> buildSepayIpnPayload(String invoice, String txnId, int amount) {
        return Map.of(
                "notification_type", "ORDER_PAID",
                "order", Map.of(
                        "order_status", "CAPTURED",
                        "order_invoice_number", invoice,
                        "order_amount", amount,
                        "order_currency", "VND"
                ),
                "transaction", Map.of(
                        "id", txnId,
                        "transaction_type", "PAYMENT",
                        "transaction_status", "APPROVED",
                        "transaction_amount", amount,
                        "transaction_currency", "VND"
                )
        );
    }

    @Test
    void preview_generatesExactly12ConsecutiveTuesdaysCrossingNewYearAndIgnores13thOccurrence() {
        // 01/12/2026 là Thứ Ba; trong 3 tháng Dec->Feb có 13 ngày Thứ Ba, nhưng gói chỉ được sinh đúng 12 buổi
        RecurringDtos.PackagePreviewResponse preview = recurringService.previewPackage(
                new RecurringDtos.PreviewRequest("court1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"), "WEEKLY_12"),
                "u1"
        );

        assertTrue(preview.allAvailable());
        assertEquals(12, preview.totalSessions());
        assertEquals(12, preview.sessions().size());
        assertEquals("01/12/2026", preview.sessions().get(0).sessionDate());
        assertEquals("29/12/2026", preview.sessions().get(4).sessionDate());
        assertEquals("05/01/2027", preview.sessions().get(5).sessionDate()); // qua năm mới
        assertEquals("16/02/2027", preview.sessions().get(11).sessionDate()); // buổi thứ 12 (không lấy 23/02/2027)
        assertEquals(250000, preview.unitPrice()); // giá khung tối (>=17h): eveningPrice 250.000
        assertEquals(3_000_000, preview.totalPrice());
    }

    @Test
    void checkout_atomicRollbackWhenSession8HasConflict() {
        // Buổi #8 là 19/01/2027 đã có người đặt lẻ trên sân 5A (hoặc sân 7A dùng chung r5a)
        slotStore.put("busy-8", new CourtHoldSlot(
                "busy-8", "other-hold", "court1", "r5a", "19/01/2027",
                19 * 60, 20 * 60 + 30, "19:00 - 20:30", "CONFIRMED",
                LocalDateTime.now(clock).plusDays(90)
        ));

        SlotConflictException ex = assertThrows(SlotConflictException.class, () ->
                recurringService.startRecurringCheckout(
                        new RecurringDtos.RecurringCheckoutRequest(
                                "court1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                                "WEEKLY_12", "QR_TRANSFER", "Nguyễn Minh", "0901111111", "idem-conflict-8"
                        ),
                        "u1"
                )
        );

        assertEquals(1, ex.getConflictingSlots().size());
        assertEquals("19/01/2027", ex.getConflictingSlots().get(0).getDateLabel());
        assertEquals(8, ex.getConflictingSlots().get(0).getSessionIndex());

        assertTrue(pkgStore.isEmpty());
        assertTrue(sessionStore.isEmpty());
        assertTrue(holdStore.isEmpty());
        assertTrue(paymentStore.isEmpty());
        assertTrue(bookingStore.isEmpty());
        assertEquals(1, slotStore.size());
    }

    @Test
    void checkout_idempotencyAndPhysicalResourceContentionBlocksSingleAndWalkIn() {
        RecurringDtos.RecurringCheckoutRequest req = new RecurringDtos.RecurringCheckoutRequest(
                "court1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                "WEEKLY_12", "QR_TRANSFER", "Nguyễn Minh", "0901111111", "idem-pkg-1"
        );
        Map<String, Object> res1 = recurringService.startRecurringCheckout(req, "u1");
        Map<String, Object> res2 = recurringService.startRecurringCheckout(req, "u1");
        assertEquals(res1.get("packageId"), res2.get("packageId"));
        assertEquals(res1.get("holdId"), res2.get("holdId"));
        assertEquals(1, pkgStore.size());
        assertEquals(12, bookingStore.size());
        assertEquals(12, sessionStore.size());

        // Đặt sân lẻ 7A (ghép 5A+5B) cùng ngày đầu 01/12/2026 -> phải bị chặn vì r5a đang được giữ cho gói
        StartCheckoutRequest singleReq = new StartCheckoutRequest();
        singleReq.setCourtId("court1");
        singleReq.setFieldDetail("Sân 7A");
        singleReq.setDateLabel("01/12/2026");
        singleReq.setSlots(List.of("19:00 - 20:30"));
        singleReq.setPaymentMethod("QR_TRANSFER");
        singleReq.setCustomerName("Khách lẻ");
        singleReq.setCustomerPhone("0902222222");
        singleReq.setIdempotencyKey("single-overlap");
        assertThrows(SlotConflictException.class, () -> holdService.startCheckout(singleReq, "u2"));

        // Cho đồng hồ tới đúng ngày buổi 1 (01/12/2026 10:00) và nhận IPN kích hoạt gói -> walk-in tại quầy cũng bị chặn
        String invoice = (String) res1.get("orderInvoiceNumber");
        assertTrue(sepayService.processIpn("IPN_SECRET_KEY", buildSepayIpnPayload(invoice, "TX-01", 3_000_000)));
        clock.setInstant(LocalDateTime.of(2026, 12, 1, 10, 0).atZone(BookingRules.ZONE).toInstant());
        ResponseStatusException walkInEx = assertThrows(ResponseStatusException.class, () ->
                staffController.createWalkInBooking("Bearer staff1", Map.of(
                        "courtId", "court1",
                        "fieldDetail", "Sân 5A",
                        "timeRange", "19:00 - 20:30",
                        "totalPrice", 250000
                ))
        );
        assertEquals(HttpStatus.CONFLICT, walkInEx.getStatusCode());
    }

    @Test
    void sepayIpn_duplicateConfirmsOnce_andLateIpnAfterExpiryMovesToReviewWithoutStealingSlot() {
        Map<String, Object> res1 = recurringService.startRecurringCheckout(
                new RecurringDtos.RecurringCheckoutRequest(
                        "court1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                        "WEEKLY_12", "QR_TRANSFER", "Nguyễn Minh", "0901111111", "idem-dup-ipn"
                ),
                "u1"
        );
        String pkgId = (String) res1.get("packageId");
        String inv1 = (String) res1.get("orderInvoiceNumber");

        assertTrue(sepayService.processIpn("IPN_SECRET_KEY", buildSepayIpnPayload(inv1, "TX-DUP-1", 3_000_000)));
        assertTrue(sepayService.processIpn("IPN_SECRET_KEY", buildSepayIpnPayload(inv1, "TX-DUP-1", 3_000_000)));
        assertEquals("ACTIVE", pkgStore.get(pkgId).getStatus());
        assertEquals(3_000_000, pkgStore.get(pkgId).getTotalPaid());
        verify(notificationService, times(1)).createRecurringConfirmedNotif(
                eq("u1"), eq(pkgId), anyString(), anyString(), anyString(), anyString(), eq(12), anyString()
        );

        Map<String, Object> res2 = recurringService.startRecurringCheckout(
                new RecurringDtos.RecurringCheckoutRequest(
                        "court1", "Sân 5B", "01/12/2026", List.of("19:00 - 20:30"),
                        "WEEKLY_12", "QR_TRANSFER", "Nguyễn Minh", "0901111111", "idem-late-ipn"
                ),
                "u1"
        );
        String pkg2Id = (String) res2.get("packageId");
        String inv2 = (String) res2.get("orderInvoiceNumber");

        clock.plusMinutes(7);
        holdService.cleanupExpiredHolds();
        assertEquals("EXPIRED", pkgStore.get(pkg2Id).getStatus());

        StartCheckoutRequest rivalReq = new StartCheckoutRequest();
        rivalReq.setCourtId("court1");
        rivalReq.setFieldDetail("Sân 5B");
        rivalReq.setDateLabel("15/12/2026");
        rivalReq.setSlots(List.of("19:00 - 20:30"));
        rivalReq.setPaymentMethod("QR_TRANSFER");
        rivalReq.setCustomerName("Khách mới");
        rivalReq.setCustomerPhone("0903333333");
        rivalReq.setIdempotencyKey("rival-5b");
        var rivalHold = holdService.startCheckout(rivalReq, "u2");

        assertTrue(sepayService.processIpn("IPN_SECRET_KEY", buildSepayIpnPayload(inv2, "TX-LATE-2", 3_000_000)));
        assertEquals("REVIEW_REQUIRED", pkgStore.get(pkg2Id).getStatus());
        assertEquals("AWAITING_PAYMENT", holdStore.get(rivalHold.getHoldId()).getStatus());
    }

    @Test
    void earlyCancelAtExact168Hours_slidesSkippingOwnWeek6AndGoingPastWeek12_andHandlesConflict() {
        Map<String, Object> res = recurringService.startRecurringCheckout(
                new RecurringDtos.RecurringCheckoutRequest(
                        "court1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                        "WEEKLY_12", "QR_TRANSFER", "Nguyễn Minh", "0901111111", "idem-slide-pkg"
                ),
                "u1"
        );
        String pkgId = (String) res.get("packageId");
        sepayService.processIpn("IPN_SECRET_KEY", buildSepayIpnPayload((String) res.get("orderInvoiceNumber"), "TX-SLIDE", 3_000_000));

        // Buổi #5 là 29/12/2026 19:00; 168 giờ trước đó là 22/12/2026 19:00 (đúng mốc biên inclusive)
        List<RecurringSession> sessions = sessionStore.values().stream()
                .filter(s -> pkgId.equals(s.getPackageId()))
                .sorted(Comparator.comparingInt(RecurringSession::getSessionIndex))
                .toList();
        RecurringSession session5 = sessions.get(4);
        assertEquals("29/12/2026", session5.getSessionDate());

        // Chặn luôn tuần 13 (23/02/2027) để buộc đề xuất nhảy sang tuần 14 (02/03/2027)
        slotStore.put("week13-busy", new CourtHoldSlot(
                "week13-busy", "other", "court1", "r5a", "23/02/2027",
                19 * 60, 20 * 60 + 30, "19:00 - 20:30", "CONFIRMED",
                LocalDateTime.of(2027, 3, 10, 0, 0)
        ));

        clock.setInstant(LocalDateTime.of(2026, 12, 22, 19, 0).atZone(BookingRules.ZONE).toInstant());
        RecurringDtos.CancelPreviewResponse preview = recurringService.previewSessionAction(session5.getId(), "PERSONAL", "u1");
        assertTrue(preview.eligibleForEarlyCancel());
        assertTrue(preview.canSlide());
        assertEquals("02/03/2027", preview.proposedSlideDate());

        // Có người nhanh tay đặt mất 02/03/2027 trước khi khách bấm Xác nhận -> 409 CONFLICT
        slotStore.put("raced-slot", new CourtHoldSlot(
                "raced-slot", "racer", "court1", "r5a", "02/03/2027",
                19 * 60, 20 * 60 + 30, "19:00 - 20:30", "CONFIRMED",
                LocalDateTime.of(2027, 3, 20, 0, 0)
        ));
        ResponseStatusException raceEx = assertThrows(ResponseStatusException.class, () ->
                recurringService.cancelOrPostponeSession(
                        session5.getId(),
                        new RecurringDtos.CancelOrPostponeRequest("PERSONAL", "SLIDE", "02/03/2027", null),
                        "u1"
                )
        );
        assertEquals(HttpStatus.CONFLICT, raceEx.getStatusCode());
        assertEquals("UPCOMING", sessionStore.get(session5.getId()).getStatus());

        RecurringDtos.CancelPreviewResponse preview2 = recurringService.previewSessionAction(session5.getId(), "PERSONAL", "u1");
        assertEquals("09/03/2027", preview2.proposedSlideDate());
        Map<String, Object> slid = recurringService.cancelOrPostponeSession(
                session5.getId(),
                new RecurringDtos.CancelOrPostponeRequest("PERSONAL", "SLIDE", "09/03/2027", null),
                "u1"
        );
        assertEquals("RESCHEDULED", slid.get("outcome"));
        assertEquals("09/03/2027", sessionStore.get(session5.getId()).getRescheduledToDate());

        String holdId = pkgStore.get(pkgId).getHoldId();
        assertTrue(slotRepository.findByHoldIdAndDateLabel(holdId, "29/12/2026").stream().allMatch(s -> "RELEASED".equals(s.getStatus())));
        assertTrue(slotRepository.findByHoldIdAndDateLabel(holdId, "05/01/2027").stream().allMatch(s -> "CONFIRMED".equals(s.getStatus())));

        ResponseStatusException secondCancel = assertThrows(ResponseStatusException.class, () ->
                recurringService.previewSessionAction(session5.getId(), "PERSONAL", "u1")
        );
        assertEquals(HttpStatus.CONFLICT, secondCancel.getStatusCode());
    }

    @Test
    void unconfiguredNearStartRoutesToOwnerReview_andWeatherPostponeNeverCountsAsSelfCancel() {
        Map<String, Object> res = recurringService.startRecurringCheckout(
                new RecurringDtos.RecurringCheckoutRequest(
                        "court1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                        "WEEKLY_12", "QR_TRANSFER", "Nguyễn Minh", "0901111111", "idem-policy-pkg"
                ),
                "u1"
        );
        String pkgId = (String) res.get("packageId");
        sepayService.processIpn("IPN_SECRET_KEY", buildSepayIpnPayload((String) res.get("orderInvoiceNumber"), "TX-POL", 3_000_000));

        List<RecurringSession> sessions = sessionStore.values().stream()
                .filter(s -> pkgId.equals(s.getPackageId()))
                .sorted(Comparator.comparingInt(RecurringSession::getSessionIndex))
                .toList();
        RecurringSession s1 = sessions.get(0); // 01/12/2026 19:00
        RecurringSession s2 = sessions.get(1); // 08/12/2026 19:00
        RecurringSession s3 = sessions.get(2); // 15/12/2026 19:00

        // S1: Check-in tại quầy -> không thể hủy hay gửi yêu cầu hoãn
        staffController.checkInBooking(s1.getBookingId(), "Bearer staff1");
        assertEquals("CHECKED_IN", sessionStore.get(s1.getId()).getStatus());
        assertThrows(ResponseStatusException.class, () -> recurringService.previewSessionAction(s1.getId(), "PERSONAL", "u1"));

        // S2: Khách hủy muộn khi còn 24h, chính sách chưa cấu hình -> PENDING_OWNER_REVIEW
        clock.setInstant(LocalDateTime.of(2026, 12, 7, 19, 0).atZone(BookingRules.ZONE).toInstant());
        RecurringDtos.CancelPreviewResponse latePreview = recurringService.previewSessionAction(s2.getId(), "PERSONAL", "u1");
        assertTrue(latePreview.requiresOwnerApproval());
        assertFalse(latePreview.canAutoRefund());
        assertEquals(List.of("REQUEST_REVIEW"), latePreview.availableActions());

        Map<String, Object> lateSubmit = recurringService.cancelOrPostponeSession(
                s2.getId(), new RecurringDtos.CancelOrPostponeRequest("PERSONAL", "REQUEST_REVIEW", null, "Bận việc gấp"), "u1"
        );
        assertEquals("PENDING_OWNER_REVIEW", lateSubmit.get("outcome"));
        long countAfterPersonal = changeRequestRepository.countSelfCancellationsSince("u1", LocalDateTime.now(clock).minusDays(30));

        // S3: Thời tiết còn 1h trước trận -> Chủ sân duyệt -> KEEP_CREDIT -> KHÔNG tính self-cancel
        clock.setInstant(LocalDateTime.of(2026, 12, 15, 18, 0).atZone(BookingRules.ZONE).toInstant());
        Map<String, Object> weatherReq = recurringService.cancelOrPostponeSession(
                s3.getId(), new RecurringDtos.CancelOrPostponeRequest("WEATHER", "KEEP_CREDIT", null, "Mưa to ngập sân"), "u1"
        );
        assertEquals("WEATHER_PENDING", weatherReq.get("outcome"));
        String reqId = ((SessionChangeRequest) weatherReq.get("request")).getId();

        ownerController.resolveRecurringRequest(reqId, "Bearer owner1",
                new RecurringDtos.OwnerResolveChangeRequest("APPROVE_WEATHER", null, "Xác nhận mưa lớn"));
        assertEquals("WEATHER_APPROVED", sessionStore.get(s3.getId()).getStatus());

        Map<String, Object> creditRes = recurringService.cancelOrPostponeSession(
                s3.getId(), new RecurringDtos.CancelOrPostponeRequest("WEATHER", "KEEP_CREDIT", null, null), "u1"
        );
        assertEquals("CREDIT_GRANTED", creditRes.get("outcome"));

        assertEquals(countAfterPersonal, changeRequestRepository.countSelfCancellationsSince("u1", LocalDateTime.now(clock).minusDays(30)));
    }

    @Test
    void walletPaymentAndOwnerRefund_revenueNeverDoubleCountsPackageOrCountsWalletAsCash() {
        walletService.creditRefund("u1", 4_000_000, "Số dư ví ban đầu", null, null, null, "INIT_U1");

        // Khách mua gói 12 buổi x 250.000đ = 3.000.000đ bằng Ví EasySport
        Map<String, Object> res = recurringService.startRecurringCheckout(
                new RecurringDtos.RecurringCheckoutRequest(
                        "court1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                        "WEEKLY_12", "WALLET", "Nguyễn Minh", "0901111111", "idem-wallet-pkg"
                ),
                "u1"
        );
        String pkgId = (String) res.get("packageId");
        assertEquals("ACTIVE", res.get("packageStatus"));
        assertEquals(1_000_000, walletService.getOrCreateWallet("u1").getBalance());

        RecurringSession s4 = sessionStore.values().stream()
                .filter(s -> pkgId.equals(s.getPackageId()) && s.getSessionIndex() == 4)
                .findFirst().orElseThrow();
        Map<String, Object> refundReq = recurringService.cancelOrPostponeSession(
                s4.getId(), new RecurringDtos.CancelOrPostponeRequest("PERSONAL", "REFUND_WALLET", null, "Hoàn ví"), "u1"
        );
        String reqId = ((SessionChangeRequest) refundReq.get("request")).getId();
        ownerController.resolveRecurringRequest(reqId, "Bearer owner1",
                new RecurringDtos.OwnerResolveChangeRequest("APPROVE_REFUND", 150_000, "Đồng ý hoàn"));

        assertEquals(1_150_000, walletService.getOrCreateWallet("u1").getBalance());
        assertEquals(150_000, pkgStore.get(pkgId).getTotalRefunded());

        // Đặt thêm 1 đơn lẻ 250.000đ thanh toán bằng Ví
        StartCheckoutRequest singleWalletReq = new StartCheckoutRequest();
        singleWalletReq.setCourtId("court1");
        singleWalletReq.setFieldDetail("Sân 5B");
        singleWalletReq.setDateLabel("25/11/2026");
        singleWalletReq.setSlots(List.of("19:00 - 20:30"));
        singleWalletReq.setPaymentMethod("WALLET");
        singleWalletReq.setCustomerName("Nguyễn Minh");
        singleWalletReq.setCustomerPhone("0901111111");
        singleWalletReq.setIdempotencyKey("single-wallet-1");
        var singleRes = holdService.startCheckout(singleWalletReq, "u1");
        assertEquals("CONFIRMED", singleRes.getHoldStatus());
        assertEquals(900_000, walletService.getOrCreateWallet("u1").getBalance());

        // Owner Overview: Doanh thu = (3.000.000 - 150.000) gói + 250.000 đơn lẻ = 3.100.000đ, cashRevenue = 0
        Map<String, Object> overview = ownerController.getOverview("Bearer owner1", "ALL").getBody().getData();
        assertEquals(3_100_000L, overview.get("totalRevenue"));
        assertEquals(0L, overview.get("cashRevenue"));
        assertEquals(3_100_000L, overview.get("walletRevenue"));

        // Shift summary không tính WALLET / CREDIT / gói cố định vào tiền mặt két
        Map<String, Object> shiftSummary = staffController.getShiftSummary("Bearer staff1").getBody().getData();
        assertEquals(0, ((Number) shiftSummary.get("cashFromBookings")).intValue());
    }
}
