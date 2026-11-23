package com.example.easysport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.easysport.data.model.Booking
import com.example.easysport.data.model.PaymentStatus

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val courtId: String,
    val courtName: String,
    val fieldDetail: String,
    val timeRange: String,
    val dateLabel: String,
    val totalPrice: Int,
    val paymentStatus: String,
    val address: String
)

fun BookingEntity.toDomainModel(): Booking {
    return Booking(
        id = id,
        courtId = courtId,
        courtName = courtName,
        fieldDetail = fieldDetail,
        timeRange = timeRange,
        dateLabel = dateLabel,
        totalPrice = totalPrice,
        payment = runCatching { PaymentStatus.valueOf(paymentStatus) }.getOrDefault(PaymentStatus.PENDING),
        address = address
    )
}

fun Booking.toEntity(): BookingEntity {
    return BookingEntity(
        id = id,
        courtId = courtId,
        courtName = courtName,
        fieldDetail = fieldDetail,
        timeRange = timeRange,
        dateLabel = dateLabel,
        totalPrice = totalPrice,
        paymentStatus = payment.name,
        address = address
    )
}
