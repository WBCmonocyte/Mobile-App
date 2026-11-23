package com.example.easysport.data.remote

import android.net.Uri
import com.example.easysport.data.model.Booking
import com.example.easysport.data.model.BookingRequest
import com.example.easysport.data.model.Court
import com.example.easysport.data.model.PaymentMethod
import com.example.easysport.data.model.PaymentStatus
import com.example.easysport.data.model.User
import com.example.easysport.data.model.mergedTimeRange
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

data class ConflictSlotInfo(
    val slot: String,
    val status: String,
    val expiresAt: String?
)

class SlotConflictException(
    val conflictingSlots: List<ConflictSlotInfo>,
    val serverNow: String?
) : Exception("Khung giờ này đã được người khác chọn hoặc đang thanh toán. Vui lòng chọn khung giờ khác.")

data class StartCheckoutResult(
    val holdId: String,
    val bookingId: String,
    val paymentId: String,
    val orderInvoiceNumber: String,
    val checkoutUrl: String,
    val amount: Int,
    val totalPrice: Int,
    val depositAmount: Int,
    val remainingAmount: Int,
    val expiresAt: String,
    val serverNow: String,
    val paymentDeadline: String? = null,
    val verificationDeadline: String? = null,
    val holdStatus: String? = null,
)

data class CourtAvailabilitySlot(
    val slot: String,
    val status: String,
    val expiresAt: String?
)

data class CreateBookingResponse(
    val id: String,
    val courtId: String,
    val courtName: String,
    val fieldDetail: String,
    val timeRange: String,
    val dateLabel: String,
    val totalPrice: Int,
    val depositAmount: Int,
    val remainingAmount: Int,
    val paymentMethod: PaymentMethod,
    val status: PaymentStatus,
    val address: String,
    val holdExpiresAt: String?,
    val customerName: String,
    val phone: String,
)

data class InitiatePaymentResponse(
    val checkoutUrl: String,
    val checkoutToken: String,
    val orderInvoiceNumber: String,
    val amount: Int,
    val expiresAt: String?,
)

data class BookingStatusResponse(
    val bookingId: String,
    val bookingStatus: String,
    val totalPrice: Int,
    val depositAmount: Int,
    val remainingAmount: Int,
    val paymentMethod: String,
    val isPaid: Boolean,
    val isDepositPaid: Boolean,
    val isExpired: Boolean,
    val paymentId: String?,
    val orderInvoiceNumber: String?,
    val paymentStatus: String?,
    val paymentAmount: Int?,
    val checkoutUrl: String?,
    val expiresAt: String?,
    val paymentDeadline: String? = null,
    val verificationDeadline: String? = null,
    val serverNow: String? = null,
    val holdStatus: String? = null,
)

data class AuthResultData(
    val token: String,
    val user: User,
    val expiresAt: String? = null
)

object PaymentApiClient {

    /**
     * URL backend mặc định:
     * - Trên Android Emulator: tự động dùng http://10.0.2.2:8080
     * - Trên Thiết bị thật: nếu cấu hình là 10.0.2.2 thì tự động đổi sang http://127.0.0.1:8080 (hoạt động hoàn hảo với `adb reverse tcp:8080 tcp:8080`)
     * - Có thể tùy biến qua local.properties (api.base.url=...) hoặc gán runtime.
     */
    var baseUrl: String = resolveDefaultBaseUrl()

    private fun resolveDefaultBaseUrl(): String {
        val configured = com.example.easysport.BuildConfig.API_BASE_URL
        if (configured.contains("10.0.2.2") && !isProbablyEmulator()) {
            return configured.replace("10.0.2.2", "127.0.0.1")
        }
        return configured
    }

    private fun isProbablyEmulator(): Boolean {
        val fp = android.os.Build.FINGERPRINT ?: ""
        val model = android.os.Build.MODEL ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        val hardware = android.os.Build.HARDWARE ?: ""
        return fp.startsWith("generic") || fp.startsWith("unknown")
                || model.contains("google_sdk") || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || hardware.contains("goldfish") || hardware.contains("ranchu")
                || product.contains("sdk") || product.contains("emulator")
    }

