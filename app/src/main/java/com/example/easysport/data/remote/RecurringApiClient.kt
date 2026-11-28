package com.example.easysport.data.remote

import com.example.easysport.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RecurringSlotConflictException(
    val conflicts: List<RecurringConflictItem>,
    val serverNow: String?,
    message: String,
) : Exception(message)

object RecurringApiClient {

    suspend fun previewPackage(
        courtId: String,
        fieldDetail: String,
        startDate: String,
        slots: List<String>,
        packageType: String = "WEEKLY_12",
    ): Result<RecurringPackagePreview> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyJson = JSONObject().apply {
                put("courtId", courtId)
                put("fieldDetail", fieldDetail)
                put("startDate", startDate)
                put("slots", JSONArray(slots))
                put("packageType", packageType)
            }
            val (code, body) = send("POST", "${PaymentApiClient.baseUrl}/api/recurring-packages/preview", bodyJson.toString())
            val data = extractData(code, body, "Không thể kiểm tra lịch 12 buổi")
            parsePreview(data)
        }
    }

    suspend fun startRecurringCheckout(
        courtId: String,
        fieldDetail: String,
        startDate: String,
        slots: List<String>,
        paymentMethod: String,
        customerName: String,
        customerPhone: String,
        idempotencyKey: String,
        packageType: String = "WEEKLY_12",
    ): Result<RecurringCheckoutResult> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyJson = JSONObject().apply {
                put("courtId", courtId)
                put("fieldDetail", fieldDetail)
                put("startDate", startDate)
                put("slots", JSONArray(slots))
                put("packageType", packageType)
                put("paymentMethod", paymentMethod)
                put("customerName", customerName)
                put("customerPhone", customerPhone)
                put("idempotencyKey", idempotencyKey)
            }
            val (code, body) = send("POST", "${PaymentApiClient.baseUrl}/api/recurring-packages/checkout", bodyJson.toString())
            if (code == 409) {
                throwRecurringConflictIfPresent(body)
            }
            val data = extractData(code, body, "Không thể tạo gói lịch cố định")
            RecurringCheckoutResult(
                packageId = data.optString("packageId"),
                holdId = data.optNullable("holdId"),
                bookingId = data.optNullable("bookingId"),
                paymentId = data.optNullable("paymentId"),
                orderInvoiceNumber = data.optNullable("orderInvoiceNumber"),
                checkoutUrl = data.optNullable("checkoutUrl"),
                status = data.optString("packageStatus", data.optString("status", "AWAITING_PAYMENT")),
                holdStatus = data.optNullable("holdStatus"),
                paymentMethod = data.optString("paymentMethod", paymentMethod),
                amount = data.optInt("amount", 0),
                unitPrice = data.optInt("unitPrice", 0),
                totalPrice = data.optInt("totalPrice", 0),
                totalSessions = data.optInt("totalSessions", 12),
                expiresAt = data.optNullable("expiresAt"),
                paymentDeadline = data.optNullable("paymentDeadlineIso") ?: data.optNullable("paymentDeadline"),
                verificationDeadline = data.optNullable("verificationDeadlineIso") ?: data.optNullable("verificationDeadline"),
                serverNow = data.optNullable("serverNowIso") ?: data.optNullable("serverNow"),
                packageTitle = data.optString("packageTitle", "3 tháng · 12 buổi · 1 buổi/tuần"),
                startDate = data.optString("startDate", startDate),
                weekdayLabel = data.optString("weekdayLabel", ""),
                timeRange = data.optString("timeRange", slots.joinToString(", ")),
            )
        }
    }

    suspend fun getPackagePaymentStatus(packageId: String): Result<BookingStatusResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${PaymentApiClient.baseUrl}/api/recurring-packages/$packageId/payment-status", null)
            val data = extractData(code, body, "Không kiểm tra được trạng thái thanh toán gói")
            BookingStatusResponse(
                bookingId = data.optString("bookingId", packageId),
                bookingStatus = data.optString("bookingStatus", "AWAITING_PAYMENT"),
                totalPrice = data.optInt("totalPrice", 0),
                depositAmount = data.optInt("depositAmount", 0),
                remainingAmount = data.optInt("remainingAmount", 0),
                paymentMethod = data.optString("paymentMethod", "QR_TRANSFER"),
                isPaid = data.optBoolean("paid", false),
                isDepositPaid = data.optBoolean("depositPaid", false),
                isExpired = data.optBoolean("expired", false),
                paymentId = data.optNullable("paymentId"),
                orderInvoiceNumber = data.optNullable("orderInvoiceNumber"),
                paymentStatus = data.optNullable("paymentStatus"),
                paymentAmount = if (data.isNull("paymentAmount")) null else data.optInt("paymentAmount"),
                checkoutUrl = data.optNullable("checkoutUrl"),
                expiresAt = data.optNullable("expiresAt"),
                holdStatus = data.optNullable("holdStatus"),
                paymentDeadline = data.optNullable("paymentDeadlineIso") ?: data.optNullable("paymentDeadline"),
                verificationDeadline = data.optNullable("verificationDeadlineIso") ?: data.optNullable("verificationDeadline"),
                serverNow = data.optNullable("serverNowIso") ?: data.optNullable("serverNow"),
            )
        }
    }

    suspend fun getMyPackages(): Result<List<RecurringPackageDetail>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${PaymentApiClient.baseUrl}/api/recurring-packages", null)
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không tải được danh sách lịch cố định (HTTP $code)"))
            }
            val arr = JSONObject(body).optJSONArray("data") ?: JSONArray()
            val list = ArrayList<RecurringPackageDetail>(arr.length())
            for (i in 0 until arr.length()) {
                list.add(parsePackageDetail(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun getPackageDetail(packageId: String): Result<RecurringPackageDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${PaymentApiClient.baseUrl}/api/recurring-packages/$packageId", null)
            parsePackageDetail(extractData(code, body, "Không tải được chi tiết gói lịch cố định"))
        }
    }

    suspend fun previewSessionAction(sessionId: String, reasonType: String): Result<CancelPreviewResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = URLEncoder.encode(reasonType, "UTF-8")
                val (code, body) = send(
                    "GET",
                    "${PaymentApiClient.baseUrl}/api/recurring-sessions/$sessionId/cancel-preview?reasonType=$q",
                    null
                )
                val d = extractData(code, body, "Không xem được phương án xử lý")
                val actionsArr = d.optJSONArray("availableActions") ?: JSONArray()
                val actions = List(actionsArr.length()) { idx -> actionsArr.getString(idx) }
                CancelPreviewResult(
                    sessionId = d.optString("sessionId"),
                    sessionDate = d.optString("sessionDate"),
                    timeRange = d.optString("timeRange"),
                    reasonType = d.optString("reasonType"),
                    sessionStatus = d.optString("sessionStatus"),
                    hoursUntilStart = d.optDouble("hoursUntilStart", 0.0),
                    earlyCancelMinHours = d.optInt("earlyCancelMinHours", 168),
                    eligibleForEarlyCancel = d.optBoolean("eligibleForEarlyCancel", false),
                    weatherAlreadyConfirmed = d.optBoolean("weatherAlreadyConfirmed", false),
                    excessiveCancelTriggered = d.optBoolean("excessiveCancelTriggered", false),
                    recentSelfCancelCount = d.optLong("recentSelfCancelCount", 0L),
                    excessiveCancelLimit = d.optNullableInt("excessiveCancelLimit"),
                    excessiveCancelWindowDays = d.optNullableInt("excessiveCancelWindowDays"),
                    nearStartMaxHours = d.optNullableInt("nearStartMaxHours"),
                    configuredRefundPercent = d.optNullableInt("configuredRefundPercent"),
                    paidAmountSnapshot = d.optInt("paidAmountSnapshot", 0),
                    refundedAmount = d.optInt("refundedAmount", 0),
                    maxRefundableAmount = d.optInt("maxRefundableAmount", 0),
                    requiresOwnerApproval = d.optBoolean("requiresOwnerApproval", false),
                    canSlide = d.optBoolean("canSlide", false),
                    canKeepCredit = d.optBoolean("canKeepCredit", false),
                    canAutoRefund = d.optBoolean("canAutoRefund", false),
                    estimatedWalletRefundAmount = d.optNullableInt("estimatedWalletRefundAmount"),
                    proposedSlideDate = d.optNullable("proposedSlideDate"),
                    policyNotice = d.optString("policyNotice", ""),
                    availableActions = actions,
                )
            }
        }

    suspend fun cancelOrPostponeSession(
        sessionId: String,
        reasonType: String,
        chosenAction: String,
        expectedSlideDate: String? = null,
        note: String? = null,
    ): Result<CancelOrPostponeResult> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("reasonType", reasonType)
                put("chosenAction", chosenAction)
                if (!expectedSlideDate.isNullOrBlank()) put("expectedSlideDate", expectedSlideDate)
                if (!note.isNullOrBlank()) put("note", note)
            }
            val (code, body) = send(
                "POST",
                "${PaymentApiClient.baseUrl}/api/recurring-sessions/$sessionId/cancel-or-postpone",
                payload.toString()
            )
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không thể xử lý buổi (HTTP $code)"))
            }
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: JSONObject()
            CancelOrPostponeResult(
                outcome = data.optString("outcome", ""),
                message = data.optString("message", json.optString("message", "Đã xử lý")),
                proposedSlideDate = data.optNullable("proposedSlideDate"),
            )
        }
    }

    suspend fun redeemCredit(
        creditId: String,
        dateLabel: String,
        fieldDetail: String,
        slots: List<String>,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("dateLabel", dateLabel)
                put("fieldDetail", fieldDetail)
                put("slots", JSONArray(slots))
            }
            val (code, body) = send(
                "POST",
                "${PaymentApiClient.baseUrl}/api/recurring-credits/$creditId/redeem",
                payload.toString()
            )
            if (code == 409) {
                throwRecurringConflictIfPresent(body)
                throw IllegalStateException(parseError(body, "Khung giờ này vừa có người đặt. Buổi còn dư của bạn vẫn được giữ nguyên."))
            }
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không thể đổi buổi còn dư (HTTP $code)"))
            }
            val json = JSONObject(body)
            json.optJSONObject("data")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("message", "Đặt bù thành công!")
        }
    }

    suspend fun releaseHold(packageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("POST", "${PaymentApiClient.baseUrl}/api/recurring-packages/$packageId/release", "{}")
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không nhả được giữ chỗ (HTTP $code)"))
            }
        }
    }

    private fun parsePreview(d: JSONObject): RecurringPackagePreview {
        val slotsArr = d.optJSONArray("slots") ?: JSONArray()
        val sessArr = d.optJSONArray("sessions") ?: JSONArray()
        val confArr = d.optJSONArray("conflictingSessions") ?: JSONArray()
        val sessions = ArrayList<RecurringSessionPreviewItem>(sessArr.length())
        for (i in 0 until sessArr.length()) {
            val s = sessArr.getJSONObject(i)
            sessions.add(
                RecurringSessionPreviewItem(
                    sessionIndex = s.optInt("sessionIndex"),
                    sessionDate = s.optString("sessionDate"),
                    weekdayLabel = s.optString("weekdayLabel"),
                    timeRange = s.optString("timeRange"),
                    fieldDetail = s.optString("fieldDetail"),
                    unitPrice = s.optInt("unitPrice"),
                    available = s.optBoolean("available", true),
                    conflictStatus = s.optNullable("conflictStatus"),
                    conflictMessage = s.optNullable("conflictMessage"),
                )
            )
        }
        val conflicts = parseConflicts(confArr)
        return RecurringPackagePreview(
            courtId = d.optString("courtId"),
            courtName = d.optString("courtName"),
            fieldId = d.optString("fieldId"),
            fieldDetail = d.optString("fieldDetail"),
            packageType = d.optString("packageType", "WEEKLY_12"),
            packageTitle = d.optString("packageTitle", "3 tháng · 12 buổi · 1 buổi/tuần"),
            startDate = d.optString("startDate"),
            weekdayLabel = d.optString("weekdayLabel"),
            timeRange = d.optString("timeRange"),
            slots = List(slotsArr.length()) { idx -> slotsArr.getString(idx) },
            totalSessions = d.optInt("totalSessions", 12),
            unitPrice = d.optInt("unitPrice", 0),
            totalPrice = d.optInt("totalPrice", 0),
            allAvailable = d.optBoolean("allAvailable", false),
            sessions = sessions,
            conflictingSessions = conflicts,
            walletBalance = d.optNullableInt("walletBalance"),
        )
    }

    private fun parsePackageDetail(d: JSONObject): RecurringPackageDetail {
        val pkgJson = if (d.has("package") && !d.isNull("package")) d.getJSONObject("package") else d
        val pkg = RecurringPackageSummary(
            id = pkgJson.optString("id"),
            userId = pkgJson.optString("userId"),
            courtId = pkgJson.optString("courtId"),
            courtName = pkgJson.optString("courtName"),
            courtAddress = pkgJson.optString("courtAddress", ""),
            fieldId = pkgJson.optString("fieldId"),
            fieldDetail = pkgJson.optString("fieldDetail"),
            packageType = pkgJson.optString("packageType", "WEEKLY_12"),
            packageTitle = pkgJson.optString("packageTitle", "3 tháng · 12 buổi · 1 buổi/tuần"),
            startDate = pkgJson.optString("startDate"),
            weekdayLabel = pkgJson.optString("weekdayLabel"),
            timeRange = pkgJson.optString("timeRange"),
            slotsCsv = pkgJson.optString("slotsCsv", ""),
            slotCount = pkgJson.optInt("slotCount", 1),
            totalSessions = pkgJson.optInt("totalSessions", 12),
            unitPrice = pkgJson.optInt("unitPrice", 0),
            totalPrice = pkgJson.optInt("totalPrice", 0),
            totalPaid = pkgJson.optInt("totalPaid", 0),
            totalRefunded = pkgJson.optInt("totalRefunded", 0),
            paymentMethod = pkgJson.optString("paymentMethod", "QR_TRANSFER"),
            status = pkgJson.optString("status", "AWAITING_PAYMENT"),
            holdId = pkgJson.optNullable("holdId"),
            paymentId = pkgJson.optNullable("paymentId"),
            customerName = pkgJson.optString("customerName", ""),
            customerPhone = pkgJson.optString("customerPhone", ""),
            createdAt = pkgJson.optNullable("createdAt"),
            playedCount = d.optInt("completedCount", d.optInt("playedCount", 0)),
            upcomingCount = d.optInt("upcomingCount", 0),
            availableCreditsCount = d.optInt("availableCreditsCount", 0),
        )
        val sessArr = d.optJSONArray("sessions") ?: JSONArray()
        val sessions = List(sessArr.length()) { idx ->
            val s = sessArr.getJSONObject(idx)
            RecurringSessionItem(
                id = s.optString("id"),
                packageId = s.optString("packageId"),
                sessionIndex = s.optInt("sessionIndex"),
                sessionDate = s.optString("sessionDate"),
                timeRange = s.optString("timeRange"),
                fieldDetail = s.optString("fieldDetail"),
                bookingId = s.optNullable("bookingId"),
                status = s.optString("status", "UPCOMING"),
                unitPriceSnapshot = s.optInt("unitPriceSnapshot", 0),
                paidAmountSnapshot = s.optInt("paidAmountSnapshot", 0),
                refundedAmount = s.optInt("refundedAmount", 0),
                rescheduledToDate = s.optNullable("rescheduledToDate"),
                replacementBookingId = s.optNullable("replacementBookingId"),
                resolutionType = s.optNullable("resolutionType"),
                resolutionNote = s.optNullable("resolutionNote"),
            )
        }
        val credArr = d.optJSONArray("credits") ?: JSONArray()
        val credits = List(credArr.length()) { idx ->
            val c = credArr.getJSONObject(idx)
            RecurringCreditItem(
                id = c.optString("id"),
                packageId = c.optString("packageId"),
                sourceSessionId = c.optString("sourceSessionId"),
                courtId = c.optString("courtId"),
                courtName = c.optString("courtName"),
                fieldDetail = c.optString("fieldDetail"),
                timeRange = c.optString("timeRange"),
                slotCount = c.optInt("slotCount", 1),
                status = c.optString("status", "AVAILABLE"),
                expiresAt = c.optNullable("expiresAt"),
                redeemedBookingId = c.optNullable("redeemedBookingId"),
                redeemedDate = c.optNullable("redeemedDate"),
                redeemedTimeRange = c.optNullable("redeemedTimeRange"),
                createdAt = c.optNullable("createdAt"),
            )
        }
        val reqArr = d.optJSONArray("changeRequests") ?: JSONArray()
        val reqs = List(reqArr.length()) { idx ->
            val r = reqArr.getJSONObject(idx)
            SessionChangeRequestItem(
                id = r.optString("id"),
                packageId = r.optString("packageId"),
                sessionId = r.optString("sessionId"),
                userId = r.optString("userId"),
                courtId = r.optString("courtId"),
                courtName = r.optString("courtName"),
                customerName = r.optString("customerName", ""),
                customerPhone = r.optString("customerPhone", ""),
                requestType = r.optString("requestType"),
                requestedAction = r.optNullable("requestedAction"),
                reasonNote = r.optString("reasonNote", ""),
                countsTowardCancelLimit = r.optBoolean("countsTowardCancelLimit", false),
                status = r.optString("status", "PENDING"),
                approvedRefundAmount = r.optNullableInt("approvedRefundAmount"),
                resolvedOutcome = r.optNullable("resolvedOutcome"),
                ownerNote = r.optNullable("ownerNote"),
                resolvedBy = r.optNullable("resolvedBy"),
                createdAt = r.optNullable("createdAt"),
                resolvedAt = r.optNullable("resolvedAt"),
            )
        }
        return RecurringPackageDetail(pkg, sessions, credits, reqs)
    }

    private fun throwRecurringConflictIfPresent(body: String) {
        try {
            val j = JSONObject(body)
            val arr = j.optJSONArray("conflictingSlots")
            if (arr != null && arr.length() > 0) {
                val list = parseConflicts(arr)
                val msg = j.optString("message", "Có buổi trong gói bị trùng lịch.")
                throw RecurringSlotConflictException(list, j.optNullable("serverNow"), msg)
            }
        } catch (e: RecurringSlotConflictException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun parseConflicts(arr: JSONArray): List<RecurringConflictItem> = List(arr.length()) { i ->
        val c = arr.getJSONObject(i)
        RecurringConflictItem(
            slot = c.optString("slot", ""),
            status = c.optString("status", ""),
            expiresAt = c.optNullable("expiresAt"),
            dateLabel = c.optNullable("dateLabel"),
            sessionIndex = c.optNullableInt("sessionIndex"),
        )
    }

    private fun extractData(code: Int, body: String, fallback: String): JSONObject {
        if (code !in 200..299) throw IllegalStateException(parseError(body, "$fallback (HTTP $code)"))
        val j = JSONObject(body)
        return if (j.has("data") && !j.isNull("data")) j.getJSONObject("data") else j
    }

    private fun JSONObject.optNullable(k: String): String? =
        if (isNull(k)) null else optString(k).takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.optNullableInt(k: String): Int? =
        if (isNull(k)) null else optInt(k)

    private fun parseError(body: String, fallback: String): String {
        if (body.isBlank()) return fallback
        return try {
            val j = JSONObject(body)
            j.optString("message", j.optString("error", fallback)).ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun send(method: String, urlStr: String, body: String?): Pair<Int, String> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            PaymentApiClient.authToken?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
            if (body != null) {
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }
        }
        return try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: ""
            Pair(code, text)
        } catch (e: Exception) {
            throw IllegalStateException("Không kết nối được backend: ${e.localizedMessage ?: e.message}")
        } finally {
            conn.disconnect()
        }
    }
}
