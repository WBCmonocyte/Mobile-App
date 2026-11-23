package com.easysport.controller;

import com.easysport.dto.ApiResponse;
import com.easysport.dto.CourtAvailabilityResponse;
import com.easysport.entity.Court;
import com.easysport.repository.CourtRepository;
import com.easysport.service.CourtHoldService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/courts")
public class CourtController {

    private final CourtRepository courtRepository;
    private final CourtHoldService courtHoldService;

    public CourtController(CourtRepository courtRepository, CourtHoldService courtHoldService) {
        this.courtRepository = courtRepository;
        this.courtHoldService = courtHoldService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Court>>> getAllCourts(
            @RequestParam(required = false) String search
    ) {
        List<Court> courts;
        if (search != null && !search.trim().isEmpty()) {
            courts = courtRepository.findByNameContainingIgnoreCase(search.trim());
        } else {
            courts = courtRepository.findAll();
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sân thành công", courts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Court>> getCourtById(@PathVariable String id) {
        return courtRepository.findById(id)
                .map(court -> ResponseEntity.ok(ApiResponse.success("Chi tiết sân", court)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy sân với ID: " + id)));
    }

    /**
     * Tra cứu trạng thái các slot (AVAILABLE, HELD, BOOKED) của sân vào ngày được chọn.
     * Khi HELD, trả về expiresAt để client biết thời gian hết hạn giữ chỗ.
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<CourtAvailabilityResponse>> getCourtAvailability(
            @PathVariable String id,
            @RequestParam(required = false) String fieldDetail,
            @RequestParam(required = false) String date
    ) {
        String effectiveDate = (date != null && !date.isBlank())
                ? date.trim()
                : LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        try {
            CourtAvailabilityResponse res = courtHoldService.getAvailability(id, fieldDetail, effectiveDate);
            return ResponseEntity.ok(ApiResponse.success("Trạng thái khung giờ của sân", res));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
