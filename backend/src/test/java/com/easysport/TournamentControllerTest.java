package com.easysport;

import com.easysport.controller.TournamentController;
import com.easysport.dto.ApiResponse;
import com.easysport.dto.TournamentResponse;
import com.easysport.entity.Court;
import com.easysport.entity.Tournament;
import com.easysport.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TournamentControllerTest {

    private TournamentRepository tournamentRepository;
    private Clock clock;
    private TournamentController tournamentController;

    @BeforeEach
    void setUp() {
        tournamentRepository = mock(TournamentRepository.class);
        // Fixed instant: 2026-11-24T10:00:00Z
        Instant instant = Instant.parse("2026-11-24T10:00:00Z");
        clock = Clock.fixed(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
        tournamentController = new TournamentController(tournamentRepository, clock);
    }

    @Test
    @DisplayName("Có giải đấu sắp tới: Trả về danh sách giải kèm thông tin sân và múi giờ +07:00")
    void testGetUpcomingTournaments_hasTournaments() {
        Court court = new Court();
        court.setId("hanoi_fb_hoang_cau");
        court.setName("Sân bóng Hoàng Cầu");
        court.setAddress("Số 69 Hoàng Cầu, Đống Đa, Hà Nội");

        Tournament t = new Tournament(
                "demo_hc_dec26",
                court,
                "Cúp bóng đá 7 người Hoàng Cầu",
                LocalDateTime.of(2026, 12, 5, 8, 0),
                LocalDateTime.of(2026, 12, 5, 17, 0),
                8,
                true
        );

        when(tournamentRepository.findUpcomingTournaments(any(LocalDateTime.class)))
                .thenReturn(List.of(t));

        ResponseEntity<ApiResponse<List<TournamentResponse>>> response = tournamentController.getUpcomingTournaments();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());

        List<TournamentResponse> list = response.getBody().getData();
        assertEquals(1, list.size());

        TournamentResponse item = list.get(0);
        assertEquals("demo_hc_dec26", item.getId());
        assertEquals("Cúp bóng đá 7 người Hoàng Cầu", item.getTitle());
        assertEquals("hanoi_fb_hoang_cau", item.getCourtId());
        assertEquals("Sân bóng Hoàng Cầu", item.getCourtName());
        assertEquals("Số 69 Hoàng Cầu, Đống Đa, Hà Nội", item.getAddress());
        assertEquals(8, item.getMaxTeams());
        assertTrue(item.getIsDemo());

        // Kiểm tra múi giờ +07:00 trong chuỗi ISO
        assertTrue(item.getStartsAt().contains("2026-12-05T08:00:00+07:00"));
        assertTrue(item.getEndsAt().contains("2026-12-05T17:00:00+07:00"));
    }

    @Test
    @DisplayName("Không còn giải đấu sắp tới: Trả về danh sách rỗng thành công")
    void testGetUpcomingTournaments_noTournaments() {
        when(tournamentRepository.findUpcomingTournaments(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<ApiResponse<List<TournamentResponse>>> response = tournamentController.getUpcomingTournaments();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getData().isEmpty());
    }
}
