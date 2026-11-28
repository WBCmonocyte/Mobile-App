package com.easysport.dto;

import java.util.List;

public final class RecurringDtos {
    private RecurringDtos() {}

    public record PreviewRequest(
            String courtId,
            String fieldDetail,
            String startDate,
            List<String> slots,
            String packageType
    ) {}

    public record SessionPreviewItem(
            int sessionIndex,
            String sessionDate,
            String weekdayLabel,
            String timeRange,
            String fieldDetail,
            int unitPrice,
            boolean available,
            String conflictStatus,
            String conflictMessage
    ) {}

    public record PackagePreviewResponse(
            String courtId,
            String courtName,
            String fieldId,
            String fieldDetail,
            String packageType,
            String packageTitle,
            String startDate,
            String weekdayLabel,
            String timeRange,
            List<String> slots,
            int totalSessions,
            int unitPrice,
            int totalPrice,
            boolean allAvailable,
            List<SessionPreviewItem> sessions,
            List<ConflictSlotDto> conflictingSessions,
            Integer walletBalance
    ) {}

    public record RecurringCheckoutRequest(
            String courtId,
            String fieldDetail,
            String startDate,
            List<String> slots,
            String packageType,
            String paymentMethod,
            String customerName,
            String customerPhone,
            String idempotencyKey
    ) {}

    public record CancelOrPostponeRequest(
            String reasonType,      // PERSONAL | WEATHER
            String chosenAction,    // SLIDE | KEEP_CREDIT | REFUND_WALLET | REQUEST_REVIEW
            String expectedSlideDate,
            String note
    ) {}

    public record CancelPreviewResponse(
            String sessionId,
            String sessionDate,
            String timeRange,
            String reasonType,
            String sessionStatus,
            double hoursUntilStart,
            int earlyCancelMinHours,
            boolean eligibleForEarlyCancel,
            boolean weatherAlreadyConfirmed,
            boolean excessiveCancelTriggered,
            long recentSelfCancelCount,
            Integer excessiveCancelLimit,
            Integer excessiveCancelWindowDays,
            Integer nearStartMaxHours,
            Integer configuredRefundPercent,
            int paidAmountSnapshot,
            int refundedAmount,
            int maxRefundableAmount,
            boolean requiresOwnerApproval,
            boolean canSlide,
            boolean canKeepCredit,
            boolean canAutoRefund,
            Integer estimatedWalletRefundAmount,
            String proposedSlideDate,
            String policyNotice,
            List<String> availableActions
    ) {}

    public record RedeemCreditRequest(
            String dateLabel,
            String fieldDetail,
            List<String> slots
    ) {}

    public record OwnerResolveChangeRequest(
            String decision,       // APPROVE_WEATHER | APPROVE_REFUND | APPROVE_CREDIT | APPROVE_SLIDE | REJECT
            Integer refundAmount,
            String ownerNote
    ) {}
}
