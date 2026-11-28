package com.easysport.service;

import com.easysport.entity.StaffShift;
import com.easysport.entity.User;
import com.easysport.repository.StaffShiftRepository;
import com.easysport.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nghiệp vụ lập lịch phân ca, kiểm tra xung đột/giờ công và chấm công nhân viên.
 */
@Service
public class ShiftPlanningService {

    public record SlotTemplate(String code, String label, LocalTime start, LocalTime end) {
        public double hours() {
            return Duration.between(start, end).toMinutes() / 60.0;
        }
    }

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final double MAX_DAILY_HOURS = 12.0;
    public static final double MAX_WEEKLY_HOURS = 48.0;
    public static final long MIN_REST_HOURS = 8L;

    public static final List<SlotTemplate> TEMPLATES = List.of(
            new SlotTemplate("MORNING", "Ca Sáng (06:00 - 12:00)", LocalTime.of(6, 0), LocalTime.of(12, 0)),
            new SlotTemplate("AFTERNOON", "Ca Chiều (12:00 - 17:00)", LocalTime.of(12, 0), LocalTime.of(17, 0)),
            new SlotTemplate("EVENING", "Ca Tối (17:00 - 23:00)", LocalTime.of(17, 0), LocalTime.of(23, 0))
    );

    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("(\\d{2}:\\d{2})\\s*-\\s*(\\d{2}:\\d{2})");

    private final StaffShiftRepository staffShiftRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final boolean enforceTimeWindow;

    @Autowired
    public ShiftPlanningService(
            StaffShiftRepository staffShiftRepository,
            UserRepository userRepository,
            Clock clock,
            @Value("${app.shift.enforce-time-window:true}") boolean enforceTimeWindow
    ) {
        this.staffShiftRepository = staffShiftRepository;
        this.userRepository = userRepository;
        this.clock = clock;
        this.enforceTimeWindow = enforceTimeWindow;
    }

    public static int minStaff(String code, LocalDate date) {
        if ("EVENING".equals(code)) {
            DayOfWeek dow = date.getDayOfWeek();
            if (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                return 2;
            }
        }
        return 1;
    }