    /**
     * Auth token của phiên đăng nhập hiện tại. Tự động gửi kèm header Authorization.
     */
    var authToken: String? = null

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000

    /**
     * Giải quyết chính xác URL thanh toán tương đối / tuyệt đối.
     * - URL tương đối (/payments/.../checkout) -> ghép an toàn với baseUrl.
     * - URL localhost/127.0.0.1 -> chuyển host/cổng sang host/cổng của baseUrl để hoạt động trên Android Emulator & Thiết bị thật.
     * - Kiểm tra scheme (http/https) và host hợp lệ trước khi mở.
     */
    fun resolveUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return ""
        val trimmed = rawUrl.trim()
        return try {
            val base = URL(baseUrl)
            val resolved = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val parsed = URL(trimmed)
                if (parsed.host == "localhost" || parsed.host == "127.0.0.1") {
                    URL(base.protocol, base.host, base.port, parsed.file).toString()
                } else {
                    trimmed
                }
            } else {
                URL(base, trimmed).toString()
            }
            val finalParsed = URL(resolved)
            if (finalParsed.protocol in listOf("http", "https") && finalParsed.host == base.host
                && finalParsed.port == base.port && finalParsed.userInfo == null
                && (com.example.easysport.BuildConfig.DEBUG || finalParsed.protocol == "https")) {
                resolved
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    // =========================================================================
    // API XÁC THỰC & HỒ SƠ NGƯỜI DÙNG (AUTHENTICATION & PROFILE)
    // =========================================================================

    suspend fun login(emailOrPhone: String, password: String): Result<AuthResultData> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/auth/login"
            val payload = JSONObject().apply {
                put("email", emailOrPhone.trim())
                put("password", password)
            }
            val (code, responseBody) = sendHttpRequest("POST", endpoint, payload.toString())
            if (code in 200..299) {
                val json = JSONObject(responseBody)
                val data = if (json.has("data")) json.getJSONObject("data") else json
                val token = data.getString("token")
                val userJson = data.getJSONObject("user")
                val user = parseUser(userJson)
                val expiresAt = data.optNullableString("expiresAt")
                authToken = token
                AuthResultData(token, user, expiresAt)
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Đăng nhập thất bại (HTTP $code)")
                throw IllegalStateException(errorMsg)
            }
        }
    }

    suspend fun register(
        fullName: String,
        dob: String,
        phone: String,
        email: String,
        address: String,
        password: String
    ): Result<AuthResultData> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/auth/register"
            val payload = JSONObject().apply {
                put("fullName", fullName.trim())
                put("dateOfBirth", dob.trim())
                put("phone", phone.trim())
                put("email", email.trim().lowercase())
                put("address", address.trim())
                put("password", password)
            }
            val (code, responseBody) = sendHttpRequest("POST", endpoint, payload.toString())
            if (code in 200..299) {
                val json = JSONObject(responseBody)
                val data = if (json.has("data")) json.getJSONObject("data") else json
                val token = data.getString("token")
                val userJson = data.getJSONObject("user")
                val user = parseUser(userJson)
                val expiresAt = data.optNullableString("expiresAt")
                authToken = token
                AuthResultData(token, user, expiresAt)
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Đăng ký thất bại (HTTP $code)")
                throw IllegalStateException(errorMsg)
            }
        }
    }

    suspend fun getProfile(): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/auth/me"
            val (code, responseBody) = sendHttpRequest("GET", endpoint, body = null)
            if (code in 200..299) {
                val json = JSONObject(responseBody)
                val data = if (json.has("data")) json.getJSONObject("data") else json
                parseUser(data)
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Không thể tải thông tin hồ sơ (HTTP $code)")
                throw IllegalStateException(errorMsg)
            }
        }
    }

    suspend fun updateProfile(
        fullName: String,
        phone: String,
        address: String,
        dob: String
    ): Result<User> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/auth/profile"
            val payload = JSONObject().apply {
                put("fullName", fullName.trim())
                put("phone", phone.trim())
                put("address", address.trim())
                put("dateOfBirth", dob.trim())
            }
            val (code, responseBody) = sendHttpRequest("PUT", endpoint, payload.toString())
            if (code in 200..299) {
                val json = JSONObject(responseBody)
                val data = if (json.has("data")) json.getJSONObject("data") else json
                parseUser(data)
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Không thể cập nhật hồ sơ (HTTP $code)")
                throw IllegalStateException(errorMsg)
            }
        }
    }

    suspend fun logout(token: String? = authToken): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/auth/logout"
            sendHttpRequest("POST", endpoint, body = null, token = token)
            Unit
        }
    }

    private fun parseUser(obj: JSONObject): User {
        return User(
            id = obj.getString("id"),
            email = obj.optString("email", ""),
            name = obj.optString("fullName", obj.optString("name", "")),
            phone = obj.optString("phone", ""),
            address = obj.optString("address", ""),
            dateOfBirth = obj.optString("dateOfBirth", "")
        )
    }

    // =========================================================================
    // API GIỮ SÂN, CHECKOUT & THANH TOÁN
    // =========================================================================

    suspend fun startCheckout(
        request: BookingRequest,
        court: Court,
        idempotencyKey: String,
        userId: String? = null
    ): Result<StartCheckoutResult> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/bookings/checkout"
            val payload = JSONObject().apply {
                put("courtId", request.courtId)
                put("fieldDetail", request.field.id.ifBlank { request.field.detail })
                put("dateLabel", request.dateLabel)
                put("paymentMethod", request.paymentMethod.name)
                put("customerName", request.customerName)
                put("customerPhone", request.phone)
                put("address", court.address)
                if (!userId.isNullOrBlank()) {
                    put("userId", userId)
                }
                put("idempotencyKey", idempotencyKey)

                val slotsArray = JSONArray()
                request.slots.forEach { slotsArray.put(it.label) }
                put("slots", slotsArray)
            }

            val (code, responseBody) = sendHttpRequest("POST", endpoint, payload.toString())

            if (code == 409) {
                val json = JSONObject(responseBody)
                val conflictingList = mutableListOf<ConflictSlotInfo>()
                if (json.has("conflictingSlots")) {
                    val arr = json.getJSONArray("conflictingSlots")
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        conflictingList.add(
                            ConflictSlotInfo(
                                slot = item.getString("slot"),
                                status = item.optString("status", "HELD"),
                                expiresAt = item.optNullableString("expiresAt")
                            )
                        )
                    }
                }
                val serverNow = json.optNullableString("serverNow")
                throw SlotConflictException(conflictingList, serverNow)
            }

            if (code in 200..299) {
                val json = JSONObject(responseBody)
                val data = if (json.has("data")) json.getJSONObject("data") else json
                val rawCheckoutUrl = data.getString("checkoutUrl")
                val safeCheckoutUrl = resolveUrl(rawCheckoutUrl)

                StartCheckoutResult(
                    holdId = data.getString("holdId"),
                    bookingId = data.getString("bookingId"),
                    paymentId = data.getString("paymentId"),
                    orderInvoiceNumber = data.getString("orderInvoiceNumber"),
                    checkoutUrl = safeCheckoutUrl,
                    amount = data.getInt("amount"),
                    totalPrice = data.optInt("totalPrice", 0),
                    depositAmount = data.optInt("depositAmount", 0),
                    remainingAmount = data.optInt("remainingAmount", 0),
                    expiresAt = data.optString("paymentDeadlineIso", data.optString("expiresAt", "")),
                    serverNow = data.optString("serverNowIso", data.optString("serverNow", "")),
                    paymentDeadline = data.optNullableString("paymentDeadlineIso") ?: data.optNullableString("paymentDeadline"),
                    verificationDeadline = data.optNullableString("verificationDeadlineIso") ?: data.optNullableString("verificationDeadline"),
                    holdStatus = data.optNullableString("holdStatus")
                )
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Không thể bắt đầu thanh toán (HTTP $code)")
                throw IllegalStateException(errorMsg)
            }
        }
    }

    suspend fun getCourtAvailability(
        courtId: String,
        fieldDetail: String,
        dateLabel: String
    ): Result<List<CourtAvailabilitySlot>> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedField = URLEncoder.encode(fieldDetail, "UTF-8")
            val encodedDate = URLEncoder.encode(dateLabel, "UTF-8")
            val endpoint = "$baseUrl/api/courts/$courtId/availability?fieldDetail=$encodedField&date=$encodedDate"

            val (code, responseBody) = sendHttpRequest("GET", endpoint, body = null)
            if (code in 200..299) {
                val json = JSONObject(responseBody)
                val data = if (json.has("data")) json.getJSONObject("data") else json
                val slotsArr = data.getJSONArray("slots")
                val list = mutableListOf<CourtAvailabilitySlot>()
                for (i in 0 until slotsArr.length()) {
                    val item = slotsArr.getJSONObject(i)
                    list.add(
                        CourtAvailabilitySlot(
                            slot = item.getString("slot"),
                            status = item.getString("status"),
                            expiresAt = item.optNullableString("expiresAt")
                        )
                    )
                }
                list
            } else {
                throw IllegalStateException(parseErrorMessage(responseBody, "Không thể tải lịch trống (HTTP $code)"))
            }
        }
    }

    suspend fun getPaymentStatus(
        bookingId: String,
    ): Result<BookingStatusResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/bookings/$bookingId/payment-status"
            val (code, responseBody) = sendHttpRequest("GET", endpoint, body = null)
            if (code in 200..299) {
                val root = JSONObject(responseBody)
                val json = if (root.has("data")) root.getJSONObject("data") else root
                val rawCheckoutUrl = json.optNullableString("checkoutUrl")?.let { resolveUrl(it) }

                BookingStatusResponse(
                    bookingId = json.getString("bookingId"),
                    bookingStatus = json.optString("bookingStatus", "PENDING"),
                    totalPrice = json.optInt("totalPrice", 0),
                    depositAmount = json.optInt("depositAmount", 0),
                    remainingAmount = json.optInt("remainingAmount", 0),
                    paymentMethod = json.optString("paymentMethod", "QR_TRANSFER"),
                    isPaid = json.optBoolean("paid", false),
                    isDepositPaid = json.optBoolean("depositPaid", false),
                    isExpired = json.optBoolean("expired", false),
                    paymentId = json.optNullableString("paymentId"),
                    orderInvoiceNumber = json.optNullableString("orderInvoiceNumber"),
                    paymentStatus = json.optNullableString("paymentStatus"),
                    paymentAmount = if (json.has("paymentAmount") && !json.isNull("paymentAmount")) json.getInt("paymentAmount") else null,
                    checkoutUrl = rawCheckoutUrl,
                    expiresAt = json.optNullableString("paymentDeadlineIso") ?: json.optNullableString("expiresAt"),
                    paymentDeadline = json.optNullableString("paymentDeadlineIso") ?: json.optNullableString("paymentDeadline"),
                    verificationDeadline = json.optNullableString("verificationDeadlineIso") ?: json.optNullableString("verificationDeadline"),
                    serverNow = json.optNullableString("serverNowIso") ?: json.optNullableString("serverNow"),
                    holdStatus = json.optNullableString("holdStatus")
                )
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Không thể kiểm tra trạng thái thanh toán (HTTP $code)")
                throw IllegalStateException(errorMsg)
            }
        }
    }

    // =========================================================================
    // API LỊCH ĐẶT SÂN (BOOKINGS)
    // =========================================================================

    suspend fun getBookings(): Result<List<Booking>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/bookings"
            val (code, responseBody) = sendHttpRequest("GET", endpoint, body = null)
            if (code in 200..299) {
                val root = JSONObject(responseBody)
                val dataArr = root.optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<Booking>()
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.getJSONObject(i)
                    val methodStr = obj.optString("paymentMethod", "QR_TRANSFER")
                    val statusStr = obj.optString("status", obj.optString("payment", "PENDING"))
                    val pMethod = runCatching { PaymentMethod.valueOf(methodStr) }.getOrDefault(PaymentMethod.QR_TRANSFER)
                    val pStatus = PaymentStatus.fromString(statusStr)

                    list.add(
                        Booking(
                            id = obj.getString("id"),
                            courtId = obj.getString("courtId"),
                            courtName = obj.optString("courtName", "Sân thể thao"),
                            fieldDetail = obj.optString("fieldDetail", "Sân tiêu chuẩn"),
                            timeRange = obj.optString("timeRange", ""),
                            dateLabel = obj.optString("dateLabel", obj.optString("bookingDate", "")),
                            totalPrice = obj.optInt("totalPrice", 0),
                            payment = pStatus,
                            address = obj.optString("address", ""),
                            depositAmount = obj.optInt("depositAmount", 0),
                            remainingAmount = obj.optInt("remainingAmount", 0),
                            paymentMethod = pMethod
                        )
                    )
                }
                list
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Không thể tải danh sách lịch đặt (HTTP $code)")
                throw IllegalStateException(errorMsg)
            }
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$baseUrl/api/bookings/$bookingId"
            val (code, responseBody) = sendHttpRequest("DELETE", endpoint, body = null)
            if (code !in 200..299) {
                val errorMsg = parseErrorMessage(responseBody, "Không thể hủy lịch đặt (HTTP $code)")
                throw IllegalStateException(errorMsg)
            }
        }
    }

    fun toDomainBooking(
        bookingId: String,
        court: Court,
        statusResponse: BookingStatusResponse,
        request: BookingRequest,
    ): Booking {
        val paymentStatus = when {
            statusResponse.isPaid -> PaymentStatus.PAID
            statusResponse.isDepositPaid -> PaymentStatus.DEPOSIT_PAID
            statusResponse.isExpired -> PaymentStatus.EXPIRED
            else -> PaymentStatus.fromString(statusResponse.bookingStatus)
        }

        return Booking(
            id = bookingId,
            courtId = court.id,
            courtName = court.name,
            fieldDetail = request.field.detail,
            timeRange = request.slots.mergedTimeRange(),
            dateLabel = request.dateLabel,
            totalPrice = if (statusResponse.totalPrice > 0) statusResponse.totalPrice else request.total,
            payment = paymentStatus,
            address = court.address,
            depositAmount = statusResponse.depositAmount,
            remainingAmount = statusResponse.remainingAmount,
            paymentMethod = runCatching { PaymentMethod.valueOf(statusResponse.paymentMethod) }.getOrDefault(request.paymentMethod)
        )
    }

    // =========================================================================
    // API DANH SÁCH SÂN (COURTS) TỪ MYSQL
    // =========================================================================

    suspend fun getCourts(): List<Court> = withContext(Dispatchers.IO) {
        val (code, respText) = sendHttpRequest(
            method = "GET",
            urlStr = "$baseUrl/api/courts",
            body = null
        )
        if (code !in 200..299) {
            throw IllegalStateException(parseErrorMessage(respText, "Không thể tải danh sách sân từ máy chủ."))
        }

        val json = JSONObject(respText)
        val dataArray = json.optJSONArray("data") ?: JSONArray()
        val result = mutableListOf<Court>()

        for (i in 0 until dataArray.length()) {
            val obj = dataArray.getJSONObject(i)
            val id = obj.getString("id")
            val name = obj.getString("name")
            val address = obj.getString("address")
            val rating = obj.optDouble("rating", 5.0)
            val reviewCount = obj.optInt("reviewCount", 0)
            val distanceKm = obj.optDouble("distanceKm", 1.0)
            val pricePerHour = obj.optInt("pricePerHour", 300000)
            val morningPrice = obj.optInt("morningPrice", 150000)
            val afternoonPrice = obj.optInt("afternoonPrice", 250000)
            val eveningPrice = obj.optInt("eveningPrice", 300000)
            val imageResName = obj.optString("imageResName", "court_thanh_phat")
            val sportType = obj.optString("sportType", "Bóng đá")
            val indoor = obj.optBoolean("indoor", false)

            val lat = if (obj.has("latitude") && !obj.isNull("latitude")) obj.getDouble("latitude") else null
            val lng = if (obj.has("longitude") && !obj.isNull("longitude")) obj.getDouble("longitude") else null

            val imageRes = when (imageResName) {
                "court_hoang_gia" -> com.example.easysport.R.drawable.court_hoang_gia
                "court_chao_lua" -> com.example.easysport.R.drawable.court_chao_lua
                "court_badminton", "court_cau_long", "court_pickleball" -> com.example.easysport.R.drawable.court_badminton
                else -> com.example.easysport.R.drawable.court_thanh_phat
            }

            val fields = mutableListOf<com.example.easysport.data.model.CourtField>()
            val fieldsArray = obj.optJSONArray("fields")
            if (fieldsArray != null) {
                for (j in 0 until fieldsArray.length()) {
                    val fObj = fieldsArray.getJSONObject(j)
                    if (!fObj.optBoolean("active", true)) continue
                    fields.add(
                        com.example.easysport.data.model.CourtField(
                            label = fObj.getString("label"),
                            detail = fObj.getString("detail"),
                            id = fObj.getString("id")
                        )
                    )
                }
            }

            result.add(
                Court(
                    id = id,
                    name = name,
                    address = address,
                    rating = rating,
                    reviewCount = reviewCount,
                    distanceKm = distanceKm,
                    pricePerHour = pricePerHour,
                    sessionPrices = mapOf(
                        com.example.easysport.data.model.Session.MORNING to morningPrice,
                        com.example.easysport.data.model.Session.AFTERNOON to afternoonPrice,
                        com.example.easysport.data.model.Session.EVENING to eveningPrice
                    ),
                    amenities = listOf(
                        com.example.easysport.data.model.Amenity.LIGHT,
                        com.example.easysport.data.model.Amenity.PARKING
                    ),
                    imageRes = imageRes,
                    fields = fields,
                    sportType = sportType,
                    indoor = indoor,
                    latitude = lat,
                    longitude = lng
                )
            )
        }
        result
    }

    private fun sendHttpRequest(
        method: String,
        urlStr: String,
        body: String?,
        token: String? = authToken,
    ): Pair<Int, String> {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            instanceFollowRedirects = false
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }
        }

        try {
            if (body != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val statusCode = conn.responseCode
            val inputStream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }
            return Pair(statusCode, responseText)
        } catch (e: Exception) {
            throw IllegalStateException("Không thể kết nối đến máy chủ backend ($baseUrl). Vui lòng đảm bảo backend đang hoạt động. Chi tiết: ${e.localizedMessage ?: e.message}")
        } finally {
            conn.disconnect()
        }
    }

    private fun parseErrorMessage(jsonStr: String, fallback: String): String {
        if (jsonStr.isBlank()) return fallback
        return try {
            val json = JSONObject(jsonStr)
            when {
                json.has("message") -> json.getString("message")
                json.has("error") -> json.getString("error")
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (has(key) && !isNull(key)) {
            val str = getString(key).trim()
            if (str.isEmpty() || str == "null") null else str
        } else {
            null
        }
    }
}
