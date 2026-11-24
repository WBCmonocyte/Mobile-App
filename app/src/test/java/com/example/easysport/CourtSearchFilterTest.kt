package com.example.easysport

import com.example.easysport.data.model.Court
import com.example.easysport.data.repository.SampleData
import com.example.easysport.ui.booking.CourtFilterCriteria
import com.example.easysport.ui.booking.normalizeVietnamese
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourtSearchFilterTest {

    private val testCourts = SampleData.courts

    private fun filterCourts(
        query: String = "",
        criteria: CourtFilterCriteria = CourtFilterCriteria()
    ): List<Court> {
        val queryNorm = normalizeVietnamese(query)
        return testCourts.filter { court ->
            val matchesQuery = if (queryNorm.isEmpty()) {
                true
            } else {
                val nameNorm = normalizeVietnamese(court.name)
                val addrNorm = normalizeVietnamese(court.address)
                val distNorm = normalizeVietnamese(court.district ?: "")
                val cityNorm = normalizeVietnamese(court.city ?: "")
                nameNorm.contains(queryNorm) || addrNorm.contains(queryNorm) ||
                        distNorm.contains(queryNorm) || cityNorm.contains(queryNorm)
            }

            val matchesCity = criteria.city.isNullOrBlank() ||
                    court.city.equals(criteria.city, ignoreCase = true)

            val matchesDistrict = criteria.district.isNullOrBlank() ||
                    court.district.equals(criteria.district, ignoreCase = true)

            val matchesSport = criteria.sportType.isNullOrBlank() ||
                    court.sportType.equals(criteria.sportType, ignoreCase = true)

            val matchesMinPrice = criteria.minPrice == null || court.pricePerHour >= criteria.minPrice!!
            val matchesMaxPrice = criteria.maxPrice == null || court.pricePerHour <= criteria.maxPrice!!

            matchesQuery && matchesCity && matchesDistrict && matchesSport && matchesMinPrice && matchesMaxPrice
        }
    }

    @Test
    fun testHanoiDongDaFootballHoangCau() {
        // Hà Nội -> Đống Đa -> Bóng đá -> 500.000 đến 500.000 đ/giờ ra Sân bóng đá Hoàng Cầu
        val criteria = CourtFilterCriteria(
            city = "Hà Nội",
            district = "Đống Đa",
            sportType = "Bóng đá",
            minPrice = 500_000,
            maxPrice = 500_000,
        )
        val result = filterCourts(criteria = criteria)
        assertEquals(1, result.size)
        assertEquals("hanoi_fb_hoang_cau", result[0].id)
        assertEquals("Sân bóng đá Hoàng Cầu", result[0].name)

        // Thử tìm kiếm không dấu "Hoang Cau" kết hợp cùng bộ lọc
        val resultWithQuery = filterCourts(query = "Hoang Cau", criteria = criteria)
        assertEquals(1, resultWithQuery.size)
        assertEquals("hanoi_fb_hoang_cau", resultWithQuery[0].id)

        // Thử tìm kiếm "hoang cau" không bộ lọc cũng ra đúng
        val resultOnlyQuery = filterCourts(query = "hoang cau")
        assertTrue(resultOnlyQuery.any { it.id == "hanoi_fb_hoang_cau" })
    }

    @Test
    fun testTpHcmTanBinhBadmintonPriceBoundary() {
        // TP. Hồ Chí Minh -> Tân Bình -> Cầu lông -> tối đa 120.000 đ/giờ ra CLB Cầu Lông Tân Bình
        val criteria120k = CourtFilterCriteria(
            city = "TP. Hồ Chí Minh",
            district = "Tân Bình",
            sportType = "Cầu lông",
            maxPrice = 120_000,
        )
        val result120k = filterCourts(criteria = criteria120k)
        assertEquals(1, result120k.size)
        assertEquals("CLB Cầu Lông Tân Bình", result120k[0].name)

        // 119.999 đ/giờ -> biến mất (không thỏa mãn biên)
        val criteria119k = CourtFilterCriteria(
            city = "TP. Hồ Chí Minh",
            district = "Tân Bình",
            sportType = "Cầu lông",
            maxPrice = 119_999,
        )
        val result119k = filterCourts(criteria = criteria119k)
        assertEquals(0, result119k.size)
    }

    @Test
    fun testDistrictResetWhenCityChanges() {
        // Giả lập logic khi đổi TP. HCM sang Hà Nội sau khi chọn Tân Bình -> Tân Bình bị bỏ
        var selectedCity: String? = "TP. Hồ Chí Minh"
        var selectedDistrict: String? = "Tân Bình"

        val hanoiDistricts = testCourts
            .filter { it.city.equals("Hà Nội", ignoreCase = true) }
            .mapNotNull { it.district }
            .distinct()

        // Đổi sang Hà Nội
        selectedCity = "Hà Nội"
        if (selectedDistrict !in hanoiDistricts) {
            selectedDistrict = null
        }

        assertNull("Quận Tân Bình phải bị bỏ khi chuyển sang Hà Nội", selectedDistrict)
    }

    @Test
    fun testNormalizeVietnamese() {
        assertEquals("san bong da hoang cau", normalizeVietnamese("Sân bóng đá Hoàng Cầu"))
        assertEquals("dong da", normalizeVietnamese("Đống Đa"))
        assertEquals("tp. ho chi minh", normalizeVietnamese("TP. Hồ Chí Minh"))
        assertEquals("tan binh", normalizeVietnamese("Tân Bình"))
    }

    @Test
    fun testPriceRangeInclusiveBoundaries() {
        // Giá cơ bản 300.000 đ/giờ (Sân bóng Thành Phát)
        val criteriaExact = CourtFilterCriteria(
            minPrice = 300_000,
            maxPrice = 300_000,
        )
        val result = filterCourts(criteria = criteriaExact)
        assertTrue(result.any { it.name == "Sân bóng Thành Phát" })
    }

    @Test
    fun testFilterCriteriaActiveState() {
        val empty = CourtFilterCriteria()
        assertFalse(empty.isActive)

        val withCity = CourtFilterCriteria(city = "Hà Nội")
        assertTrue(withCity.isActive)

        val withMinPrice = CourtFilterCriteria(minPrice = 200_000)
        assertTrue(withMinPrice.isActive)
    }
}
