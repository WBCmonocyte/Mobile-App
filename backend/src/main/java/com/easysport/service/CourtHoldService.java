package com.easysport.service;

import com.easysport.config.SepayConfig;
import com.easysport.dto.AvailabilitySlotDto;
import com.easysport.dto.CourtAvailabilityResponse;
import com.easysport.dto.StartCheckoutRequest;
import com.easysport.dto.StartCheckoutResponse;
import com.easysport.entity.Court;
import com.easysport.entity.CourtField;
import com.easysport.entity.CourtHoldSlot;
import com.easysport.entity.Payment;
import com.easysport.repository.CourtHoldSlotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CourtHoldService {

    private final CourtHoldTransactionService transactionService;
    private final CourtHoldSlotRepository courtHoldSlotRepository;
    private final SepayConfig sepayConfig;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CourtHoldService(CourtHoldTransactionService transactionService,
                            CourtHoldSlotRepository courtHoldSlotRepository,
                            SepayConfig sepayConfig,
                            Clock clock) {
        this.transactionService = transactionService;
        this.courtHoldSlotRepository = courtHoldSlotRepository;
        this.sepayConfig = sepayConfig;
        this.clock = clock;
    }

    // Constructor tiện lợi cho Unit Tests tương thích ngược
    public CourtHoldService(com.easysport.repository.CourtRepository courtRepository,
                            com.easysport.repository.CourtFieldRepository courtFieldRepository,
                            com.easysport.repository.CourtResourceRepository courtResourceRepository,
                            com.easysport.repository.CourtHoldRepository courtHoldRepository,
                            com.easysport.repository.CourtHoldSlotRepository courtHoldSlotRepository,
                            com.easysport.repository.BookingRepository bookingRepository,
                            com.easysport.repository.PaymentRepository paymentRepository,
                            SepayService sepayService,
                            SepayConfig sepayConfig,
                            Clock clock) {
        this(new CourtHoldTransactionService(
                courtRepository, courtFieldRepository, courtResourceRepository,
                courtHoldRepository, courtHoldSlotRepository, bookingRepository,
                paymentRepository, sepayService, clock
        ), courtHoldSlotRepository, sepayConfig, clock);
    }

    /**
     * DTO kết quả nội bộ tương thích ngược
     */
    public static class HoldResult {
        public final com.easysport.entity.CourtHold hold;
        public final com.easysport.entity.Booking booking;
        public final com.easysport.entity.Payment payment;
        public final com.easysport.entity.Court court;
        public final boolean isExisting;

        public HoldResult(com.easysport.entity.CourtHold hold, com.easysport.entity.Booking booking,
                          com.easysport.entity.Payment payment, com.easysport.entity.Court court, boolean isExisting) {
            this.hold = hold;
            this.booking = booking;
            this.payment = payment;
            this.court = court;
            this.isExisting = isExisting;
        }

        public HoldResult(CourtHoldTransactionService.HoldResult r) {
            this.hold = r.hold;
            this.booking = r.booking;
            this.payment = r.payment;
            this.court = r.court;
            this.isExisting = r.isExisting;
        }
    }

    public static class SlotInterval {
        public final int startMinute;
        public final int endMinute;
        public final String originalLabel;

        public SlotInterval(int startMinute, int endMinute, String originalLabel) {
            this.startMinute = startMinute;
            this.endMinute = endMinute;
            this.originalLabel = originalLabel;
        }
    }

    /**
     * Bắt đầu phiên checkout:
     * 1. Gọi transactionService.acquireHoldTransaction qua Spring proxy (triệt tiêu self-invocation).
     * 2. Chuẩn bị checkoutUrl ngoài transaction DB.
     * 3. Trả về response kèm các mốc thời gian rõ ràng (paymentDeadline, verificationDeadline, serverNow).
     */
    public StartCheckoutResponse startCheckout(StartCheckoutRequest req) {
        return startCheckout(req, req.getUserId());
    }

    public StartCheckoutResponse startCheckout(StartCheckoutRequest req, String authenticatedUserId) {
        CourtHoldTransactionService.HoldResult result = transactionService.acquireHoldTransaction(req, authenticatedUserId);

        try {
            String checkoutToken = result.payment.getCheckoutToken();
            String checkoutUrl = "/payments/" + checkoutToken + "/checkout";
            LocalDateTime now = LocalDateTime.now(clock);

            LocalDateTime paymentDeadline = result.hold.getPaymentDeadline();
            LocalDateTime verificationDeadline = result.hold.getVerificationDeadline();
            String holdStatus = result.hold.getDynamicStatus(now);

            return new StartCheckoutResponse(
                    result.hold.getId(),
                    result.booking.getId(),
                    result.payment.getId(),
                    result.payment.getOrderInvoiceNumber(),
                    checkoutUrl,
                    result.payment.getAmount(),
                    result.booking.getTotalPrice(),
                    result.booking.getDepositAmount(),
                    result.booking.getRemainingAmount(),
                    paymentDeadline,
                    verificationDeadline,
                    now,
                    holdStatus
            );
        } catch (Exception ex) {
            if (!result.isExisting) {
                transactionService.releaseHoldTransaction(result.hold.getId(), authenticatedUserId);
            }
            throw new IllegalStateException("Lỗi chuẩn bị phiên thanh toán SePay: " + ex.getMessage(), ex);
        }
    }

    public HoldResult acquireHoldTransaction(StartCheckoutRequest req) {
        CourtHoldTransactionService.HoldResult r = transactionService.acquireHoldTransaction(req, req.getUserId());
        return new HoldResult(r);
    }

    public void releaseHold(String holdId) {
        transactionService.releaseHoldTransaction(holdId, null);
    }

    public void releaseHold(String holdId, String userId) {
        transactionService.releaseHoldTransaction(holdId, userId);
    }

    /**
     * Tra cứu lịch trống theo thời gian thực (AVAILABLE, HELD, BOOKED).
     */
    @Transactional(readOnly = true)
    public CourtAvailabilityResponse getAvailability(String courtId, String fieldDetail, String dateLabel) {
        LocalDateTime now = LocalDateTime.now(clock);
        Court court = transactionService.resolveCourt(courtId);
        CourtField field = transactionService.resolveField(court, fieldDetail);
        List<String> resourceIds = transactionService.resolvePhysicalResourceIds(court, field);

        java.time.LocalDate date = BookingRules.parseDate(dateLabel);
        dateLabel = date.format(BookingRules.DATE);
        List<String> standardSlots = BookingRules.SLOTS;

        List<CourtHoldSlot> activeSlots = courtHoldSlotRepository.findActiveSlotsForCourt(court.getId(), dateLabel.trim(), now);

        List<AvailabilitySlotDto> resultList = new ArrayList<>();
        for (String slotLabel : standardSlots) {
            SlotInterval interval = parseSlot(slotLabel);
            if (interval == null) continue;

            Optional<CourtHoldSlot> conflicting = activeSlots.stream()
                    .filter(s -> resourceIds.contains(s.getResourceId()))
                    .filter(s -> interval.startMinute < s.getEndMinute() && s.getStartMinute() < interval.endMinute)
                    .filter(s -> "BOOKED".equalsIgnoreCase(s.getStatus()) ||
                            "CONFIRMED".equalsIgnoreCase(s.getStatus()) ||
                            ("HELD".equalsIgnoreCase(s.getStatus()) || "AWAITING_PAYMENT".equalsIgnoreCase(s.getStatus()) || "VERIFYING_PAYMENT".equalsIgnoreCase(s.getStatus())) && s.getExpiresAt().isAfter(now))
                    .findFirst();

            if (conflicting.isPresent()) {
                CourtHoldSlot s = conflicting.get();
                String status = ("BOOKED".equalsIgnoreCase(s.getStatus()) || "CONFIRMED".equalsIgnoreCase(s.getStatus()))
                        ? "BOOKED"
                        : "HELD";
                resultList.add(new AvailabilitySlotDto(slotLabel, status, s.getExpiresAt()));
            } else {
                boolean future = date.atTime(java.time.LocalTime.parse(slotLabel.substring(0, 5)))
                        .isAfter(java.time.LocalDateTime.ofInstant(clock.instant(), BookingRules.ZONE).plusMinutes(6));
                resultList.add(new AvailabilitySlotDto(slotLabel, future ? "AVAILABLE" : "UNAVAILABLE", null));
            }
        }

        return new CourtAvailabilityResponse(court.getId(), fieldDetail, dateLabel, resultList, now);
    }

    /**
     * Job định kỳ quét và giải phóng các hold đã quá thời hạn 6 phút
     */
    @Scheduled(fixedDelay = 15000)
    public void cleanupExpiredHolds() {
        transactionService.cleanupExpiredHoldsTransaction();
    }

    public Court resolveCourt(String courtId) {
        return transactionService.resolveCourt(courtId);
    }

    public CourtField resolveField(Court court, String fieldDetail) {
        return transactionService.resolveField(court, fieldDetail);
    }

    public List<String> resolvePhysicalResourceIds(Court court, CourtField field) {
        return transactionService.resolvePhysicalResourceIds(court, field);
    }

    public SlotInterval parseSlot(String slotLabel) {
        if (slotLabel == null || !slotLabel.contains("-")) return null;
        String[] parts = slotLabel.split("-");
        if (parts.length < 2) return null;
        try {
            int start = toMinutes(parts[0].trim());
            int end = toMinutes(parts[1].trim());
            return new SlotInterval(start, end, slotLabel.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int toMinutes(String timeStr) {
        String clean = timeStr.replace("–", "-").trim();
        String[] hm = clean.split(":");
        int hours = Integer.parseInt(hm[0].trim());
        int minutes = hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0;
        return hours * 60 + minutes;
    }
}
