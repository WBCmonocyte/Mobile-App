package com.example.easysport.data.model

import androidx.annotation.DrawableRes

enum class Amenity(val label: String) {
    LIGHT("Đèn"),
    PARKING("Bãi xe"),
    WIFI("Wifi"),
    DRINKS("Nước uống"),
}

/** Ca thuê trong ngày. Mỗi ca nhỏ dài 1 giờ 30 phút. */
enum class Session(val title: String, val range: String) {
    MORNING("Sáng", "07:00–11:30"),
    AFTERNOON("Chiều", "13:00–17:30"),
    EVENING("Tối", "19:00–22:00"),
}

/** label: hiện trên chip chọn sân; detail: hiện ở màn chi tiết lịch đặt. */
data class CourtField(val label: String, val detail: String, val id: String = "") : java.io.Serializable

data class Court(
    val id: String,
    val name: String,
    val address: String,
    val rating: Double,
    val reviewCount: Int,
    val distanceKm: Double,
    val pricePerHour: Int,
    /** Giá mỗi ca (1,5 giờ) theo buổi. */
    val sessionPrices: Map<Session, Int>,
    val amenities: List<Amenity>,
    @field:DrawableRes val imageRes: Int,
    val fields: List<CourtField>,
    val sportType: String = "Bóng đá",
    val indoor: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    val district: String? = null,
)

enum class SlotAvailabilityStatus {
    AVAILABLE,
    HELD,
    BOOKED
}

data class TimeSlot(
    val label: String,            // "18:00 - 19:30"
    val session: Session,
    val status: SlotAvailabilityStatus = SlotAvailabilityStatus.AVAILABLE,
    val holdExpiresAt: String? = null,
) : java.io.Serializable {
    val booked: Boolean get() = status == SlotAvailabilityStatus.BOOKED
    val isHeld: Boolean get() = status == SlotAvailabilityStatus.HELD
    val isAvailable: Boolean get() = status == SlotAvailabilityStatus.AVAILABLE
    val start: String get() = label.substringBefore(" - ")
    val end: String get() = label.substringAfter(" - ")

    // Constructor tương thích ngược cho code cũ dùng booked: Boolean
    constructor(label: String, session: Session, booked: Boolean) : this(
        label = label,
        session = session,
        status = if (booked) SlotAvailabilityStatus.BOOKED else SlotAvailabilityStatus.AVAILABLE,
        holdExpiresAt = null
    )
}

/** Gộp các ca liền nhau: 18:00-19:30 + 19:30-21:00 -> "18:00 - 21:00". */
fun List<TimeSlot>.mergedTimeRange(): String {
    val parts = mutableListOf<Pair<String, String>>()
    for (slot in sortedBy { it.start }) {
        val last = parts.lastOrNull()
        if (last != null && last.second == slot.start) {
            parts[parts.lastIndex] = last.first to slot.end
        } else {
            parts.add(slot.start to slot.end)
        }
    }
    return parts.joinToString(", ") { "${it.first} - ${it.second}" }
}

enum class PaymentMethod { QR_TRANSFER, CASH_ON_SITE, WALLET, CREDIT }

enum class PaymentStatus(val label: String) {
    PENDING("Chờ thanh toán"),
    DEPOSIT_PAID("Đã đặt cọc 30%"),
    PAID("Đã thanh toán đủ"),
    FAILED("Thanh toán thất bại"),
    EXPIRED("Đã hết hạn"),
    CANCELLED("Đã hủy");

    companion object {
        fun fromString(value: String?): PaymentStatus {
            return when (value?.trim()?.uppercase()) {
                "PAID" -> PAID
                "DEPOSIT_PAID" -> DEPOSIT_PAID
                "PENDING" -> PENDING
                "FAILED" -> FAILED
                "EXPIRED" -> EXPIRED
                "CANCELLED" -> CANCELLED
                else -> PENDING
            }
        }
    }
}

/** Dữ liệu người dùng chọn ở màn "Chi tiết & Đặt sân". */
data class BookingRequest(
    val courtId: String,
    val field: CourtField,
    val slots: List<TimeSlot>,
    val dateLabel: String,
    val customerName: String,
    val phone: String,
    val paymentMethod: PaymentMethod,
    val total: Int,
    val deposit: Int,
) : java.io.Serializable

data class Booking(
    val id: String,
    val courtId: String,
    val courtName: String,
    val fieldDetail: String,
    val timeRange: String,
    val dateLabel: String,
    val totalPrice: Int,
    val payment: PaymentStatus,
    val address: String,
    val depositAmount: Int = 0,
    val remainingAmount: Int = 0,
    val paymentMethod: PaymentMethod = PaymentMethod.QR_TRANSFER,
    val recurringPackageId: String? = null,
    val recurringSessionId: String? = null,
)
