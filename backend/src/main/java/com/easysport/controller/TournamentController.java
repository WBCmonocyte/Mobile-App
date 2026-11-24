package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.dto.TournamentResponse;
import com.easysport.entity.Tournament;
import com.easysport.repository.TournamentRepository;
import com.easysport.service.BookingRules;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentRepository tournamentRepository;
    private final Clock clock;

    public TournamentController(TournamentRepository tournamentRepository, Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.clock = clock;
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<TournamentResponse>>> getUpcomingTournaments() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Tournament> list = tournamentRepository.findUpcomingTournaments(now);

        List<TournamentResponse> response = list.stream().map(t -> {
            String startsAtIso = t.getStartsAt() != null
                    ? t.getStartsAt().atZone(BookingRules.ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : null;
            String endsAtIso = t.getEndsAt() != null
                    ? t.getEndsAt().atZone(BookingRules.ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    : null;
            String courtId = t.getCourt() != null ? t.getCourt().getId() : null;
            String courtName = t.getCourt() != null ? t.getCourt().getName() : null;
            String address = t.getCourt() != null ? t.getCourt().getAddress() : null;

            return new TournamentResponse(
                    t.getId(),
                    t.getTitle(),
                    courtId,
                    courtName,
                    address,
                    startsAtIso,
                    endsAtIso,
                    t.getMaxTeams(),
                    Boolean.TRUE.equals(t.getIsDemo())
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giải đấu sắp tới thành công", response));
    }
}
