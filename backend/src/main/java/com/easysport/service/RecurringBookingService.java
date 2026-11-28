package com.easysport.service;

import com.easysport.config.BookingPolicyProperties;
import com.easysport.controller.StaffController;
import com.easysport.dto.BookingPaymentStatusResponse;
import com.easysport.dto.ConflictSlotDto;
import com.easysport.dto.RecurringDtos;
import com.easysport.dto.SlotConflictException;
import com.easysport.entity.*;
import com.easysport.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ Đặt lịch cố định theo tuần (3 tháng · 12 buổi), Hoãn/Hủy, Trượt lịch,
 * Quản lý Buổi còn dư và Phê duyệt/Hoàn tiền ví EasySport của Chủ sân.
 */
@Service
public class RecurringBookingService {

    public static final int DEFAULT_TOTAL_SESSIONS = 12;
    public static final int MAX_RECURRING_ADVANCE_DAYS = 365;
    public static final String DEFAULT_PACKAGE_TYPE = "WEEKLY_12";
    public static final String DEFAULT_PACKAGE_TITLE = "3 tháng · 12 buổi · 1 buổi/tuần";

    private final CourtRepository courtRepository;
    private final CourtResourceRepository courtResourceRepository;
    private final CourtHoldRepository courtHoldRepository;
    private final CourtHoldSlotRepository courtHoldSlotRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RecurringPackageRepository packageRepository;
    private final RecurringSessionRepository sessionRepository;
    private final RecurringCreditRepository creditRepository;
    private final SessionChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;
    private final CourtHoldTransactionService holdTransactionService;
    private final SepayService sepayService;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final BookingPolicyProperties policyProperties;
    private final Clock clock;

    public RecurringBookingService(
            CourtRepository courtRepository,
            CourtResourceRepository courtResourceRepository,
            CourtHoldRepository courtHoldRepository,
            CourtHoldSlotRepository courtHoldSlotRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            RecurringPackageRepository packageRepository,
            RecurringSessionRepository sessionRepository,
            RecurringCreditRepository creditRepository,
            SessionChangeRequestRepository changeRequestRepository,
            UserRepository userRepository,
            CourtHoldTransactionService holdTransactionService,
            SepayService sepayService,
            WalletService walletService,
            NotificationService notificationService,
            BookingPolicyProperties policyProperties,
            Clock clock
    ) {
        this.courtRepository = courtRepository;
        this.courtResourceRepository = courtResourceRepository;
        this.courtHoldRepository = courtHoldRepository;
        this.courtHoldSlotRepository = courtHoldSlotRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.packageRepository = packageRepository;
        this.sessionRepository = sessionRepository;
        this.creditRepository = creditRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.userRepository = userRepository;
        this.holdTransactionService = holdTransactionService;
        this.sepayService = sepayService;
        this.walletService = walletService;
        this.notificationService = notificationService;
        this.policyProperties = policyProperties;
        this.clock = clock;
    }

    /**
     * Sinh chính xác 12 ngày cùng thứ liên tiếp theo múi giờ Asia/Ho_Chi_Minh,
     * kể cả khi khoảng 3 tháng lịch có 13 ngày cùng thứ hoặc đi qua tháng mới / năm mới.
     */
    public List<LocalDate> generateWeeklyDates(LocalDate startDate, int totalSessions) {
        List<LocalDate> dates = new ArrayList<>(totalSessions);
        for (int i = 0; i < totalSessions; i++) {
            dates.add(startDate.plusWeeks(i));
        }
        return dates;
    }

