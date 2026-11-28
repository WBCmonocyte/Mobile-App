package com.easysport.controller;

import com.easysport.config.SepayConfig;
import com.easysport.entity.Booking;
import com.easysport.entity.Payment;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.PaymentRepository;
import com.easysport.service.SepayService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@RestController
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final SepayService sepayService;
    private final SepayConfig sepayConfig;
    private final com.easysport.repository.CourtHoldRepository holdRepository;
    private final java.time.Clock clock;

    public PaymentController(PaymentRepository paymentRepository,
                             BookingRepository bookingRepository,
                             SepayService sepayService,
                             SepayConfig sepayConfig,
                             com.easysport.repository.CourtHoldRepository holdRepository, java.time.Clock clock) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.sepayService = sepayService;
        this.sepayConfig = sepayConfig;
        this.holdRepository = holdRepository;
        this.clock = clock;
    }

    /**
     * Phục vụ trang HTML POST form sang SePay Sandbox checkout/init
     */
    @GetMapping(value = "/payments/{token}/checkout", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getCheckoutPage(@PathVariable String token) {
        Optional<Payment> paymentOpt = paymentRepository.findByCheckoutToken(token);
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(renderErrorPage("Không tìm thấy phiên thanh toán hoặc liên kết đã hết hạn."));
        }

        Payment payment = paymentOpt.get();
        if ("PAID".equalsIgnoreCase(payment.getStatus())) {
            return ResponseEntity.ok(renderInfoPage("Giao dịch này đã được thanh toán thành công.", "success"));
        }

        Optional<Booking> bookingOpt = bookingRepository.findById(payment.getBookingId());
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(renderErrorPage("Không tìm thấy thông tin đơn đặt sân tương ứng."));
        }

        Booking booking = bookingOpt.get();
        var hold = (payment.getRecurringPackageId() != null
                ? holdRepository.findByRecurringPackageId(payment.getRecurringPackageId())
                : holdRepository.findByBookingId(booking.getId())).orElse(null);
        if (hold == null || !hold.isAwaitingPayment(java.time.LocalDateTime.now(clock))
                || !"PENDING".equals(booking.getStatus()) || !"PENDING".equals(payment.getStatus())) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(renderErrorPage("Thời gian thanh toán 5 phút đã kết thúc hoặc đơn không còn được phép thanh toán. Vui lòng quay lại ứng dụng để kiểm tra."));
        }

        try {
            String html = sepayService.renderCheckoutHtml(payment, booking);
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl("no-store");
            headers.set("Referrer-Policy", "no-referrer");
            headers.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
            return new ResponseEntity<>(html, headers, HttpStatus.OK);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(renderErrorPage("Lỗi cấu hình: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(renderErrorPage("Không thể khởi tạo cổng thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Xác nhận thanh toán thành công trực tiếp cho Demo / Testing khi chưa cấu hình SePay
     */
    @PostMapping(value = "/payments/{token}/mock-confirm", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> mockConfirmPayment(@PathVariable String token) {
        Optional<Payment> paymentOpt = paymentRepository.findByCheckoutToken(token);
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(renderErrorPage("Không tìm thấy phiên thanh toán."));
        }
        Payment payment = paymentOpt.get();
        Optional<Booking> bookingOpt = bookingRepository.findById(payment.getBookingId());
        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(renderErrorPage("Không tìm thấy đơn đặt sân."));
        }
        boolean ok = sepayService.confirmMockPayment(payment, bookingOpt.get());
        if (ok) {
            return ResponseEntity.ok(renderInfoPage("Thanh toán thành công!<br/>Vui lòng quay lại ứng dụng <b>EasySport</b> để xem chi tiết lịch đặt.", "success"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(renderErrorPage("Không thể xác nhận thanh toán (đơn đã hết hạn hoặc không hợp lệ)."));
        }
    }

    /**
     * Webhook IPN nhận kết quả thanh toán từ SePay
     */
    @PostMapping(value = "/api/payments/sepay/ipn", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleSepayIpn(
            @RequestHeader(value = "X-Secret-Key", required = false) String ipnSecret,
            @RequestBody Map<String, Object> payload
    ) {
        try {
            boolean success = sepayService.processIpn(ipnSecret, payload);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "IPN đã được xử lý thành công"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Dữ liệu hoặc chữ ký xác thực IPN không hợp lệ"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Không thể xử lý thông báo lúc này. Vui lòng thử lại."));
        }
    }

    /**
     * Trang chuyển hướng điều hướng khi thanh toán thành công
     */
    @GetMapping(value = "/payments/callback/success", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callbackSuccess(@RequestParam(required = false) String invoice) {
        return ResponseEntity.ok(renderInfoPage(
                "Giao dịch đã được ghi nhận trên cổng SePay Sandbox!<br/>Vui lòng quay lại ứng dụng <b>EasySport</b> để xem chi tiết lịch đặt sân.",
                "success"
        ));
    }

    @GetMapping(value = "/payments/callback/cancel", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callbackCancel(@RequestParam(required = false) String invoice) {
        return ResponseEntity.ok(renderInfoPage(
                "Bạn đã hủy phiên thanh toán trên cổng SePay.<br/>Vui lòng quay lại ứng dụng <b>EasySport</b> nếu muốn thử lại.",
                "warning"
        ));
    }

    @GetMapping(value = "/payments/callback/error", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callbackError(@RequestParam(required = false) String invoice) {
        return ResponseEntity.ok(renderInfoPage(
                "Có lỗi xảy ra trong quá trình thanh toán tại cổng SePay.<br/>Vui lòng quay lại ứng dụng <b>EasySport</b>.",
                "error"
        ));
    }

    private String renderInfoPage(String message, String type) {
        String color = switch (type) {
            case "success" -> "#1E8E3E";
            case "warning" -> "#B26A00";
            default -> "#C34E50";
        };
        String title = switch (type) {
            case "success" -> "Giao dịch hoàn tất";
            case "warning" -> "Đã hủy thanh toán";
            default -> "Thanh toán thất bại";
        };

        return "<!DOCTYPE html>\n" +
                "<html lang=\"vi\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + title + "</title>\n" +
                "    <style>\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #F4F7FB; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; padding: 16px; }\n" +
                "        .card { background: white; padding: 32px 24px; border-radius: 20px; box-shadow: 0 8px 30px rgba(0,0,0,0.06); text-align: center; max-width: 420px; width: 100%; box-sizing: border-box; }\n" +
                "        .icon { font-size: 48px; color: " + color + "; margin-bottom: 12px; }\n" +
                "        h2 { color: #1A2E46; margin: 0 0 12px; font-size: 22px; }\n" +
                "        p { color: #748398; font-size: 14px; line-height: 1.6; margin: 0 0 24px; }\n" +
                "        .btn { background: #3B6698; color: white; border: none; padding: 14px 28px; border-radius: 14px; font-weight: 600; text-decoration: none; display: inline-block; font-size: 15px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <div class=\"icon\">●</div>\n" +
                "        <h2>" + title + "</h2>\n" +
                "        <p>" + message + "</p>\n" +
                "        <a href=\"#\" onclick=\"window.close();\" class=\"btn\">Quay lại ứng dụng EasySport</a>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String renderErrorPage(String error) {
        return renderInfoPage(error, "error");
    }
}

