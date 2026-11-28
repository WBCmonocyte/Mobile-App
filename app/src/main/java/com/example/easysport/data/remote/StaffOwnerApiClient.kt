package com.example.easysport.data.remote

import com.example.easysport.data.model.BranchFieldItem
import com.example.easysport.data.model.BranchRevenueItem
import com.example.easysport.data.model.BranchStockStatModel
import com.example.easysport.data.model.CategoryStockStatModel
import com.example.easysport.data.model.FieldRevenueStat
import com.example.easysport.data.model.InventoryItemModel
import com.example.easysport.data.model.InventoryTransactionModel
import com.example.easysport.data.model.OwnerInventoryData
import com.example.easysport.data.model.OwnerInventoryStatsData
import com.example.easysport.data.model.OwnerOverviewData
import com.example.easysport.data.model.OwnerRecurringSessionItem
import com.example.easysport.data.model.OwnerStaffAccount
import com.example.easysport.data.model.OwnerWeekSchedule
import com.example.easysport.data.model.PosOrderItemRequest
import com.example.easysport.data.model.PosSaleItem
import com.example.easysport.data.model.ReorderSuggestionModel
import com.example.easysport.data.model.ScheduleDay
import com.example.easysport.data.model.ScheduleSlot
import com.example.easysport.data.model.SessionChangeRequestItem
import com.example.easysport.data.model.ShiftReportItem
import com.example.easysport.data.model.ShiftTemplate
import com.example.easysport.data.model.StaffBookingItem
import com.example.easysport.data.model.StaffMemberOption
import com.example.easysport.data.model.StaffQrCheckoutSession
import com.example.easysport.data.model.StaffShiftItem
import com.example.easysport.data.model.StaffShiftSummary
import com.example.easysport.data.model.StaffWorkload
import com.example.easysport.data.model.TimeSlotRevenueStat
import com.example.easysport.data.model.TopSellingItemModel
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

object StaffOwnerApiClient {

    private fun baseUrl(): String = PaymentApiClient.baseUrl
    private fun token(): String? = PaymentApiClient.authToken

