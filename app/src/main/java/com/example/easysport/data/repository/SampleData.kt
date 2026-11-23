package com.example.easysport.data.repository

import com.example.easysport.R
import com.example.easysport.data.model.Amenity
import com.example.easysport.data.model.Booking
import com.example.easysport.data.model.Court
import com.example.easysport.data.model.CourtField
import com.example.easysport.data.model.PaymentStatus
import com.example.easysport.data.model.Session
import com.example.easysport.data.model.TimeSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Dữ liệu mẫu để dựng UI (phục vụ Compose Preview). */
object SampleData {

    val fields = listOf(
        CourtField("Sân 1", "Sân số 1 (Sân 5 người)"),
        CourtField("Sân 2", "Sân số 2 (Sân 5 người)"),
        CourtField("Sân 3 (Sân 7)", "Sân số 3 (Sân 7 người)"),
        CourtField("Sân 4", "Sân số 4 (Sân 5 người)"),
    )

    val timeSlots = listOf(
        TimeSlot("07:00 - 08:30", Session.MORNING, booked = true),
        TimeSlot("08:30 - 10:00", Session.MORNING),
        TimeSlot("10:00 - 11:30", Session.MORNING),
        TimeSlot("13:00 - 14:30", Session.AFTERNOON),
        TimeSlot("14:30 - 16:00", Session.AFTERNOON),
        TimeSlot("16:00 - 17:30", Session.AFTERNOON),
        TimeSlot("19:00 - 20:30", Session.EVENING),
        TimeSlot("20:30 - 22:00", Session.EVENING, booked = true),
    )

    val courts = listOf(
        Court(
            id = "cau-long-thanh-phat",
            name = "Sân cầu lông Thành Phát",
            address = "Phường 12, Tân Bình, TP. Hồ Chí Minh",
            rating = 4.8, reviewCount = 128, distanceKm = 1.2, pricePerHour = 120_000,
            sessionPrices = mapOf(Session.MORNING to 80_000, Session.AFTERNOON to 100_000, Session.EVENING to 120_000),
            amenities = listOf(Amenity.LIGHT, Amenity.PARKING, Amenity.DRINKS),
            imageRes = R.drawable.court_badminton,
            fields = listOf(
                CourtField("Sân 1", "Sân cầu lông số 1 (Trong nhà)"),
                CourtField("Sân 2", "Sân cầu lông số 2 (Trong nhà)"),
                CourtField("Sân 3", "Sân cầu lông số 3 (Trong nhà)"),
                CourtField("Sân 4", "Sân cầu lông số 4 (Trong nhà)"),
            ),
            sportType = "Cầu lông",
            indoor = true,
            latitude = 10.8002,
            longitude = 106.6558,
        ),
        Court(
            id = "thanh-phat",
            name = "Sân bóng Thành Phát",
            address = "123 Cộng Hòa, P.12, Tân Bình, TP. HCM",
            rating = 4.8, reviewCount = 128, distanceKm = 1.2, pricePerHour = 300_000,
            sessionPrices = mapOf(Session.MORNING to 150_000, Session.AFTERNOON to 250_000, Session.EVENING to 300_000),
            amenities = listOf(Amenity.LIGHT, Amenity.PARKING, Amenity.WIFI),
            imageRes = R.drawable.court_thanh_phat,
            fields = fields,
            sportType = "Bóng đá",
            indoor = false,
            latitude = 10.8016,
            longitude = 106.6508,
        ),
        Court(
            id = "hoang-gia",
            name = "Sân bóng Hoàng Gia",
            address = "18D Phan Văn Trị, P.10, Gò Vấp, TP. HCM",
            rating = 4.7, reviewCount = 96, distanceKm = 2.5, pricePerHour = 350_000,
            sessionPrices = mapOf(Session.MORNING to 170_000, Session.AFTERNOON to 280_000, Session.EVENING to 350_000),
            amenities = listOf(Amenity.LIGHT, Amenity.DRINKS),
            imageRes = R.drawable.court_hoang_gia,
            fields = fields,
            sportType = "Bóng đá",
            indoor = false,
            latitude = 10.8322,
            longitude = 106.6715,
        ),
        Court(
            id = "chao-lua",
            name = "Sân bóng Chảo Lửa",
            address = "30 Phan Thúc Duyện, P.4, Tân Bình, TP. HCM",
            rating = 4.9, reviewCount = 214, distanceKm = 3.1, pricePerHour = 400_000,
            sessionPrices = mapOf(Session.MORNING to 200_000, Session.AFTERNOON to 300_000, Session.EVENING to 400_000),
            amenities = listOf(Amenity.LIGHT, Amenity.PARKING),
            imageRes = R.drawable.court_chao_lua,
            fields = fields,
            sportType = "Bóng đá",
            indoor = false,
            latitude = 10.8065,
            longitude = 106.6617,
        ),
        Court(
            id = "pickleball-tan-binh",
            name = "Sân Pickleball Tân Bình",
            address = "45 Hoàng Hoa Thám, P.12, Tân Bình, TP. HCM",
            rating = 4.9, reviewCount = 85, distanceKm = 1.5, pricePerHour = 150_000,
            sessionPrices = mapOf(Session.MORNING to 100_000, Session.AFTERNOON to 130_000, Session.EVENING to 150_000),
            amenities = listOf(Amenity.LIGHT, Amenity.PARKING, Amenity.WIFI),
            imageRes = R.drawable.court_badminton,
            fields = listOf(
                CourtField("Sân PB 1", "Sân Pickleball số 1 (Tiêu chuẩn)"),
                CourtField("Sân PB 2", "Sân Pickleball số 2 (Tiêu chuẩn)"),
            ),
            sportType = "Pickleball",
            indoor = true,
            latitude = 10.8051,
            longitude = 106.6582,
        ),
    )

    fun todayLabel(): String =
        "Hôm nay, " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    val upcomingBooking: Booking
        get() = Booking(
            id = "b1",
            courtId = "thanh-phat",
            courtName = "Sân bóng Thành Phát",
            fieldDetail = "Sân số 3 (Sân 7 người)",
            timeRange = "18:00 - 19:30",
            dateLabel = todayLabel(),
            totalPrice = 450_000,
            payment = PaymentStatus.PAID,
            address = "123 Cộng Hòa, Tân Bình, TP. Hồ Chí Minh",
        )
}
