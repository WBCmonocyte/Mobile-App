package com.easysport.service;

import java.time.*;
import java.time.format.*;
import java.util.*;

/** Canonical booking date and 90-minute sessions, shared by checkout and availability. */
public final class BookingRules {
    private BookingRules() {}
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    public static final List<String> SLOTS = List.of(
            "07:00 - 08:30", "08:30 - 10:00", "10:00 - 11:30",
            "13:00 - 14:30", "14:30 - 16:00", "16:00 - 17:30",
            "17:30 - 19:00", "19:00 - 20:30", "20:30 - 22:00");

    public static LocalDate parseDate(String value) {
        try { return LocalDate.parse(value.trim(), DATE); }
        catch (Exception ex) { throw new IllegalArgumentException("Ngày thuê phải đúng định dạng dd/MM/yyyy."); }
    }

    public static List<String> validateSlots(List<String> slots, LocalDate date, Clock clock) {
        return validateSlots(slots, date, clock, 90);
    }

    public static List<String> validateSlots(List<String> slots, LocalDate date, Clock clock, int maxAdvanceDays) {
        if (slots == null || slots.isEmpty() || slots.size() > SLOTS.size())
            throw new IllegalArgumentException("Vui lòng chọn khung giờ hợp lệ.");
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZONE);
        if (date.isBefore(now.toLocalDate()) || date.isAfter(now.toLocalDate().plusDays(maxAdvanceDays)))
            throw new IllegalArgumentException("Chỉ được đặt sân trong " + maxAdvanceDays + " ngày tới.");
        List<String> normalized = slots.stream().map(s -> s == null ? "" : s.trim()).sorted().toList();
        if (new HashSet<>(normalized).size() != normalized.size() || !SLOTS.containsAll(normalized))
            throw new IllegalArgumentException("Ca thuê không hợp lệ hoặc bị trùng.");
        for (String slot : normalized) {
            if (!date.atTime(LocalTime.parse(slot.substring(0, 5))).isAfter(now.plusMinutes(6)))
                throw new IllegalArgumentException("Ca thuê phải bắt đầu sau thời gian thanh toán và xác minh.");
        }
        return normalized;
    }

    public static String iso(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant().toString();
    }
}
