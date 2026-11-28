package com.easysport.service;

import com.easysport.entity.Booking;
import com.easysport.entity.Payment;
import com.easysport.entity.UserNotification;
import com.easysport.repository.UserNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_NOTIFICATIONS = 50;

    private final UserNotificationRepository notifRepo;
    private final com.easysport.repository.BookingRepository bookingRepository;
    private final Clock clock;

    public NotificationService(UserNotificationRepository notifRepo,
                               com.easysport.repository.BookingRepository bookingRepository,
                               Clock clock) {
        this.notifRepo = notifRepo;
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    // ── Core CRUD ────────────────────────────────────────────────────────────

    /**
     * Idempotent: bỏ qua nếu dedupKey đã tồn tại.
     */
    @Transactional
    public UserNotification createNotification(String userId, String type,
                                               String title, String body,
                                               String bookingId, String dedupKey) {
        if (dedupKey != null && notifRepo.existsByDedupKey(dedupKey)) {
            log.debug("Notification already exists for dedupKey={}", dedupKey);
            return null;
        }
        UserNotification n = new UserNotification(
                UUID.randomUUID().toString(), userId, type, title, body,
                bookingId, dedupKey, LocalDateTime.now(clock));
        return notifRepo.save(n);
    }

    @Transactional
    public boolean markRead(String notifId, String userId) {
        return notifRepo.findById(notifId).map(n -> {
            if (!n.getUserId().equals(userId)) return false;
            if (n.getReadAt() == null) {
                n.setReadAt(LocalDateTime.now(clock));
                notifRepo.save(n);
            }
            return true;
        }).orElse(false);
    }

    @Transactional
    public void markAllRead(String userId) {
        List<UserNotification> unread = notifRepo.findUnreadByUserId(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        for (UserNotification n : unread) {
            n.setReadAt(now);
        }
        notifRepo.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notifRepo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional(readOnly = true)
    public List<UserNotification> getNotifications(String userId) {
        return notifRepo.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(0, MAX_NOTIFICATIONS));
    }

    // ── Domain event helpers ──────────────────────────────────────────────────

    @Transactional
    public void createBookingConfirmedNotif(Booking booking) {
        String dedupKey = "BOOKING_CONFIRMED:" + booking.getId();
        String timeRange = booking.getTimeRange();
        String dateLabel = booking.getDateLabel() != null ? booking.getDateLabel() : "";
        String body = String.format(
                "Lịch đặt tại %s (%s, %s) đã được xác nhận thành công.",
                booking.getCourtName(), timeRange, dateLabel);
        createNotification(booking.getUserId(), "BOOKING_CONFIRMED",
                "Đặt sân thành công!", body, booking.getId(), dedupKey);
    }

    @Transactional
    public void createPaymentReceivedNotif(Booking booking, Payment payment) {
        String dedupKey = "PAYMENT:" + payment.getId();
        String amount = payment.getAmount() != null
                ? String.format("%,d đ", payment.getAmount()).replace(",", ".") : "?";
        String body = String.format(
                "Hệ thống đã nhận được tiền đặt sân %s qua chuyển khoản VietQR.",
                amount);
        createNotification(booking.getUserId(), "PAYMENT_RECEIVED",
                "Xác nhận thanh toán", body, booking.getId(), dedupKey);
    }

    @Transactional
    public void createRecurringConfirmedNotif(String userId, String packageId, String courtName,
                                              String fieldDetail, String timeRange, String startDate,
                                              int totalSessions, String referenceBookingId) {
        String dedupKey = "RECURRING_CONFIRMED:" + packageId;
        String body = String.format(
                "Gói cố định %d buổi tại %s (%s · %s, bắt đầu %s) đã được xác nhận thành công.",
                totalSessions, courtName, fieldDetail, timeRange, startDate);
        createNotification(userId, "RECURRING_CONFIRMED",
                "Xác nhận lịch cố định", body, referenceBookingId, dedupKey);
    }

    @Transactional
    public void createRecurringRescheduledNotif(String userId, String sessionId, String courtName,
                                                String oldDate, String newDate, String timeRange,
                                                String replacementBookingId) {
        String dedupKey = "RECURRING_SLID:" + sessionId + ":" + newDate;
        String body = String.format(
                "Buổi ngày %s tại %s (%s) đã được chốt lịch bù sang ngày %s.",
                oldDate, courtName, timeRange, newDate);
        createNotification(userId, "RECURRING_RESCHEDULED",
                "Đã chốt lịch bù", body, replacementBookingId, dedupKey);
    }

    @Transactional
    public void createMakeupBookedNotif(String userId, String creditId, String courtName,
                                        String dateLabel, String timeRange, String bookingId) {
        String dedupKey = "MAKEUP_BOOKED:" + creditId;
        String body = String.format(
                "Đã đặt bù thành công tại %s (%s, ngày %s) từ quyền buổi còn dư.",
                courtName, timeRange, dateLabel);
        createNotification(userId, "RECURRING_MAKEUP",
                "Đặt lịch bù thành công", body, bookingId, dedupKey);
    }

    @Transactional
    public void createWeatherPostponeNotif(String userId, String sessionId, String courtName,
                                           String sessionDate, String timeRange, String bookingId) {
        String dedupKey = "WEATHER_APPROVED:" + sessionId;
        String body = String.format(
                "Chủ sân đã xác nhận hoãn buổi ngày %s tại %s (%s) do thời tiết. Bạn có thể chọn Trượt lịch hoặc Giữ 1 buổi còn dư trong Lịch cố định.",
                sessionDate, courtName, timeRange);
        createNotification(userId, "WEATHER_POSTPONED",
                "Xác nhận hoãn do thời tiết", body, bookingId, dedupKey);
    }

    @Transactional
    public void createWalletRefundNotif(String userId, String walletTxId, int amount,
                                        String reason, String referenceBookingId) {
        String dedupKey = "WALLET_REFUND:" + walletTxId;
        String formatted = String.format("%,d đ", amount).replace(",", ".");
        String body = String.format(
                "Ví EasySport vừa nhận +%s (%s).",
                formatted, reason != null && !reason.isBlank() ? reason : "Hoàn tiền đặt sân");
        createNotification(userId, "WALLET_REFUND",
                "Tiền đã vào Ví EasySport", body, referenceBookingId, dedupKey);
    }

    /**
     * Chạy mỗi phút; tạo thông báo nhắc 2h cho booking CONFIRMED chưa bắt đầu.
     * Chỉ tạo nếu chưa có (dedup_key).
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void scheduleReminder2h() {
        LocalDateTime now = LocalDateTime.now(clock);
        // Lấy tất cả booking CONFIRMED và lọc trong Java vì booking_date là String
        List<Booking> candidates = bookingRepository.findByStatus("CONFIRMED");
        for (Booking b : candidates) {
            if (b.getUserId() == null || b.getTimeRange() == null || b.getDateLabel() == null) continue;
            try {
                // Parse giờ bắt đầu từ timeRange: "HH:mm-HH:mm" hoặc "HH:mm–HH:mm"
                String startStr = b.getTimeRange().split("[-–]")[0].trim();
                // Parse ngày từ dateLabel: hỗ trợ "dd/MM/yyyy" hoặc "yyyy-MM-dd"
                java.time.LocalDate bookingDate = parseBookingDate(b.getDateLabel());
                if (bookingDate == null) continue;
                LocalDateTime startTime = bookingDate.atTime(
                        java.time.LocalTime.parse(startStr,
                                DateTimeFormatter.ofPattern("HH:mm")));
                long minutesUntil = java.time.Duration.between(now, startTime).toMinutes();
                if (minutesUntil >= 100 && minutesUntil <= 130) { // 100-130 phút ≈ 2h
                    String dedupKey = "REMINDER_2H:" + b.getId();
                    String body = String.format(
                            "Trận đấu của bạn tại %s sẽ bắt đầu trong 2 giờ tới. Đừng quên mang đồ nhé!",
                            b.getCourtName());
                    createNotification(b.getUserId(), "REMINDER_2H",
                            "Nhắc nhở trận đấu", body, b.getId(), dedupKey);
                }
            } catch (Exception e) {
                log.warn("Cannot parse timeRange/date for booking {}: {}", b.getId(), e.getMessage());
            }
        }
    }

    private java.time.LocalDate parseBookingDate(String dateLabel) {
        if (dateLabel == null) return null;
        // Try "dd/MM/yyyy"
        try {
            return java.time.LocalDate.parse(dateLabel,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}
        // Try ISO "yyyy-MM-dd"
        try {
            return java.time.LocalDate.parse(dateLabel);
        } catch (Exception ignored) {}
        return null;
    }
}