    suspend fun getStaffBookings(): Result<List<StaffBookingItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/staff/bookings", null)
            val data = extractDataArray(code, body, "Không thể tải danh sách đơn sân")
            parseBookings(data)
        }
    }

    suspend fun createWalkInBooking(
        courtId: String,
        fieldDetail: String,
        timeRange: String,
        customerName: String,
        customerPhone: String,
        totalPrice: Int,
        depositAmount: Int = totalPrice,
        remainingAmount: Int = 0,
        paymentMethod: String = "CASH_ON_SITE",
    ): Result<StaffBookingItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("courtId", courtId)
                put("fieldDetail", fieldDetail)
                put("timeRange", timeRange)
                put("customerName", customerName)
                put("customerPhone", customerPhone)
                put("totalPrice", totalPrice)
                put("depositAmount", depositAmount)
                put("remainingAmount", remainingAmount)
                put("paymentMethod", paymentMethod)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/bookings/walk-in", payload.toString())
            parseBooking(extractDataObject(code, body, "Không thể tạo lượt thuê tại quầy"))
        }
    }

    suspend fun checkInBooking(bookingId: String): Result<StaffBookingItem> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("POST", "${baseUrl()}/api/staff/bookings/$bookingId/check-in", "{}")
            parseBooking(extractDataObject(code, body, "Check-in thất bại"))
        }
    }

    suspend fun addPosServices(
        bookingId: String,
        serviceNote: String,
        serviceAmount: Int,
        items: List<PosOrderItemRequest> = emptyList(),
    ): Result<StaffBookingItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("serviceNote", serviceNote)
                put("serviceAmount", serviceAmount)
                if (items.isNotEmpty()) {
                    put("items", JSONArray().apply {
                        items.forEach { itItem ->
                            put(JSONObject().apply {
                                put("itemId", itItem.itemId)
                                put("quantity", itItem.quantity)
                            })
                        }
                    })
                }
            }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/bookings/$bookingId/services", payload.toString())
            parseBooking(extractDataObject(code, body, "Không thể thêm dịch vụ"))
        }
    }

    suspend fun checkOutBooking(
        bookingId: String,
        paymentMethod: String = "CASH",
    ): Result<StaffBookingItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply { put("paymentMethod", paymentMethod) }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/bookings/$bookingId/check-out", payload.toString())
            parseBooking(extractDataObject(code, body, "Trả sân thất bại"))
        }
    }

    suspend fun cancelBooking(bookingId: String, reason: String): Result<StaffBookingItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply { put("reason", reason) }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/bookings/$bookingId/cancel", payload.toString())
            parseBooking(extractDataObject(code, body, "Không thể hủy đơn"))
        }
    }

    suspend fun createRetailPosSale(
        itemsNote: String,
        amount: Int,
        paymentMethod: String,
        courtId: String? = null,
        courtName: String? = null,
        items: List<PosOrderItemRequest> = emptyList(),
    ): Result<PosSaleItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("itemsNote", itemsNote)
                put("amount", amount)
                put("paymentMethod", paymentMethod)
                if (!courtId.isNullOrBlank()) put("courtId", courtId)
                if (!courtName.isNullOrBlank()) put("courtName", courtName)
                if (items.isNotEmpty()) {
                    put("items", JSONArray().apply {
                        items.forEach { itItem ->
                            put(JSONObject().apply {
                                put("itemId", itItem.itemId)
                                put("quantity", itItem.quantity)
                            })
                        }
                    })
                }
            }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/pos/retail", payload.toString())
            parsePosSale(extractDataObject(code, body, "Không thể ghi nhận đơn bán lẻ"))
        }
    }

    suspend fun getMyRetailSales(): Result<List<PosSaleItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/staff/pos/sales", null)
            val arr = extractDataArray(code, body, "Không thể tải danh sách bán lẻ")
            val list = mutableListOf<PosSaleItem>()
            for (i in 0 until arr.length()) {
                list.add(parsePosSale(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun getShiftSummary(): Result<StaffShiftSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/staff/shifts/summary", null)
            val o = extractDataObject(code, body, "Không thể tải tổng hợp ca")
            StaffShiftSummary(
                shiftId = o.optNullable("shiftId"),
                cashFromBookings = o.optInt("cashFromBookings", 0),
                qrFromBookings = o.optInt("qrFromBookings", 0),
                retailCash = o.optInt("retailCash", 0),
                retailQr = o.optInt("retailQr", 0),
                totalCash = o.optInt("totalCash", 0),
                totalQr = o.optInt("totalQr", 0),
                orderCount = o.optInt("orderCount", 0),
                retailCount = o.optInt("retailCount", 0),
                unpaidActiveCount = o.optInt("unpaidActiveCount", 0),
                unpaidTotal = o.optInt("unpaidTotal", 0),
            )
        }
    }

    suspend fun createStaffQrCheckout(
        amount: Int,
        description: String,
        referenceId: String? = null,
    ): Result<StaffQrCheckoutSession> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("amount", amount)
                put("description", description)
                if (!referenceId.isNullOrBlank()) put("referenceId", referenceId)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/qr/checkout", payload.toString())
            parseStaffQrSession(extractDataObject(code, body, "Không thể tạo phiên thanh toán SePay QR"))
        }
    }

    suspend fun getStaffQrStatus(paymentId: String): Result<StaffQrCheckoutSession> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/staff/qr/$paymentId/status", null)
            parseStaffQrSession(extractDataObject(code, body, "Không thể kiểm tra trạng thái thanh toán QR"))
        }
    }

    private fun parseStaffQrSession(o: JSONObject): StaffQrCheckoutSession = StaffQrCheckoutSession(
        paymentId = o.optString("paymentId", ""),
        orderInvoiceNumber = o.optString("orderInvoiceNumber", ""),
        checkoutUrl = o.optString("checkoutUrl", ""),
        amount = o.optInt("amount", 0),
        status = o.optString("status", "PENDING"),
        isPaid = o.optBoolean("isPaid", false),
        isExpired = o.optBoolean("isExpired", false),
        expiresAt = o.optString("expiresAt", "").takeIf { it.isNotBlank() && it != "null" },
        serverNow = o.optString("serverNow", "").takeIf { it.isNotBlank() && it != "null" },
    )

    suspend fun getMyShifts(): Result<List<StaffShiftItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/staff/shifts/my", null)
            parseShifts(extractDataArray(code, body, "Không thể tải lịch làm việc"))
        }
    }

    suspend fun clockInShift(shiftId: String): Result<StaffShiftItem> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("POST", "${baseUrl()}/api/staff/shifts/$shiftId/clock-in", "{}")
            parseShift(extractDataObject(code, body, "Chấm công vào ca thất bại"))
        }
    }

    suspend fun clockOutShift(shiftId: String, reason: String? = null): Result<StaffShiftItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                if (!reason.isNullOrBlank()) put("reason", reason)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/shifts/$shiftId/clock-out", payload.toString())
            parseShift(extractDataObject(code, body, "Kết thúc ca thất bại"))
        }
    }

    suspend fun getMyReports(): Result<List<ShiftReportItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/staff/reports/my", null)
            parseReports(extractDataArray(code, body, "Không thể tải lịch sử báo cáo két"))
        }
    }

    suspend fun submitShiftReport(
        cashAmount: Int,
        qrAmount: Int,
        totalOrders: Int,
        handoverNote: String,
        shiftId: String? = null,
        courtId: String? = null,
        courtName: String? = null,
    ): Result<ShiftReportItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("cashAmount", cashAmount)
                put("qrAmount", qrAmount)
                put("totalOrders", totalOrders)
                put("handoverNote", handoverNote)
                if (!shiftId.isNullOrBlank()) put("shiftId", shiftId)
                if (!courtId.isNullOrBlank()) put("courtId", courtId)
                if (!courtName.isNullOrBlank()) put("courtName", courtName)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/reports", payload.toString())
            parseReport(extractDataObject(code, body, "Nộp báo cáo két thất bại"))
        }
    }

    suspend fun resubmitShiftReport(
        reportId: String,
        cashAmount: Int,
        qrAmount: Int,
        note: String,
    ): Result<ShiftReportItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("cashAmount", cashAmount)
                put("qrAmount", qrAmount)
                put("handoverNote", note)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/staff/reports/$reportId/resubmit", payload.toString())
            parseReport(extractDataObject(code, body, "Gửi giải trình đối soát thất bại"))
        }
    }

    suspend fun getOwnerOverview(period: String = "ALL"): Result<OwnerOverviewData> = withContext(Dispatchers.IO) {
        runCatching {
            val query = "?period=" + URLEncoder.encode(period, "UTF-8")
            val (code, body) = send("GET", "${baseUrl()}/api/owner/overview$query", null)
            val data = extractDataObject(code, body, "Không thể tải tổng quan chủ sân")
            val branchArr = data.optJSONArray("branchRevenues") ?: JSONArray()
            val branches = mutableListOf<BranchRevenueItem>()
            for (i in 0 until branchArr.length()) {
                val b = branchArr.getJSONObject(i)
                val fieldsArr = b.optJSONArray("fields") ?: JSONArray()
                val fieldItems = mutableListOf<BranchFieldItem>()
                for (j in 0 until fieldsArr.length()) {
                    val f = fieldsArr.getJSONObject(j)
                    fieldItems.add(
                        BranchFieldItem(
                            id = f.optString("id"),
                            label = f.optString("label"),
                            detail = f.optString("detail", ""),
                            fieldType = f.optString("fieldType", "5_VS_5"),
                        )
                    )
                }
                branches.add(
                    BranchRevenueItem(
                        courtId = b.optString("courtId"),
                        courtName = b.optString("courtName", "Cụm sân"),
                        address = b.optString("address", ""),
                        sportType = b.optString("sportType", "Bóng đá"),
                        fieldCount = b.optInt("fieldCount", fieldItems.size),
                        pricePerHour = b.optInt("pricePerHour", 300000),
                        morningPrice = b.optInt("morningPrice", 150000),
                        afternoonPrice = b.optInt("afternoonPrice", 250000),
                        eveningPrice = b.optInt("eveningPrice", 300000),
                        bookingCount = b.optInt("bookingCount", 0),
                        activeCount = b.optInt("activeCount", 0),
                        revenue = b.optLong("revenue", 0L),
                        courtRevenue = b.optLong("courtRevenue", 0L),
                        posRevenue = b.optLong("posRevenue", 0L),
                        latitude = if (b.isNull("latitude")) null else b.optDouble("latitude"),
                        longitude = if (b.isNull("longitude")) null else b.optDouble("longitude"),
                        indoor = b.optBoolean("indoor", false),
                        fields = fieldItems,
                    )
                )
            }
            val fieldArr = data.optJSONArray("fieldRevenues") ?: JSONArray()
            val fieldRevenues = buildList {
                for (i in 0 until fieldArr.length()) {
                    val f = fieldArr.getJSONObject(i)
                    add(
                        FieldRevenueStat(
                            courtId = f.optString("courtId"),
                            courtName = f.optString("courtName", "Cụm sân"),
                            fieldName = f.optString("fieldName", "Sân"),
                            bookingCount = f.optInt("bookingCount", 0),
                            courtRevenue = f.optLong("courtRevenue", 0L),
                            serviceRevenue = f.optLong("serviceRevenue", 0L),
                            totalRevenue = f.optLong("totalRevenue", 0L),
                        )
                    )
                }
            }
            val slotArr = data.optJSONArray("timeSlotRevenues") ?: JSONArray()
            val timeSlotRevenues = buildList {
                for (i in 0 until slotArr.length()) {
                    val s = slotArr.getJSONObject(i)
                    add(
                        TimeSlotRevenueStat(
                            slot = s.optString("slot"),
                            label = s.optString("label"),
                            bookingCount = s.optInt("bookingCount", 0),
                            revenue = s.optLong("revenue", 0L),
                        )
                    )
                }
            }
            OwnerOverviewData(
                totalRevenue = data.optLong("totalRevenue", 0L),
                cashRevenue = data.optLong("cashRevenue", 0L),
                qrRevenue = data.optLong("qrRevenue", 0L),
                walletRevenue = data.optLong("walletRevenue", 0L),
                courtRevenue = data.optLong("courtRevenue", 0L),
                posRevenue = data.optLong("posRevenue", 0L),
                totalBookings = data.optInt("totalBookings", 0),
                activeBookings = data.optInt("activeBookings", 0),
                totalCourts = data.optInt("totalCourts", 0),
                pendingReports = data.optInt("pendingReports", 0),
                pendingRecurringRequests = data.optInt("pendingRecurringRequests", 0),
                bookings = parseBookings(data.optJSONArray("bookings") ?: JSONArray()),
                branchRevenues = branches,
                fieldRevenues = fieldRevenues,
                timeSlotRevenues = timeSlotRevenues,
            )
        }
    }

    suspend fun getOwnerShiftsAndStaff(): Result<Pair<List<StaffShiftItem>, List<StaffMemberOption>>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/owner/shifts", null)
            val data = extractDataObject(code, body, "Không thể tải lịch phân ca")
            val shifts = parseShifts(data.optJSONArray("shifts") ?: JSONArray())
            val staffArr = data.optJSONArray("staffList") ?: JSONArray()
            val staff = mutableListOf<StaffMemberOption>()
            for (i in 0 until staffArr.length()) {
                val s = staffArr.getJSONObject(i)
                staff.add(
                    StaffMemberOption(
                        id = s.optString("id"),
                        name = s.optString("name"),
                        phone = s.optString("phone"),
                        email = s.optString("email"),
                    )
                )
            }
            Pair(shifts, staff)
        }
    }

    suspend fun getOwnerWeekSchedule(weekStart: String? = null, courtId: String? = null): Result<OwnerWeekSchedule> = withContext(Dispatchers.IO) {
        runCatching {
            val params = buildList {
                if (!weekStart.isNullOrBlank()) add("weekStart=" + URLEncoder.encode(weekStart, "UTF-8"))
                if (!courtId.isNullOrBlank() && courtId != "ALL") add("courtId=" + URLEncoder.encode(courtId, "UTF-8"))
            }
            val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
            val (code, body) = send("GET", "${baseUrl()}/api/owner/shifts/schedule$query", null)
            val data = extractDataObject(code, body, "Không thể tải lịch phân ca tuần")

            val templatesArr = data.optJSONArray("templates") ?: JSONArray()
            val templates = mutableListOf<ShiftTemplate>()
            for (i in 0 until templatesArr.length()) {
                val t = templatesArr.getJSONObject(i)
                templates.add(
                    ShiftTemplate(
                        code = t.optString("code"),
                        label = t.optString("label"),
                        start = t.optString("start"),
                        end = t.optString("end"),
                        hours = t.optDouble("hours", 0.0),
                    )
                )
            }

            val daysArr = data.optJSONArray("days") ?: JSONArray()
            val days = mutableListOf<ScheduleDay>()
            for (i in 0 until daysArr.length()) {
                val d = daysArr.getJSONObject(i)
                val slotsArr = d.optJSONArray("slots") ?: JSONArray()
                val slots = mutableListOf<ScheduleSlot>()
                for (j in 0 until slotsArr.length()) {
                    val sl = slotsArr.getJSONObject(j)
                    slots.add(
                        ScheduleSlot(
                            code = sl.optString("code"),
                            label = sl.optString("label"),
                            start = sl.optString("start"),
                            end = sl.optString("end"),
                            minStaff = sl.optInt("minStaff", 1),
                            shifts = parseShifts(sl.optJSONArray("shifts") ?: JSONArray()),
                        )
                    )
                }
                days.add(
                    ScheduleDay(
                        date = d.optString("date"),
                        weekday = d.optString("weekday"),
                        isPast = d.optBoolean("isPast", false),
                        slots = slots,
                    )
                )
            }

            val statsArr = data.optJSONArray("staffStats") ?: JSONArray()
            val stats = mutableListOf<StaffWorkload>()
            for (i in 0 until statsArr.length()) {
                val st = statsArr.getJSONObject(i)
                stats.add(
                    StaffWorkload(
                        staffId = st.optString("staffId"),
                        name = st.optString("name"),
                        phone = st.optString("phone"),
                        hours = st.optDouble("hours", 0.0),
                        shiftCount = st.optInt("shiftCount", 0),
                        lateCount = st.optInt("lateCount", 0),
                        totalLateMinutes = st.optInt("totalLateMinutes", 0),
                        earlyCount = st.optInt("earlyCount", 0),
                    )
                )
            }

            OwnerWeekSchedule(
                weekStart = data.optString("weekStart"),
                weekEnd = data.optString("weekEnd"),
                maxWeeklyHours = data.optInt("maxWeeklyHours", 48),
                totalPositions = data.optInt("totalPositions", 0),
                filledPositions = data.optInt("filledPositions", 0),
                templates = templates,
                days = days,
                staffStats = stats,
            )
        }
    }

    suspend fun autoAssignWeek(
        weekStart: String,
        courtId: String? = null,
        courtName: String? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("weekStart", weekStart)
                if (!courtId.isNullOrBlank() && courtId != "ALL") put("courtId", courtId)
                if (!courtName.isNullOrBlank()) put("courtName", courtName)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/shifts/auto-assign", payload.toString())
            val data = extractDataObject(code, body, "Tự động xếp ca thất bại")
            val json = JSONObject(body)
            json.optString("message").takeIf { it.isNotBlank() && it != "null" }
                ?: data.optString("message", "Đã tự động xếp ca")
        }
    }

    suspend fun assignOwnerShift(
        staffId: String,
        shiftDate: String,
        shiftSlot: String,
        duty: String,
        notes: String,
        courtId: String? = null,
        courtName: String? = null,
    ): Result<StaffShiftItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("staffId", staffId)
                put("shiftDate", shiftDate)
                put("shiftSlot", shiftSlot)
                put("duty", duty)
                put("notes", notes)
                if (!courtId.isNullOrBlank()) put("courtId", courtId)
                if (!courtName.isNullOrBlank()) put("courtName", courtName)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/shifts", payload.toString())
            parseShift(extractDataObject(code, body, "Phân ca thất bại"))
        }
    }

    suspend fun deleteOwnerShift(shiftId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("DELETE", "${baseUrl()}/api/owner/shifts/$shiftId", null)
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không thể hủy ca ($code)"))
            }
        }
    }

    suspend fun getOwnerReports(): Result<List<ShiftReportItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/owner/reports", null)
            parseReports(extractDataArray(code, body, "Không thể tải báo cáo két"))
        }
    }

    suspend fun confirmOwnerReport(reportId: String): Result<ShiftReportItem> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("POST", "${baseUrl()}/api/owner/reports/$reportId/confirm", "{}")
            parseReport(extractDataObject(code, body, "Xác nhận két thất bại"))
        }
    }

    suspend fun rejectOwnerReport(reportId: String, note: String): Result<ShiftReportItem> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply { put("note", note) }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/reports/$reportId/reject", payload.toString())
            parseReport(extractDataObject(code, body, "Không thể báo lệch két"))
        }
    }

    suspend fun updateCourtPricing(
        courtId: String,
        pricePerHour: Int,
        morningPrice: Int,
        afternoonPrice: Int,
        eveningPrice: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("pricePerHour", pricePerHour)
                put("morningPrice", morningPrice)
                put("afternoonPrice", afternoonPrice)
                put("eveningPrice", eveningPrice)
            }
            val (code, body) = send("PUT", "${baseUrl()}/api/owner/courts/$courtId/pricing", payload.toString())
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không thể cập nhật giá ($code)"))
            }
        }
    }

    suspend fun createOwnerCourt(
        name: String,
        address: String,
        sportType: String,
        fieldCount: Int,
        pricePerHour: Int,
        morningPrice: Int,
        afternoonPrice: Int,
        eveningPrice: Int,
        latitude: Double? = null,
        longitude: Double? = null,
        indoor: Boolean = false,
        fieldTypeLabel: String = sportType,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("name", name)
                put("address", address)
                put("sportType", sportType)
                put("fieldCount", fieldCount)
                put("pricePerHour", pricePerHour)
                put("morningPrice", morningPrice)
                put("afternoonPrice", afternoonPrice)
                put("eveningPrice", eveningPrice)
                put("indoor", indoor)
                put("fieldTypeLabel", fieldTypeLabel)
                if (latitude != null) put("latitude", latitude)
                if (longitude != null) put("longitude", longitude)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/courts", payload.toString())
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không thể tạo cụm sân ($code)"))
            }
        }
    }

    suspend fun updateOwnerCourt(
        courtId: String,
        name: String,
        address: String,
        pricePerHour: Int,
        morningPrice: Int,
        afternoonPrice: Int,
        eveningPrice: Int,
        sportType: String? = null,
        indoor: Boolean? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("name", name)
                put("address", address)
                put("pricePerHour", pricePerHour)
                put("morningPrice", morningPrice)
                put("afternoonPrice", afternoonPrice)
                put("eveningPrice", eveningPrice)
                if (!sportType.isNullOrBlank()) put("sportType", sportType)
                if (indoor != null) put("indoor", indoor)
                if (latitude != null) put("latitude", latitude)
                if (longitude != null) put("longitude", longitude)
            }
            val (code, body) = send("PUT", "${baseUrl()}/api/owner/courts/$courtId", payload.toString())
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không thể cập nhật cụm sân ($code)"))
            }
        }
    }

    suspend fun addCourtField(
        courtId: String,
        label: String,
        detail: String,
        fieldType: String = "5_VS_5",
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("label", label)
                put("detail", detail)
                put("fieldType", fieldType)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/courts/$courtId/fields", payload.toString())
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không thể thêm sân con ($code)"))
            }
        }
    }

    suspend fun deleteCourtField(courtId: String, fieldId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("DELETE", "${baseUrl()}/api/owner/courts/$courtId/fields/$fieldId", null)
            if (code !in 200..299) {
                throw IllegalStateException(parseError(body, "Không thể xóa sân con ($code)"))
            }
        }
    }

    suspend fun getOwnerStaffList(): Result<List<OwnerStaffAccount>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/owner/staff", null)
            val arr = extractDataArray(code, body, "Không thể tải danh sách nhân viên")
            val list = mutableListOf<OwnerStaffAccount>()
            for (i in 0 until arr.length()) {
                list.add(parseStaffAccount(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun createOwnerStaff(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        address: String = "",
    ): Result<OwnerStaffAccount> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("fullName", fullName)
                put("email", email)
                put("phone", phone)
                put("password", password)
                if (address.isNotBlank()) put("address", address)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/staff", payload.toString())
            parseStaffAccount(extractDataObject(code, body, "Không thể tạo tài khoản nhân viên"))
        }
    }

    suspend fun updateOwnerStaff(
        staffId: String,
        fullName: String,
        phone: String,
        address: String,
        newPassword: String? = null,
    ): Result<OwnerStaffAccount> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("fullName", fullName)
                put("phone", phone)
                put("address", address)
                if (!newPassword.isNullOrBlank()) put("newPassword", newPassword)
            }
            val (code, body) = send("PUT", "${baseUrl()}/api/owner/staff/$staffId", payload.toString())
            parseStaffAccount(extractDataObject(code, body, "Không thể cập nhật thông tin nhân viên"))
        }
    }

    suspend fun toggleOwnerStaffLock(staffId: String): Result<OwnerStaffAccount> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("POST", "${baseUrl()}/api/owner/staff/$staffId/toggle-lock", "{}")
            parseStaffAccount(extractDataObject(code, body, "Không thể cập nhật trạng thái nhân viên"))
        }
    }

    suspend fun getOwnerRecurringRequests(): Result<List<SessionChangeRequestItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/owner/recurring-requests", null)
            val arr = extractDataArray(code, body, "Không thể tải danh sách yêu cầu lịch cố định")
            List(arr.length()) { idx ->
                val o = arr.getJSONObject(idx)
                SessionChangeRequestItem(
                    id = o.optString("id"),
                    packageId = o.optString("packageId"),
                    sessionId = o.optString("sessionId"),
                    userId = o.optString("userId"),
                    courtId = o.optString("courtId"),
                    courtName = o.optString("courtName", "Cụm sân"),
                    customerName = o.optString("customerName", "Khách hàng"),
                    customerPhone = o.optString("customerPhone", ""),
                    requestType = o.optString("requestType"),
                    requestedAction = o.optNullable("requestedAction"),
                    reasonNote = o.optString("reasonNote", ""),
                    countsTowardCancelLimit = o.optBoolean("countsTowardCancelLimit", false),
                    status = o.optString("status", "PENDING"),
                    approvedRefundAmount = if (o.isNull("approvedRefundAmount")) null else o.optInt("approvedRefundAmount"),
                    resolvedOutcome = o.optNullable("resolvedOutcome"),
                    ownerNote = o.optNullable("ownerNote"),
                    resolvedBy = o.optNullable("resolvedBy"),
                    createdAt = o.optNullable("createdAt"),
                    resolvedAt = o.optNullable("resolvedAt"),
                    sessionDate = o.optString("sessionDate", ""),
                    timeRange = o.optString("timeRange", ""),
                    paidAmountSnapshot = o.optInt("paidAmountSnapshot", 0),
                    refundedAmount = o.optInt("refundedAmount", 0),
                    maxRefundableAmount = o.optInt("maxRefundableAmount", 0),
                )
            }
        }
    }

    suspend fun getOwnerRecurringSessions(): Result<List<OwnerRecurringSessionItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("GET", "${baseUrl()}/api/owner/recurring-sessions", null)
            val arr = extractDataArray(code, body, "Không thể tải danh sách buổi lịch cố định")
            List(arr.length()) { idx ->
                val o = arr.getJSONObject(idx)
                OwnerRecurringSessionItem(
                    sessionId = o.optString("sessionId"),
                    packageId = o.optString("packageId"),
                    courtId = o.optString("courtId"),
                    courtName = o.optString("courtName", "Cụm sân"),
                    fieldDetail = o.optString("fieldDetail", ""),
                    sessionIndex = o.optInt("sessionIndex", 1),
                    sessionDate = o.optString("sessionDate", ""),
                    timeRange = o.optString("timeRange", ""),
                    status = o.optString("status", "UPCOMING"),
                    customerName = o.optString("customerName", "Khách hàng"),
                    customerPhone = o.optString("customerPhone", ""),
                )
            }
        }
    }

    suspend fun resolveOwnerRecurringRequest(
        requestId: String,
        decision: String,
        refundAmount: Int? = null,
        ownerNote: String = "",
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("decision", decision)
                if (refundAmount != null) put("refundAmount", refundAmount)
                if (ownerNote.isNotBlank()) put("ownerNote", ownerNote)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/recurring-requests/$requestId/resolve", payload.toString())
            val data = extractDataObject(code, body, "Không thể xử lý yêu cầu lịch cố định")
            data.optString("message", "Đã xử lý yêu cầu")
        }
    }

    suspend fun ownerWeatherPostponeSession(
        sessionId: String,
        ownerNote: String = "Chủ sân chủ động hoãn do thời tiết xấu",
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply { put("ownerNote", ownerNote) }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/recurring-sessions/$sessionId/weather-postpone", payload.toString())
            val data = extractDataObject(code, body, "Không thể hoãn buổi lịch cố định")
            data.optString("message", "Đã hoãn buổi do thời tiết")
        }
    }

    // ==================== INVENTORY (KHO HÀNG) ====================

    suspend fun getStaffInventory(courtId: String? = null): Result<List<InventoryItemModel>> = withContext(Dispatchers.IO) {
        runCatching {
            val query = if (!courtId.isNullOrBlank()) "?courtId=${URLEncoder.encode(courtId, "UTF-8")}" else ""
            val (code, body) = send("GET", "${baseUrl()}/api/staff/inventory$query", null)
            val arr = extractDataArray(code, body, "Không thể tải kho hàng tại sân")
            val list = mutableListOf<InventoryItemModel>()
            for (i in 0 until arr.length()) {
                list.add(parseInventoryItem(arr.getJSONObject(i)))
            }
            list
        }
    }

    suspend fun getOwnerInventory(courtId: String? = null): Result<OwnerInventoryData> = withContext(Dispatchers.IO) {
        runCatching {
            val query = if (!courtId.isNullOrBlank() && courtId != "ALL") {
                "?courtId=${URLEncoder.encode(courtId, "UTF-8")}"
            } else ""
            val (code, body) = send("GET", "${baseUrl()}/api/owner/inventory$query", null)
            val data = extractDataObject(code, body, "Không thể tải danh sách kho hàng")
            val itemsArr = data.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<InventoryItemModel>()
            for (i in 0 until itemsArr.length()) {
                items.add(parseInventoryItem(itemsArr.getJSONObject(i)))
            }
            val txArr = data.optJSONArray("transactions") ?: JSONArray()
            val txs = mutableListOf<InventoryTransactionModel>()
            for (i in 0 until txArr.length()) {
                txs.add(parseInventoryTransaction(txArr.getJSONObject(i)))
            }
            OwnerInventoryData(
                items = items,
                transactions = txs,
                totalItems = data.optInt("totalItems", items.size),
                totalUnits = data.optInt("totalUnits", 0),
                lowStockCount = data.optInt("lowStockCount", 0),
                totalStockValue = data.optLong("totalStockValue", 0L),
            )
        }
    }

    suspend fun createInventoryItem(
        courtId: String,
        name: String,
        category: String,
        unit: String,
        importPrice: Int,
        sellPrice: Int,
        stockQuantity: Int,
        minAlertThreshold: Int,
        sku: String = "",
        barcode: String = "",
        supplierName: String = "",
    ): Result<InventoryItemModel> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("courtId", courtId)
                put("name", name)
                put("category", category)
                put("unit", unit)
                put("importPrice", importPrice)
                put("sellPrice", sellPrice)
                put("stockQuantity", stockQuantity)
                put("minAlertThreshold", minAlertThreshold)
                if (sku.isNotBlank()) put("sku", sku)
                if (barcode.isNotBlank()) put("barcode", barcode)
                if (supplierName.isNotBlank()) put("supplierName", supplierName)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/inventory", payload.toString())
            parseInventoryItem(extractDataObject(code, body, "Không thể thêm mặt hàng"))
        }
    }

    suspend fun updateInventoryItem(
        id: String,
        name: String,
        category: String,
        unit: String,
        importPrice: Int,
        sellPrice: Int,
        minAlertThreshold: Int,
        sku: String = "",
        barcode: String = "",
        supplierName: String = "",
    ): Result<InventoryItemModel> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("name", name)
                put("category", category)
                put("unit", unit)
                put("importPrice", importPrice)
                put("sellPrice", sellPrice)
                put("minAlertThreshold", minAlertThreshold)
                put("sku", sku)
                put("barcode", barcode)
                put("supplierName", supplierName)
            }
            val (code, body) = send("PUT", "${baseUrl()}/api/owner/inventory/$id", payload.toString())
            parseInventoryItem(extractDataObject(code, body, "Không thể cập nhật mặt hàng"))
        }
    }

    suspend fun restockInventoryItem(
        id: String,
        quantity: Int,
        unitCost: Int,
        note: String,
        supplierName: String = "",
    ): Result<InventoryItemModel> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("quantity", quantity)
                put("unitCost", unitCost)
                put("note", note)
                if (supplierName.isNotBlank()) put("supplierName", supplierName)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/inventory/$id/restock", payload.toString())
            parseInventoryItem(extractDataObject(code, body, "Nhập kho thất bại"))
        }
    }

    suspend fun adjustInventoryItem(
        id: String,
        newStock: Int,
        note: String,
    ): Result<InventoryItemModel> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("newStock", newStock)
                put("note", note)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/inventory/$id/adjust", payload.toString())
            parseInventoryItem(extractDataObject(code, body, "Kiểm kê kho thất bại"))
        }
    }

    suspend fun wastageInventoryItem(
        id: String,
        quantity: Int,
        reasonType: String,
        note: String,
    ): Result<InventoryItemModel> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("quantity", quantity)
                put("reasonType", reasonType)
                put("note", note)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/inventory/$id/wastage", payload.toString())
            parseInventoryItem(extractDataObject(code, body, "Xuất hủy thất bại"))
        }
    }

    suspend fun transferInventoryItem(
        id: String,
        targetCourtId: String,
        quantity: Int,
        note: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("targetCourtId", targetCourtId)
                put("quantity", quantity)
                put("note", note)
            }
            val (code, body) = send("POST", "${baseUrl()}/api/owner/inventory/$id/transfer", payload.toString())
            if (code !in 200..299) throw IllegalStateException(parseError(body, "Chuyển kho thất bại"))
            JSONObject(body).optString("message", "Đã điều chuyển kho thành công")
        }
    }

    suspend fun getOwnerInventoryStats(
        courtId: String? = null,
        period: String = "ALL",
    ): Result<OwnerInventoryStatsData> = withContext(Dispatchers.IO) {
        runCatching {
            val params = mutableListOf<String>()
            if (!courtId.isNullOrBlank() && courtId != "ALL") {
                params.add("courtId=${URLEncoder.encode(courtId, "UTF-8")}")
            }
            if (period.isNotBlank()) {
                params.add("period=${URLEncoder.encode(period, "UTF-8")}")
            }
            val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
            val (code, body) = send("GET", "${baseUrl()}/api/owner/inventory/stats$query", null)
            val data = extractDataObject(code, body, "Không thể tải thống kê kho hàng")

            val topArr = data.optJSONArray("topSelling") ?: JSONArray()
            val topSelling = buildList {
                for (i in 0 until topArr.length()) {
                    val o = topArr.getJSONObject(i)
                    add(
                        TopSellingItemModel(
                            itemId = o.optString("itemId"),
                            itemName = o.optString("itemName"),
                            courtName = o.optString("courtName", ""),
                            unit = o.optString("unit", "sp"),
                            soldUnits = o.optLong("soldUnits", 0L),
                            revenue = o.optLong("revenue", 0L),
                            profit = o.optLong("profit", 0L),
                            currentStock = o.optInt("currentStock", 0),
                        )
                    )
                }
            }

            val catArr = data.optJSONArray("categoryBreakdown") ?: JSONArray()
            val categoryBreakdown = buildList {
                for (i in 0 until catArr.length()) {
                    val o = catArr.getJSONObject(i)
                    add(
                        CategoryStockStatModel(
                            category = o.optString("category"),
                            label = o.optString("label"),
                            itemCount = o.optInt("itemCount", 0),
                            stockUnits = o.optInt("stockUnits", 0),
                            stockValue = o.optLong("stockValue", 0L),
                            soldUnits = o.optLong("soldUnits", 0L),
                            revenue = o.optLong("revenue", 0L),
                        )
                    )
                }
            }

            val brArr = data.optJSONArray("branchBreakdown") ?: JSONArray()
            val branchBreakdown = buildList {
                for (i in 0 until brArr.length()) {
                    val o = brArr.getJSONObject(i)
                    add(
                        BranchStockStatModel(
                            courtId = o.optString("courtId"),
                            courtName = o.optString("courtName"),
                            itemCount = o.optInt("itemCount", 0),
                            stockUnits = o.optInt("stockUnits", 0),
                            stockValue = o.optLong("stockValue", 0L),
                            lowStockCount = o.optInt("lowStockCount", 0),
                            revenue = o.optLong("revenue", 0L),
                            profit = o.optLong("profit", 0L),
                        )
                    )
                }
            }

            val sugArr = data.optJSONArray("reorderSuggestions") ?: JSONArray()
            val reorderSuggestions = buildList {
                for (i in 0 until sugArr.length()) {
                    val o = sugArr.getJSONObject(i)
                    add(
                        ReorderSuggestionModel(
                            itemId = o.optString("itemId"),
                            name = o.optString("name"),
                            courtId = o.optString("courtId"),
                            courtName = o.optString("courtName"),
                            unit = o.optString("unit", "sp"),
                            currentStock = o.optInt("currentStock", 0),
                            minAlertThreshold = o.optInt("minAlertThreshold", 10),
                            suggestedQty = o.optInt("suggestedQty", 12),
                            estimatedCost = o.optLong("estimatedCost", 0L),
                        )
                    )
                }
            }

            OwnerInventoryStatsData(
                period = data.optString("period", period),
                totalRevenue = data.optLong("totalRevenue", 0L),
                totalCogs = data.optLong("totalCogs", 0L),
                grossProfit = data.optLong("grossProfit", 0L),
                profitMarginPercent = data.optDouble("profitMarginPercent", 0.0),
                totalSoldUnits = data.optInt("totalSoldUnits", 0),
                totalImportSpend = data.optLong("totalImportSpend", 0L),
                totalImportedUnits = data.optInt("totalImportedUnits", 0),
                totalWastageCost = data.optLong("totalWastageCost", 0L),
                totalWastageUnits = data.optInt("totalWastageUnits", 0),
                totalTransferCount = data.optInt("totalTransferCount", 0),
                totalStockValue = data.optLong("totalStockValue", 0L),
                totalPotentialRevenue = data.optLong("totalPotentialRevenue", 0L),
                lowStockCount = data.optInt("lowStockCount", 0),
                outOfStockCount = data.optInt("outOfStockCount", 0),
                topSelling = topSelling,
                categoryBreakdown = categoryBreakdown,
                branchBreakdown = branchBreakdown,
                reorderSuggestions = reorderSuggestions,
            )
        }
    }

    suspend fun deleteInventoryItem(id: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val (code, body) = send("DELETE", "${baseUrl()}/api/owner/inventory/$id", null)
            if (code !in 200..299) throw IllegalStateException(parseError(body, "Không thể xóa mặt hàng"))
            id
        }
    }

    private fun parseInventoryItem(o: JSONObject): InventoryItemModel {
        return InventoryItemModel(
            id = o.optString("id"),
            courtId = o.optString("courtId"),
            courtName = o.optString("courtName", ""),
            name = o.optString("name"),
            category = o.optString("category", "DRINK"),
            unit = o.optString("unit", "chai"),
            importPrice = o.optInt("importPrice", 0),
            sellPrice = o.optInt("sellPrice", 0),
            stockQuantity = o.optInt("stockQuantity", 0),
            minAlertThreshold = o.optInt("minAlertThreshold", 10),
            lowStock = o.optBoolean("lowStock", false),
            sku = o.optString("sku", ""),
            barcode = o.optString("barcode", ""),
            supplierName = o.optString("supplierName", ""),
            updatedAt = o.optString("updatedAt", ""),
        )
    }

    private fun parseInventoryTransaction(o: JSONObject): InventoryTransactionModel {
        return InventoryTransactionModel(
            id = o.optString("id"),
            itemId = o.optString("itemId"),
            itemName = o.optString("itemName", ""),
            courtId = o.optString("courtId", ""),
            type = o.optString("type", "IMPORT"),
            quantityDelta = o.optInt("quantityDelta", 0),
            stockAfter = o.optInt("stockAfter", 0),
            unitCost = o.optInt("unitCost", 0),
            note = o.optString("note", ""),
            createdBy = o.optString("createdBy", ""),
            createdAt = o.optString("createdAt", ""),
        )
    }

    private fun parseStaffAccount(o: JSONObject): OwnerStaffAccount = OwnerStaffAccount(
        id = o.optString("id"),
        name = o.optString("name"),
        phone = o.optString("phone"),
        email = o.optString("email"),
        locked = o.optBoolean("locked", false),
        shiftCount = o.optInt("shiftCount", 0),
        address = o.optString("address", ""),
        completedShifts = o.optInt("completedShifts", 0),
        lateCount = o.optInt("lateCount", 0),
        earlyCount = o.optInt("earlyCount", 0),
        totalHours = o.optDouble("totalHours", 0.0),
    )

    private fun parsePosSale(o: JSONObject): PosSaleItem = PosSaleItem(
        id = o.optString("id"),
        courtId = o.optString("courtId"),
        courtName = o.optString("courtName", "Sân bóng"),
        staffId = o.optString("staffId"),
        staffName = o.optString("staffName"),
        shiftId = o.optNullable("shiftId"),
        itemsNote = o.optString("itemsNote", ""),
        amount = o.optInt("amount", 0),
        paymentMethod = o.optString("paymentMethod", "CASH"),
        saleDate = o.optString("saleDate", ""),
    )

    private fun parseBookings(arr: JSONArray): List<StaffBookingItem> {
        val list = mutableListOf<StaffBookingItem>()
        for (i in 0 until arr.length()) {
            list.add(parseBooking(arr.getJSONObject(i)))
        }
        return list
    }

    private fun parseBooking(o: JSONObject): StaffBookingItem {
        return StaffBookingItem(
            id = o.optString("id"),
            courtId = o.optString("courtId"),
            courtName = o.optString("courtName", "Sân bóng"),
            fieldDetail = o.optString("fieldDetail", ""),
            timeRange = o.optString("timeRange", ""),
            dateLabel = o.optString("dateLabel", ""),
            totalPrice = o.optInt("totalPrice", 0),
            depositAmount = o.optInt("depositAmount", 0),
            remainingAmount = o.optInt("remainingAmount", 0),
            paymentMethod = o.optString("paymentMethod", "QR_TRANSFER"),
            status = o.optString("status", "PENDING"),
            customerName = o.optString("customerName", "Khách hàng"),
            customerPhone = o.optString("customerPhone", ""),
            serviceNote = o.optString("serviceNote", ""),
            serviceAmount = o.optInt("serviceAmount", 0),
        )
    }

    private fun parseShifts(arr: JSONArray): List<StaffShiftItem> {
        val list = mutableListOf<StaffShiftItem>()
        for (i in 0 until arr.length()) {
            list.add(parseShift(arr.getJSONObject(i)))
        }
        return list
    }

    private fun JSONObject.optNullable(k: String): String? =
        if (isNull(k)) null else optString(k).takeIf { it.isNotBlank() && it != "null" }

    private fun parseShift(o: JSONObject): StaffShiftItem {
        return StaffShiftItem(
            id = o.optString("id"),
            staffId = o.optString("staffId"),
            staffName = o.optString("staffName"),
            staffPhone = o.optString("staffPhone"),
            courtId = o.optString("courtId"),
            courtName = o.optString("courtName"),
            shiftDate = o.optString("shiftDate"),
            shiftSlot = o.optString("shiftSlot"),
            duty = o.optString("duty"),
            status = o.optString("status", "UPCOMING"),
            checkInTime = o.optNullable("checkInTime"),
            checkOutTime = o.optNullable("checkOutTime"),
            lateMinutes = if (o.isNull("lateMinutes")) null else o.optInt("lateMinutes"),
            notes = o.optString("notes", ""),
        )
    }

    private fun parseReports(arr: JSONArray): List<ShiftReportItem> {
        val list = mutableListOf<ShiftReportItem>()
        for (i in 0 until arr.length()) {
            list.add(parseReport(arr.getJSONObject(i)))
        }
        return list
    }

    private fun parseReport(o: JSONObject): ShiftReportItem {
        return ShiftReportItem(
            id = o.optString("id"),
            courtId = o.optString("courtId"),
            courtName = o.optString("courtName"),
            staffId = o.optString("staffId"),
            staffName = o.optString("staffName"),
            shiftDate = o.optString("shiftDate"),
            cashAmount = o.optInt("cashAmount", 0),
            qrAmount = o.optInt("qrAmount", 0),
            totalOrders = o.optInt("totalOrders", 0),
            handoverNote = o.optString("handoverNote", ""),
            status = o.optString("status", "PENDING"),
            confirmedBy = o.optNullable("confirmedBy"),
            shiftId = o.optNullable("shiftId"),
        )
    }

    private fun extractDataObject(code: Int, body: String, fallback: String): JSONObject {
        if (code !in 200..299) throw IllegalStateException(parseError(body, "$fallback (HTTP $code)"))
        val json = JSONObject(body)
        return if (json.has("data") && !json.isNull("data")) json.getJSONObject("data") else json
    }

    private fun extractDataArray(code: Int, body: String, fallback: String): JSONArray {
        if (code !in 200..299) throw IllegalStateException(parseError(body, "$fallback (HTTP $code)"))
        val json = JSONObject(body)
        return if (json.has("data") && !json.isNull("data")) json.getJSONArray("data") else JSONArray()
    }

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
            token()?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Authorization", "Bearer $it") }
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
            throw IllegalStateException("Không kết nối được backend (${baseUrl()}): ${e.localizedMessage ?: e.message}")
        } finally {
            conn.disconnect()
        }
    }
}