    public static Optional<SlotTemplate> templateOf(String shiftSlotLabel) {
        if (shiftSlotLabel == null) {
            return Optional.empty();
        }
        String trimmed = shiftSlotLabel.trim();
        for (SlotTemplate t : TEMPLATES) {
            String prefix = t.label().substring(0, t.label().indexOf(" ("));
            if (trimmed.equalsIgnoreCase(t.code()) || trimmed.startsWith(prefix)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public static LocalTime[] timesOf(String label) {
        if (label != null) {
            Matcher m = TIME_RANGE_PATTERN.matcher(label);
            if (m.find()) {
                try {
                    return new LocalTime[]{
                            LocalTime.parse(m.group(1), TIME_FORMAT),
                            LocalTime.parse(m.group(2), TIME_FORMAT)
                    };
                } catch (DateTimeParseException ignored) {
                    // Fallback to template
                }
            }
        }
        return templateOf(label)
                .map(t -> new LocalTime[]{t.start(), t.end()})
                .orElse(null);
    }

    public static double hoursOf(String label) {
        LocalTime[] times = timesOf(label);
        if (times == null) {
            return 0.0;
        }
        return Duration.between(times[0], times[1]).toMinutes() / 60.0;
    }

    public static LocalDate weekStartOf(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate parseDateOrNull(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public List<String> validate(String staffId, LocalDate date, String slotLabel, String excludeShiftId) {
        if (staffId == null || staffId.isBlank()) {
            return List.of("Thiếu mã nhân viên");
        }
        Optional<User> userOpt = userRepository.findById(staffId);
        if (userOpt.isEmpty() || !"STAFF".equalsIgnoreCase(userOpt.get().getRole())) {
            return List.of("Nhân viên không tồn tại hoặc không có vai trò STAFF");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = weekStartOf(date);
        List<String> queryDates = new ArrayList<>();
        // Lấy đủ 7 ngày trong tuần + ngày trước/sau để kiểm tra nghỉ giữa 2 ca
        queryDates.add(date.minusDays(1).format(DATE_FORMAT));
        String nextDayStr = date.plusDays(1).format(DATE_FORMAT);
        if (!queryDates.contains(nextDayStr)) {
            queryDates.add(nextDayStr);
        }
        for (int i = 0; i < 7; i++) {
            String ds = weekStart.plusDays(i).format(DATE_FORMAT);
            if (!queryDates.contains(ds)) {
                queryDates.add(ds);
            }
        }
        List<StaffShift> existing = staffShiftRepository.findByStaffIdAndShiftDateIn(staffId, queryDates);
        return validateAgainstShifts(date, slotLabel, excludeShiftId, today, existing);
    }

    /**
     * Tách phần kiểm tra thuần túy để dùng chung cho auto-assign và unit test.
     */
    public List<String> validateAgainstShifts(
            LocalDate date,
            String slotLabel,
            String excludeShiftId,
            LocalDate today,
            List<StaffShift> existingShifts
    ) {
        List<String> errors = new ArrayList<>();
        if (date.isBefore(today)) {
            errors.add("Không thể phân ca cho ngày đã qua (" + date.format(DATE_FORMAT) + ")");
        }

        LocalTime[] newTimes = timesOf(slotLabel);
        if (newTimes == null) {
            errors.add("Khung giờ ca không hợp lệ: " + slotLabel);
            return errors;
        }
        double newHours = Duration.between(newTimes[0], newTimes[1]).toMinutes() / 60.0;
        if (newHours <= 0) {
            errors.add("Giờ kết thúc ca phải sau giờ bắt đầu");
            return errors;
        }

        LocalDate weekStart = weekStartOf(date);
        LocalDate weekEnd = weekStart.plusDays(6);
        String targetDateStr = date.format(DATE_FORMAT);
        String prevDateStr = date.minusDays(1).format(DATE_FORMAT);
        String nextDateStr = date.plusDays(1).format(DATE_FORMAT);

        double dailyHours = 0.0;
        double weeklyHours = 0.0;
        boolean overlapFound = false;
        boolean restViolated = false;

        LocalDateTime newStartDt = date.atTime(newTimes[0]);
        LocalDateTime newEndDt = date.atTime(newTimes[1]);

        for (StaffShift s : existingShifts) {
            if (excludeShiftId != null && excludeShiftId.equals(s.getId())) {
                continue;
            }
            LocalDate sDate = parseDateOrNull(s.getShiftDate());
            LocalTime[] sTimes = timesOf(s.getShiftSlot());
            if (sDate == null || sTimes == null) {
                continue;
            }
            double sHours = Duration.between(sTimes[0], sTimes[1]).toMinutes() / 60.0;

            if (targetDateStr.equals(s.getShiftDate())) {
                dailyHours += sHours;
                if (newTimes[0].isBefore(sTimes[1]) && sTimes[0].isBefore(newTimes[1])) {
                    overlapFound = true;
                }
            }
            if (!sDate.isBefore(weekStart) && !sDate.isAfter(weekEnd)) {
                weeklyHours += sHours;
            }
            if (prevDateStr.equals(s.getShiftDate())) {
                LocalDateTime prevEndDt = sDate.atTime(sTimes[1]);
                if (Duration.between(prevEndDt, newStartDt).toHours() < MIN_REST_HOURS) {
                    restViolated = true;
                }
            }
            if (nextDateStr.equals(s.getShiftDate())) {
                LocalDateTime nextStartDt = sDate.atTime(sTimes[0]);
                if (Duration.between(newEndDt, nextStartDt).toHours() < MIN_REST_HOURS) {
                    restViolated = true;
                }
            }
        }

        if (overlapFound) {
            errors.add("Nhân viên đã có ca trùng giờ trong ngày " + targetDateStr);
        }
        if (dailyHours + newHours > MAX_DAILY_HOURS) {
            errors.add("Tổng giờ làm ngày " + targetDateStr + " vượt quá 12 giờ");
        }
        if (weeklyHours + newHours > MAX_WEEKLY_HOURS) {
            errors.add("Tổng giờ làm tuần vượt quá giới hạn 48 giờ");
        }
        if (restViolated) {
            errors.add("Nhân viên cần nghỉ tối thiểu 8 giờ giữa hai ca liên tiếp (tránh Ca Tối hôm trước + Ca Sáng hôm sau)");
        }
        return errors;
    }

    public StaffShift assign(
            User staff,
            LocalDate date,
            String slotLabel,
            String duty,
            String notes,
            String courtId,
            String courtName
    ) {
        if (staff == null || !"STAFF".equalsIgnoreCase(staff.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhân viên không tồn tại hoặc không có vai trò STAFF");
        }
        String resolvedSlot = templateOf(slotLabel)
                .map(SlotTemplate::label)
                .orElse(slotLabel);

        List<String> errors = validate(staff.getId(), date, resolvedSlot, null);
        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.join("; ", errors));
        }

        StaffShift shift = new StaffShift(
                "sh_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                staff.getId(),
                staff.getFullName(),
                staff.getPhone(),
                courtId != null && !courtId.isBlank() ? courtId : "court_thanh_phat",
                courtName != null && !courtName.isBlank() ? courtName : "Sân bóng Thành Phát",
                date.format(DATE_FORMAT),
                resolvedSlot,
                duty != null && !duty.isBlank() ? duty : "Lễ tân & Thu ngân",
                "UPCOMING",
                null,
                null,
                notes != null ? notes : ""
        );
        return staffShiftRepository.save(shift);
    }

    public Map<String, Object> buildWeekSchedule(LocalDate requestedWeekStart) {
        return buildWeekSchedule(requestedWeekStart, null, null);
    }

    public Map<String, Object> buildWeekSchedule(LocalDate requestedWeekStart, String courtIdFilter) {
        return buildWeekSchedule(requestedWeekStart, courtIdFilter, null);
    }

    public Map<String, Object> buildWeekSchedule(LocalDate requestedWeekStart, String courtIdFilter, Set<String> allowedCourtIds) {
        LocalDate weekStart = weekStartOf(requestedWeekStart);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDate today = LocalDate.now(clock);

        List<String> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekDates.add(weekStart.plusDays(i).format(DATE_FORMAT));
        }

        boolean filterByCourt = courtIdFilter != null && !courtIdFilter.isBlank() && !"ALL".equalsIgnoreCase(courtIdFilter);
        boolean filterByAllowed = allowedCourtIds != null && !allowedCourtIds.isEmpty();
        List<StaffShift> weekShifts = staffShiftRepository.findByShiftDateIn(weekDates).stream()
                .filter(s -> !filterByAllowed || s.getCourtId() == null || allowedCourtIds.contains(s.getCourtId()))
                .filter(s -> !filterByCourt || courtIdFilter.equals(s.getCourtId()))
                .toList();
        List<User> staffUsers = userRepository.findByRole("STAFF");

        Map<String, Double> hoursByStaff = new HashMap<>();
        Map<String, Integer> countByStaff = new HashMap<>();
        Map<String, Integer> lateCountByStaff = new HashMap<>();
        Map<String, Integer> lateMinutesByStaff = new HashMap<>();
        Map<String, Integer> earlyCountByStaff = new HashMap<>();
        Map<String, Map<String, List<StaffShift>>> grid = new HashMap<>();

        for (StaffShift s : weekShifts) {
            hoursByStaff.merge(s.getStaffId(), hoursOf(s.getShiftSlot()), Double::sum);
            countByStaff.merge(s.getStaffId(), 1, Integer::sum);
            if (s.getLateMinutes() != null && s.getLateMinutes() > 0) {
                lateCountByStaff.merge(s.getStaffId(), 1, Integer::sum);
                lateMinutesByStaff.merge(s.getStaffId(), s.getLateMinutes(), Integer::sum);
            }
            if (s.getNotes() != null && s.getNotes().contains("[Về sớm")) {
                earlyCountByStaff.merge(s.getStaffId(), 1, Integer::sum);
            }

            Optional<SlotTemplate> tmpl = templateOf(s.getShiftSlot());
            if (tmpl.isPresent()) {
                grid.computeIfAbsent(s.getShiftDate(), k -> new HashMap<>())
                        .computeIfAbsent(tmpl.get().code(), k -> new ArrayList<>())
                        .add(s);
            }
        }

        int totalPositions = 0;
        int filledPositions = 0;
        List<Map<String, Object>> daysList = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            String dStr = d.format(DATE_FORMAT);
            boolean isPast = d.isBefore(today);

            List<Map<String, Object>> slotsList = new ArrayList<>();
            for (SlotTemplate t : TEMPLATES) {
                int min = minStaff(t.code(), d);
                List<StaffShift> cellShifts = grid.getOrDefault(dStr, Map.of()).getOrDefault(t.code(), List.of());
                totalPositions += min;
                filledPositions += Math.min(cellShifts.size(), min);

                Map<String, Object> slotMap = new LinkedHashMap<>();
                slotMap.put("code", t.code());
                slotMap.put("label", t.label());
                slotMap.put("start", t.start().format(TIME_FORMAT));
                slotMap.put("end", t.end().format(TIME_FORMAT));
                slotMap.put("minStaff", min);
                slotMap.put("shifts", cellShifts);
                slotsList.add(slotMap);
            }

            Map<String, Object> dayMap = new LinkedHashMap<>();
            dayMap.put("date", dStr);
            dayMap.put("weekday", weekdayLabel(d.getDayOfWeek()));
            dayMap.put("isPast", isPast);
            dayMap.put("slots", slotsList);
            daysList.add(dayMap);
        }

        List<Map<String, Object>> templatesList = new ArrayList<>();
        for (SlotTemplate t : TEMPLATES) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("code", t.code());
            tm.put("label", t.label());
            tm.put("start", t.start().format(TIME_FORMAT));
            tm.put("end", t.end().format(TIME_FORMAT));
            tm.put("hours", t.hours());
            templatesList.add(tm);
        }

        List<Map<String, Object>> staffStats = new ArrayList<>();
        for (User u : staffUsers) {
            Map<String, Object> st = new LinkedHashMap<>();
            st.put("staffId", u.getId());
            st.put("name", u.getFullName());
            st.put("phone", u.getPhone() != null ? u.getPhone() : "");
            st.put("hours", hoursByStaff.getOrDefault(u.getId(), 0.0));
            st.put("shiftCount", countByStaff.getOrDefault(u.getId(), 0));
            st.put("lateCount", lateCountByStaff.getOrDefault(u.getId(), 0));
            st.put("totalLateMinutes", lateMinutesByStaff.getOrDefault(u.getId(), 0));
            st.put("earlyCount", earlyCountByStaff.getOrDefault(u.getId(), 0));
            staffStats.add(st);
        }
        staffStats.sort(Comparator
                .comparingDouble((Map<String, Object> m) -> ((Number) m.get("hours")).doubleValue())
                .thenComparing(m -> String.valueOf(m.get("name"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("weekStart", weekStart.format(DATE_FORMAT));
        result.put("weekEnd", weekEnd.format(DATE_FORMAT));
        result.put("maxWeeklyHours", (int) MAX_WEEKLY_HOURS);
        result.put("totalPositions", totalPositions);
        result.put("filledPositions", filledPositions);
        result.put("templates", templatesList);
        result.put("days", daysList);
        result.put("staffStats", staffStats);
        return result;
    }

    public Map<String, Object> autoAssign(LocalDate requestedWeekStart) {
        return autoAssign(requestedWeekStart, null, null);
    }

    public Map<String, Object> autoAssign(LocalDate requestedWeekStart, String courtId, String courtName) {
        boolean explicitBranch = courtId != null && !courtId.isBlank() && !"ALL".equalsIgnoreCase(courtId);
        String targetCourtId = explicitBranch ? courtId : "court_thanh_phat";
        String targetCourtName = courtName != null && !courtName.isBlank() ? courtName : "Sân bóng Thành Phát";
        LocalDate weekStart = weekStartOf(requestedWeekStart);
        LocalDate today = LocalDate.now(clock);

        // Nạp tuần hiện tại + ngày D-1 của tuần để kiểm tra quy tắc nghỉ 8h
        List<String> queryDates = new ArrayList<>();
        queryDates.add(weekStart.minusDays(1).format(DATE_FORMAT));
        for (int i = 0; i < 7; i++) {
            queryDates.add(weekStart.plusDays(i).format(DATE_FORMAT));
        }

        List<User> staffUsers = userRepository.findByRole("STAFF");
        List<StaffShift> allExisting = staffShiftRepository.findByShiftDateIn(queryDates);

        Map<String, List<StaffShift>> shiftsByStaff = new HashMap<>();
        Map<String, Map<String, Set<String>>> assignedByDateAndSlot = new HashMap<>();
        Map<String, Double> hoursByStaff = new HashMap<>();
        Map<String, Integer> countByStaff = new HashMap<>();

        for (User u : staffUsers) {
            shiftsByStaff.put(u.getId(), new ArrayList<>());
            hoursByStaff.put(u.getId(), 0.0);
            countByStaff.put(u.getId(), 0);
        }

        LocalDate weekEnd = weekStart.plusDays(6);
        for (StaffShift s : allExisting) {
            shiftsByStaff.computeIfAbsent(s.getStaffId(), k -> new ArrayList<>()).add(s);
            LocalDate sd = parseDateOrNull(s.getShiftDate());
            if (sd != null && !sd.isBefore(weekStart) && !sd.isAfter(weekEnd)) {
                hoursByStaff.merge(s.getStaffId(), hoursOf(s.getShiftSlot()), Double::sum);
                countByStaff.merge(s.getStaffId(), 1, Integer::sum);
                if (!explicitBranch || targetCourtId.equals(s.getCourtId())) {
                    templateOf(s.getShiftSlot()).ifPresent(t ->
                            assignedByDateAndSlot
                                    .computeIfAbsent(s.getShiftDate(), k -> new HashMap<>())
                                    .computeIfAbsent(t.code(), k -> new HashSet<>())
                                    .add(s.getStaffId()));
                }
            }
        }

        int created = 0;
        int missingPositions = 0;
        List<StaffShift> toSave = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            if (d.isBefore(today)) {
                continue;
            }
            String dStr = d.format(DATE_FORMAT);
            for (SlotTemplate t : TEMPLATES) {
                int min = minStaff(t.code(), d);
                Set<String> alreadyInSlot = assignedByDateAndSlot
                        .computeIfAbsent(dStr, k -> new HashMap<>())
                        .computeIfAbsent(t.code(), k -> new HashSet<>());

                while (alreadyInSlot.size() < min) {
                    int posIndex = alreadyInSlot.size();
                    String duty = posIndex == 0 ? "Lễ tân & Thu ngân" : "Trực sân & POS";

                    // Chọn nhân viên hợp lệ có ít giờ tuần nhất -> ít ca hơn -> theo tên
                    List<User> sortedCandidates = new ArrayList<>(staffUsers);
                    sortedCandidates.sort(Comparator
                            .comparingDouble((User u) -> hoursByStaff.getOrDefault(u.getId(), 0.0))
                            .thenComparingInt(u -> countByStaff.getOrDefault(u.getId(), 0))
                            .thenComparing(User::getFullName));

                    User picked = null;
                    for (User cand : sortedCandidates) {
                        if (alreadyInSlot.contains(cand.getId())) {
                            continue;
                        }
                        List<StaffShift> candShifts = shiftsByStaff.getOrDefault(cand.getId(), List.of());
                        List<String> errs = validateAgainstShifts(d, t.label(), null, today, candShifts);
                        if (errs.isEmpty()) {
                            picked = cand;
                            break;
                        }
                    }

                    if (picked == null) {
                        missingPositions++;
                        break;
                    }

                    StaffShift createdShift = new StaffShift(
                            "sh_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                            picked.getId(),
                            picked.getFullName(),
                            picked.getPhone(),
                            targetCourtId,
                            targetCourtName,
                            dStr,
                            t.label(),
                            duty,
                            "UPCOMING",
                            null,
                            null,
                            "Tự động xếp ca cân bằng giờ công"
                    );
                    toSave.add(createdShift);
                    shiftsByStaff.computeIfAbsent(picked.getId(), k -> new ArrayList<>()).add(createdShift);
                    alreadyInSlot.add(picked.getId());
                    hoursByStaff.merge(picked.getId(), t.hours(), Double::sum);
                    countByStaff.merge(picked.getId(), 1, Integer::sum);
                    created++;
                }
            }
        }

        if (!toSave.isEmpty()) {
            staffShiftRepository.saveAll(toSave);
        }

        String msg;
        if (created == 0 && missingPositions == 0) {
            msg = "Tất cả vị trí ca trong tuần đã được phủ đủ.";
        } else if (missingPositions > 0) {
            msg = String.format("Đã xếp thêm %d ca. Còn %d vị trí thiếu nhân sự (cần tuyển thêm NV).", created, missingPositions);
        } else {
            msg = String.format("Đã tự động xếp %d ca và phủ đủ lịch tuần.", created);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("missingPositions", missingPositions);
        result.put("message", msg);
        return result;
    }

    public StaffShift clockIn(User actor, String shiftId) {
        StaffShift shift = staffShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

        ensureShiftActor(actor, shift);
        if (!"UPCOMING".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ca đã chấm công");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate shiftDate = parseDateOrNull(shift.getShiftDate());
        LocalTime[] times = timesOf(shift.getShiftSlot());

        if (enforceTimeWindow && shiftDate != null && times != null) {
            LocalDateTime windowStart = shiftDate.atTime(times[0]).minusMinutes(30);
            LocalDateTime windowEnd = shiftDate.atTime(times[1]);
            if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        String.format("Chỉ được chấm công từ %s đến %s ngày %s",
                                windowStart.toLocalTime().format(TIME_FORMAT),
                                windowEnd.toLocalTime().format(TIME_FORMAT),
                                shift.getShiftDate())
                );
            }
        }

        if (shiftDate != null && times != null) {
            LocalDateTime scheduledStart = shiftDate.atTime(times[0]);
            long late = Duration.between(scheduledStart, now).toMinutes();
            if (late > 10) {
                shift.setLateMinutes((int) late);
            } else {
                shift.setLateMinutes(null);
            }
        }

        shift.setStatus("ACTIVE");
        shift.setCheckInTime(now.toLocalTime().format(TIME_FORMAT));
        return staffShiftRepository.save(shift);
    }

    public StaffShift clockOut(User actor, String shiftId) {
        return clockOut(actor, shiftId, null);
    }

    public StaffShift clockOut(User actor, String shiftId, String reason) {
        StaffShift shift = staffShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ca làm việc"));

        ensureShiftActor(actor, shift);
        if (!"ACTIVE".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ca chưa bắt đầu");
        }

        LocalTime nowTime = LocalDateTime.now(clock).toLocalTime();
        LocalTime[] times = timesOf(shift.getShiftSlot());
        int earlyMinutes = 0;
        if (times != null && nowTime.isBefore(times[1])) {
            earlyMinutes = (int) Duration.between(nowTime, times[1]).toMinutes();
        }

        if (enforceTimeWindow && earlyMinutes > 15 && (reason == null || reason.trim().isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Bạn đang kết thúc ca sớm %d phút (ca kết thúc lúc %s). Vui lòng nhập lý do kết thúc ca sớm.",
                            earlyMinutes, times[1].format(TIME_FORMAT))
            );
        }

        if (earlyMinutes > 0 && reason != null && !reason.trim().isEmpty()) {
            String noteAppend = String.format("[Về sớm %d phút: %s]", earlyMinutes, reason.trim());
            String existingNotes = shift.getNotes() != null ? shift.getNotes().trim() : "";
            shift.setNotes(existingNotes.isEmpty() ? noteAppend : existingNotes + " | " + noteAppend);
        }

        shift.setStatus("COMPLETED");
        shift.setCheckOutTime(nowTime.format(TIME_FORMAT));
        return staffShiftRepository.save(shift);
    }

    private void ensureShiftActor(User actor, StaffShift shift) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn.");
        }
        boolean isOwnerRole = "OWNER".equalsIgnoreCase(actor.getRole());
        boolean isShiftOwner = actor.getId() != null && actor.getId().equals(shift.getStaffId());
        if (!isOwnerRole && !isShiftOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải nhân viên được phân ca này");
        }
    }

    private static String weekdayLabel(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }
}
