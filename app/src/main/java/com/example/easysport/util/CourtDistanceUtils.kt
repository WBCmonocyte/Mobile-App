package com.example.easysport.util

import com.example.easysport.data.model.Court
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hằng số cấu hình vị trí và khoảng cách thống nhất cho toàn bộ EasySport.
 */
object CourtDistanceConstants {
    /** Bán kính tìm sân mặc định xung quanh người dùng (tính theo km). */
    const val DEFAULT_SEARCH_RADIUS_KM: Double = 5.0

    /** Thông điệp ghi chú rõ ràng loại khoảng cách hiển thị trên giao diện người dùng. */
    const val DISTANCE_ESTIMATE_NOTE: String = "Khoảng cách ước tính (đường chim bay)"
}

data class CourtWithDistance(
    val court: Court,
    val distanceKm: Double?,
) {
    val formattedDistance: String
        get() = if (distanceKm != null) {
            String.format(Locale.US, "%.1f km", distanceKm)
        } else {
            "Chưa xác định"
        }

    val hasCoordinates: Boolean
        get() = CourtDistanceUtils.isValidCoordinate(court.latitude, court.longitude)
}

object CourtDistanceUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Kiểm tra tính hợp lệ của tọa độ địa lý.
     * Loại bỏ tọa độ null, vượt ngưỡng [-90, 90] / [-180, 180] hoặc tọa độ rỗng (0.0, 0.0).
     */
    fun isValidCoordinate(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    /**
     * Tính khoảng cách địa lý đường chim bay theo công thức Haversine (đơn vị: km).
     * @return khoảng cách tính theo km giữa 2 điểm tọa độ.
     */
    fun calculateHaversineKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
        val bounded = a.coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(bounded), sqrt(1 - bounded))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Lọc và sắp xếp danh sách sân thể thao thống nhất cho các màn hình:
     * - Lọc theo môn thể thao ("Tất cả", "Bóng đá", "Cầu lông", "Pickleball").
     * - Lọc theo từ khóa tìm kiếm (tên sân hoặc địa chỉ).
     * - Tính khoảng cách thực nếu có vị trí người dùng và sân có tọa độ hợp lệ.
     * - Lọc theo bán kính (nếu applyRadiusFilter = true).
     * - Đếm theo ID sân duy nhất (distinctBy id).
     * - Sắp xếp theo khoảng cách từ gần đến xa khi có vị trí người dùng.
     */
    fun filterAndSortCourts(
        courts: List<Court>,
        userLat: Double?,
        userLng: Double?,
        selectedSport: String = "Tất cả",
        searchQuery: String = "",
        radiusKm: Double? = CourtDistanceConstants.DEFAULT_SEARCH_RADIUS_KM,
        applyRadiusFilter: Boolean = false,
    ): List<CourtWithDistance> {
        val hasUserLocation = isValidCoordinate(userLat, userLng)
        val cleanQuery = searchQuery.trim().lowercase(Locale.getDefault())

        // 1. Loại bỏ trùng ID sân (đảm bảo mỗi cụm sân chỉ tính 1 lần)
        val uniqueCourts = courts.distinctBy { it.id }

        // 2. Lọc theo môn thể thao & từ khóa tìm kiếm
        val sportAndTextFiltered = uniqueCourts.filter { court ->
            val matchesSport = if (selectedSport == "Tất cả" || selectedSport.isBlank()) {
                true
            } else {
                court.sportType.equals(selectedSport, ignoreCase = true)
            }

            val matchesQuery = if (cleanQuery.isBlank()) {
                true
            } else {
                court.name.lowercase(Locale.getDefault()).contains(cleanQuery) ||
                        court.address.lowercase(Locale.getDefault()).contains(cleanQuery)
            }

            matchesSport && matchesQuery
        }

        // 3. Tính khoảng cách địa lý
        val withDistanceList = sportAndTextFiltered.map { court ->
            val distance = if (hasUserLocation && isValidCoordinate(court.latitude, court.longitude)) {
                calculateHaversineKm(userLat!!, userLng!!, court.latitude!!, court.longitude!!)
            } else {
                null
            }
            CourtWithDistance(court = court, distanceKm = distance)
        }

        // 4. Lọc theo bán kính (nếu được yêu cầu và có vị trí người dùng)
        val radiusFiltered = if (applyRadiusFilter && hasUserLocation && radiusKm != null) {
            withDistanceList.filter { item ->
                item.distanceKm != null && item.distanceKm <= radiusKm
            }
        } else {
            withDistanceList
        }

        // 5. Sắp xếp: gần nhất lên đầu nếu có khoảng cách, sau đó theo đánh giá rating giảm dần
        return radiusFiltered.sortedWith(
            compareBy<CourtWithDistance> { it.distanceKm ?: Double.MAX_VALUE }
                .thenByDescending { it.court.rating }
        )
    }
}

