package com.example.easysport

import com.example.easysport.data.model.Booking
import com.example.easysport.data.model.CancelPreviewResult
import com.example.easysport.data.model.OwnerOverviewData
import com.example.easysport.data.model.PaymentMethod
import com.example.easysport.data.model.PaymentStatus
import com.example.easysport.data.model.RecurringConflictItem
import com.example.easysport.data.model.RecurringCreditItem
import com.example.easysport.data.model.RecurringPackagePreview
import com.example.easysport.data.model.RecurringPackageSummary
import com.example.easysport.data.model.RecurringSessionPreviewItem
import com.example.easysport.data.model.WalletSummary
import com.example.easysport.data.model.WalletTransactionItem
import com.example.easysport.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringAndWalletUiTest {

    @Test
    fun paymentMethodAndBookingBackwardCompatibility() {
        val methods = PaymentMethod.values().map { it.name }.toSet()
        assertTrue(methods.containsAll(listOf("QR_TRANSFER", "CASH_ON_SITE", "WALLET", "CREDIT")))

        val legacyBooking = Booking(
            id = "b1",
            courtId = "c1",
            courtName = "Sân A",
            fieldDetail = "Sân 5A",
            timeRange = "19:00 - 20:30",
            dateLabel = "01/12/2026",
            totalPrice = 250_000,
            payment = PaymentStatus.PAID,
            address = "TP. HCM",
        )
        assertNull(legacyBooking.recurringPackageId)
        assertNull(legacyBooking.recurringSessionId)
        assertEquals(PaymentMethod.QR_TRANSFER, legacyBooking.paymentMethod)
    }

    @Test
    fun recurringPreviewBlocksCheckoutUntilAll12SessionsAreAvailable() {
        val sessionsWithConflict = (1..12).map { idx ->
            RecurringSessionPreviewItem(
                sessionIndex = idx,
                sessionDate = "01/12/2026",
                weekdayLabel = "Thứ Ba",
                timeRange = "19:00 - 20:30",
                fieldDetail = "Sân 5A",
                unitPrice = 250_000,
                available = idx != 8,
                conflictStatus = if (idx == 8) "BOOKED" else null,
                conflictMessage = if (idx == 8) "Đã có lịch đặt" else null,
            )
        }
        val preview = RecurringPackagePreview(
            courtId = "c1",
            courtName = "Sân Test",
            fieldId = "f1",
            fieldDetail = "Sân 5A",
            packageType = "WEEKLY_12",
            packageTitle = "3 tháng · 12 buổi · 1 buổi/tuần",
            startDate = "01/12/2026",
            weekdayLabel = "Thứ Ba",
            timeRange = "19:00 - 20:30",
            slots = listOf("19:00 - 20:30"),
            totalSessions = 12,
            unitPrice = 250_000,
            totalPrice = 3_000_000,
            allAvailable = false,
            sessions = sessionsWithConflict,
            conflictingSessions = listOf(RecurringConflictItem("19:00 - 20:30", "BOOKED", null, "19/01/2027", 8)),
            walletBalance = 500_000,
        )

        assertFalse(preview.allAvailable)
        assertEquals(12, preview.sessions.size)
        assertEquals(8, preview.conflictingSessions.first().sessionIndex)
        assertEquals("19/01/2027", preview.conflictingSessions.first().dateLabel)
    }

    @Test
    fun walletSummaryAndMakeupCreditRemainStrictlySeparateConcepts() {
        val wallet = WalletSummary(
            walletId = "wal_1",
            userId = "u1",
            balance = 300_000,
            transactions = listOf(
                WalletTransactionItem("tx1", "u1", "CREDIT_REFUND", "CREDIT", 300_000, 300_000, "Hoàn tiền buổi #4"),
                WalletTransactionItem("tx2", "u1", "DEBIT_BOOKING", "DEBIT", 100_000, 200_000, "Đặt sân lẻ"),
            )
        )
        assertTrue(wallet.transactions[0].isPositive)
        assertFalse(wallet.transactions[1].isPositive)
        val pkgSummary = RecurringPackageSummary(
            id = "pkg1", userId = "u1", courtId = "c1", courtName = "Sân Test",
            courtAddress = "Q1", fieldId = "f1", fieldDetail = "Sân 5A",
            packageType = "WEEKLY_12", packageTitle = "3 tháng · 12 buổi · 1 buổi/tuần",
            startDate = "01/12/2026", weekdayLabel = "Thứ Ba", timeRange = "19:00 - 20:30",
            slotsCsv = "19:00 - 20:30", slotCount = 1, totalSessions = 12,
            unitPrice = 250_000, totalPrice = 3_000_000, totalPaid = 3_000_000,
            totalRefunded = 0, paymentMethod = "QR_TRANSFER", status = "ACTIVE",
            playedCount = 2, upcomingCount = 9, availableCreditsCount = 1,
        )
        val credit = RecurringCreditItem(
            id = "cred1", packageId = "pkg1", sourceSessionId = "s3", courtId = "c1",
            courtName = "Sân Test", fieldDetail = "Sân 5A", timeRange = "19:00 - 20:30",
            slotCount = 1, status = "AVAILABLE",
        )

        assertEquals(300_000, wallet.balance)
        assertEquals(1, pkgSummary.availableCreditsCount)
        assertEquals("AVAILABLE", credit.status)
    }

    @Test
    fun unconfiguredNearStartCancelRoutesToOwnerApprovalWithoutAutoRefund() {
        val cancelPreview = CancelPreviewResult(
            sessionId = "s2",
            sessionDate = "08/12/2026",
            timeRange = "19:00 - 20:30",
            reasonType = "PERSONAL",
            sessionStatus = "UPCOMING",
            hoursUntilStart = 20.0,
            earlyCancelMinHours = 168,
            eligibleForEarlyCancel = false,
            weatherAlreadyConfirmed = false,
            excessiveCancelTriggered = false,
            recentSelfCancelCount = 0,
            paidAmountSnapshot = 250_000,
            refundedAmount = 0,
            maxRefundableAmount = 250_000,
            requiresOwnerApproval = true,
            canSlide = false,
            canKeepCredit = false,
            canAutoRefund = false,
            estimatedWalletRefundAmount = null,
            proposedSlideDate = "02/03/2027",
            policyNotice = "Hủy dưới 168 giờ trước trận và chính sách tỷ lệ hoàn sát giờ chưa được cấu hình tự động.",
            availableActions = listOf("REQUEST_REVIEW"),
        )

        assertTrue(cancelPreview.requiresOwnerApproval)
        assertFalse(cancelPreview.canAutoRefund)
        assertEquals(listOf("REQUEST_REVIEW"), cancelPreview.availableActions)
        assertEquals(250_000, cancelPreview.maxRefundableAmount)
    }

    @Test
    fun navigationRoutesAndOwnerOverviewDefaultsWorkCleanly() {
        assertEquals("wallet", Routes.WALLET)
        assertEquals("recurring_packages", Routes.RECURRING_PACKAGES)
        assertEquals("recurring_payment/pkg_abc", Routes.recurringPayment("pkg_abc"))

        val overview = OwnerOverviewData(
            totalRevenue = 3_100_000L,
            cashRevenue = 0L,
            qrRevenue = 0L,
            walletRevenue = 3_100_000L,
            totalBookings = 13,
            activeBookings = 1,
            totalCourts = 1,
            pendingReports = 0,
            pendingRecurringRequests = 2,
            bookings = emptyList(),
        )
        assertEquals(3_100_000L, overview.walletRevenue)
        assertEquals(2, overview.pendingRecurringRequests)
    }
}
