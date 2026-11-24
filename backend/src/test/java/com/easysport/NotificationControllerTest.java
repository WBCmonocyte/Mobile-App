package com.easysport;

import com.easysport.controller.NotificationController;
import com.easysport.dto.ApiResponse;
import com.easysport.dto.NotificationDto;
import com.easysport.entity.User;
import com.easysport.entity.UserNotification;
import com.easysport.repository.BookingRepository;
import com.easysport.repository.UserNotificationRepository;
import com.easysport.service.AuthService;
import com.easysport.service.BookingRules;
import com.easysport.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    private UserNotificationRepository notifRepo;
    private BookingRepository bookingRepo;
    private NotificationService notifService;
    private AuthService authService;
    private NotificationController controller;
    private User testUser;

    @BeforeEach
    void setUp() {
        notifRepo = mock(UserNotificationRepository.class);
        bookingRepo = mock(BookingRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), BookingRules.ZONE);

        notifService = new NotificationService(notifRepo, bookingRepo, clock);

        testUser = new User("u1", "test@user.com", "hash", "Test User", "0900000000", "HN", "01/01/2000");

        authService = new AuthService(null, null, clock) {
            @Override
            public User authenticate(String token) {
                if ("Bearer valid_token".equals(token)) {
                    return testUser;
                }
                return null;
            }
        };

        controller = new NotificationController(notifService, authService);
    }

    @Test
    void getNotifications_withoutAuth_throws401() {
        assertThrows(ResponseStatusException.class, () ->
                controller.getNotifications(null));
    }

    @Test
    void getNotifications_withValidAuth_returnsDtos() {
        UserNotification notif = new UserNotification(
                "n1", "u1", "BOOKING_CONFIRMED", "Đặt sân thành công!",
                "Chi tiết", "b1", "dedup", LocalDateTime.of(2026, 11, 24, 15, 0));
        when(notifRepo.findByUserIdOrderByCreatedAtDesc(eq("u1"), any(Pageable.class)))
                .thenReturn(List.of(notif));

        ResponseEntity<ApiResponse<List<NotificationDto>>> resp =
                controller.getNotifications("Bearer valid_token");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().getData().size());
        assertEquals("n1", resp.getBody().getData().get(0).getId());
        assertEquals("Đặt sân thành công!", resp.getBody().getData().get(0).getTitle());
    }

    @Test
    void markRead_notFound_returns404() {
        when(notifRepo.findById("n_missing")).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<Void>> resp =
                controller.markRead("n_missing", "Bearer valid_token");

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void markRead_success_returnsOk() {
        UserNotification notif = new UserNotification(
                "n1", "u1", "BOOKING_CONFIRMED", "Đặt sân thành công!",
                "Chi tiết", "b1", "dedup", LocalDateTime.of(2026, 11, 24, 15, 0));
        when(notifRepo.findById("n1")).thenReturn(Optional.of(notif));

        ResponseEntity<ApiResponse<Void>> resp =
                controller.markRead("n1", "Bearer valid_token");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(notif.isRead());
    }

    @Test
    void getUnreadCount_withValidAuth_returnsCount() {
        when(notifRepo.countByUserIdAndReadAtIsNull("u1")).thenReturn(3L);

        ResponseEntity<ApiResponse<Long>> resp =
                controller.getUnreadCount("Bearer valid_token");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(3L, resp.getBody().getData());
    }
}
