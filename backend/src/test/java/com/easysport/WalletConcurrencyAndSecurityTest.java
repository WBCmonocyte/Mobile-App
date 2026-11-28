package com.easysport;

import com.easysport.config.BookingPolicyProperties;
import com.easysport.config.SepayConfig;
import com.easysport.controller.OwnerController;
import com.easysport.controller.WalletController;
import com.easysport.dto.RecurringDtos;
import com.easysport.dto.SlotConflictException;
import com.easysport.entity.*;
import com.easysport.repository.*;
import com.easysport.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WalletConcurrencyAndSecurityTest {

    private Clock clock;
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

    // Mô phỏng PESSIMISTIC_WRITE row-lock của InnoDB giữ khóa tới cuối mỗi operation thread
    private final Map<String, ReentrantLock> rowLocks = new ConcurrentHashMap<>();
    private final ThreadLocal<List<ReentrantLock>> heldLocks = ThreadLocal.withInitial(ArrayList::new);

    private void acquireRowLock(String key) {
        ReentrantLock lock = rowLocks.computeIfAbsent(key, k -> new ReentrantLock(true));
        if (!lock.isHeldByCurrentThread()) {
            lock.lock();
            heldLocks.get().add(lock);
        }
    }

    private void releaseThreadLocks() {
        List<ReentrantLock> locks = heldLocks.get();
        for (int i = locks.size() - 1; i >= 0; i--) {
            ReentrantLock lock = locks.get(i);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        locks.clear();
    }

    private WalletService walletService;
    private RecurringBookingService recurringService;
    private SepayService sepayService;
    private WalletController walletController;
    private OwnerController ownerController;

    private User u1;
    private User u2;
    private User owner1;
    private User owner2;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(LocalDateTime.of(2026, 11, 24, 10, 0).atZone(BookingRules.ZONE).toInstant(), BookingRules.ZONE);
        courtStore.clear(); resourceStore.clear(); holdStore.clear(); slotStore.clear();
        bookingStore.clear(); paymentStore.clear(); pkgStore.clear(); sessionStore.clear();
        creditStore.clear(); reqStore.clear(); walletStore.clear(); txStore.clear(); rowLocks.clear();

        Court c1 = new Court("c1", "Sân 1", "Đường 1", 4.5, 5, 1.0, 200000, 150000, 200000, 250000, "img");
        c1.setOwnerId("owner1");
        c1.setFields(List.of(new CourtField("f1", c1, "Sân 5A", "Sân 5A", "5_VS_5", "c1:f1")));
        courtStore.put("c1", c1);

        Court c2 = new Court("c2", "Sân 2", "Đường 2", 4.5, 5, 1.0, 200000, 150000, 200000, 250000, "img");
        c2.setOwnerId("owner2");
        c2.setFields(List.of(new CourtField("f2", c2, "Sân 5B", "Sân 5B", "5_VS_5", "c2:f2")));
        courtStore.put("c2", c2);

        u1 = new User(); u1.setId("u1"); u1.setFullName("User 1"); u1.setPhone("0901"); u1.setRole("USER");
        u2 = new User(); u2.setId("u2"); u2.setFullName("User 2"); u2.setPhone("0902"); u2.setRole("USER");
        owner1 = new User(); owner1.setId("owner1"); owner1.setFullName("Owner 1"); owner1.setPhone("0903"); owner1.setRole("OWNER");
        owner2 = new User(); owner2.setId("owner2"); owner2.setFullName("Owner 2"); owner2.setPhone("0904"); owner2.setRole("OWNER");

        CourtRepository courtRepo = mock(CourtRepository.class);
        CourtFieldRepository fieldRepo = mock(CourtFieldRepository.class);
        CourtResourceRepository resRepo = mock(CourtResourceRepository.class);
        CourtHoldRepository holdRepo = mock(CourtHoldRepository.class);
        CourtHoldSlotRepository slotRepo = mock(CourtHoldSlotRepository.class);
        BookingRepository bookingRepo = mock(BookingRepository.class);
        PaymentRepository payRepo = mock(PaymentRepository.class);
        RecurringPackageRepository pkgRepo = mock(RecurringPackageRepository.class);
        RecurringSessionRepository sessRepo = mock(RecurringSessionRepository.class);
        RecurringCreditRepository credRepo = mock(RecurringCreditRepository.class);
        SessionChangeRequestRepository reqRepo = mock(SessionChangeRequestRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        UserWalletRepository walletRepo = mock(UserWalletRepository.class);
        WalletTransactionRepository txRepo = mock(WalletTransactionRepository.class);

        when(courtRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(courtStore.get(i.getArgument(0))));
        when(courtRepo.lockById(anyString())).thenAnswer(i -> {
            String id = i.getArgument(0);
            acquireRowLock("court:" + id);
            return Optional.ofNullable(courtStore.get(id));
        });
        when(courtRepo.findAll()).thenAnswer(i -> new ArrayList<>(courtStore.values()));

        when(resRepo.existsById(anyString())).thenAnswer(i -> resourceStore.containsKey(i.getArgument(0)));
        when(resRepo.save(any())).thenAnswer(i -> { CourtResource r = i.getArgument(0); resourceStore.put(r.getId(), r); return r; });
        doAnswer(i -> {
            List<String> ids = i.getArgument(0);
            for (String id : ids) acquireRowLock("res:" + id);
            return null;
        }).when(resRepo).lockResourcesForUpdate(anyList());

        when(holdRepo.save(any())).thenAnswer(i -> { CourtHold h = i.getArgument(0); holdStore.put(h.getId(), h); return h; });
        when(holdRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(holdStore.get(i.getArgument(0))));
        when(holdRepo.findByIdWithLock(anyString())).thenAnswer(i -> {
            String id = i.getArgument(0);
            acquireRowLock("hold:" + id);
            return Optional.ofNullable(holdStore.get(id));
        });
        when(holdRepo.findByRecurringPackageId(anyString())).thenAnswer(i ->
                holdStore.values().stream().filter(h -> Objects.equals(h.getRecurringPackageId(), i.getArgument(0))).findFirst());

        when(slotRepo.save(any())).thenAnswer(i -> { CourtHoldSlot s = i.getArgument(0); slotStore.put(s.getId(), s); return s; });
        when(slotRepo.saveAll(anyIterable())).thenAnswer(i -> {
            List<CourtHoldSlot> out = new ArrayList<>();
            for (CourtHoldSlot s : (Iterable<CourtHoldSlot>) i.getArgument(0)) { slotStore.put(s.getId(), s); out.add(s); }
            return out;
        });
        when(slotRepo.findByHoldId(anyString())).thenAnswer(i ->
                slotStore.values().stream().filter(s -> Objects.equals(s.getHoldId(), i.getArgument(0))).collect(Collectors.toList()));
        when(slotRepo.findByHoldIdAndDateLabel(anyString(), anyString())).thenAnswer(i ->
                slotStore.values().stream()
                        .filter(s -> Objects.equals(s.getHoldId(), i.getArgument(0)) && Objects.equals(s.getDateLabel(), i.getArgument(1)))
                        .collect(Collectors.toList()));
        when(slotRepo.findOverlappingActiveSlots(anyList(), anyString(), anyInt(), anyInt(), any())).thenAnswer(i -> {
            List<String> resIds = i.getArgument(0);
            String date = i.getArgument(1);
            int sMin = i.getArgument(2), eMin = i.getArgument(3);
            LocalDateTime now = i.getArgument(4);
            return slotStore.values().stream()
                    .filter(s -> resIds.contains(s.getResourceId()) && Objects.equals(date, s.getDateLabel()))
                    .filter(s -> "BOOKED".equals(s.getStatus()) || "CONFIRMED".equals(s.getStatus())
                            || (List.of("HELD", "AWAITING_PAYMENT", "VERIFYING_PAYMENT").contains(s.getStatus()) && s.getExpiresAt().isAfter(now)))
                    .filter(s -> sMin < s.getEndMinute() && s.getStartMinute() < eMin)
                    .collect(Collectors.toList());
        });

        when(bookingRepo.save(any())).thenAnswer(i -> { Booking b = i.getArgument(0); bookingStore.put(b.getId(), b); return b; });
        when(bookingRepo.saveAll(anyIterable())).thenAnswer(i -> {
            List<Booking> out = new ArrayList<>();
            for (Booking b : (Iterable<Booking>) i.getArgument(0)) { bookingStore.put(b.getId(), b); out.add(b); }
            return out;
        });
        when(bookingRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(bookingStore.get(i.getArgument(0))));
        when(bookingRepo.findAllByOrderByCreatedAtDesc()).thenAnswer(i -> new ArrayList<>(bookingStore.values()));
        when(bookingRepo.findByRecurringPackageId(anyString())).thenAnswer(i ->
                bookingStore.values().stream().filter(b -> Objects.equals(b.getRecurringPackageId(), i.getArgument(0))).collect(Collectors.toList()));

        when(payRepo.save(any())).thenAnswer(i -> { Payment p = i.getArgument(0); paymentStore.put(p.getId(), p); return p; });
        when(payRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(paymentStore.get(i.getArgument(0))));
        when(payRepo.lockById(anyString())).thenAnswer(i -> Optional.ofNullable(paymentStore.get(i.getArgument(0))));
        when(payRepo.findByOrderInvoiceNumber(anyString())).thenAnswer(i ->
                paymentStore.values().stream().filter(p -> Objects.equals(p.getOrderInvoiceNumber(), i.getArgument(0))).findFirst());
        when(payRepo.existsBySepayTransactionId(anyString())).thenAnswer(i ->
                paymentStore.values().stream().anyMatch(p -> Objects.equals(p.getSepayTransactionId(), i.getArgument(0))));

        when(pkgRepo.save(any())).thenAnswer(i -> { RecurringPackage p = i.getArgument(0); pkgStore.put(p.getId(), p); return p; });
        when(pkgRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(pkgStore.get(i.getArgument(0))));
        when(pkgRepo.lockById(anyString())).thenAnswer(i -> {
            String id = i.getArgument(0);
            acquireRowLock("pkg:" + id);
            return Optional.ofNullable(pkgStore.get(id));
        });

        when(sessRepo.save(any())).thenAnswer(i -> { RecurringSession s = i.getArgument(0); sessionStore.put(s.getId(), s); return s; });
        when(sessRepo.saveAll(anyIterable())).thenAnswer(i -> {
            List<RecurringSession> out = new ArrayList<>();
            for (RecurringSession s : (Iterable<RecurringSession>) i.getArgument(0)) { sessionStore.put(s.getId(), s); out.add(s); }
            return out;
        });
        when(sessRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(sessionStore.get(i.getArgument(0))));
        when(sessRepo.lockById(anyString())).thenAnswer(i -> {
            String id = i.getArgument(0);
            acquireRowLock("sess:" + id);
            return Optional.ofNullable(sessionStore.get(id));
        });
        when(sessRepo.findByPackageIdOrderBySessionIndexAsc(anyString())).thenAnswer(i ->
                sessionStore.values().stream()
                        .filter(s -> Objects.equals(s.getPackageId(), i.getArgument(0)))
                        .sorted(Comparator.comparingInt(RecurringSession::getSessionIndex))
                        .collect(Collectors.toList()));

        when(credRepo.save(any())).thenAnswer(i -> { RecurringCredit c = i.getArgument(0); creditStore.put(c.getId(), c); return c; });
        when(credRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(creditStore.get(i.getArgument(0))));
        when(credRepo.lockById(anyString())).thenAnswer(i -> {
            String id = i.getArgument(0);
            acquireRowLock("cred:" + id);
            return Optional.ofNullable(creditStore.get(id));
        });

        when(reqRepo.save(any())).thenAnswer(i -> { SessionChangeRequest r = i.getArgument(0); reqStore.put(r.getId(), r); return r; });
        when(reqRepo.findById(anyString())).thenAnswer(i -> Optional.ofNullable(reqStore.get(i.getArgument(0))));
        when(reqRepo.lockById(anyString())).thenAnswer(i -> {
            String id = i.getArgument(0);
            acquireRowLock("req:" + id);
            return Optional.ofNullable(reqStore.get(id));
        });

        when(walletRepo.findByUserId(anyString())).thenAnswer(i -> Optional.ofNullable(walletStore.get(i.getArgument(0))));
        when(walletRepo.lockByUserId(anyString())).thenAnswer(i -> {
            String uid = i.getArgument(0);
            acquireRowLock("wallet:" + uid);
            return Optional.ofNullable(walletStore.get(uid));
        });
        when(walletRepo.save(any())).thenAnswer(i -> { UserWallet w = i.getArgument(0); walletStore.put(w.getUserId(), w); return w; });
        when(walletRepo.saveAndFlush(any())).thenAnswer(i -> { UserWallet w = i.getArgument(0); walletStore.put(w.getUserId(), w); return w; });

        when(txRepo.save(any())).thenAnswer(i -> {
            WalletTransaction t = i.getArgument(0);
            synchronized (txStore) {
                if (t.getDedupKey() != null && txStore.stream().anyMatch(x -> Objects.equals(x.getDedupKey(), t.getDedupKey()))) {
                    throw new org.springframework.dao.DataIntegrityViolationException("Duplicate dedupKey");
                }
                txStore.add(t);
            }
            return t;
        });
        when(txRepo.existsByDedupKey(anyString())).thenAnswer(i ->
                txStore.stream().anyMatch(t -> Objects.equals(t.getDedupKey(), i.getArgument(0))));
        when(txRepo.findByUserIdOrderByCreatedAtDesc(anyString())).thenAnswer(i ->
                txStore.stream().filter(t -> Objects.equals(t.getUserId(), i.getArgument(0))).collect(Collectors.toList()));

        when(userRepo.findById("u1")).thenReturn(Optional.of(u1));

        SepayConfig cfg = new SepayConfig();
        cfg.setMerchantId("M"); cfg.setSecretKey("S"); cfg.setIpnSecret("IPN");
        walletService = new WalletService(walletRepo, txRepo, clock);
        NotificationService notifService = mock(NotificationService.class);
        sepayService = new SepayService(cfg, payRepo, bookingRepo, courtRepo, holdRepo, slotRepo, clock, notifService, pkgRepo, sessRepo);
        CourtHoldTransactionService holdTx = new CourtHoldTransactionService(
                courtRepo, fieldRepo, resRepo, holdRepo, slotRepo, bookingRepo, payRepo, sepayService, clock, walletService, pkgRepo, sessRepo, notifService
        );
        recurringService = new RecurringBookingService(
                courtRepo, resRepo, holdRepo, slotRepo, bookingRepo, payRepo, pkgRepo, sessRepo, credRepo, reqRepo,
                userRepo, holdTx, sepayService, walletService, notifService, new BookingPolicyProperties(), clock
        );

        AuthService auth = new AuthService(userRepo, mock(UserSessionRepository.class), clock) {
            @Override
            public User authenticate(String authHeader) {
                return switch (String.valueOf(authHeader)) {
                    case "Bearer u1" -> u1;
                    case "Bearer u2" -> u2;
                    case "Bearer owner1" -> owner1;
                    case "Bearer owner2" -> owner2;
                    default -> null;
                };
            }
        };
        walletController = new WalletController(auth, walletService);
        ownerController = new OwnerController(
                auth, bookingRepo, courtRepo, mock(StaffShiftRepository.class), mock(ShiftReportRepository.class),
                mock(PosSaleRepository.class), userRepo, new ShiftPlanningService(mock(StaffShiftRepository.class), userRepo, clock, false),
                clock, pkgRepo, recurringService
        );
    }

    @Test
    void concurrentWalletDebits_preventNegativeBalanceUnderRaceCondition() throws Exception {
        walletService.creditRefund("u1", 200_000, "Số dư test", null, null, null, "INIT");
        releaseThreadLocks();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await(3, TimeUnit.SECONDS);
                try {
                    walletService.debitWallet("u1", 150_000, "DEBIT_SINGLE", "Mua sân #" + idx, "b" + idx, null, "DEDUP_" + idx);
                    return true;
                } catch (ResponseStatusException | IllegalStateException ex) {
                    return false;
                } finally {
                    releaseThreadLocks();
                }
            }));
        }
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        go.countDown();

        int successCount = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(5, TimeUnit.SECONDS)) successCount++;
        }
        pool.shutdownNow();

        assertEquals(1, successCount, "Chỉ duy nhất 1 luồng được trừ tiền thành công");
        int finalBalance = walletService.getOrCreateWallet("u1").getBalance();
        assertEquals(50_000, finalBalance);
        assertTrue(finalBalance >= 0);
    }

    @Test
    void concurrentCreditRedemption_onlyOneSucceeds_andConflictPreservesCredit() throws Exception {
        walletService.creditRefund("u1", 3_000_000, "Nạp ví test", null, null, null, "SEED");
        releaseThreadLocks();

        Map<String, Object> pkgRes = recurringService.startRecurringCheckout(
                new RecurringDtos.RecurringCheckoutRequest(
                        "c1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                        "WEEKLY_12", "WALLET", "User Một", "0901111111", "idem-cred"
                ),
                "u1"
        );
        releaseThreadLocks();
        String pkgId = (String) pkgRes.get("packageId");
        RecurringSession s1 = sessionStore.values().stream()
                .filter(s -> pkgId.equals(s.getPackageId()) && s.getSessionIndex() == 1)
                .findFirst().orElseThrow();
        // Đánh dấu session 1 đủ điều kiện hủy sớm (24/11 -> 01/12 = 176h >= 168h) và giữ 1 buổi còn dư
        Map<String, Object> credRes = recurringService.cancelOrPostponeSession(
                s1.getId(), new RecurringDtos.CancelOrPostponeRequest("PERSONAL", "KEEP_CREDIT", null, null), "u1"
        );
        releaseThreadLocks();
        String creditId = ((RecurringCredit) credRes.get("credit")).getId();

        // Thử đặt bù vào ô đang bận (08/12/2026 là buổi #2 của chính gói này) -> ném SlotConflictException, credit VẪN AVAILABLE
        assertThrows(SlotConflictException.class, () ->
                recurringService.redeemCredit(creditId, new RecurringDtos.RedeemCreditRequest("08/12/2026", "Sân 5A", List.of("19:00 - 20:30")), "u1")
        );
        releaseThreadLocks();
        assertEquals("AVAILABLE", creditStore.get(creditId).getStatus());

        // Bây giờ 2 luồng đồng thời cố đổi cùng 1 credit vào 2 ngày trống khác nhau -> chỉ đúng 1 luồng thành công!
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (String targetDate : List.of("23/02/2027", "02/03/2027")) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await(3, TimeUnit.SECONDS);
                try {
                    recurringService.redeemCredit(creditId, new RecurringDtos.RedeemCreditRequest(targetDate, "Sân 5A", List.of("19:00 - 20:30")), "u1");
                    return true;
                } catch (ResponseStatusException ex) {
                    return false;
                } finally {
                    releaseThreadLocks();
                }
            }));
        }
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        go.countDown();

        int redeemedCount = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(5, TimeUnit.SECONDS)) redeemedCount++;
        }
        pool.shutdownNow();

        assertEquals(1, redeemedCount);
        assertEquals("USED", creditStore.get(creditId).getStatus());
        long makeupBookings = bookingStore.values().stream().filter(b -> "CREDIT".equals(b.getPaymentMethod())).count();
        assertEquals(1L, makeupBookings);
    }

    @Test
    void doubleOwnerRefundApproval_creditsWalletOnlyOnce_andCapsAtPaidSnapshot() throws Exception {
        walletService.creditRefund("u1", 3_000_000, "Nạp ví", null, null, null, "SEED2");
        releaseThreadLocks();

        Map<String, Object> pkgRes = recurringService.startRecurringCheckout(
                new RecurringDtos.RecurringCheckoutRequest(
                        "c1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                        "WEEKLY_12", "WALLET", "User Một", "0901111111", "idem-double-ref"
                ),
                "u1"
        );
        releaseThreadLocks();
        int balAfterBuy = walletService.getOrCreateWallet("u1").getBalance(); // 600.000đ

        String pkgId = (String) pkgRes.get("packageId");
        RecurringSession s2 = sessionStore.values().stream()
                .filter(s -> pkgId.equals(s.getPackageId()) && s.getSessionIndex() == 2)
                .findFirst().orElseThrow();
        Map<String, Object> refundSubmit = recurringService.cancelOrPostponeSession(
                s2.getId(), new RecurringDtos.CancelOrPostponeRequest("PERSONAL", "REFUND_WALLET", null, "Xin hoàn"), "u1"
        );
        releaseThreadLocks();
        String reqId = ((SessionChangeRequest) refundSubmit.get("request")).getId();

        // Không được hoàn vượt quá giá thực trả của buổi (250.000đ)
        assertThrows(IllegalArgumentException.class, () ->
                ownerController.resolveRecurringRequest(reqId, "Bearer owner1",
                        new RecurringDtos.OwnerResolveChangeRequest("APPROVE_REFUND", 300_000, "Vượt trần"))
        );
        releaseThreadLocks();

        // Hai luồng/hai cú click duyệt song song -> chỉ 1 lần cộng ví, lần 2 bị chặn 409 CONFLICT
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await(3, TimeUnit.SECONDS);
                try {
                    ownerController.resolveRecurringRequest(reqId, "Bearer owner1",
                            new RecurringDtos.OwnerResolveChangeRequest("APPROVE_REFUND", 150_000, "Duyệt"));
                    return true;
                } catch (ResponseStatusException ex) {
                    return false;
                } finally {
                    releaseThreadLocks();
                }
            }));
        }
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        go.countDown();

        int approved = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(5, TimeUnit.SECONDS)) approved++;
        }
        pool.shutdownNow();

        assertEquals(1, approved);
        assertEquals(balAfterBuy + 150_000, walletService.getOrCreateWallet("u1").getBalance());
        assertEquals(150_000, pkgStore.get(pkgId).getTotalRefunded());
    }

    @Test
    void securityIsolation_crossOwnerForbidden403_andCrossUserWalletOrSessionForbidden() {
        walletService.creditRefund("u1", 3_000_000, "Nạp ví", null, null, null, "SEED3");
        releaseThreadLocks();

        Map<String, Object> pkgRes = recurringService.startRecurringCheckout(
                new RecurringDtos.RecurringCheckoutRequest(
                        "c1", "Sân 5A", "01/12/2026", List.of("19:00 - 20:30"),
                        "WEEKLY_12", "WALLET", "User Một", "0901111111", "idem-sec"
                ),
                "u1"
        );
        releaseThreadLocks();
        String pkgId = (String) pkgRes.get("packageId");
        RecurringSession s1 = sessionStore.values().stream()
                .filter(s -> pkgId.equals(s.getPackageId()) && s.getSessionIndex() == 1)
                .findFirst().orElseThrow();
        Map<String, Object> reqRes = recurringService.cancelOrPostponeSession(
                s1.getId(), new RecurringDtos.CancelOrPostponeRequest("WEATHER", "SLIDE", null, "Mưa"), "u1"
        );
        releaseThreadLocks();
        String reqId = ((SessionChangeRequest) reqRes.get("request")).getId();

        // Owner 2 (chỉ sở hữu c2) KHÔNG được phép thao tác yêu cầu hoặc buổi thuộc c1
        ResponseStatusException ownerResolveEx = assertThrows(ResponseStatusException.class, () ->
                ownerController.resolveRecurringRequest(reqId, "Bearer owner2",
                        new RecurringDtos.OwnerResolveChangeRequest("APPROVE_WEATHER", null, "Duyệt chéo"))
        );
        assertEquals(HttpStatus.FORBIDDEN, ownerResolveEx.getStatusCode());

        ResponseStatusException ownerWeatherEx = assertThrows(ResponseStatusException.class, () ->
                ownerController.ownerWeatherPostpone(s1.getId(), "Bearer owner2", Map.of("ownerNote", "Hoãn chéo"))
        );
        assertEquals(HttpStatus.FORBIDDEN, ownerWeatherEx.getStatusCode());

        // User 2 KHÔNG được phép xem/sửa gói, buổi, hoặc ví của User 1
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () ->
                recurringService.getPackageDetail(pkgId, "u2")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () ->
                recurringService.previewSessionAction(s1.getId(), "PERSONAL", "u2")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () ->
                walletController.getMyWallet("Bearer u2", "u1")).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(ResponseStatusException.class, () ->
                walletController.getMyWallet(null, null)).getStatusCode());
    }
}
