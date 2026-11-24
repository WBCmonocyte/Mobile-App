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
