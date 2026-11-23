package com.easysport;

import com.easysport.service.BookingRules;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BookingRulesTest {
    final Clock clock = Clock.fixed(Instant.parse("2026-11-15T00:00:00Z"), BookingRules.ZONE);
    @Test void rejectsInvalidCalendarDates() {
        assertThrows(IllegalArgumentException.class, () -> BookingRules.parseDate("31/02/2026"));
    }
    @Test void rejectsDuplicateSlots() {
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validateSlots(
                List.of("07:00 - 08:30", "07:00 - 08:30"), LocalDate.of(2026,11,16), clock));
    }
    @Test void rejectsArbitraryDurationAndInvalidHours() {
        for (String slot : List.of("07:00 - 22:00", "99:00 - 99:30", "22:00 - 07:00"))
            assertThrows(IllegalArgumentException.class, () -> BookingRules.validateSlots(List.of(slot), LocalDate.of(2026,11,16), clock));
    }
    @Test void rejectsPastDateAndStartedSession() {
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validateSlots(List.of("07:00 - 08:30"), LocalDate.of(2026,11,14), clock));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validateSlots(List.of("07:00 - 08:30"), LocalDate.of(2026,11,15), clock));
    }
    @Test void validatesAndSortsAdjacentSessions() {
        assertEquals(List.of("07:00 - 08:30", "08:30 - 10:00"), BookingRules.validateSlots(
                List.of("08:30 - 10:00", "07:00 - 08:30"), LocalDate.of(2026,11,16), clock));
    }
    @Test void vietnamLocalTimeMustNotBeLabelledUtc() {
        assertEquals("2026-11-15T00:00:00Z", BookingRules.iso(LocalDateTime.of(2026,11,15,7,0)));
    }
}
