package com.easysport;

import com.easysport.entity.Booking;
import com.easysport.entity.Payment;
import com.easysport.entity.UserNotification;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.UserNotificationRepository;
import com.easysport.service.BookingRules;
import com.easysport.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private UserNotificationRepository notifRepo;
    private BookingRepository bookingRepo;
    private Clock clock;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notifRepo = mock(UserNotificationRepository.class);
        bookingRepo = mock(BookingRepository.class);
        // Fixed time: 2026-11-24 10:00:00 UTC (17:00:00 Vietnam time)
        clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), BookingRules.ZONE);
        service = new NotificationService(notifRepo, bookingRepo, clock);
    }

    @Test
    void createNotification_dedupKeyExists_doesNotSave() {
        when(notifRepo.existsByDedupKey("dup_key")).thenReturn(true);

        UserNotification result = service.createNotification(
                "u1", "SYSTEM", "Title", "Body", null, "dup_key");

        assertNull(result);
        verify(notifRepo, never()).save(any());
    }

    @Test
    void createNotification_newKey_savesSuccessfully() {
        when(notifRepo.existsByDedupKey("new_key")).thenReturn(false);
        when(notifRepo.save(any(UserNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        UserNotification result = service.createNotification(
                "u1", "SYSTEM", "Title", "Body", null, "new_key");

        assertNotNull(result);
        assertEquals("u1", result.getUserId());
        assertEquals("Title", result.getTitle());
        verify(notifRepo).save(any(UserNotification.class));
    }

    @Test
    void markRead_matchesUser_updatesReadAt() {
        UserNotification n = new UserNotification("n1", "u1", "SYSTEM", "T", "B", null, "k1", LocalDateTime.now(clock));
        when(notifRepo.findById("n1")).thenReturn(Optional.of(n));

        boolean ok = service.markRead("n1", "u1");

        assertTrue(ok);
        assertTrue(n.isRead());
        verify(notifRepo).save(n);
    }

    @Test
    void markRead_differentUser_returnsFalse() {
        UserNotification n = new UserNotification("n1", "u1", "SYSTEM", "T", "B", null, "k1", LocalDateTime.now(clock));
        when(notifRepo.findById("n1")).thenReturn(Optional.of(n));

        boolean ok = service.markRead("n1", "u2");

        assertFalse(ok);
        assertFalse(n.isRead());
        verify(notifRepo, never()).save(any());
    }

    @Test
    void markAllRead_updatesAllUnread() {
        UserNotification n1 = new UserNotification("n1", "u1", "SYSTEM", "T1", "B1", null, "k1", LocalDateTime.now(clock));
        UserNotification n2 = new UserNotification("n2", "u1", "SYSTEM", "T2", "B2", null, "k2", LocalDateTime.now(clock));
        when(notifRepo.findUnreadByUserId("u1")).thenReturn(List.of(n1, n2));

        service.markAllRead("u1");

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
        verify(notifRepo).saveAll(any());
    }

    @Test
    void getUnreadCount_returnsCorrectCount() {
        when(notifRepo.countByUserIdAndReadAtIsNull("u1")).thenReturn(5L);

        long count = service.getUnreadCount("u1");

        assertEquals(5L, count);
    }

    @Test
    void createBookingConfirmedNotif_createsWithCorrectDedupKey() {
        when(notifRepo.existsByDedupKey(anyString())).thenReturn(false);
        when(notifRepo.save(any(UserNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = new Booking();
        booking.setId("b100");
        booking.setUserId("u1");
        booking.setCourtName("Sân bóng MIC Nguyễn Trãi");
        booking.setTimeRange("18:00 - 19:30");
        booking.setDateLabel("24/11/2026");

        service.createBookingConfirmedNotif(booking);

        verify(notifRepo).save(argThat(n ->
                n.getDedupKey().equals("BOOKING_CONFIRMED:b100")
                && n.getTitle().equals("Đặt sân thành công!")
                && n.getBookingId().equals("b100")
        ));
    }

    @Test
    void createPaymentReceivedNotif_formatsAmountProperly() {
        when(notifRepo.existsByDedupKey(anyString())).thenReturn(false);
        when(notifRepo.save(any(UserNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = new Booking();
        booking.setId("b100");
        booking.setUserId("u1");

        Payment payment = new Payment();
        payment.setId("p200");
        payment.setAmount(150000);

        service.createPaymentReceivedNotif(booking, payment);

        verify(notifRepo).save(argThat(n ->
                n.getDedupKey().equals("PAYMENT:p200")
                && n.getTitle().equals("Xác nhận thanh toán")
                && n.getBody().contains("150.000 đ")
        ));
    }

    @Test
    void scheduleReminder2h_notifiesForMatchInTwoHours() {
        // Vietnam time now: 17:00
        // Match start: 19:00 (120 minutes away -> in 100-130 min window)
        Booking booking = new Booking();
        booking.setId("b_remind");
        booking.setUserId("u1");
        booking.setCourtName("Sân MIC");
        booking.setTimeRange("19:00 - 20:30");
        booking.setDateLabel("24/11/2026");
        booking.setStatus("CONFIRMED");

        when(bookingRepo.findByStatus("CONFIRMED")).thenReturn(List.of(booking));
        when(notifRepo.existsByDedupKey(anyString())).thenReturn(false);

        service.scheduleReminder2h();

        verify(notifRepo).save(argThat(n ->
                n.getDedupKey().equals("REMINDER_2H:b_remind")
                && n.getTitle().equals("Nhắc nhở trận đấu")
        ));
    }
}
