package com.easysport.controller;

import com.easysport.config.SepayConfig;
import com.easysport.dto.ApiResponse;
import com.easysport.dto.BookingPaymentStatusResponse;
import com.easysport.dto.CreateBookingRequest;
import com.easysport.dto.InitiatePaymentResponse;
import com.easysport.dto.SlotConflictException;
import com.easysport.dto.StartCheckoutRequest;
import com.easysport.dto.StartCheckoutResponse;
import com.easysport.entity.Booking;
import com.easysport.entity.Court;
import com.easysport.entity.CourtHold;
import com.easysport.entity.Payment;
import com.easysport.entity.User;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.CourtHoldRepository;
import com.easysport.repository.CourtRepository;
import com.easysport.repository.PaymentRepository;
import com.easysport.service.AuthService;
import com.easysport.service.CourtHoldService;
import com.easysport.service.SepayService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final PaymentRepository paymentRepository;
    private final CourtHoldRepository courtHoldRepository;
    private final CourtHoldService courtHoldService;
    private final SepayService sepayService;
    private final SepayConfig sepayConfig;
    private final Clock clock;
    private final AuthService authService;

    @org.springframework.beans.factory.annotation.Autowired
    public BookingController(BookingRepository bookingRepository,
                             CourtRepository courtRepository,
                             PaymentRepository paymentRepository,
                             CourtHoldRepository courtHoldRepository,
                             CourtHoldService courtHoldService,
                             SepayService sepayService,
                             SepayConfig sepayConfig,
                             Clock clock,
                             AuthService authService) {
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.paymentRepository = paymentRepository;
        this.courtHoldRepository = courtHoldRepository;
        this.courtHoldService = courtHoldService;
        this.sepayService = sepayService;
        this.sepayConfig = sepayConfig;
        this.clock = clock;
        this.authService = authService;
    }

    // Constructor tương thích cho test cũ
    public BookingController(BookingRepository bookingRepository,
                             CourtRepository courtRepository,
                             PaymentRepository paymentRepository,
                             CourtHoldRepository courtHoldRepository,
                             CourtHoldService courtHoldService,
                             SepayService sepayService,
                             SepayConfig sepayConfig,
                             Clock clock) {
        this(bookingRepository, courtRepository, paymentRepository, courtHoldRepository, courtHoldService, sepayService, sepayConfig, clock, null);
    }

    private String resolveUserId(String authHeader, String headerUserId) {
        User user = authService == null ? null : authService.authenticate(authHeader);
        if (user == null) throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập lại.");
        return user.getId();
    }

    /**
     * Bắt đầu phiên thanh toán SePay:
     * - Xác thực người dùng (từ authHeader hoặc X-User-Id)
     * - Giành giữ sân nguyên tử đúng 5 phút (+ 1 phút xác minh) trong Transaction DB.
     * - Chống tranh chấp khung giờ, all-or-nothing cho nhiều ca.
     * - Chuẩn bị checkout SePay ngoài transaction.
     * - Trả checkoutUrl, holdId, paymentDeadline, verificationDeadline, serverNow, holdStatus.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> startCheckout(
            @Valid @RequestBody StartCheckoutRequest req,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        String effectiveUserId = resolveUserId(authHeader, headerUserId);
        if (effectiveUserId == null) {
            effectiveUserId = (req.getUserId() != null && !req.getUserId().isBlank()) ? req.getUserId().trim() : null;
        }
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để thực hiện đặt sân."));
        }

        req.setUserId(effectiveUserId);
        StartCheckoutResponse response = courtHoldService.startCheckout(req, effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success("Giành giữ sân thành công và khởi tạo thanh toán SePay", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Booking>>> getBookings(
            @RequestParam(required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        String effectiveUserId = resolveUserId(authHeader, headerUserId);
        if (effectiveUserId == null) {
            effectiveUserId = userId;
        }

        List<Booking> list = bookingRepository.findByUserIdOrderByCreatedAtDesc(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch đặt thành công", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBookingById(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Optional<Booking> opt = bookingRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy đơn đặt sân: " + id));
        }
        Booking b = opt.get();
        String effectiveUserId = resolveUserId(authHeader, headerUserId);
        if (effectiveUserId != null && !b.getUserId().equals(effectiveUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền xem đơn đặt sân này."));
        }
        return ResponseEntity.ok(ApiResponse.success("Chi tiết đơn đặt sân", b));
    }

    public ResponseEntity<ApiResponse<Booking>> getBookingById(String id, String headerUserId) {
        return getBookingById(id, null, headerUserId);
    }

    /**
     * Tạo đơn đặt sân (tương thích ngược với client cũ), sử dụng luồng giữ sân
     */
    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody CreateBookingRequest req,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        String effectiveUserId = resolveUserId(authHeader, headerUserId);
        if (effectiveUserId == null) {
            effectiveUserId = (req.getUserId() != null && !req.getUserId().isBlank()) ? req.getUserId().trim() : null;
        }
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để thực hiện đặt sân."));
        }

        StartCheckoutRequest checkoutReq = new StartCheckoutRequest();
        checkoutReq.setIdempotencyKey(UUID.randomUUID().toString());
        checkoutReq.setCourtId(req.getCourtId());
        checkoutReq.setFieldDetail(req.getFieldDetail());
        checkoutReq.setDateLabel(req.getDateLabel());
        checkoutReq.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "QR_TRANSFER");
        checkoutReq.setCustomerName(req.getCustomerName());
        checkoutReq.setCustomerPhone(req.getCustomerPhone());
        checkoutReq.setAddress(req.getAddress());
        checkoutReq.setUserId(effectiveUserId);

        if (req.getSlots() != null && !req.getSlots().isEmpty()) {
            checkoutReq.setSlots(req.getSlots());
        } else if (req.getTimeRange() != null && !req.getTimeRange().isBlank()) {
            checkoutReq.setSlots(Arrays.asList(req.getTimeRange().split(",")));
        } else {
            throw new IllegalArgumentException("Vui lòng chọn ca thuê.");
        }

        CourtHoldService.HoldResult holdResult = courtHoldService.acquireHoldTransaction(checkoutReq);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo đơn đặt sân thành công (Đang giữ chỗ)", holdResult.booking));
    }

    /**
     * Khởi tạo phiên thanh toán SePay cho đơn đặt sân đã có
     */
    @PostMapping("/{id}/payments")
    @Transactional
    public ResponseEntity<ApiResponse<InitiatePaymentResponse>> initiatePayment(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy đơn đặt sân: " + id));
        }

        Booking booking = bookingOpt.get();
        String effectiveUserId = resolveUserId(authHeader, headerUserId);

        if (effectiveUserId != null && !booking.getUserId().equals(effectiveUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền thanh toán cho đơn này."));
        }

        if ("PAID".equalsIgnoreCase(booking.getStatus()) || "DEPOSIT_PAID".equalsIgnoreCase(booking.getStatus()) || "CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Đơn đặt sân này đã được thanh toán thành công trước đó."));
        }

        // Kiểm tra xem hold còn hiệu lực hay không
        CourtHold hold = courtHoldRepository.findByBookingId(id).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);

        if (hold != null && hold.isExpired(now)) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(ApiResponse.error("Thời gian giữ chỗ thanh toán đã hết hạn. Vui lòng tạo đơn mới."));
        }

        // Nếu đang trong phút xác minh (phút thứ 6), không cho phép tạo phiên thanh toán mới
        if (hold != null && hold.isInVerificationWindow(now)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Đang trong thời gian xác minh thanh toán, không thể tạo phiên thanh toán mới."));
        }

        if (hold == null || !hold.isAwaitingPayment(now)) {
            throw new IllegalStateException("Đơn không còn được phép thanh toán.");
        }
        Payment payment = paymentRepository.findById(hold.getPaymentAttemptId()).orElseThrow();
        InitiatePaymentResponse response = new InitiatePaymentResponse(
                payment.getId(), booking.getId(), payment.getOrderInvoiceNumber(),
                payment.getAmount(), payment.getCurrency(),
                "/payments/" + payment.getCheckoutToken() + "/checkout", hold.getPaymentDeadline());
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo giao dịch thanh toán thành công", response));
    }

    /**
     * Tra cứu trạng thái thanh toán và giữ chỗ của đơn đặt sân
     */
    @GetMapping("/{id}/payment-status")
    public ResponseEntity<ApiResponse<BookingPaymentStatusResponse>> getPaymentStatus(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Optional<Booking> bookingOpt = bookingRepository.findById(id);
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy đơn đặt sân: " + id));
        }

        Booking booking = bookingOpt.get();
        String effectiveUserId = resolveUserId(authHeader, headerUserId);

        if (effectiveUserId != null && !booking.getUserId().equals(effectiveUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền xem thông tin thanh toán của đơn này."));
        }

        List<Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(id);
        Payment latestPayment = payments.isEmpty() ? null : payments.get(0);
        CourtHold hold = courtHoldRepository.findByBookingId(id).orElse(null);

        LocalDateTime now = LocalDateTime.now(clock);
        boolean isExpired = (hold != null) ? hold.isExpired(now) : booking.isExpired();

        BookingPaymentStatusResponse res = new BookingPaymentStatusResponse();
        res.setBookingId(booking.getId());
        res.setBookingStatus(booking.getStatus());
        res.setTotalPrice(booking.getTotalPrice());
        res.setDepositAmount(booking.getDepositAmount());
        res.setRemainingAmount(booking.getRemainingAmount());
        res.setPaymentMethod(booking.getPaymentMethod());
        res.setPaid("PAID".equalsIgnoreCase(booking.getStatus()) || "CONFIRMED".equalsIgnoreCase(booking.getStatus()));
        res.setDepositPaid("DEPOSIT_PAID".equalsIgnoreCase(booking.getStatus()));
        res.setExpired(isExpired);
        res.setServerNow(now);
        res.setServerNowIso(com.easysport.service.BookingRules.iso(now));

        if (hold != null) {
            res.setPaymentDeadline(hold.getPaymentDeadline());
            res.setVerificationDeadline(hold.getVerificationDeadline());
            res.setHoldStatus(hold.getDynamicStatus(now));
            res.setPaymentDeadlineIso(hold.getPaymentDeadline() != null ? com.easysport.service.BookingRules.iso(hold.getPaymentDeadline()) : null);
            res.setVerificationDeadlineIso(hold.getVerificationDeadline() != null ? com.easysport.service.BookingRules.iso(hold.getVerificationDeadline()) : null);
            res.setExpiresAt(hold.getPaymentDeadline());
        }

        if (latestPayment != null) {
            res.setPaymentId(latestPayment.getId());
            res.setOrderInvoiceNumber(latestPayment.getOrderInvoiceNumber());
            res.setPaymentStatus(latestPayment.getStatus());
            res.setPaymentAmount(latestPayment.getAmount());
            res.setCheckoutUrl("/payments/" + latestPayment.getCheckoutToken() + "/checkout");
            if (res.getExpiresAt() == null) {
                res.setExpiresAt(latestPayment.getExpiresAt());
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Trạng thái đơn thanh toán", res));
    }

    public ResponseEntity<ApiResponse<BookingPaymentStatusResponse>> getPaymentStatus(String id, String headerUserId) {
        return getPaymentStatus(id, null, headerUserId);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteBooking(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Optional<Booking> opt = bookingRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy đơn đặt để hủy: " + id));
        }
        Booking booking = opt.get();
        String effectiveUserId = resolveUserId(authHeader, headerUserId);
        if (effectiveUserId != null && !booking.getUserId().equals(effectiveUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền hủy đơn này."));
        }

        CourtHold hold = courtHoldRepository.findByBookingId(id).orElseThrow(() ->
                new IllegalStateException("Đơn cũ cần được bộ phận hỗ trợ xử lý."));
        courtHoldService.releaseHold(hold.getId(), effectiveUserId);

        return ResponseEntity.ok(ApiResponse.success("Hủy đơn đặt sân thành công", null));
    }

    /**
     * Xử lý ngoại lệ tranh chấp khung giờ: trả về HTTP 409 Conflict với danh sách slot xung đột
     */
    @ExceptionHandler(SlotConflictException.class)
    public ResponseEntity<Map<String, Object>> handleSlotConflict(SlotConflictException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 409);
        body.put("errorCode", ex.getErrorCode());
        body.put("message", ex.getMessage());
        body.put("conflictingSlots", ex.getConflictingSlots());
        body.put("serverNow", ex.getServerNow());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
