package com.example.easysport

import com.example.easysport.data.model.Court
import com.example.easysport.util.CourtDistanceConstants
import com.example.easysport.util.CourtDistanceUtils
import org.junit.Assert.*
import org.junit.Test

class CourtDistanceUtilsTest {

    private fun createTestCourt(
        id: String,
        name: String,
        sportType: String,
        lat: Double?,
        lng: Double?,
        rating: Double = 4.8
    ): Court {
        return Court(
            id = id,
            name = name,
            address = "Test Address",
            rating = rating,
            reviewCount = 50,
            distanceKm = 0.0,
            pricePerHour = 100_000,
            sessionPrices = emptyMap(),
            amenities = emptyList(),
            imageRes = 0,
            fields = emptyList(),
            sportType = sportType,
            indoor = false,
            latitude = lat,
            longitude = lng
        )
    }

    @Test
    fun testHaversineDistanceCalculationAccuracy() {
        // Tọa độ Sân bóng Thành Phát (Tân Bình): 10.8016, 106.6508
        // Tọa độ Sân bóng Hoàng Gia (Gò Vấp): 10.8322, 106.6715
        val distance = CourtDistanceUtils.calculateHaversineKm(
            10.8016, 106.6508,
            10.8322, 106.6715
        )
        // Khoảng cách thực tế giữa 2 điểm này khoảng 4.0 - 4.2 km
        assertTrue("Distance should be around 4.1 km", distance in 3.9..4.3)
    }

    @Test
    fun testSameCoordinatesReturnZeroDistance() {
        val distance = CourtDistanceUtils.calculateHaversineKm(10.8016, 106.6508, 10.8016, 106.6508)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun testInvalidCoordinatesValidation() {
        assertFalse(CourtDistanceUtils.isValidCoordinate(null, 106.65))
        assertFalse(CourtDistanceUtils.isValidCoordinate(10.80, null))
        assertFalse(CourtDistanceUtils.isValidCoordinate(null, null))
        assertFalse(CourtDistanceUtils.isValidCoordinate(0.0, 0.0))
        assertFalse(CourtDistanceUtils.isValidCoordinate(95.0, 106.65))
        assertFalse(CourtDistanceUtils.isValidCoordinate(-91.0, 106.65))
        assertFalse(CourtDistanceUtils.isValidCoordinate(10.80, 185.0))
        assertTrue(CourtDistanceUtils.isValidCoordinate(10.8016, 106.6508))
    }

    @Test
    fun testDistinctCourtCounting() {
        val courtsWithDuplicates = listOf(
            createTestCourt("c1", "Sân A", "Bóng đá", 10.80, 106.65),
            createTestCourt("c1", "Sân A Duplicate", "Bóng đá", 10.80, 106.65),
            createTestCourt("c2", "Sân B", "Bóng đá", 10.81, 106.66),
        )

        val result = CourtDistanceUtils.filterAndSortCourts(
            courts = courtsWithDuplicates,
            userLat = 10.80,
            userLng = 106.65,
            selectedSport = "Tất cả"
        )
        assertEquals(2, result.size)
        assertEquals(listOf("c1", "c2"), result.map { it.court.id })
    }

    @Test
    fun testRadiusFiltering() {
        // Vị trí người dùng: 10.8000, 106.6500
        val nearCourt = createTestCourt("near", "Sân Gần (1 km)", "Bóng đá", 10.8050, 106.6550) // ~0.76 km
        val farCourt = createTestCourt("far", "Sân Xa (15 km)", "Bóng đá", 10.9300, 106.6500) // ~14.4 km
        val noCoordCourt = createTestCourt("no_coord", "Sân Chưa Tọa Độ", "Bóng đá", null, null)

        val courts = listOf(nearCourt, farCourt, noCoordCourt)

        // Khi bật lọc bán kính 5 km
        val filtered = CourtDistanceUtils.filterAndSortCourts(
            courts = courts,
            userLat = 10.8000,
            userLng = 106.6500,
            selectedSport = "Bóng đá",
            radiusKm = CourtDistanceConstants.DEFAULT_SEARCH_RADIUS_KM,
            applyRadiusFilter = true
        )

        assertEquals(1, filtered.size)
        assertEquals("near", filtered.first().court.id)
        assertTrue(filtered.first().distanceKm!! <= 5.0)
    }

    @Test
    fun testSportAndKeywordFiltering() {
        val footballCourt = createTestCourt("f1", "Sân bóng Chảo Lửa", "Bóng đá", 10.8065, 106.6617)
        val badmintonCourt = createTestCourt("b1", "Sân cầu lông Thành Phát", "Cầu lông", 10.8002, 106.6558)
        val pickleballCourt = createTestCourt("p1", "Sân Pickleball Tân Bình", "Pickleball", 10.8051, 106.6582)

        val courts = listOf(footballCourt, badmintonCourt, pickleballCourt)

        // 1. Lọc theo Cầu lông
        val badmintonResult = CourtDistanceUtils.filterAndSortCourts(
            courts = courts,
            userLat = null,
            userLng = null,
            selectedSport = "Cầu lông"
        )
        assertEquals(1, badmintonResult.size)
        assertEquals("b1", badmintonResult.first().court.id)

        // 2. Lọc theo từ khóa "chảo lửa"
        val queryResult = CourtDistanceUtils.filterAndSortCourts(
            courts = courts,
            userLat = null,
            userLng = null,
            selectedSport = "Tất cả",
            searchQuery = "Chảo Lửa"
        )
        assertEquals(1, queryResult.size)
        assertEquals("f1", queryResult.first().court.id)

        // 3. Không tìm thấy kết quả phù hợp (empty state)
        val emptyResult = CourtDistanceUtils.filterAndSortCourts(
            courts = courts,
            userLat = null,
            userLng = null,
            selectedSport = "Bóng đá",
            searchQuery = "Không Tồn Tại"
        )
        assertTrue(emptyResult.isEmpty())
    }

    @Test
    fun testSortingFromNearestToFurthest() {
        val userLat = 10.8000
        val userLng = 106.6500

        val court1km = createTestCourt("c1", "Sân 1km", "Bóng đá", 10.8080, 106.6500) // ~0.89 km
        val court3km = createTestCourt("c3", "Sân 3km", "Bóng đá", 10.8250, 106.6500) // ~2.78 km
        val court500m = createTestCourt("c05", "Sân 500m", "Bóng đá", 10.8040, 106.6500) // ~0.44 km

        val courts = listOf(court1km, court3km, court500m)

        val sorted = CourtDistanceUtils.filterAndSortCourts(
            courts = courts,
            userLat = userLat,
            userLng = userLng,
            selectedSport = "Tất cả"
        )

        assertEquals(3, sorted.size)
        assertEquals("c05", sorted[0].court.id)
        assertEquals("c1", sorted[1].court.id)
        assertEquals("c3", sorted[2].court.id)
    }
}

