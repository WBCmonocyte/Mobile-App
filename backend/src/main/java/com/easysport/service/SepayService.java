package com.easysport.service;

import com.easysport.config.SepayConfig;
import com.easysport.entity.Booking;
import com.easysport.entity.Court;
import com.easysport.entity.CourtHold;
import com.easysport.entity.CourtHoldSlot;
import com.easysport.entity.Payment;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.CourtHoldRepository;
import com.easysport.repository.CourtHoldSlotRepository;
import com.easysport.repository.CourtRepository;
import com.easysport.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SepayService {

    private static final Logger log = LoggerFactory.getLogger(SepayService.class);

    private final SepayConfig sepayConfig;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final CourtHoldRepository courtHoldRepository;
    private final CourtHoldSlotRepository courtHoldSlotRepository;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public SepayService(SepayConfig sepayConfig,
                        PaymentRepository paymentRepository,
                        BookingRepository bookingRepository,
                        CourtRepository courtRepository,
                        CourtHoldRepository courtHoldRepository,
                        CourtHoldSlotRepository courtHoldSlotRepository,
                        Clock clock) {
        this.sepayConfig = sepayConfig;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.courtHoldRepository = courtHoldRepository;
        this.courtHoldSlotRepository = courtHoldSlotRepository;
        this.clock = clock;
    }

    // Constructor tiện lợi cho test độc lập (không đánh dấu @Autowired để Spring dùng constructor đầy đủ)
    public SepayService(SepayConfig sepayConfig,
                        PaymentRepository paymentRepository,
                        BookingRepository bookingRepository,
                        CourtRepository courtRepository) {
        this(sepayConfig, paymentRepository, bookingRepository, courtRepository, null, null, Clock.systemDefaultZone());
    }

    public static class PriceResult {
        public final int totalPrice;
        public final int depositAmount;
        public final int onlineAmount;
        public final int remainingAmount;

        public PriceResult(int totalPrice, int depositAmount, int onlineAmount, int remainingAmount) {
            this.totalPrice = totalPrice;
            this.depositAmount = depositAmount;
            this.onlineAmount = onlineAmount;
            this.remainingAmount = remainingAmount;
        }
    }

    /**
     * Tính toán giá tiền từ database sân thể thao.
     */
    public PriceResult calculatePrice(Court court, String timeRange, String paymentMethod) {
        int total = 0;
        if (timeRange != null && !timeRange.isBlank()) {
            String[] slots = timeRange.split(",");
            for (String slot : slots) {
                total += calculateSingleSlotPrice(court, slot.trim());
            }
        }
        if (total <= 0) {
            total = (court.getPricePerHour() != null ? court.getPricePerHour() : 300000);
        }

        int deposit = (int) Math.round(total * 0.3);
        boolean isDeposit = "CASH_ON_SITE".equalsIgnoreCase(paymentMethod);
        int onlineAmount = isDeposit ? deposit : total;
        int remaining = isDeposit ? (total - deposit) : 0;

        return new PriceResult(total, deposit, onlineAmount, remaining);
    }

    private int calculateSingleSlotPrice(Court court, String slot) {
        if (slot.contains("-")) {
            String startPart = slot.split("-")[0].trim().replace("–", "-");
            try {
                int hour = Integer.parseInt(startPart.split(":")[0].trim());
                if (hour >= 6 && hour < 12) {
                    return (court.getMorningPrice() != null ? court.getMorningPrice() : 150000);
                } else if (hour >= 12 && hour < 18) {
                    return (court.getAfternoonPrice() != null ? court.getAfternoonPrice() : 250000);
                } else {
                    return (court.getEveningPrice() != null ? court.getEveningPrice() : 300000);
                }
            } catch (Exception ignored) {}
        }
        return (court.getPricePerHour() != null ? court.getPricePerHour() : 300000);
    }

    /**
     * Tạo chữ ký HMAC-SHA256 Base64 đúng theo chuẩn tài liệu SePay
     */
    public String generateSignature(Map<String, String> fields, String secretKey) {
        String[] orderedKeys = new String[]{
                "order_amount", "merchant", "currency", "operation",
                "order_description", "order_invoice_number", "customer_id",
                "payment_method", "success_url", "error_url", "cancel_url"
        };

        StringBuilder signData = new StringBuilder();
        for (String key : orderedKeys) {
            String value = fields.get(key);
            if (value != null && !value.isEmpty() && !value.equals("0")) {
                if (signData.length() > 0) {
                    signData.append(",");
                }
                signData.append(key).append("=").append(value);
            }
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(signData.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh chữ ký SePay: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo form HTML POST có chữ ký để gửi sang SePay Sandbox checkout/init,
     * hoặc render trang thanh toán mô phỏng khi chưa cấu hình SePay (Demo mode).
     */
    public String renderCheckoutHtml(Payment payment, Booking booking) {
        if (!sepayConfig.isConfigured()) {
            return renderDemoCheckoutHtml(payment, booking);
        }

        String publicUrl = sepayConfig.getAppPublicUrl();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("order_amount", String.valueOf(payment.getAmount()));
        fields.put("merchant", sepayConfig.getMerchantId());
        fields.put("currency", payment.getCurrency());
        fields.put("operation", "PURCHASE");
        fields.put("order_description", ("Thanh toan EasySport: " + booking.getCourtName()
                + " - " + booking.getFieldDetail()).replaceAll("[\\r\\n]+", " ").strip());
        fields.put("order_invoice_number", payment.getOrderInvoiceNumber());
        fields.put("success_url", publicUrl + "/payments/callback/success?invoice=" + payment.getOrderInvoiceNumber());
        fields.put("error_url", publicUrl + "/payments/callback/error?invoice=" + payment.getOrderInvoiceNumber());
        fields.put("cancel_url", publicUrl + "/payments/callback/cancel?invoice=" + payment.getOrderInvoiceNumber());

        String signature = generateSignature(fields, sepayConfig.getSecretKey());
        fields.put("signature", signature);

        StringBuilder formInputs = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            formInputs.append(String.format("        <input type=\"hidden\" name=\"%s\" value=\"%s\" />\n",
                    escapeHtml(entry.getKey()), escapeHtml(entry.getValue())));
        }

        return "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Chuyển hướng đến SePay Sandbox...</title>\n" +
                "    <style>\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "               background: #F4F7FB; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }\n" +
                "        .card { background: white; padding: 32px; border-radius: 20px; box-shadow: 0 8px 30px rgba(0,0,0,0.06); text-align: center; max-width: 420px; width: 90%; }\n" +
                "        .spinner { border: 4px solid #EBF2FA; border-top: 4px solid #3B6698; border-radius: 50%; width: 44px; height: 44px; animation: spin 1s linear infinite; margin: 0 auto 20px; }\n" +
                "        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }\n" +
                "        h2 { color: #1A2E46; margin: 0 0 8px; font-size: 20px; }\n" +
                "        p { color: #748398; font-size: 14px; line-height: 1.5; margin: 0 0 20px; }\n" +
                "        .btn { background: #3B6698; color: white; border: none; padding: 12px 24px; border-radius: 12px; font-weight: 600; cursor: pointer; text-decoration: none; display: inline-block; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <div class=\"spinner\"></div>\n" +
                "        <h2>Đang kết nối SePay Sandbox</h2>\n" +
                "        <p>Vui lòng đợi giây lát để chuyển tiếp đến cổng thanh toán an toàn...</p>\n" +
                "        <form id=\"sepayForm\" action=\"" + escapeHtml(sepayConfig.getCheckoutUrl()) + "\" method=\"POST\">\n" +
                formInputs +
                "            <button type=\"submit\" class=\"btn\">Nhấn vào đây nếu không tự chuyển</button>\n" +
                "        </form>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        setTimeout(function() { document.getElementById('sepayForm').submit(); }, 600);\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Trang thanh toán Demo hỗ trợ kiểm thử luồng đặt sân trực tiếp trên ứng dụng
     * khi chưa thiết lập tài khoản SePay thật.
     */
    public String renderDemoCheckoutHtml(Payment payment, Booking booking) {
        String amountVnd = String.format("%,d VNĐ", payment.getAmount());
        String invoice = escapeHtml(payment.getOrderInvoiceNumber());
        String courtName = escapeHtml(booking.getCourtName());
        String fieldDetail = escapeHtml(booking.getFieldDetail());
        String timeRange = escapeHtml(booking.getTimeRange());
        String dateLabel = escapeHtml(booking.getDateLabel());
        String token = escapeHtml(payment.getCheckoutToken());

        return "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Cổng Thanh Toán EasySport Demo</title>\n" +
                "    <style>\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #F4F7FB; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 16px; box-sizing: border-box; }\n" +
                "        .card { background: white; padding: 28px 24px; border-radius: 20px; box-shadow: 0 8px 30px rgba(0,0,0,0.08); max-width: 440px; width: 100%; box-sizing: border-box; text-align: center; }\n" +
                "        .badge { display: inline-block; background: #E8F5E9; color: #2E7D32; font-size: 12px; font-weight: 700; padding: 4px 12px; border-radius: 20px; margin-bottom: 12px; }\n" +
                "        h2 { color: #1A2E46; margin: 0 0 8px; font-size: 22px; }\n" +
                "        .amount { font-size: 28px; font-weight: 800; color: #1E8E3E; margin: 12px 0 20px; }\n" +
                "        .info-box { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 14px; text-align: left; margin-bottom: 20px; font-size: 14px; line-height: 1.8; color: #334155; }\n" +
                "        .info-row { display: flex; justify-content: space-between; }\n" +
                "        .info-label { color: #64748B; }\n" +
                "        .info-val { font-weight: 600; color: #0F172A; text-align: right; }\n" +
                "        .btn { background: #1E8E3E; color: white; border: none; padding: 14px 20px; border-radius: 12px; font-weight: 700; width: 100%; font-size: 15px; cursor: pointer; transition: background 0.2s; box-shadow: 0 4px 12px rgba(30,142,62,0.25); }\n" +
                "        .btn:hover { background: #166534; }\n" +
                "        .note { margin-top: 14px; font-size: 12px; color: #94A3B8; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <div class=\"badge\">MÔ PHỎNG THANH TOÁN (DEMO MODE)</div>\n" +
                "        <h2>EasySport Payment</h2>\n" +
                "        <div class=\"amount\">" + amountVnd + "</div>\n" +
                "        <div class=\"info-box\">\n" +
                "            <div class=\"info-row\"><span class=\"info-label\">Mã hóa đơn:</span><span class=\"info-val\">" + invoice + "</span></div>\n" +
                "            <div class=\"info-row\"><span class=\"info-label\">Sân:</span><span class=\"info-val\">" + courtName + "</span></div>\n" +
                "            <div class=\"info-row\"><span class=\"info-label\">Loại sân:</span><span class=\"info-val\">" + fieldDetail + "</span></div>\n" +
                "            <div class=\"info-row\"><span class=\"info-label\">Ngày thuê:</span><span class=\"info-val\">" + dateLabel + "</span></div>\n" +
                "            <div class=\"info-row\"><span class=\"info-label\">Khung giờ:</span><span class=\"info-val\">" + timeRange + "</span></div>\n" +
                "        </div>\n" +
                "        <form method=\"POST\" action=\"/payments/" + token + "/mock-confirm\">\n" +
                "            <button type=\"submit\" class=\"btn\">✓ Xác nhận đã thanh toán thành công</button>\n" +
                "        </form>\n" +
                "        <div class=\"note\">Sau khi nhấn, hệ thống sẽ xác nhận đơn và tự động đồng bộ về ứng dụng EasySport.</div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Xác nhận thanh toán thành công trực tiếp cho chế độ Demo / Mock test
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public boolean confirmMockPayment(Payment payment, Booking booking) {
        LocalDateTime now = LocalDateTime.now(clock);
        courtRepository.lockById(booking.getCourtId()).orElseThrow();
        CourtHold hold = courtHoldRepository.findByBookingId(booking.getId()).orElse(null);
        if (hold != null) hold = courtHoldRepository.findByIdWithLock(hold.getId()).orElseThrow();
        Payment lockedPayment = paymentRepository.lockById(payment.getId()).orElseThrow();

        boolean valid = hold != null && (hold.isAwaitingPayment(now) || hold.isVerifyingPayment(now))
                && "PENDING".equals(booking.getStatus()) && now.isBefore(hold.getVerificationDeadline());

        if (!valid) return false;

        lockedPayment.setConfirmedAmount(lockedPayment.getAmount());
        lockedPayment.setConfirmedAt(now);
        lockedPayment.setSepayTransactionId("MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        lockedPayment.setStatus("PAID");
        paymentRepository.save(lockedPayment);

        hold.setStatus("CONFIRMED");
        hold.setConfirmedAt(now);
        for (CourtHoldSlot slot : courtHoldSlotRepository.findByHoldId(hold.getId())) {
            slot.setStatus("CONFIRMED");
            courtHoldSlotRepository.save(slot);
        }
        courtHoldRepository.save(hold);

        String status = "CASH_ON_SITE".equals(lockedPayment.getPaymentMethod()) ? "DEPOSIT_PAID" : "PAID";
        booking.setStatus(status);
        booking.setPayment(status);
        bookingRepository.save(booking);
        return true;
    }

    /**
     * Xử lý IPN Webhook từ SePay gửi tới backend.
     * Quy tắc:
     * - Thanh toán trong vòng 6 phút (5 phút thanh toán + 1 phút xác minh) -> CONFIRMED (PAID/DEPOSIT_PAID)
     * - Thanh toán đến sau 6 phút (đã hết hạn) -> REVIEW_REQUIRED, không cướp slot người mới.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public boolean processIpn(String ipnSecretHeader, Map<String, Object> payload) {
        if (!sepayConfig.isConfigured()) throw new IllegalStateException("SePay chưa được cấu hình.");
        if (ipnSecretHeader == null || !java.security.MessageDigest.isEqual(
                sepayConfig.getIpnSecret().getBytes(StandardCharsets.UTF_8),
                ipnSecretHeader.getBytes(StandardCharsets.UTF_8))) {
            return rejectIpn(ipnSecretHeader == null ? "missing_secret_header" : "invalid_secret_header");
        }
        if (isSandboxSetupProbe(payload)) {
            log.info("SePay Sandbox integration test IPN acknowledged");
            return true;
        }
        if (!"ORDER_PAID".equals(payload.get("notification_type"))) return rejectIpn("unexpected_notification_type");
        if (!(payload.get("order") instanceof Map<?, ?> order)
                || !(payload.get("transaction") instanceof Map<?, ?> txn)) return rejectIpn("missing_order_or_transaction");
        if (!"CAPTURED".equals(order.get("order_status")) || !"PAYMENT".equals(txn.get("transaction_type"))
                || !"APPROVED".equals(txn.get("transaction_status"))) return rejectIpn("unexpected_payment_status");
        String invoice = Objects.toString(order.get("order_invoice_number"), "");
        String txnId = Objects.toString(txn.get("id"), "");
        if (invoice.isBlank() || txnId.isBlank() || txnId.length() > 100) return rejectIpn("invalid_identifiers");
        Payment snapshot = paymentRepository.findByOrderInvoiceNumber(invoice).orElse(null);
        if (snapshot == null) return rejectIpn("unknown_invoice");
        Booking booking = bookingRepository.findById(snapshot.getBookingId()).orElse(null);
        if (booking == null) return rejectIpn("booking_not_found");
        // All reservation writers take the same court lock BEFORE the hold/payment locks.
        courtRepository.lockById(booking.getCourtId()).orElseThrow();
        CourtHold hold = courtHoldRepository.findByBookingId(booking.getId()).orElse(null);
        if (hold != null) hold = courtHoldRepository.findByIdWithLock(hold.getId()).orElseThrow();
        Payment payment = paymentRepository.lockById(snapshot.getId()).orElseThrow();
        int orderAmount = parseAmount(order.get("order_amount"));
        int received = parseAmount(txn.get("transaction_amount"));
        if (!"VND".equals(order.get("order_currency")) || !"VND".equals(txn.get("transaction_currency"))
                || orderAmount != payment.getAmount() || received != payment.getAmount()) return rejectIpn("amount_or_currency_mismatch");
        if (payment.getSepayTransactionId() != null)
            return txnId.equals(payment.getSepayTransactionId()) || rejectIpn("different_transaction_for_paid_invoice");
        if (paymentRepository.existsBySepayTransactionId(txnId)) return rejectIpn("transaction_already_used");
        LocalDateTime now = LocalDateTime.now(clock);
        // Provider's transaction_date has no documented timezone: use receipt time conservatively.
        // Do not guess its timezone or revive cancelled/expired reservations.
        boolean valid = hold != null && (hold.isAwaitingPayment(now) || hold.isVerifyingPayment(now))
                && "PENDING".equals(booking.getStatus()) && now.isBefore(hold.getVerificationDeadline());
        payment.setConfirmedAmount(received);
        payment.setConfirmedAt(now);
        payment.setSepayTransactionId(txnId);
        payment.setStatus(valid ? "PAID" : "REVIEW_REQUIRED");
        paymentRepository.save(payment);
        if (valid) {
            hold.setStatus("CONFIRMED");
            hold.setConfirmedAt(now);
            for (CourtHoldSlot slot : courtHoldSlotRepository.findByHoldId(hold.getId())) {
                slot.setStatus("CONFIRMED");
                courtHoldSlotRepository.save(slot);
            }
            String status = "CASH_ON_SITE".equals(payment.getPaymentMethod()) ? "DEPOSIT_PAID" : "PAID";
            booking.setStatus(status);
            booking.setPayment(status);
        } else {
            // Preserve a previously confirmed booking if a legacy second attempt is paid.
            if (hold == null || !hold.isConfirmed()) {
                booking.setStatus("REVIEW_REQUIRED");
                booking.setPayment("REVIEW_REQUIRED");
                if (hold != null) {
                    hold.setStatus("REVIEW_REQUIRED");
                    for (CourtHoldSlot slot : courtHoldSlotRepository.findByHoldId(hold.getId())) {
                        slot.setStatus("EXPIRED");
                        courtHoldSlotRepository.save(slot);
                    }
                }
            }
        }
        if (hold != null) courtHoldRepository.save(hold);
        bookingRepository.save(booking);
        return true;
    }

    private boolean isSandboxSetupProbe(Map<String, Object> payload) {
        if (!"https://pay-sandbox.sepay.vn/v1/checkout/init".equals(sepayConfig.getCheckoutUrl())
                || !"PAYMENT_SUCCESS".equals(payload.get("notification_type"))) return false;
        if (!(payload.get("order") instanceof Map<?, ?> order)
                || !(order.get("custom_data") instanceof Map<?, ?> flags)
                || !(payload.get("transaction") instanceof Map<?, ?> transaction)) return false;
        return Boolean.TRUE.equals(flags.get("test_mode"))
                && Boolean.TRUE.equals(flags.get("webhook_test"))
                && Objects.toString(order.get("order_id"), "").startsWith("TEST_ORDER_")
                && Objects.toString(transaction.get("transaction_id"), "").startsWith("TEST_TXN_")
                && "CAPTURE".equals(transaction.get("transaction_type"));
    }

    private boolean rejectIpn(String reason) {
        log.warn("SePay IPN rejected: {}", reason);
        return false;
    }

    private int parseAmount(Object value) {
        try {
            int amount = new java.math.BigDecimal(String.valueOf(value)).intValueExact();
            return amount > 0 ? amount : -1;
        } catch (NumberFormatException | ArithmeticException ex) { return -1; }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
