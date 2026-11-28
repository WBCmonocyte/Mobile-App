package com.easysport.service;

import com.easysport.controller.StaffController;
import com.easysport.dto.ConflictSlotDto;
import com.easysport.dto.SlotConflictException;
import com.easysport.dto.StartCheckoutRequest;
import com.easysport.entity.*;
import com.easysport.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourtHoldTransactionService {

    private final CourtRepository courtRepository;
    private final CourtFieldRepository courtFieldRepository;
    private final CourtResourceRepository courtResourceRepository;
    private final CourtHoldRepository courtHoldRepository;
    private final CourtHoldSlotRepository courtHoldSlotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SepayService sepayService;
    private final Clock clock;
    private final WalletService walletService;
    private final RecurringPackageRepository recurringPackageRepository;
    private final RecurringSessionRepository recurringSessionRepository;
    private final NotificationService notificationService;

    public CourtHoldTransactionService(CourtRepository courtRepository,
                                       CourtFieldRepository courtFieldRepository,
                                       CourtResourceRepository courtResourceRepository,
                                       CourtHoldRepository courtHoldRepository,
                                       CourtHoldSlotRepository courtHoldSlotRepository,
                                       BookingRepository bookingRepository,
                                       PaymentRepository paymentRepository,
                                       SepayService sepayService,
                                       Clock clock) {
        this(courtRepository, courtFieldRepository, courtResourceRepository,
                courtHoldRepository, courtHoldSlotRepository, bookingRepository,
                paymentRepository, sepayService, clock,
                null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CourtHoldTransactionService(CourtRepository courtRepository,
                                       CourtFieldRepository courtFieldRepository,
                                       CourtResourceRepository courtResourceRepository,
                                       CourtHoldRepository courtHoldRepository,
                                       CourtHoldSlotRepository courtHoldSlotRepository,
                                       BookingRepository bookingRepository,
                                       PaymentRepository paymentRepository,
                                       SepayService sepayService,
                                       Clock clock,
                                       @org.springframework.beans.factory.annotation.Autowired(required = false) WalletService walletService,
                                       @org.springframework.beans.factory.annotation.Autowired(required = false) RecurringPackageRepository recurringPackageRepository,
                                       @org.springframework.beans.factory.annotation.Autowired(required = false) RecurringSessionRepository recurringSessionRepository,
                                       @org.springframework.beans.factory.annotation.Autowired(required = false) NotificationService notificationService) {
        this.courtRepository = courtRepository;
        this.courtFieldRepository = courtFieldRepository;
        this.courtResourceRepository = courtResourceRepository;
        this.courtHoldRepository = courtHoldRepository;
        this.courtHoldSlotRepository = courtHoldSlotRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.sepayService = sepayService;
        this.clock = clock;
        this.walletService = walletService;
        this.recurringPackageRepository = recurringPackageRepository;
        this.recurringSessionRepository = recurringSessionRepository;
        this.notificationService = notificationService;
    }

    public static class HoldResult {
        public final CourtHold hold;
        public final Booking booking;
        public final Payment payment;
        public final Court court;
        public final boolean isExisting;

        public HoldResult(CourtHold hold, Booking booking, Payment payment, Court court, boolean isExisting) {
            this.hold = hold;
            this.booking = booking;
            this.payment = payment;
            this.court = court;
            this.isExisting = isExisting;
        }
    }

    /**
     * Giành giữ sân nguyên tử trong Transaction Database.
     * Áp dụng Pessimistic Lock trên tài nguyên vật lý theo thứ tự ID tăng dần để chống deadlock.
     * Quy tắc thời hạn:
     * - paymentDeadline = T0 + 5 phút
     * - verificationDeadline = paymentDeadline + 1 phút (T0 + 6 phút)
     */
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public HoldResult acquireHoldTransaction(StartCheckoutRequest req, String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank())
            throw new IllegalArgumentException("Người dùng chưa được xác thực.");
        Court court = resolveCourt(req.getCourtId());
        courtRepository.lockById(court.getId()).orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại."));
        CourtField field = resolveField(court, req.getFieldDetail());
        LocalDateTime now = LocalDateTime.now(clock);
        req.setDateLabel(BookingRules.parseDate(req.getDateLabel()).format(BookingRules.DATE));
        if (!List.of("QR_TRANSFER", "CASH_ON_SITE", "WALLET").contains(req.getPaymentMethod()))
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        if (req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank() || req.getIdempotencyKey().length() > 100)
            throw new IllegalArgumentException("Thiếu hoặc sai mã yêu cầu thanh toán.");
        String key = UUID.nameUUIDFromBytes((authenticatedUserId + ":" + req.getIdempotencyKey())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        Optional<CourtHold> existingOpt = courtHoldRepository.findByIdempotencyKey(key);
        if (existingOpt.isPresent()) {
            CourtHold h = existingOpt.get();
            Booking b = bookingRepository.findById(h.getBookingId()).orElseThrow();
            Payment p = paymentRepository.findById(h.getPaymentAttemptId()).orElseThrow();
            String requestedSlots = req.getSlots() == null ? "" : req.getSlots().stream().map(String::trim).sorted().collect(Collectors.joining(","));
            if (!authenticatedUserId.equals(h.getUserId()) || !court.getId().equals(h.getCourtId())
                    || !field.getId().equals(h.getFieldId()) || !req.getDateLabel().equals(h.getDateLabel())
                    || !requestedSlots.equals(h.getSlotsJson()) || !req.getPaymentMethod().equals(b.getPaymentMethod()))
                throw new IllegalStateException("Mã yêu cầu đã được sử dụng cho lựa chọn khác.");
            return new HoldResult(h, b, p, court, true);
        }
        req.setSlots(BookingRules.validateSlots(req.getSlots(), BookingRules.parseDate(req.getDateLabel()), clock));
        if (req.getCustomerName() == null || req.getCustomerName().isBlank() || req.getCustomerName().length() > 100
                || req.getCustomerPhone() == null || !req.getCustomerPhone().matches("[+0-9]{9,15}"))
            throw new IllegalArgumentException("Vui lòng nhập họ tên và số điện thoại hợp lệ.");

        // 3. Phân rã tài nguyên sân vật lý (Xử lý cả trường hợp dùng chung mặt sân)
        List<String> resourceIds = resolvePhysicalResourceIds(court, field);
        if (resourceIds.isEmpty()) {
            throw new IllegalArgumentException("Không xác định được tài nguyên vật lý cho sân: " + req.getFieldDetail());
        }

        // Khóa các tài nguyên theo thứ tự tăng dần cố định để triệt tiêu deadlock
        Collections.sort(resourceIds);
        ensureResourcesExist(resourceIds, court.getId());
        courtResourceRepository.lockResourcesForUpdate(resourceIds);

        // 4. Phân tích các khung giờ yêu cầu thành các khoảng [startMinute, endMinute)
        List<CourtHoldService.SlotInterval> requestedIntervals = parseSlots(req.getSlots());
        if (requestedIntervals.isEmpty()) {
            throw new IllegalArgumentException("Khung giờ thuê không hợp lệ");
        }

        // 5. Kiểm tra xung đột cho TOÀN BỘ các ca (startA < endB AND startB < endA)
        // Lưu ý: Các slot trong 1 phút xác minh (phút thứ 6) vẫn đang bị chiếm giữ và tính là xung đột
        List<ConflictSlotDto> conflicts = new ArrayList<>();
        for (CourtHoldService.SlotInterval interval : requestedIntervals) {
            List<CourtHoldSlot> activeConflicts = courtHoldSlotRepository.findOverlappingActiveSlots(
                    resourceIds,
                    req.getDateLabel().trim(),
                    interval.startMinute,
                    interval.endMinute,
                    now
            );

            if (!activeConflicts.isEmpty()) {
                for (CourtHoldSlot s : activeConflicts) {
                    conflicts.add(new ConflictSlotDto(interval.originalLabel, s.getStatus(), s.getExpiresAt()));
                }
            }
        }

        // Kiểm tra đối ứng với các đơn đặt sân tại quầy hoặc đã thanh toán trong bookingRepository
        if (bookingRepository != null) {
            List<Booking> activeBookings = bookingRepository.findAllByOrderByCreatedAtDesc();
            if (activeBookings != null) {
                for (Booking b : activeBookings) {
                    if (!court.getId().equals(b.getCourtId())) continue;
                    String st = b.getStatus() != null ? b.getStatus().toUpperCase() : "";
                    if (!st.equals("PAID") && !st.equals("DEPOSIT_PAID") && !st.equals("CHECKED_IN")) continue;
                    String bDate = StaffController.extractDate(b.getDateLabel());
                    if (bDate == null && b.getDateLabel() != null) {
                        bDate = b.getDateLabel().trim();
                    }
                    if (!req.getDateLabel().trim().equals(bDate)) continue;
                    boolean overlaps = StaffController.subFieldsOverlap(field.getDetail(), b.getFieldDetail())
                            || StaffController.subFieldsOverlap(field.getLabel(), b.getFieldDetail())
                            || StaffController.subFieldsOverlap(field.getId(), b.getFieldDetail());
                    if (!overlaps) continue;
                    LocalTime[] bTimes = StaffController.extractTimeInterval(b.getTimeRange());
                    if (bTimes == null) continue;
                    int bStart = bTimes[0].getHour() * 60 + bTimes[0].getMinute();
                    int bEnd = bTimes[1].getHour() * 60 + bTimes[1].getMinute();
                    for (CourtHoldService.SlotInterval interval : requestedIntervals) {
                        if (interval.startMinute < bEnd && bStart < interval.endMinute) {
                            conflicts.add(new ConflictSlotDto(interval.originalLabel, "BOOKED", null));
                        }
                    }
                }
            }
        }

        // Nếu một ca xung đột, rollback toàn bộ; không giữ một phần (All-or-Nothing)
        if (!conflicts.isEmpty()) {
            throw new SlotConflictException(conflicts, now);
        }

        // 6. Tính toán giá tiền trên máy chủ (Server-side calculation)
        String method = req.getPaymentMethod() != null ? req.getPaymentMethod().trim() : "QR_TRANSFER";
        boolean isWallet = "WALLET".equalsIgnoreCase(method);
        String mergedTimeRange = req.getSlots().stream().collect(Collectors.joining(", "));
        SepayService.PriceResult price = sepayService.calculatePrice(court, mergedTimeRange, method);

        // 7. Tạo bản ghi Giữ Sân:
        // paymentDeadline = T0 + 5 phút
        // verificationDeadline = paymentDeadline + 1 phút
        LocalDateTime paymentDeadline = now.plusMinutes(5);
        LocalDateTime verificationDeadline = paymentDeadline.plusMinutes(1);
        LocalDateTime confirmedExpiry = BookingRules.parseDate(req.getDateLabel()).plusDays(2).atStartOfDay();

        String holdId = "HOLD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String bookingId = "BK" + UUID.randomUUID().toString().replace("-", "");
        String paymentId = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String invoiceNumber = "ES" + UUID.randomUUID().toString().replace("-", "");
        String checkoutToken = UUID.randomUUID().toString().replace("-", "");

        String userId = (authenticatedUserId != null && !authenticatedUserId.isBlank())
                ? authenticatedUserId.trim()
                : (req.getUserId() != null && !req.getUserId().isBlank() ? req.getUserId().trim() : "user_unknown");

        if (isWallet) {
            if (walletService == null) {
                throw new IllegalStateException("Dịch vụ Ví EasySport chưa được khởi tạo.");
            }
            walletService.debitWallet(
                    userId,
                    price.totalPrice,
                    "DEBIT_BOOKING",
                    "Thanh toán đặt sân " + court.getName() + " (" + req.getDateLabel().trim() + " · " + mergedTimeRange + ")",
                    bookingId,
                    null,
                    "DEBIT_SINGLE:" + key
            );
        }

        CourtHold hold = new CourtHold(
                holdId,
                bookingId,
                paymentId,
                userId,
                court.getId(),
                field.getId(),
                field.getDetail(),
                req.getDateLabel().trim(),
                mergedTimeRange,
                String.join(",", req.getSlots()),
                String.join(",", resourceIds),
                now,
                paymentDeadline,
                verificationDeadline,
                key
        );
        if (isWallet) {
            hold.setStatus("CONFIRMED");
            hold.setConfirmedAt(now);
        }
        courtHoldRepository.save(hold);

        // Lưu các slot tương ứng cho từng tài nguyên vật lý.
        // Hạn giữ của slot là verificationDeadline (hết phút thứ 6 mới mở lại)
        for (String resId : resourceIds) {
            for (CourtHoldService.SlotInterval interval : requestedIntervals) {
                CourtHoldSlot slot = new CourtHoldSlot(
                        UUID.randomUUID().toString().replace("-", ""),
                        holdId,
                        court.getId(),
                        resId,
                        req.getDateLabel().trim(),
                        interval.startMinute,
                        interval.endMinute,
                        interval.originalLabel,
                        isWallet ? "CONFIRMED" : "AWAITING_PAYMENT",
                        isWallet ? confirmedExpiry : verificationDeadline
                );
                courtHoldSlotRepository.save(slot);
            }
        }

        // Tạo Booking trạng thái PENDING (hoặc PAID ngay nếu thanh toán bằng Ví)
        String address = court.getAddress() != null && !court.getAddress().isBlank() ? court.getAddress() : req.getAddress();
        int initialDeposit = isWallet ? price.totalPrice : price.depositAmount;
        int initialRemaining = isWallet ? 0 : price.remainingAmount;
        Booking booking = new Booking(
                bookingId,
                userId,
                court.getId(),
                court.getName(),
                field.getDetail(),
                mergedTimeRange,
                req.getDateLabel().trim(),
                price.totalPrice,
                initialDeposit,
                initialRemaining,
                method,
                isWallet ? "PAID" : "PENDING",
                req.getCustomerName(),
                req.getCustomerPhone(),
                address,
                isWallet ? confirmedExpiry : verificationDeadline
        );
        bookingRepository.save(booking);

        // Tạo Payment
        int onlineAmount = "CASH_ON_SITE".equalsIgnoreCase(method) ? price.depositAmount : price.totalPrice;
        Payment payment = new Payment(
                paymentId,
                bookingId,
                invoiceNumber,
                onlineAmount,
                "VND",
                method,
                checkoutToken,
                paymentDeadline
        );
        if (isWallet) {
            payment.setStatus("PAID");
            payment.setConfirmedAmount(onlineAmount);
            payment.setConfirmedAt(now);
        }
        paymentRepository.save(payment);

        if (isWallet && notificationService != null) {
            notificationService.createBookingConfirmedNotif(booking);
        }

        return new HoldResult(hold, booking, payment, court, false);
    }

    /**
     * Giải phóng một hold tự nguyện do người dùng hủy
     */
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void releaseHoldTransaction(String holdId, String userId) {
        CourtHold snapshot = courtHoldRepository.findById(holdId).orElseThrow();
        courtRepository.lockById(snapshot.getCourtId()).orElseThrow();
        Optional<CourtHold> holdOpt = courtHoldRepository.findByIdWithLock(holdId);
        if (holdOpt.isPresent()) {
            CourtHold hold = holdOpt.get();
            if (userId != null && !userId.isBlank() && !hold.getUserId().equals(userId.trim())) {
                throw new IllegalStateException("Bạn không có quyền giải phóng hold của người khác.");
            }
            if (hold.isConfirmed() || "REVIEW_REQUIRED".equals(hold.getStatus()))
                throw new IllegalStateException("Đơn đã có thanh toán cần liên hệ hỗ trợ để hủy và hoàn tiền.");
            if (!hold.isConfirmed()) {
                LocalDateTime now = LocalDateTime.now(clock);
                hold.setStatus("RELEASED");
                hold.setReleasedAt(now);
                courtHoldRepository.save(hold);

                List<CourtHoldSlot> slots = courtHoldSlotRepository.findByHoldId(holdId);
                for (CourtHoldSlot s : slots) {
                    s.setStatus("RELEASED");
                }
                courtHoldSlotRepository.saveAll(slots);

                if (hold.getRecurringPackageId() != null) {
                    syncRecurringLifecycle(hold.getRecurringPackageId(), "RELEASED", "CANCELLED");
                } else if (hold.getBookingId() != null) {
                    bookingRepository.findById(hold.getBookingId()).ifPresent(b -> {
                        if ("PENDING".equalsIgnoreCase(b.getStatus())) {
                            b.setStatus("CANCELLED");
                            b.setPayment("CANCELLED");
                            bookingRepository.save(b);
                        }
                    });
                }
            }
        }
    }

    /**
     * Dọn dẹp các hold đã quá thời hạn xác minh (6 phút)
     */
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void cleanupExpiredHoldsTransaction() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<CourtHold> expiredList = courtHoldRepository.findExpiredHolds(now);

        expiredList.stream().map(CourtHold::getCourtId).distinct().sorted()
                .forEach(id -> courtRepository.lockById(id).orElseThrow());
        for (CourtHold candidate : expiredList) {
            CourtHold hold = courtHoldRepository.findByIdWithLock(candidate.getId()).orElseThrow();
            if (hold.isExpired(now) && !hold.isConfirmed() && !"REVIEW_REQUIRED".equalsIgnoreCase(hold.getStatus())) {
                hold.setStatus("EXPIRED");
                hold.setReleasedAt(now);
                courtHoldRepository.save(hold);

                List<CourtHoldSlot> slots = courtHoldSlotRepository.findByHoldId(hold.getId());
                for (CourtHoldSlot s : slots) {
                    s.setStatus("EXPIRED");
                }
                courtHoldSlotRepository.saveAll(slots);

                if (hold.getRecurringPackageId() != null) {
                    syncRecurringLifecycle(hold.getRecurringPackageId(), "EXPIRED", "EXPIRED");
                } else if (hold.getBookingId() != null) {
                    bookingRepository.findById(hold.getBookingId()).ifPresent(b -> {
                        if ("PENDING".equalsIgnoreCase(b.getStatus())) {
                            b.setStatus("EXPIRED");
                            b.setPayment("EXPIRED");
                            bookingRepository.save(b);
                        }
                    });
                }
            }
        }
    }

    private void syncRecurringLifecycle(String packageId, String packageAndSessionStatus, String bookingStatus) {
        if (recurringPackageRepository != null) {
            recurringPackageRepository.findById(packageId).ifPresent(pkg -> {
                if ("AWAITING_PAYMENT".equalsIgnoreCase(pkg.getStatus())) {
                    pkg.setStatus(packageAndSessionStatus);
                    recurringPackageRepository.save(pkg);
                }
            });
        }
        if (recurringSessionRepository != null) {
            List<RecurringSession> sessions = recurringSessionRepository.findByPackageIdOrderBySessionIndexAsc(packageId);
            for (RecurringSession rs : sessions) {
                if ("PENDING_PAYMENT".equalsIgnoreCase(rs.getStatus())) {
                    rs.setStatus(packageAndSessionStatus);
                    recurringSessionRepository.save(rs);
                }
            }
        }
        for (Booking b : bookingRepository.findByRecurringPackageId(packageId)) {
            if ("PENDING".equalsIgnoreCase(b.getStatus())) {
                b.setStatus(bookingStatus);
                b.setPayment(bookingStatus);
                bookingRepository.save(b);
            }
        }
    }

    public Court resolveCourt(String courtId) {
        if (courtId == null) throw new IllegalArgumentException("courtId không được để trống");
        String trimmed = courtId.trim();

        Optional<Court> opt = courtRepository.findById(trimmed);
        if (opt.isPresent()) return opt.get();

        throw new IllegalArgumentException("Không tìm thấy sân thể thao với ID hoặc tên: " + courtId);
    }

    public CourtField resolveField(Court court, String fieldDetail) {
        if (fieldDetail == null || fieldDetail.isBlank()) {
            return court.getFields().stream().filter(f -> Boolean.TRUE.equals(f.getActive())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Sân chưa có sân con đang hoạt động."));
        }
        String query = fieldDetail.trim();
        return court.getFields().stream().filter(f -> Boolean.TRUE.equals(f.getActive()))
                .filter(f -> f.getId().equals(query) || f.getDetail().equals(query) || f.getLabel().equals(query))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Sân con không tồn tại hoặc đã ngừng hoạt động."));
    }

    public List<String> resolvePhysicalResourceIds(Court court, CourtField field) {
        if (field.getResourceKeys() != null && !field.getResourceKeys().isBlank()) {
            return Arrays.stream(field.getResourceKeys().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return Collections.singletonList(court.getId() + ":" + field.getId());
    }

    private void ensureResourcesExist(List<String> resourceIds, String courtId) {
        for (String resId : resourceIds) {
            if (!courtResourceRepository.existsById(resId)) {
                courtResourceRepository.save(new CourtResource(resId, courtId, "Resource " + resId, "Auto-created"));
            }
        }
    }

    private List<CourtHoldService.SlotInterval> parseSlots(List<String> slots) {
        List<CourtHoldService.SlotInterval> list = new ArrayList<>();
        if (slots == null) return list;
        for (String s : slots) {
            CourtHoldService.SlotInterval interval = parseSlot(s);
            if (interval != null) {
                list.add(interval);
            }
        }
        return list;
    }

    private CourtHoldService.SlotInterval parseSlot(String slotLabel) {
        if (slotLabel == null || !slotLabel.contains("-")) return null;
        String[] parts = slotLabel.split("-");
        if (parts.length < 2) return null;
        try {
            int start = toMinutes(parts[0].trim());
            int end = toMinutes(parts[1].trim());
            return new CourtHoldService.SlotInterval(start, end, slotLabel.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int toMinutes(String timeStr) {
        String clean = timeStr.replace("–", "-").trim();
        String[] hm = clean.split(":");
        int hours = Integer.parseInt(hm[0].trim());
        int minutes = hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0;
        return hours * 60 + minutes;
    }
}