    public static String weekdayLabel(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "Thứ Hai";
            case TUESDAY -> "Thứ Ba";
            case WEDNESDAY -> "Thứ Tư";
            case THURSDAY -> "Thứ Năm";
            case FRIDAY -> "Thứ Sáu";
            case SATURDAY -> "Thứ Bảy";
            case SUNDAY -> "Chủ Nhật";
        };
    }

    // =========================================================================
    // 1. XEM TRƯỚC GÓI 12 BUỔI TRƯỚC THANH TOÁN
    // =========================================================================

    @Transactional(readOnly = true)
    public RecurringDtos.PackagePreviewResponse previewPackage(RecurringDtos.PreviewRequest req, String userId) {
        Court court = holdTransactionService.resolveCourt(req.courtId());
        CourtField field = holdTransactionService.resolveField(court, req.fieldDetail());
        LocalDate startDate = BookingRules.parseDate(req.startDate());
        List<String> normalizedSlots = BookingRules.validateSlots(req.slots(), startDate, clock, MAX_RECURRING_ADVANCE_DAYS);
        String mergedTimeRange = String.join(", ", normalizedSlots);

        List<String> resourceIds = holdTransactionService.resolvePhysicalResourceIds(court, field);
        List<CourtHoldService.SlotInterval> intervals = parseSlotIntervals(normalizedSlots);
        int unitPrice = sepayService.calculatePrice(court, mergedTimeRange, "QR_TRANSFER").totalPrice;

        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> activeBookings = loadActiveBookingsForCourt(court.getId());
        List<LocalDate> sessionDates = generateWeeklyDates(startDate, DEFAULT_TOTAL_SESSIONS);

        List<RecurringDtos.SessionPreviewItem> previews = new ArrayList<>(DEFAULT_TOTAL_SESSIONS);
        List<ConflictSlotDto> conflicts = new ArrayList<>();

        for (int i = 0; i < sessionDates.size(); i++) {
            int idx = i + 1;
            LocalDate d = sessionDates.get(i);
            String dateStr = d.format(BookingRules.DATE);
            ConflictSlotDto conflict = findConflictOnDate(court.getId(), field, resourceIds, dateStr, intervals, now, activeBookings, idx);
            if (conflict != null) {
                conflicts.add(conflict);
                previews.add(new RecurringDtos.SessionPreviewItem(
                        idx,
                        dateStr,
                        weekdayLabel(d.getDayOfWeek()),
                        mergedTimeRange,
                        field.getDetail(),
                        unitPrice,
                        false,
                        conflict.getStatus(),
                        "Trùng khung giờ " + conflict.getSlot() + " ngày " + dateStr
                ));
            } else {
                previews.add(new RecurringDtos.SessionPreviewItem(
                        idx,
                        dateStr,
                        weekdayLabel(d.getDayOfWeek()),
                        mergedTimeRange,
                        field.getDetail(),
                        unitPrice,
                        true,
                        null,
                        null
                ));
            }
        }

        Integer walletBalance = null;
        if (userId != null && !userId.isBlank()) {
            walletBalance = walletService.getOrCreateWallet(userId).getBalance();
        }

        int totalPrice = unitPrice * DEFAULT_TOTAL_SESSIONS;
        return new RecurringDtos.PackagePreviewResponse(
                court.getId(),
                court.getName(),
                field.getId(),
                field.getDetail(),
                DEFAULT_PACKAGE_TYPE,
                DEFAULT_PACKAGE_TITLE,
                startDate.format(BookingRules.DATE),
                weekdayLabel(startDate.getDayOfWeek()),
                mergedTimeRange,
                normalizedSlots,
                DEFAULT_TOTAL_SESSIONS,
                unitPrice,
                totalPrice,
                conflicts.isEmpty(),
                previews,
                conflicts,
                walletBalance
        );
    }

    // =========================================================================
    // 2. GIỮ CHỖ NGUYÊN TỬ 12 BUỔI & KHỞI TẠO THANH TOÁN (SEPAY HOẶC VÍ)
    // =========================================================================

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> startRecurringCheckout(RecurringDtos.RecurringCheckoutRequest req, String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để đặt lịch cố định.");
        }
        if (req.idempotencyKey() == null || req.idempotencyKey().isBlank() || req.idempotencyKey().length() > 100) {
            throw new IllegalArgumentException("Thiếu hoặc sai mã yêu cầu thanh toán.");
        }
        String method = req.paymentMethod() != null ? req.paymentMethod().trim().toUpperCase() : "QR_TRANSFER";
        if (!List.of("QR_TRANSFER", "WALLET").contains(method)) {
            throw new IllegalArgumentException("Gói cố định chỉ hỗ trợ thanh toán toàn bộ qua SePay (QR_TRANSFER) hoặc Ví EasySport (WALLET).");
        }
        if (req.customerName() == null || req.customerName().isBlank() || req.customerName().length() > 100
                || req.customerPhone() == null || !req.customerPhone().matches("[+0-9]{9,15}")) {
            throw new IllegalArgumentException("Vui lòng nhập họ tên và số điện thoại hợp lệ.");
        }

        Court court = holdTransactionService.resolveCourt(req.courtId());
        courtRepository.lockById(court.getId()).orElseThrow(() -> new IllegalArgumentException("Sân không tồn tại."));
        CourtField field = holdTransactionService.resolveField(court, req.fieldDetail());

        LocalDate startDate = BookingRules.parseDate(req.startDate());
        List<String> normalizedSlots = BookingRules.validateSlots(req.slots(), startDate, clock, MAX_RECURRING_ADVANCE_DAYS);
        String startDateStr = startDate.format(BookingRules.DATE);
        String slotsCsv = String.join(",", normalizedSlots);
        String mergedTimeRange = String.join(", ", normalizedSlots);

        String key = UUID.nameUUIDFromBytes((authenticatedUserId + ":REC:" + req.idempotencyKey())
                .getBytes(StandardCharsets.UTF_8)).toString();
        Optional<RecurringPackage> existingOpt = packageRepository.findByIdempotencyKey(key);
        if (existingOpt.isPresent()) {
            RecurringPackage existingPkg = existingOpt.get();
            if (!authenticatedUserId.equals(existingPkg.getUserId())
                    || !court.getId().equals(existingPkg.getCourtId())
                    || !field.getId().equals(existingPkg.getFieldId())
                    || !startDateStr.equals(existingPkg.getStartDate())
                    || !slotsCsv.equals(existingPkg.getSlotsCsv())
                    || !method.equals(existingPkg.getPaymentMethod())) {
                throw new IllegalStateException("Mã yêu cầu đã được sử dụng cho lựa chọn gói cố định khác.");
            }
            CourtHold existingHold = courtHoldRepository.findById(existingPkg.getHoldId()).orElseThrow();
            Payment existingPayment = paymentRepository.findById(existingPkg.getPaymentId()).orElseThrow();
            return buildCheckoutResponseMap(existingPkg, existingHold, existingPayment, LocalDateTime.now(clock));
        }

        List<String> resourceIds = new ArrayList<>(holdTransactionService.resolvePhysicalResourceIds(court, field));
        if (resourceIds.isEmpty()) {
            throw new IllegalArgumentException("Không xác định được tài nguyên vật lý cho sân: " + field.getDetail());
        }
        Collections.sort(resourceIds);
        ensureResourcesExist(resourceIds, court.getId());
        courtResourceRepository.lockResourcesForUpdate(resourceIds);

        List<CourtHoldService.SlotInterval> intervals = parseSlotIntervals(normalizedSlots);
        if (intervals.isEmpty()) {
            throw new IllegalArgumentException("Khung giờ thuê không hợp lệ.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> activeBookings = loadActiveBookingsForCourt(court.getId());
        List<LocalDate> sessionDates = generateWeeklyDates(startDate, DEFAULT_TOTAL_SESSIONS);

        // Kiểm tra xung đột toàn bộ 12 buổi. Nếu 1 buổi xung đột -> rollback toàn bộ (All-or-Nothing)
        List<ConflictSlotDto> conflicts = new ArrayList<>();
        for (int i = 0; i < sessionDates.size(); i++) {
            String dateStr = sessionDates.get(i).format(BookingRules.DATE);
            ConflictSlotDto conflict = findConflictOnDate(court.getId(), field, resourceIds, dateStr, intervals, now, activeBookings, i + 1);
            if (conflict != null) {
                conflicts.add(conflict);
            }
        }
        if (!conflicts.isEmpty()) {
            throw new SlotConflictException(conflicts, now);
        }

        int unitPrice = sepayService.calculatePrice(court, mergedTimeRange, "QR_TRANSFER").totalPrice;
        int totalPrice = unitPrice * DEFAULT_TOTAL_SESSIONS;

        String packageId = "PKG_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String holdId = "HOLD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String paymentId = "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String invoiceNumber = "ESPKG" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
        String checkoutToken = UUID.randomUUID().toString().replace("-", "");

        boolean isWallet = "WALLET".equals(method);
        LocalDateTime paymentDeadline = now.plusMinutes(5);
        LocalDateTime verificationDeadline = paymentDeadline.plusMinutes(1);
        LocalDateTime confirmedSlotExpiry = sessionDates.get(sessionDates.size() - 1).plusDays(7).atStartOfDay();

        if (isWallet) {
            walletService.debitWallet(
                    authenticatedUserId,
                    totalPrice,
                    "DEBIT_PACKAGE",
                    "Thanh toán gói cố định 12 buổi tại " + court.getName() + " (" + field.getDetail() + ")",
                    null,
                    packageId,
                    "DEBIT_PKG:" + key
            );
        }

        RecurringPackage pkg = new RecurringPackage();
        pkg.setId(packageId);
        pkg.setUserId(authenticatedUserId);
        pkg.setCourtId(court.getId());
        pkg.setCourtName(court.getName());
        pkg.setCourtAddress(court.getAddress() != null ? court.getAddress() : "");
        pkg.setFieldId(field.getId());
        pkg.setFieldDetail(field.getDetail());
        pkg.setPackageType(DEFAULT_PACKAGE_TYPE);
        pkg.setPackageTitle(DEFAULT_PACKAGE_TITLE);
        pkg.setStartDate(startDateStr);
        pkg.setWeekdayLabel(weekdayLabel(startDate.getDayOfWeek()));
        pkg.setTimeRange(mergedTimeRange);
        pkg.setSlotsCsv(slotsCsv);
        pkg.setSlotCount(normalizedSlots.size());
        pkg.setTotalSessions(DEFAULT_TOTAL_SESSIONS);
        pkg.setUnitPrice(unitPrice);
        pkg.setTotalPrice(totalPrice);
        pkg.setTotalPaid(isWallet ? totalPrice : 0);
        pkg.setTotalRefunded(0);
        pkg.setPaymentMethod(method);
        pkg.setStatus(isWallet ? "ACTIVE" : "AWAITING_PAYMENT");
        pkg.setHoldId(holdId);
        pkg.setPaymentId(paymentId);
        pkg.setCustomerName(req.customerName().trim());
        pkg.setCustomerPhone(req.customerPhone().trim());
        pkg.setIdempotencyKey(key);
        pkg.setCreatedAt(now);
        packageRepository.save(pkg);

        String firstBookingId = null;
        List<Booking> bookingsToSave = new ArrayList<>(DEFAULT_TOTAL_SESSIONS);
        List<RecurringSession> sessionsToSave = new ArrayList<>(DEFAULT_TOTAL_SESSIONS);
        List<CourtHoldSlot> slotsToSave = new ArrayList<>();

        for (int i = 0; i < sessionDates.size(); i++) {
            int idx = i + 1;
            String dateStr = sessionDates.get(i).format(BookingRules.DATE);
            String sessionId = "RS_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            String bookingId = "BK" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            if (firstBookingId == null) {
                firstBookingId = bookingId;
            }

            Booking b = new Booking(
                    bookingId,
                    authenticatedUserId,
                    court.getId(),
                    court.getName(),
                    field.getDetail(),
                    mergedTimeRange,
                    dateStr,
                    unitPrice,
                    unitPrice,
                    0,
                    method,
                    isWallet ? "PAID" : "PENDING",
                    req.customerName().trim(),
                    req.customerPhone().trim(),
                    court.getAddress() != null ? court.getAddress() : "",
                    isWallet ? confirmedSlotExpiry : verificationDeadline
            );
            b.setCreatedAt(now);
            b.setRecurringPackageId(packageId);
            b.setRecurringSessionId(sessionId);
            bookingsToSave.add(b);

            RecurringSession rs = new RecurringSession();
            rs.setId(sessionId);
            rs.setPackageId(packageId);
            rs.setUserId(authenticatedUserId);
            rs.setCourtId(court.getId());
            rs.setFieldId(field.getId());
            rs.setFieldDetail(field.getDetail());
            rs.setSessionIndex(idx);
            rs.setSessionDate(dateStr);
            rs.setTimeRange(mergedTimeRange);
            rs.setUnitPriceSnapshot(unitPrice);
            rs.setPaidAmountSnapshot(isWallet ? unitPrice : 0);
            rs.setRefundedAmount(0);
            rs.setStatus(isWallet ? "UPCOMING" : "PENDING_PAYMENT");
            rs.setBookingId(bookingId);
            sessionsToSave.add(rs);

            LocalDateTime slotExp = isWallet ? confirmedSlotExpiry : verificationDeadline;
            String slotStatus = isWallet ? "CONFIRMED" : "AWAITING_PAYMENT";
            for (String resId : resourceIds) {
                for (CourtHoldService.SlotInterval interval : intervals) {
                    slotsToSave.add(new CourtHoldSlot(
                            UUID.randomUUID().toString().replace("-", ""),
                            holdId,
                            court.getId(),
                            resId,
                            dateStr,
                            interval.startMinute,
                            interval.endMinute,
                            interval.originalLabel,
                            slotStatus,
                            slotExp
                    ));
                }
            }
        }

        bookingRepository.saveAll(bookingsToSave);
        sessionRepository.saveAll(sessionsToSave);
        courtHoldSlotRepository.saveAll(slotsToSave);

        CourtHold hold = new CourtHold(
                holdId,
                firstBookingId,
                paymentId,
                authenticatedUserId,
                court.getId(),
                field.getId(),
                field.getDetail(),
                startDateStr,
                mergedTimeRange,
                slotsCsv,
                String.join(",", resourceIds),
                now,
                paymentDeadline,
                verificationDeadline,
                key
        );
        hold.setRecurringPackageId(packageId);
        if (isWallet) {
            hold.setStatus("CONFIRMED");
            hold.setConfirmedAt(now);
        }
        courtHoldRepository.save(hold);

        Payment payment = new Payment(
                paymentId,
                firstBookingId,
                invoiceNumber,
                totalPrice,
                "VND",
                method,
                checkoutToken,
                paymentDeadline
        );
        payment.setRecurringPackageId(packageId);
        if (isWallet) {
            payment.setStatus("PAID");
            payment.setConfirmedAmount(totalPrice);
            payment.setConfirmedAt(now);
        }
        paymentRepository.save(payment);

        if (isWallet && notificationService != null) {
            notificationService.createRecurringConfirmedNotif(
                    authenticatedUserId, packageId, court.getName(),
                    field.getDetail(), mergedTimeRange, startDateStr,
                    DEFAULT_TOTAL_SESSIONS, firstBookingId
            );
        }

        return buildCheckoutResponseMap(pkg, hold, payment, now);
    }

    @Transactional(readOnly = true)
    public BookingPaymentStatusResponse getPackagePaymentStatus(String packageId, String authenticatedUserId) {
        RecurringPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy gói lịch cố định."));
        if (!Objects.equals(pkg.getUserId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem giao dịch của gói này.");
        }
        Payment payment = pkg.getPaymentId() != null ? paymentRepository.findById(pkg.getPaymentId()).orElse(null) : null;
        CourtHold hold = pkg.getHoldId() != null ? courtHoldRepository.findById(pkg.getHoldId()).orElse(null) : null;
        LocalDateTime now = LocalDateTime.now(clock);

        BookingPaymentStatusResponse res = new BookingPaymentStatusResponse();
        res.setBookingId(pkg.getId());
        res.setBookingStatus(pkg.getStatus());
        res.setTotalPrice(pkg.getTotalPrice());
        res.setDepositAmount(pkg.getTotalPaid());
        res.setRemainingAmount(Math.max(0, pkg.getTotalPrice() - pkg.getTotalPaid()));
        res.setPaymentMethod(pkg.getPaymentMethod());
        res.setPaid("ACTIVE".equalsIgnoreCase(pkg.getStatus()) || "COMPLETED".equalsIgnoreCase(pkg.getStatus()));
        res.setDepositPaid(false);
        res.setExpired(hold != null ? hold.isExpired(now) : "EXPIRED".equalsIgnoreCase(pkg.getStatus()));
        res.setServerNow(now);
        res.setServerNowIso(BookingRules.iso(now));

        if (hold != null) {
            res.setPaymentDeadline(hold.getPaymentDeadline());
            res.setVerificationDeadline(hold.getVerificationDeadline());
            res.setHoldStatus(hold.getDynamicStatus(now));
            res.setPaymentDeadlineIso(hold.getPaymentDeadline() != null ? BookingRules.iso(hold.getPaymentDeadline()) : null);
            res.setVerificationDeadlineIso(hold.getVerificationDeadline() != null ? BookingRules.iso(hold.getVerificationDeadline()) : null);
            res.setExpiresAt(hold.getPaymentDeadline());
        }
        if (payment != null) {
            res.setPaymentId(payment.getId());
            res.setOrderInvoiceNumber(payment.getOrderInvoiceNumber());
            res.setPaymentStatus(payment.getStatus());
            res.setPaymentAmount(payment.getAmount());
            res.setCheckoutUrl("/payments/" + payment.getCheckoutToken() + "/checkout");
        }
        return res;
    }

    public void releasePendingPackageHold(String packageId, String authenticatedUserId) {
        RecurringPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy gói lịch cố định."));
        if (!Objects.equals(pkg.getUserId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác gói lịch cố định này.");
        }
        if (pkg.getHoldId() != null) {
            holdTransactionService.releaseHoldTransaction(pkg.getHoldId(), authenticatedUserId);
        }
    }

    // =========================================================================
    // 3. DANH SÁCH & CHI TIẾT LỊCH CỐ ĐỊNH CỦA KHÁCH
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyPackages(String userId) {
        List<RecurringPackage> packages = packageRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>(packages.size());
        for (RecurringPackage pkg : packages) {
            result.add(buildPackageDetailMap(pkg));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPackageDetail(String packageId, String userId) {
        RecurringPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy gói lịch cố định."));
        if (!Objects.equals(pkg.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem gói lịch cố định này.");
        }
        return buildPackageDetailMap(pkg);
    }

    // =========================================================================
    // 4. ĐÁNH GIÁ CHÍNH SÁCH HỦY / HOÃN / TRƯỢT LỊCH / BUỔI DƯ / HOÀN VÍ
    // =========================================================================

    @Transactional(readOnly = true)
    public RecurringDtos.CancelPreviewResponse previewSessionAction(
            String sessionId,
            String reasonTypeRaw,
            String authenticatedUserId
    ) {
        RecurringSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi học/đá cố định."));
        if (!Objects.equals(session.getUserId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác buổi sân của người khác.");
        }
        validateSessionModifiableForCustomer(session);

        String reasonType = normalizeReasonType(reasonTypeRaw);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime sessionStart = resolveSessionStartDateTime(session.getSessionDate(), session.getTimeRange());
        double hoursUntilStart = Duration.between(now, sessionStart).toMinutes() / 60.0;

        int earlyMinHours = policyProperties.getEarlyCancelMinHours();
        boolean isEarly = !now.isAfter(sessionStart.minusHours(earlyMinHours));
        boolean weatherAlreadyConfirmed = "WEATHER_APPROVED".equalsIgnoreCase(session.getStatus());

        long recentSelfCancels = countRecentSelfCancellations(authenticatedUserId, now);
        boolean excessiveTriggered = policyProperties.isExcessiveCancelTrackingEnabled()
                && recentSelfCancels >= policyProperties.getExcessiveCancelLimit();

        int maxRefundable = session.getRemainingRefundableAmount();
        String proposedSlideDate = findNextAvailableSlideDate(session).orElse(null);

        if ("WEATHER".equals(reasonType) && !weatherAlreadyConfirmed) {
            return new RecurringDtos.CancelPreviewResponse(
                    session.getId(), session.getSessionDate(), session.getTimeRange(),
                    reasonType, session.getStatus(), hoursUntilStart, earlyMinHours,
                    false, false, false,
                    recentSelfCancels, policyProperties.getExcessiveCancelLimit(), policyProperties.getExcessiveCancelWindowDays(),
                    policyProperties.getNearStartMaxHours(), policyProperties.getLateCancelRefundPercent(),
                    session.getPaidAmountSnapshot(), session.getRefundedAmount(), maxRefundable,
                    true, false, false, false, null, proposedSlideDate,
                    "Lý do thời tiết cần Chủ sân xác nhận trước. Sau khi Chủ sân xác nhận, bạn có thể chọn Trượt lịch sang tuần trống kế tiếp hoặc Giữ 1 buổi còn dư (kể cả sát giờ) và không bị tính vào số lần chủ động hủy.",
                    List.of("REQUEST_WEATHER")
            );
        }

        if (weatherAlreadyConfirmed) {
            List<String> actions = new ArrayList<>();
            if (proposedSlideDate != null) actions.add("SLIDE");
            actions.add("KEEP_CREDIT");
            return new RecurringDtos.CancelPreviewResponse(
                    session.getId(), session.getSessionDate(), session.getTimeRange(),
                    "WEATHER", session.getStatus(), hoursUntilStart, earlyMinHours,
                    true, true, false,
                    recentSelfCancels, policyProperties.getExcessiveCancelLimit(), policyProperties.getExcessiveCancelWindowDays(),
                    policyProperties.getNearStartMaxHours(), policyProperties.getLateCancelRefundPercent(),
                    session.getPaidAmountSnapshot(), session.getRefundedAmount(), maxRefundable,
                    false, proposedSlideDate != null, true, false, null, proposedSlideDate,
                    "Chủ sân đã xác nhận hoãn do thời tiết. Bạn được chọn Trượt lịch (đề xuất: "
                            + (proposedSlideDate != null ? proposedSlideDate : "Chưa có tuần trống") + ") hoặc Giữ 1 buổi còn dư để đặt bù sau.",
                    actions
            );
        }

        // PERSONAL reason
        if (isEarly && !excessiveTriggered) {
            List<String> actions = new ArrayList<>();
            if (proposedSlideDate != null) actions.add("SLIDE");
            actions.add("KEEP_CREDIT");
            actions.add("REFUND_WALLET"); // gửi yêu cầu hoàn ví để Owner duyệt nếu chính sách hoàn chưa cố định
            return new RecurringDtos.CancelPreviewResponse(
                    session.getId(), session.getSessionDate(), session.getTimeRange(),
                    reasonType, session.getStatus(), hoursUntilStart, earlyMinHours,
                    true, false, false,
                    recentSelfCancels, policyProperties.getExcessiveCancelLimit(), policyProperties.getExcessiveCancelWindowDays(),
                    policyProperties.getNearStartMaxHours(), policyProperties.getLateCancelRefundPercent(),
                    session.getPaidAmountSnapshot(), session.getRefundedAmount(), maxRefundable,
                    false, proposedSlideDate != null, true, false, null, proposedSlideDate,
                    "Bạn đủ điều kiện hủy sớm (>= " + earlyMinHours + " giờ trước trận). Chọn Trượt lịch để chuyển sang tuần trống kế tiếp"
                            + (proposedSlideDate != null ? " (" + proposedSlideDate + ")" : "")
                            + " hoặc Giữ 1 buổi còn dư (dùng đặt bù, khác với số dư Ví EasySport). Yêu cầu hoàn tiền vào Ví (tối đa "
                            + formatVnd(maxRefundable) + ") cần Chủ sân phê duyệt.",
                    actions
            );
        }

        // Hủy muộn (< 168h) hoặc vượt quá số lần chủ động hủy
        Integer configuredPct = excessiveTriggered
                ? policyProperties.getExcessiveCancelRefundPercent()
                : (policyProperties.getNearStartMaxHours() != null && hoursUntilStart < policyProperties.getNearStartMaxHours()
                ? policyProperties.getLateCancelRefundPercent()
                : policyProperties.getLateCancelRefundPercent());

        if (configuredPct != null && (!excessiveTriggered ? policyProperties.isLateCancelPolicyConfigured() : true)) {
            int refundAmt = (int) Math.min(maxRefundable, Math.round(maxRefundable * (configuredPct / 100.0)));
            return new RecurringDtos.CancelPreviewResponse(
                    session.getId(), session.getSessionDate(), session.getTimeRange(),
                    reasonType, session.getStatus(), hoursUntilStart, earlyMinHours,
                    false, false, excessiveTriggered,
                    recentSelfCancels, policyProperties.getExcessiveCancelLimit(), policyProperties.getExcessiveCancelWindowDays(),
                    policyProperties.getNearStartMaxHours(), configuredPct,
                    session.getPaidAmountSnapshot(), session.getRefundedAmount(), maxRefundable,
                    false, false, false, true, refundAmt, proposedSlideDate,
                    "Áp dụng tỷ lệ hoàn cấu hình (" + configuredPct + "%): bạn sẽ nhận " + formatVnd(refundAmt) + " vào Ví EasySport.",
                    List.of("REFUND_WALLET")
            );
        }

        // Chính sách chưa được cấu hình -> bắt buộc đưa về chờ Owner duyệt, không tự áp phạt hay 100%
        String notice = excessiveTriggered
                ? "Bạn đã hủy chủ động nhiều lần (" + recentSelfCancels + " lần). Chính sách hoàn/giữ buổi cần Chủ sân xem xét và phê duyệt (tối đa " + formatVnd(maxRefundable) + ")."
                : "Hủy dưới " + earlyMinHours + " giờ trước trận và chính sách tỷ lệ hoàn sát giờ chưa được cấu hình tự động. Yêu cầu sẽ chuyển Chủ sân quyết định (hoàn ví tối đa " + formatVnd(maxRefundable) + ", cấp 1 buổi dư hoặc trượt lịch).";

        return new RecurringDtos.CancelPreviewResponse(
                session.getId(), session.getSessionDate(), session.getTimeRange(),
                reasonType, session.getStatus(), hoursUntilStart, earlyMinHours,
                false, false, excessiveTriggered,
                recentSelfCancels, policyProperties.getExcessiveCancelLimit(), policyProperties.getExcessiveCancelWindowDays(),
                policyProperties.getNearStartMaxHours(), null,
                session.getPaidAmountSnapshot(), session.getRefundedAmount(), maxRefundable,
                true, false, false, false, null, proposedSlideDate,
                notice,
                List.of("REQUEST_REVIEW")
        );
    }

    // =========================================================================
    // 5. THỰC THI HỦY / HOÃN / TRƯỢT LỊCH / GIỮ BUỔI DƯ
    // =========================================================================

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> cancelOrPostponeSession(
            String sessionId,
            RecurringDtos.CancelOrPostponeRequest req,
            String authenticatedUserId
    ) {
        RecurringSession snapshot = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi cố định."));
        if (!Objects.equals(snapshot.getUserId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác buổi sân của người khác.");
        }

        Court court = courtRepository.lockById(snapshot.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Cụm sân không tồn tại."));
        CourtField field = holdTransactionService.resolveField(court, snapshot.getFieldDetail());
        List<String> resourceIds = new ArrayList<>(holdTransactionService.resolvePhysicalResourceIds(court, field));
        Collections.sort(resourceIds);
        ensureResourcesExist(resourceIds, court.getId());
        courtResourceRepository.lockResourcesForUpdate(resourceIds);

        RecurringPackage pkg = packageRepository.lockById(snapshot.getPackageId()).orElseThrow();
        RecurringSession session = sessionRepository.lockById(sessionId).orElseThrow();
        validateSessionModifiableForCustomer(session);

        String reasonType = normalizeReasonType(req.reasonType());
        String chosenAction = req.chosenAction() != null ? req.chosenAction().trim().toUpperCase() : "";
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime sessionStart = resolveSessionStartDateTime(session.getSessionDate(), session.getTimeRange());
        boolean isEarly = !now.isAfter(sessionStart.minusHours(policyProperties.getEarlyCancelMinHours()));
        boolean weatherAlreadyConfirmed = "WEATHER_APPROVED".equalsIgnoreCase(session.getStatus());

        long recentSelfCancels = countRecentSelfCancellations(authenticatedUserId, now);
        boolean excessiveTriggered = policyProperties.isExcessiveCancelTrackingEnabled()
                && recentSelfCancels >= policyProperties.getExcessiveCancelLimit();

        // A) Khách gửi yêu cầu hoãn do thời tiết (khi Chủ sân chưa xác nhận trước)
        if ("WEATHER".equals(reasonType) && !weatherAlreadyConfirmed) {
            SessionChangeRequest scr = buildChangeRequest(pkg, session, "WEATHER_POSTPONE", chosenAction.isEmpty() ? "SLIDE" : chosenAction, req.note(), false, now);
            changeRequestRepository.save(scr);
            session.setStatus("WEATHER_PENDING");
            session.setResolutionNote(req.note());
            sessionRepository.save(session);
            return Map.of(
                    "outcome", "WEATHER_PENDING",
                    "session", session,
                    "request", scr,
                    "message", "Đã gửi yêu cầu hoãn do thời tiết. Sau khi Chủ sân xác nhận, bạn có thể chọn Trượt lịch hoặc Giữ 1 buổi còn dư."
            );
        }

        // B) Được phép chọn ngay (Đã duyệt thời tiết HOẶC hủy sớm >= 168h và chưa vượt ngưỡng hủy)
        boolean canSelfResolve = weatherAlreadyConfirmed || (isEarly && !excessiveTriggered);
        if (canSelfResolve && "SLIDE".equals(chosenAction)) {
            RecurringSession slid = slideSessionInternal(pkg, session, court, field, resourceIds, req.expectedSlideDate(), !weatherAlreadyConfirmed, now);
            return Map.of(
                    "outcome", "RESCHEDULED",
                    "session", slid,
                    "message", "Đã chốt lịch trượt sang ngày " + slid.getRescheduledToDate() + " (" + slid.getTimeRange() + ")."
            );
        }
        if (canSelfResolve && "KEEP_CREDIT".equals(chosenAction)) {
            RecurringCredit credit = grantCreditInternal(pkg, session, !weatherAlreadyConfirmed, now);
            return Map.of(
                    "outcome", "CREDIT_GRANTED",
                    "session", session,
                    "credit", credit,
                    "message", "Đã bảo lưu 1 buổi còn dư vào gói cố định của bạn."
            );
        }

        // C) Khách yêu cầu hoàn vào Ví hoặc Hủy muộn / Vượt ngưỡng hủy
        if ("PERSONAL".equals(reasonType) && (!isEarly || excessiveTriggered)) {
            LocalDateTime hoursCheckNow = now;
            double hoursUntilStart = Duration.between(hoursCheckNow, sessionStart).toMinutes() / 60.0;
            Integer configuredPct = excessiveTriggered
                    ? policyProperties.getExcessiveCancelRefundPercent()
                    : (policyProperties.getNearStartMaxHours() != null && hoursUntilStart < policyProperties.getNearStartMaxHours()
                    ? policyProperties.getLateCancelRefundPercent()
                    : policyProperties.getLateCancelRefundPercent());
            if (configuredPct != null && (excessiveTriggered || policyProperties.isLateCancelPolicyConfigured())) {
                int refundAmt = (int) Math.min(session.getRemainingRefundableAmount(),
                        Math.round(session.getRemainingRefundableAmount() * (configuredPct / 100.0)));
                SessionChangeRequest scr = buildChangeRequest(pkg, session,
                        excessiveTriggered ? "EXCESSIVE_CANCEL" : "LATE_CANCEL_OR_REFUND",
                        "REFUND_WALLET", req.note(), true, now);
                scr.setStatus("AUTO_APPLIED");
                scr.setApprovedRefundAmount(refundAmt);
                scr.setResolvedOutcome(refundAmt > 0 ? "REFUNDED" : "CANCELLED_NO_REFUND");
                scr.setResolvedAt(now);
                changeRequestRepository.save(scr);

                applyRefundOrZeroCancelInternal(pkg, session, refundAmt, "Hoàn tiền hủy buổi ngày " + session.getSessionDate() + " (" + configuredPct + "%)", "AUTO_POLICY:" + scr.getId(), now);
                return Map.of(
                        "outcome", scr.getResolvedOutcome(),
                        "session", session,
                        "request", scr,
                        "refundedAmount", refundAmt,
                        "message", refundAmt > 0
                                ? "Đã hủy buổi và hoàn " + formatVnd(refundAmt) + " vào Ví EasySport."
                                : "Đã hủy buổi theo chính sách cấu hình (hoàn 0 đ)."
                );
            }
        }

        // Mặc định khi chưa có cấu hình tỷ lệ / sát giờ / yêu cầu hoàn ví -> chuyển Owner duyệt
        String reqType = excessiveTriggered ? "EXCESSIVE_CANCEL" : "LATE_CANCEL_OR_REFUND";
        SessionChangeRequest pendingReq = buildChangeRequest(
                pkg, session, reqType,
                chosenAction.isEmpty() ? "REFUND_WALLET" : chosenAction,
                req.note(), true, now
        );
        changeRequestRepository.save(pendingReq);
        session.setStatus("PENDING_OWNER_REVIEW");
        session.setResolutionNote(req.note());
        sessionRepository.save(session);

        return Map.of(
                "outcome", "PENDING_OWNER_REVIEW",
                "session", session,
                "request", pendingReq,
                "message", "Yêu cầu hủy/hoàn tiền đã được chuyển tới Chủ sân phê duyệt. Chúng tôi không tự ý áp phạt hoặc khấu trừ khi chưa có duyệt của Chủ sân."
        );
    }

    // =========================================================================
    // 6. SỬ DỤNG BUỔI CÒN DƯ ĐỂ ĐẶT BÙ (KHÔNG CHỒNG LẤN, TRỪ QUYỀN SAU KHI THÀNH CÔNG)
    // =========================================================================

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> redeemCredit(String creditId, RecurringDtos.RedeemCreditRequest req, String authenticatedUserId) {
        RecurringCredit creditSnapshot = creditRepository.findById(creditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy quyền buổi còn dư."));
        if (!Objects.equals(creditSnapshot.getUserId(), authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền sử dụng buổi dư của người khác.");
        }

        Court court = courtRepository.lockById(creditSnapshot.getCourtId())
                .orElseThrow(() -> new IllegalArgumentException("Cụm sân không tồn tại."));
        String targetFieldQuery = (req.fieldDetail() != null && !req.fieldDetail().isBlank())
                ? req.fieldDetail()
                : creditSnapshot.getFieldDetail();
        CourtField field = holdTransactionService.resolveField(court, targetFieldQuery);

        List<String> resourceIds = new ArrayList<>(holdTransactionService.resolvePhysicalResourceIds(court, field));
        Collections.sort(resourceIds);
        ensureResourcesExist(resourceIds, court.getId());
        courtResourceRepository.lockResourcesForUpdate(resourceIds);

        RecurringPackage pkg = packageRepository.lockById(creditSnapshot.getPackageId()).orElseThrow();
        RecurringCredit credit = creditRepository.lockById(creditId).orElseThrow();
        if (!"AVAILABLE".equalsIgnoreCase(credit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi còn dư này đã được sử dụng trước đó.");
        }

        LocalDate targetDate = BookingRules.parseDate(req.dateLabel());
        List<String> normalizedSlots = BookingRules.validateSlots(req.slots(), targetDate, clock, MAX_RECURRING_ADVANCE_DAYS);
        if (normalizedSlots.size() != credit.getSlotCount()) {
            throw new IllegalArgumentException("Mỗi buổi còn dư chỉ dùng đặt bù đúng " + credit.getSlotCount() + " ca tương ứng với gói đã mua.");
        }
        String targetDateStr = targetDate.format(BookingRules.DATE);
        String mergedTimeRange = String.join(", ", normalizedSlots);
        List<CourtHoldService.SlotInterval> intervals = parseSlotIntervals(normalizedSlots);
        LocalDateTime now = LocalDateTime.now(clock);

        // Kiểm tra sân trống: nếu trùng thì ném 409, transaction rollback -> credit VẪN CÒN AVAILABLE
        List<Booking> activeBookings = loadActiveBookingsForCourt(court.getId());
        ConflictSlotDto conflict = findConflictOnDate(court.getId(), field, resourceIds, targetDateStr, intervals, now, activeBookings, null);
        if (conflict != null) {
            throw new SlotConflictException(List.of(conflict), now);
        }

        String bookingId = "BKMU" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Booking makeupBooking = new Booking(
                bookingId,
                authenticatedUserId,
                court.getId(),
                court.getName(),
                field.getDetail(),
                mergedTimeRange,
                targetDateStr,
                0,
                0,
                0,
                "CREDIT",
                "PAID",
                pkg.getCustomerName(),
                pkg.getCustomerPhone(),
                court.getAddress() != null ? court.getAddress() : "",
                targetDate.plusDays(7).atStartOfDay()
        );
        makeupBooking.setCreatedAt(now);
        makeupBooking.setRecurringPackageId(pkg.getId());
        makeupBooking.setRecurringSessionId(credit.getSourceSessionId());
        makeupBooking.setServiceNote("Đặt bù bằng buổi còn dư #" + credit.getId());
        bookingRepository.save(makeupBooking);

        String holdId = pkg.getHoldId() != null ? pkg.getHoldId() : ("HOLD_MU_" + bookingId);
        LocalDateTime slotExp = targetDate.plusDays(7).atStartOfDay();
        for (String resId : resourceIds) {
            for (CourtHoldService.SlotInterval interval : intervals) {
                courtHoldSlotRepository.save(new CourtHoldSlot(
                        UUID.randomUUID().toString().replace("-", ""),
                        holdId,
                        court.getId(),
                        resId,
                        targetDateStr,
                        interval.startMinute,
                        interval.endMinute,
                        interval.originalLabel,
                        "CONFIRMED",
                        slotExp
                ));
            }
        }

        // Chỉ trừ quyền sau khi mọi kiểm tra & tạo booking thành công
        credit.setStatus("USED");
        credit.setRedeemedBookingId(bookingId);
        credit.setRedeemedDate(targetDateStr);
        credit.setRedeemedTimeRange(mergedTimeRange);
        credit.setUsedAt(now);
        creditRepository.save(credit);

        sessionRepository.findById(credit.getSourceSessionId()).ifPresent(src -> {
            src.setRescheduledToDate(targetDateStr);
            src.setReplacementBookingId(bookingId);
            src.setResolutionNote("Đã đặt bù ngày " + targetDateStr + " (" + mergedTimeRange + ")");
            sessionRepository.save(src);
        });

        if (notificationService != null) {
            notificationService.createMakeupBookedNotif(
                    authenticatedUserId, credit.getId(), court.getName(),
                    targetDateStr, mergedTimeRange, bookingId
            );
        }

        return Map.of(
                "booking", makeupBooking,
                "credit", credit,
                "message", "Đã đặt bù thành công cho ngày " + targetDateStr + " (" + mergedTimeRange + ")."
        );
    }

    // =========================================================================
    // 7. PHÍA CHỦ SÂN (OWNER): XÁC NHẬN THỜI TIẾT, DUYỆT YÊU CẦU & HOÀN TIỀN VÍ
    // =========================================================================

    @Transactional(readOnly = true)
    public List<SessionChangeRequest> getChangeRequestsForOwnerCourts(Set<String> ownerCourtIds) {
        if (ownerCourtIds == null || ownerCourtIds.isEmpty()) {
            return List.of();
        }
        return changeRequestRepository.findByCourtIdInOrderByCreatedAtDesc(new ArrayList<>(ownerCourtIds));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveRecurringSessionsForOwnerCourts(Set<String> ownerCourtIds) {
        if (ownerCourtIds == null || ownerCourtIds.isEmpty()) {
            return List.of();
        }
        List<RecurringPackage> pkgs = packageRepository.findByCourtIdInOrderByCreatedAtDesc(new ArrayList<>(ownerCourtIds));
        Map<String, RecurringPackage> pkgMap = pkgs.stream().collect(Collectors.toMap(RecurringPackage::getId, p -> p));
        List<RecurringSession> sessions = sessionRepository.findByCourtIdInOrderBySessionIndexAsc(new ArrayList<>(ownerCourtIds));

        List<Map<String, Object>> list = new ArrayList<>();
        for (RecurringSession s : sessions) {
            if (!"UPCOMING".equalsIgnoreCase(s.getStatus()) && !"WEATHER_PENDING".equalsIgnoreCase(s.getStatus())) {
                continue;
            }
            RecurringPackage p = pkgMap.get(s.getPackageId());
            if (p == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sessionId", s.getId());
            m.put("packageId", s.getPackageId());
            m.put("courtId", s.getCourtId());
            m.put("courtName", p.getCourtName());
            m.put("fieldDetail", s.getFieldDetail());
            m.put("sessionIndex", s.getSessionIndex());
            m.put("sessionDate", s.getSessionDate());
            m.put("timeRange", s.getTimeRange());
            m.put("status", s.getStatus());
            m.put("customerName", p.getCustomerName());
            m.put("customerPhone", p.getCustomerPhone());
            list.add(m);
        }
        return list;
    }

    /**
     * Chủ sân chủ động hoãn 1 buổi sắp tới do thời tiết.
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> ownerProactiveWeatherPostpone(
            String sessionId,
            String ownerNote,
            User owner,
            Set<String> ownerCourtIds
    ) {
        RecurringSession snapshot = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi cố định."));
        if (ownerCourtIds == null || !ownerCourtIds.contains(snapshot.getCourtId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền hoãn buổi của cụm sân khác.");
        }

        Court court = courtRepository.lockById(snapshot.getCourtId()).orElseThrow();
        if (court.getOwnerId() != null && !Objects.equals(court.getOwnerId(), owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền hoãn buổi của cụm sân khác.");
        }
        CourtField field = holdTransactionService.resolveField(court, snapshot.getFieldDetail());
        List<String> resourceIds = new ArrayList<>(holdTransactionService.resolvePhysicalResourceIds(court, field));
        Collections.sort(resourceIds);
        ensureResourcesExist(resourceIds, court.getId());
        courtResourceRepository.lockResourcesForUpdate(resourceIds);

        RecurringPackage pkg = packageRepository.lockById(snapshot.getPackageId()).orElseThrow();
        RecurringSession session = sessionRepository.lockById(sessionId).orElseThrow();
        if (!"UPCOMING".equalsIgnoreCase(session.getStatus()) && !"WEATHER_PENDING".equalsIgnoreCase(session.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi này đã diễn ra hoặc đã được xử lý trước đó.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        releaseOldSessionBookingAndSlots(pkg, session, "CANCELLED");

        String noteText = ownerNote != null && !ownerNote.isBlank() ? ownerNote.trim() : "Chủ sân chủ động hoãn do thời tiết xấu";
        session.setStatus("WEATHER_APPROVED");
        session.setResolutionNote(noteText);
        sessionRepository.save(session);

        // Nếu đang có yêu cầu thời tiết pending thì đánh dấu đã duyệt luôn
        for (SessionChangeRequest r : changeRequestRepository.findBySessionIdOrderByCreatedAtDesc(session.getId())) {
            if ("PENDING".equalsIgnoreCase(r.getStatus()) && "WEATHER_POSTPONE".equalsIgnoreCase(r.getRequestType())) {
                r.setStatus("APPROVED");
                r.setResolvedOutcome("WEATHER_APPROVED");
                r.setOwnerNote(noteText);
                r.setResolvedBy(owner.getFullName());
                r.setResolvedAt(now);
                r.setCountsTowardCancelLimit(false);
                changeRequestRepository.save(r);
            }
        }

        if (notificationService != null) {
            notificationService.createWeatherPostponeNotif(
                    session.getUserId(), session.getId(), court.getName(),
                    session.getSessionDate(), session.getTimeRange(), session.getBookingId()
            );
        }

        return Map.of(
                "session", session,
                "message", "Đã xác nhận hoãn buổi ngày " + session.getSessionDate() + " do thời tiết. Khách có thể chọn Trượt lịch hoặc Giữ 1 buổi còn dư."
        );
    }

    /**
     * Chủ sân duyệt yêu cầu (Weather, Hoàn ví, Buổi dư, Trượt lịch, Từ chối) trong cùng 1 transaction.
     * Chống bấm đúp / hoàn tiền trùng tuyệt đối bằng PESSIMISTIC_WRITE + status check + dedupKey.
     */
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> ownerResolveChangeRequest(
            String requestId,
            RecurringDtos.OwnerResolveChangeRequest req,
            User owner,
            Set<String> ownerCourtIds
    ) {
        SessionChangeRequest reqSnap = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu xử lý."));
        if (ownerCourtIds == null || !ownerCourtIds.contains(reqSnap.getCourtId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xử lý yêu cầu của cụm sân khác.");
        }

        Court court = courtRepository.lockById(reqSnap.getCourtId()).orElseThrow();
        if (court.getOwnerId() != null && !Objects.equals(court.getOwnerId(), owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xử lý yêu cầu của cụm sân khác.");
        }

        RecurringPackage pkg = packageRepository.lockById(reqSnap.getPackageId()).orElseThrow();
        RecurringSession session = sessionRepository.lockById(reqSnap.getSessionId()).orElseThrow();
        SessionChangeRequest changeReq = changeRequestRepository.lockById(requestId).orElseThrow();

        if (!"PENDING".equalsIgnoreCase(changeReq.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu này đã được xử lý trước đó.");
        }
        if (List.of("COMPLETED", "CHECKED_IN", "RESCHEDULED", "CREDIT_GRANTED", "REFUNDED", "CANCELLED")
                .contains(session.getStatus().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi gốc đã hoàn tất hoặc đã có kết quả xử lý.");
        }

        CourtField field = holdTransactionService.resolveField(court, session.getFieldDetail());
        List<String> resourceIds = new ArrayList<>(holdTransactionService.resolvePhysicalResourceIds(court, field));
        Collections.sort(resourceIds);
        ensureResourcesExist(resourceIds, court.getId());
        courtResourceRepository.lockResourcesForUpdate(resourceIds);

        LocalDateTime now = LocalDateTime.now(clock);
        String decision = req.decision() != null ? req.decision().trim().toUpperCase() : "";
        String ownerNote = req.ownerNote() != null ? req.ownerNote().trim() : "";

        switch (decision) {
            case "APPROVE_WEATHER" -> {
                releaseOldSessionBookingAndSlots(pkg, session, "CANCELLED");
                session.setStatus("WEATHER_APPROVED");
                session.setResolutionNote(ownerNote.isEmpty() ? "Chủ sân xác nhận hoãn do thời tiết" : ownerNote);
                sessionRepository.save(session);

                changeReq.setStatus("APPROVED");
                changeReq.setCountsTowardCancelLimit(false);
                changeReq.setResolvedOutcome("WEATHER_APPROVED");
                changeReq.setOwnerNote(ownerNote);
                changeReq.setResolvedBy(owner.getFullName());
                changeReq.setResolvedAt(now);
                changeRequestRepository.save(changeReq);

                if (notificationService != null) {
                    notificationService.createWeatherPostponeNotif(
                            session.getUserId(), session.getId(), court.getName(),
                            session.getSessionDate(), session.getTimeRange(), session.getBookingId()
                    );
                }
                return Map.of("request", changeReq, "session", session, "message", "Đã xác nhận hoãn do thời tiết.");
            }
            case "APPROVE_CREDIT" -> {
                RecurringCredit credit = grantCreditInternal(pkg, session, changeReq.getCountsTowardCancelLimit(), now);
                changeReq.setStatus("APPROVED");
                changeReq.setResolvedOutcome("CREDIT_GRANTED");
                changeReq.setOwnerNote(ownerNote);
                changeReq.setResolvedBy(owner.getFullName());
                changeReq.setResolvedAt(now);
                changeRequestRepository.save(changeReq);
                return Map.of("request", changeReq, "session", session, "credit", credit, "message", "Đã duyệt cấp 1 buổi còn dư.");
            }
            case "APPROVE_SLIDE" -> {
                RecurringSession slid = slideSessionInternal(pkg, session, court, field, resourceIds, null, changeReq.getCountsTowardCancelLimit(), now);
                changeReq.setStatus("APPROVED");
                changeReq.setResolvedOutcome("RESCHEDULED");
                changeReq.setOwnerNote(ownerNote);
                changeReq.setResolvedBy(owner.getFullName());
                changeReq.setResolvedAt(now);
                changeRequestRepository.save(changeReq);
                return Map.of("request", changeReq, "session", slid, "message", "Đã duyệt trượt lịch sang ngày " + slid.getRescheduledToDate() + ".");
            }
            case "APPROVE_REFUND" -> {
                int maxRefund = session.getRemainingRefundableAmount();
                int refundAmount = req.refundAmount() != null ? req.refundAmount() : -1;
                if (refundAmount <= 0 || refundAmount > maxRefund) {
                    throw new IllegalArgumentException(
                            "Số tiền hoàn phải từ 1 đ đến tối đa " + formatVnd(maxRefund) + " (phần thực trả còn lại của buổi)."
                    );
                }

                String reasonText = "Hoàn tiền buổi ngày " + session.getSessionDate() + " tại " + court.getName()
                        + (ownerNote.isEmpty() ? "" : " (" + ownerNote + ")");
                String dedupKey = "REFUND_SESSION:" + session.getId();
                WalletTransaction tx = applyRefundOrZeroCancelInternal(pkg, session, refundAmount, reasonText, dedupKey, now);

                changeReq.setStatus("APPROVED");
                changeReq.setApprovedRefundAmount(refundAmount);
                changeReq.setResolvedOutcome("REFUNDED");
                changeReq.setOwnerNote(ownerNote);
                changeReq.setResolvedBy(owner.getFullName());
                changeReq.setResolvedAt(now);
                changeRequestRepository.save(changeReq);

                return Map.of(
                        "request", changeReq,
                        "session", session,
                        "walletTransaction", tx,
                        "message", "Đã hoàn " + formatVnd(refundAmount) + " vào Ví EasySport của khách."
                );
            }
            case "REJECT" -> {
                session.setStatus("UPCOMING");
                session.setResolutionNote(ownerNote.isEmpty() ? "Chủ sân từ chối yêu cầu" : ownerNote);
                sessionRepository.save(session);

                changeReq.setStatus("REJECTED");
                changeReq.setCountsTowardCancelLimit(false);
                changeReq.setResolvedOutcome("REJECTED");
                changeReq.setOwnerNote(ownerNote);
                changeReq.setResolvedBy(owner.getFullName());
                changeReq.setResolvedAt(now);
                changeRequestRepository.save(changeReq);

                return Map.of("request", changeReq, "session", session, "message", "Đã từ chối yêu cầu, giữ nguyên lịch buổi sân.");
            }
            default -> throw new IllegalArgumentException("Quyết định xử lý không hợp lệ: " + decision);
        }
    }

    // =========================================================================
    // HELPERS NỘI BỘ
    // =========================================================================

    private RecurringSession slideSessionInternal(
            RecurringPackage pkg,
            RecurringSession session,
            Court court,
            CourtField field,
            List<String> resourceIds,
            String expectedSlideDate,
            boolean countAsSelfCancel,
            LocalDateTime now
    ) {
        String newDateStr = findNextAvailableSlideDate(session).orElse(null);
        if (expectedSlideDate != null && !expectedSlideDate.isBlank()) {
            String trimmedExpected = expectedSlideDate.trim();
            LocalDate expDate = BookingRules.parseDate(trimmedExpected);
            List<CourtHoldService.SlotInterval> intervals = parseSlotIntervals(Arrays.asList(pkg.getSlotsCsv().split(",")));
            List<Booking> activeBookings = loadActiveBookingsForCourt(court.getId());
            ConflictSlotDto expectedConflict = findConflictOnDate(court.getId(), field, resourceIds, expDate.format(BookingRules.DATE), intervals, now, activeBookings, null);
            if (expectedConflict != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ngày đề xuất (" + trimmedExpected + ") vừa có người đặt. Vui lòng chọn đề xuất mới ("
                                + (newDateStr != null ? newDateStr : "Chưa có") + ") hoặc Giữ 1 buổi còn dư."
                );
            }
            newDateStr = expDate.format(BookingRules.DATE);
        }

        if (newDateStr == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không tìm thấy tuần trống kế tiếp phù hợp để trượt lịch. Vui lòng chọn Giữ 1 buổi còn dư."
            );
        }

        releaseOldSessionBookingAndSlots(pkg, session, "RESCHEDULED");

        String replacementBookingId = "BKRS" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        LocalDate newDate = BookingRules.parseDate(newDateStr);
        LocalDateTime slotExp = newDate.plusDays(7).atStartOfDay();

        Booking replacementBooking = new Booking(
                replacementBookingId,
                pkg.getUserId(),
                court.getId(),
                court.getName(),
                field.getDetail(),
                session.getTimeRange(),
                newDateStr,
                session.getUnitPriceSnapshot(),
                session.getPaidAmountSnapshot(),
                0,
                pkg.getPaymentMethod(),
                "PAID",
                pkg.getCustomerName(),
                pkg.getCustomerPhone(),
                court.getAddress() != null ? court.getAddress() : "",
                slotExp
        );
        replacementBooking.setCreatedAt(now);
        replacementBooking.setRecurringPackageId(pkg.getId());
        replacementBooking.setRecurringSessionId(session.getId());
        replacementBooking.setServiceNote("Trượt lịch từ buổi #" + session.getSessionIndex() + " (" + session.getSessionDate() + ")");
        bookingRepository.save(replacementBooking);

        String holdId = pkg.getHoldId() != null ? pkg.getHoldId() : ("HOLD_RS_" + replacementBookingId);
        List<CourtHoldService.SlotInterval> intervals = parseSlotIntervals(Arrays.asList(pkg.getSlotsCsv().split(",")));
        for (String resId : resourceIds) {
            for (CourtHoldService.SlotInterval interval : intervals) {
                courtHoldSlotRepository.save(new CourtHoldSlot(
                        UUID.randomUUID().toString().replace("-", ""),
                        holdId,
                        court.getId(),
                        resId,
                        newDateStr,
                        interval.startMinute,
                        interval.endMinute,
                        interval.originalLabel,
                        "CONFIRMED",
                        slotExp
                ));
            }
        }

        session.setStatus("RESCHEDULED");
        session.setRescheduledToDate(newDateStr);
        session.setReplacementBookingId(replacementBookingId);
        session.setResolutionType("SLID");
        session.setResolutionNote("Đã trượt lịch sang ngày " + newDateStr);
        session.setResolvedAt(now);
        sessionRepository.save(session);

        if (countAsSelfCancel) {
            SessionChangeRequest logReq = buildChangeRequest(pkg, session, "SELF_EARLY_CANCEL", "SLIDE", "Trượt lịch sớm", true, now);
            logReq.setStatus("AUTO_APPLIED");
            logReq.setResolvedOutcome("RESCHEDULED");
            logReq.setResolvedAt(now);
            changeRequestRepository.save(logReq);
        }

        if (notificationService != null) {
            notificationService.createRecurringRescheduledNotif(
                    pkg.getUserId(), session.getId(), court.getName(),
                    session.getSessionDate(), newDateStr, session.getTimeRange(), replacementBookingId
            );
        }
        return session;
    }

    private RecurringCredit grantCreditInternal(
            RecurringPackage pkg,
            RecurringSession session,
            boolean countAsSelfCancel,
            LocalDateTime now
    ) {
        if (creditRepository.findBySourceSessionId(session.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi này đã được tạo quyền đặt bù trước đó.");
        }

        releaseOldSessionBookingAndSlots(pkg, session, "CANCELLED");

        RecurringCredit credit = new RecurringCredit();
        credit.setId("CRD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        credit.setPackageId(pkg.getId());
        credit.setSourceSessionId(session.getId());
        credit.setUserId(pkg.getUserId());
        credit.setCourtId(pkg.getCourtId());
        credit.setCourtName(pkg.getCourtName());
        credit.setFieldId(pkg.getFieldId());
        credit.setFieldDetail(pkg.getFieldDetail());
        credit.setSlotCount(pkg.getSlotCount());
        credit.setOriginalPriceSnapshot(session.getPaidAmountSnapshot());
        credit.setStatus("AVAILABLE");
        credit.setCreatedAt(now);
        creditRepository.save(credit);

        session.setStatus("CREDIT_GRANTED");
        session.setCreditId(credit.getId());
        session.setResolutionType("CREDIT_GRANTED");
        session.setResolutionNote("Đã quy đổi thành 1 buổi còn dư");
        session.setResolvedAt(now);
        sessionRepository.save(session);

        if (countAsSelfCancel) {
            SessionChangeRequest logReq = buildChangeRequest(pkg, session, "SELF_EARLY_CANCEL", "KEEP_CREDIT", "Giữ 1 buổi còn dư", true, now);
            logReq.setStatus("AUTO_APPLIED");
            logReq.setResolvedOutcome("CREDIT_GRANTED");
            logReq.setResolvedAt(now);
            changeRequestRepository.save(logReq);
        }
        return credit;
    }

    private WalletTransaction applyRefundOrZeroCancelInternal(
            RecurringPackage pkg,
            RecurringSession session,
            int refundAmount,
            String reasonText,
            String dedupKey,
            LocalDateTime now
    ) {
        releaseOldSessionBookingAndSlots(pkg, session, refundAmount > 0 ? "REFUNDED" : "CANCELLED");

        WalletTransaction tx = null;
        if (refundAmount > 0) {
            tx = walletService.creditRefund(
                    pkg.getUserId(),
                    refundAmount,
                    reasonText,
                    session.getBookingId(),
                    pkg.getId(),
                    session.getId(),
                    dedupKey
            );
            session.setRefundedAmount(session.getRefundedAmount() + refundAmount);
            session.setWalletTransactionId(tx.getId());
            session.setStatus("REFUNDED");
            session.setResolutionType("REFUNDED");
            session.setResolutionNote("Đã hoàn " + formatVnd(refundAmount) + " vào Ví EasySport");
            pkg.setTotalRefunded(pkg.getTotalRefunded() + refundAmount);
            packageRepository.save(pkg);

            if (notificationService != null) {
                notificationService.createWalletRefundNotif(
                        pkg.getUserId(), tx.getId(), refundAmount, reasonText, session.getBookingId()
                );
            }
        } else {
            session.setStatus("CANCELLED");
            session.setResolutionType("CANCELLED");
            session.setResolutionNote("Đã hủy theo chính sách (hoàn 0 đ)");
        }
        session.setResolvedAt(now);
        sessionRepository.save(session);
        return tx;
    }

    private void releaseOldSessionBookingAndSlots(RecurringPackage pkg, RecurringSession session, String newBookingStatus) {
        if (pkg.getHoldId() != null) {
            List<CourtHoldSlot> oldSlots = courtHoldSlotRepository.findByHoldIdAndDateLabel(pkg.getHoldId(), session.getSessionDate());
            List<CourtHoldService.SlotInterval> intervals = parseSlotIntervals(Arrays.asList(pkg.getSlotsCsv().split(",")));
            List<CourtHoldSlot> toRelease = new ArrayList<>();
            for (CourtHoldSlot s : oldSlots) {
                if ("RELEASED".equalsIgnoreCase(s.getStatus())) continue;
                boolean matchesTime = intervals.stream()
                        .anyMatch(iv -> iv.startMinute < s.getEndMinute() && s.getStartMinute() < iv.endMinute);
                if (matchesTime) {
                    s.setStatus("RELEASED");
                    toRelease.add(s);
                }
            }
            if (!toRelease.isEmpty()) {
                courtHoldSlotRepository.saveAll(toRelease);
            }
        }
        if (session.getBookingId() != null) {
            bookingRepository.findById(session.getBookingId()).ifPresent(b -> {
                b.setStatus(newBookingStatus);
                b.setPayment(newBookingStatus);
                bookingRepository.save(b);
            });
        }
    }

    public Optional<String> findNextAvailableSlideDate(RecurringSession session) {
        RecurringPackage pkg = packageRepository.findById(session.getPackageId()).orElse(null);
        if (pkg == null) return Optional.empty();

        Court court = courtRepository.findById(session.getCourtId()).orElse(null);
        if (court == null) return Optional.empty();
        CourtField field = holdTransactionService.resolveField(court, session.getFieldDetail());
        List<String> resourceIds = holdTransactionService.resolvePhysicalResourceIds(court, field);
        List<CourtHoldService.SlotInterval> intervals = parseSlotIntervals(Arrays.asList(pkg.getSlotsCsv().split(",")));
        List<Booking> activeBookings = loadActiveBookingsForCourt(court.getId());
        LocalDateTime now = LocalDateTime.now(clock);

        LocalDate baseDate = BookingRules.parseDate(session.getSessionDate());
        int searchLimit = pkg.getTotalSessions() + policyProperties.getRescheduleMaxWeeks();
        for (int w = 1; w <= searchLimit; w++) {
            LocalDate candidate = baseDate.plusWeeks(w);
            String candidateStr = candidate.format(BookingRules.DATE);
            ConflictSlotDto conflict = findConflictOnDate(court.getId(), field, resourceIds, candidateStr, intervals, now, activeBookings, null);
            if (conflict == null) {
                return Optional.of(candidateStr);
            }
        }
        return Optional.empty();
    }

    private ConflictSlotDto findConflictOnDate(
            String courtId,
            CourtField field,
            List<String> resourceIds,
            String dateStr,
            List<CourtHoldService.SlotInterval> intervals,
            LocalDateTime now,
            List<Booking> activeBookings,
            Integer sessionIndex
    ) {
        for (CourtHoldService.SlotInterval interval : intervals) {
            List<CourtHoldSlot> slotConflicts = courtHoldSlotRepository.findOverlappingActiveSlots(
                    resourceIds, dateStr, interval.startMinute, interval.endMinute, now
            );
            if (!slotConflicts.isEmpty()) {
                CourtHoldSlot s = slotConflicts.get(0);
                return new ConflictSlotDto(interval.originalLabel, s.getStatus(), s.getExpiresAt(), dateStr, sessionIndex);
            }
            for (Booking b : activeBookings) {
                String bDate = StaffController.extractDate(b.getDateLabel());
                if (bDate == null && b.getDateLabel() != null) {
                    bDate = b.getDateLabel().trim();
                }
                if (!dateStr.equals(bDate)) continue;
                boolean overlaps = StaffController.subFieldsOverlap(field.getDetail(), b.getFieldDetail())
                        || StaffController.subFieldsOverlap(field.getLabel(), b.getFieldDetail())
                        || StaffController.subFieldsOverlap(field.getId(), b.getFieldDetail());
                if (!overlaps) continue;
                LocalTime[] bTimes = StaffController.extractTimeInterval(b.getTimeRange());
                if (bTimes == null) continue;
                int bStart = bTimes[0].getHour() * 60 + bTimes[0].getMinute();
                int bEnd = bTimes[1].getHour() * 60 + bTimes[1].getMinute();
                if (interval.startMinute < bEnd && bStart < interval.endMinute) {
                    return new ConflictSlotDto(interval.originalLabel, "BOOKED", null, dateStr, sessionIndex);
                }
            }
        }
        return null;
    }

    private List<Booking> loadActiveBookingsForCourt(String courtId) {
        List<Booking> all = bookingRepository.findAllByOrderByCreatedAtDesc();
        if (all == null) return List.of();
        return all.stream()
                .filter(b -> Objects.equals(courtId, b.getCourtId()))
                .filter(b -> {
                    String st = b.getStatus() != null ? b.getStatus().toUpperCase() : "";
                    return st.equals("PAID") || st.equals("DEPOSIT_PAID") || st.equals("CHECKED_IN");
                })
                .toList();
    }

    private Map<String, Object> buildCheckoutResponseMap(
            RecurringPackage pkg,
            CourtHold hold,
            Payment payment,
            LocalDateTime now
    ) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("packageId", pkg.getId());
        m.put("holdId", hold.getId());
        m.put("bookingId", pkg.getId());
        m.put("paymentId", payment.getId());
        m.put("orderInvoiceNumber", payment.getOrderInvoiceNumber());
        m.put("checkoutUrl", "/payments/" + payment.getCheckoutToken() + "/checkout");
        m.put("amount", payment.getAmount());
        m.put("totalPrice", pkg.getTotalPrice());
        m.put("totalSessions", pkg.getTotalSessions());
        m.put("paymentMethod", pkg.getPaymentMethod());
        m.put("packageStatus", pkg.getStatus());
        m.put("holdStatus", hold.getDynamicStatus(now));
        m.put("paymentDeadline", hold.getPaymentDeadline());
        m.put("verificationDeadline", hold.getVerificationDeadline());
        m.put("serverNow", now);
        m.put("paymentDeadlineIso", BookingRules.iso(hold.getPaymentDeadline()));
        m.put("verificationDeadlineIso", BookingRules.iso(hold.getVerificationDeadline()));
        m.put("serverNowIso", BookingRules.iso(now));
        return m;
    }

    private Map<String, Object> buildPackageDetailMap(RecurringPackage pkg) {
        List<RecurringSession> sessions = sessionRepository.findByPackageIdOrderBySessionIndexAsc(pkg.getId());
        List<RecurringCredit> credits = creditRepository.findByPackageIdOrderByCreatedAtDesc(pkg.getId());
        int completedCount = 0;
        int upcomingCount = 0;
        for (RecurringSession s : sessions) {
            if ("COMPLETED".equalsIgnoreCase(s.getStatus()) || "CHECKED_IN".equalsIgnoreCase(s.getStatus())) {
                completedCount++;
            } else if (List.of("UPCOMING", "WEATHER_PENDING", "WEATHER_APPROVED", "PENDING_OWNER_REVIEW")
                    .contains(s.getStatus().toUpperCase())) {
                upcomingCount++;
            }
        }
        int availableCreditsCount = (int) credits.stream().filter(c -> "AVAILABLE".equalsIgnoreCase(c.getStatus())).count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pkg.getId());
        m.put("userId", pkg.getUserId());
        m.put("courtId", pkg.getCourtId());
        m.put("courtName", pkg.getCourtName());
        m.put("courtAddress", pkg.getCourtAddress());
        m.put("fieldId", pkg.getFieldId());
        m.put("fieldDetail", pkg.getFieldDetail());
        m.put("packageType", pkg.getPackageType());
        m.put("packageTitle", pkg.getPackageTitle());
        m.put("startDate", pkg.getStartDate());
        m.put("weekdayLabel", pkg.getWeekdayLabel());
        m.put("timeRange", pkg.getTimeRange());
        m.put("slotCount", pkg.getSlotCount());
        m.put("totalSessions", pkg.getTotalSessions());
        m.put("unitPrice", pkg.getUnitPrice());
        m.put("totalPrice", pkg.getTotalPrice());
        m.put("totalPaid", pkg.getTotalPaid());
        m.put("totalRefunded", pkg.getTotalRefunded());
        m.put("paymentMethod", pkg.getPaymentMethod());
        m.put("status", pkg.getStatus());
        m.put("completedCount", completedCount);
        m.put("upcomingCount", upcomingCount);
        m.put("availableCreditsCount", availableCreditsCount);
        m.put("createdAt", pkg.getCreatedAt());
        m.put("sessions", sessions);
        m.put("credits", credits);
        return m;
    }

    private SessionChangeRequest buildChangeRequest(
            RecurringPackage pkg,
            RecurringSession session,
            String requestType,
            String requestedAction,
            String note,
            boolean countsTowardCancelLimit,
            LocalDateTime now
    ) {
        SessionChangeRequest r = new SessionChangeRequest();
        r.setId("SCR_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        r.setPackageId(pkg.getId());
        r.setSessionId(session.getId());
        r.setUserId(pkg.getUserId());
        r.setCustomerName(pkg.getCustomerName());
        r.setCustomerPhone(pkg.getCustomerPhone());
        r.setCourtId(pkg.getCourtId());
        r.setCourtName(pkg.getCourtName());
        r.setSessionDate(session.getSessionDate());
        r.setTimeRange(session.getTimeRange());
        r.setRequestType(requestType);
        r.setRequestedAction(requestedAction);
        r.setReasonNote(note != null ? note.trim() : "");
        r.setStatus("PENDING");
        r.setCountsTowardCancelLimit(countsTowardCancelLimit);
        r.setMaxRefundableAmount(session.getRemainingRefundableAmount());
        r.setCreatedAt(now);
        return r;
    }

    private void validateSessionModifiableForCustomer(RecurringSession session) {
        String st = session.getStatus() != null ? session.getStatus().toUpperCase() : "";
        if ("COMPLETED".equals(st) || "CHECKED_IN".equals(st)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi đã chơi hoặc đã check-in, không thể hủy hay hoãn.");
        }
        if ("RESCHEDULED".equals(st) || "CREDIT_GRANTED".equals(st) || "REFUNDED".equals(st) || "CANCELLED".equals(st)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mỗi buổi gốc chỉ được xử lý một lần và buổi này đã có kết quả trước đó.");
        }
        if ("PENDING_PAYMENT".equals(st) || "EXPIRED".equals(st) || "REVIEW_REQUIRED".equals(st)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gói chưa được xác nhận thanh toán.");
        }
        if ("WEATHER_PENDING".equals(st) || "PENDING_OWNER_REVIEW".equals(st)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi này đang có yêu cầu chờ Chủ sân xử lý.");
        }
        if (session.getBookingId() != null) {
            bookingRepository.findById(session.getBookingId()).ifPresent(b -> {
                String bst = b.getStatus() != null ? b.getStatus().toUpperCase() : "";
                if ("CHECKED_IN".equals(bst) || "COMPLETED".equals(bst)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi đã check-in hoặc đã hoàn tất tại sân, không thể hủy.");
                }
            });
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime sessionStart = resolveSessionStartDateTime(session.getSessionDate(), session.getTimeRange());
        if (!"WEATHER_APPROVED".equals(st) && !sessionStart.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buổi sân đã đến hoặc qua giờ bắt đầu.");
        }
    }

    private long countRecentSelfCancellations(String userId, LocalDateTime now) {
        int days = policyProperties.getExcessiveCancelWindowDays() != null
                ? Math.max(1, policyProperties.getExcessiveCancelWindowDays())
                : 30;
        return changeRequestRepository.countSelfCancellationsSince(userId, now.minusDays(days));
    }

    private LocalDateTime resolveSessionStartDateTime(String dateStr, String timeRange) {
        LocalDate date = BookingRules.parseDate(dateStr);
        LocalTime[] times = StaffController.extractTimeInterval(timeRange);
        LocalTime startTime = times != null ? times[0] : LocalTime.of(0, 0);
        return date.atTime(startTime);
    }

    private static String normalizeReasonType(String reasonTypeRaw) {
        if (reasonTypeRaw == null) return "PERSONAL";
        String up = reasonTypeRaw.trim().toUpperCase();
        return up.contains("WEATHER") ? "WEATHER" : "PERSONAL";
    }

    private List<CourtHoldService.SlotInterval> parseSlotIntervals(List<String> slots) {
        List<CourtHoldService.SlotInterval> list = new ArrayList<>();
        if (slots == null) return list;
        for (String s : slots) {
            if (s == null || !s.contains("-")) continue;
            String[] parts = s.trim().split("-");
            if (parts.length < 2) continue;
            try {
                int start = toMinutes(parts[0].trim());
                int end = toMinutes(parts[1].trim());
                list.add(new CourtHoldService.SlotInterval(start, end, s.trim()));
            } catch (Exception ignored) {}
        }
        return list;
    }

    private static int toMinutes(String timeStr) {
        String clean = timeStr.replace("–", "-").trim();
        String[] hm = clean.split(":");
        return Integer.parseInt(hm[0].trim()) * 60 + (hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0);
    }

    private void ensureResourcesExist(List<String> resourceIds, String courtId) {
        for (String resId : resourceIds) {
            if (!courtResourceRepository.existsById(resId)) {
                courtResourceRepository.save(new CourtResource(resId, courtId, "Resource " + resId, "Auto-created"));
            }
        }
    }

    private static String formatVnd(int amount) {
        return String.format("%,d đ", amount).replace(",", ".");
    }
}
