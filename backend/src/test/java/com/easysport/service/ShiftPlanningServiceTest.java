package com.easysport.service;

import com.easysport.entity.StaffShift;
import com.easysport.entity.User;
import com.easysport.repository.StaffShiftRepository;
import com.easysport.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ShiftPlanningServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    // Thứ Hai 23/11/2026 09:00 giờ VN
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-23T02:00:00Z"), ZONE);

    private StaffShiftRepository shiftRepo;
    private UserRepository userRepo;
    private ShiftPlanningService service;

    private User staff1;
    private User staff2;

    @BeforeEach
    void setUp() {
        shiftRepo = mock(StaffShiftRepository.class);
        userRepo = mock(UserRepository.class);
        service = new ShiftPlanningService(shiftRepo, userRepo, clock, true);

        staff1 = new User("s1", "0901", "hash", "An NV", "s1@es.vn", "HCM", "STAFF");
        staff2 = new User("s2", "0902", "hash", "Bình NV", "s2@es.vn", "HCM", "STAFF");

        when(userRepo.findById("s1")).thenReturn(Optional.of(staff1));
        when(userRepo.findById("s2")).thenReturn(Optional.of(staff2));
        when(userRepo.findByRole("STAFF")).thenReturn(List.of(staff1, staff2));
        when(shiftRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void overlappingShiftSameDayIsBlocked() {
        StaffShift existing = new StaffShift(
                "sh1", "s1", "An NV", "0901", "c1", "Sân",
                "24/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "UPCOMING", null, null, ""
        );
        when(shiftRepo.findByStaffIdAndShiftDateIn(eq("s1"), anyCollection()))
                .thenReturn(List.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.assign(staff1, LocalDate.of(2026, 11, 24), "Ca Sáng (06:00 - 12:00)", "Lễ tân", "", "c1", "Sân")
        );
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("trùng giờ"));
    }

    @Test
    void eveningFollowedByNextMorningViolatesMinRest() {
        StaffShift prevEvening = new StaffShift(
                "sh1", "s1", "An NV", "0901", "c1", "Sân",
                "23/11/2026", "Ca Tối (17:00 - 23:00)", "Lễ tân", "UPCOMING", null, null, ""
        );
        when(shiftRepo.findByStaffIdAndShiftDateIn(eq("s1"), anyCollection()))
                .thenReturn(List.of(prevEvening));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.assign(staff1, LocalDate.of(2026, 11, 24), "Ca Sáng (06:00 - 12:00)", "Lễ tân", "", "c1", "Sân")
        );
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("8 giờ"));
    }

    @Test
    void exceedingWeekly48HoursIsBlocked() {
        // 8 ca x 6h = 48h đã xếp trong tuần 23/11 - 29/11/2026
        List<StaffShift> weekShifts = new ArrayList<>();
        String[] dates = {"23/11/2026", "24/11/2026", "25/11/2026", "26/11/2026"};
        for (int i = 0; i < dates.length; i++) {
            weekShifts.add(new StaffShift("m" + i, "s1", "An NV", "0901", "c1", "Sân",
                    dates[i], "Ca Sáng (06:00 - 12:00)", "Lễ tân", "UPCOMING", null, null, ""));
            weekShifts.add(new StaffShift("e" + i, "s1", "An NV", "0901", "c1", "Sân",
                    dates[i], "Ca Tối (17:00 - 23:00)", "Lễ tân", "UPCOMING", null, null, ""));
        }
        when(shiftRepo.findByStaffIdAndShiftDateIn(eq("s1"), anyCollection()))
                .thenReturn(weekShifts);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.assign(staff1, LocalDate.of(2026, 11, 28), "Ca Chiều (12:00 - 17:00)", "Lễ tân", "", "c1", "Sân")
        );
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("48 giờ"));
    }

    @Test
    void saturdayEveningRequiresTwoStaff() {
        assertEquals(2, ShiftPlanningService.minStaff("EVENING", LocalDate.of(2026, 11, 28)));
        assertEquals(2, ShiftPlanningService.minStaff("EVENING", LocalDate.of(2026, 11, 27)));
        assertEquals(2, ShiftPlanningService.minStaff("EVENING", LocalDate.of(2026, 11, 29)));
        assertEquals(1, ShiftPlanningService.minStaff("EVENING", LocalDate.of(2026, 11, 24)));
        assertEquals(1, ShiftPlanningService.minStaff("MORNING", LocalDate.of(2026, 11, 28)));
    }

    @Test
    void legacySlotLabelMapsToTemplateByPrefix() {
        var matched = ShiftPlanningService.templateOf("Ca Chiều (14:00 - 22:00)");
        assertTrue(matched.isPresent());
        assertEquals("AFTERNOON", matched.get().code());
    }

    @Test
    void autoAssignPrefersStaffWithFewerWeeklyHours() {
        // s1 đã có 12h (2 ca ngày 23/11), s2 đang 0h -> autoAssign chọn s2 trước cho các ca đầu tiên
        StaffShift s1a = new StaffShift("a1", "s1", "An NV", "0901", "c1", "Sân",
                "23/11/2026", "Ca Sáng (06:00 - 12:00)", "Lễ tân", "UPCOMING", null, null, "");
        StaffShift s1b = new StaffShift("a2", "s1", "An NV", "0901", "c1", "Sân",
                "23/11/2026", "Ca Chiều (12:00 - 17:00)", "Lễ tân", "UPCOMING", null, null, "");

        when(shiftRepo.findByShiftDateIn(anyCollection())).thenReturn(List.of(s1a, s1b));
        List<StaffShift> saved = new ArrayList<>();
        when(shiftRepo.saveAll(any())).thenAnswer(inv -> {
            List<StaffShift> arg = inv.getArgument(0);
            saved.addAll(arg);
            return arg;
        });

        Map<String, Object> result = service.autoAssign(LocalDate.of(2026, 11, 23));
        assertTrue(((Number) result.get("created")).intValue() > 0);
        // Ca đầu tiên được tạo (23/11 Ca Tối) phải gán cho s2 vì s2 đang ít giờ hơn (0h < 11h)
        assertEquals("s2", saved.get(0).getStaffId());
        assertEquals("23/11/2026", saved.get(0).getShiftDate());
        assertEquals("Ca Tối (17:00 - 23:00)", saved.get(0).getShiftSlot());
    }
}
