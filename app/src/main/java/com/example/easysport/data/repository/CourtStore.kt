package com.example.easysport.data.repository

import android.content.Context
import com.example.easysport.data.local.AppDatabase
import com.example.easysport.data.local.entity.toDomainModel
import com.example.easysport.data.local.entity.toEntity
import com.example.easysport.data.model.Court
import com.example.easysport.data.remote.PaymentApiClient
import com.example.easysport.util.CourtAddressArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Kho dữ liệu sân thể thao kết nối trực tiếp với backend MySQL.
 * MySQL là nguồn dữ liệu chính của hệ thống sân.
 * Không hiển thị dữ liệu mẫu như dữ liệu thật khi API thất bại.
 */
object CourtStore {

    private fun Court.withAddressArea(): Court {
        if (!city.isNullOrBlank() && !district.isNullOrBlank()) return this
        val area = CourtAddressArea.fromAddress(address)
        val existingCity = city?.takeIf { it.isNotBlank() }
        val matchingDistrict = area.district.takeIf {
            existingCity == null || area.city.equals(existingCity, ignoreCase = true)
        }
        return copy(
            city = existingCity ?: area.city,
            district = district?.takeIf { it.isNotBlank() } ?: matchingDistrict,
        )
    }

    private val _courts = MutableStateFlow<List<Court>>(emptyList())
    val courts: StateFlow<List<Court>> = _courts.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun getCourtById(id: String): Court? {
        return _courts.value.firstOrNull { it.id == id }
    }

    /**
     * Nạp dữ liệu sân từ Backend API (GET /api/courts):
     * 1. Thử lấy từ Backend API
     * 2. Nếu thành công -> cập nhật StateFlow và lưu cache vào Room
     * 3. Nếu lỗi mạng -> tải từ Room cache (nếu có); KHÔNG dùng SampleData làm dữ liệu thật
     */
    fun refresh(context: Context? = null, onComplete: ((Boolean) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            _isRefreshing.value = true
            _lastError.value = null
            try {
                val remoteCourts = PaymentApiClient.getCourts().map { it.withAddressArea() }
                if (remoteCourts.isNotEmpty()) {
                    _courts.value = remoteCourts
                    _isLoaded.value = true

                    // Lưu cache vào Room Database nếu có Context
                    context?.let { ctx ->
                        try {
                            val db = AppDatabase.getInstance(ctx)
                            db.courtDao().insertCourts(remoteCourts.map { it.toEntity() })
                        } catch (_: Exception) {}
                    }
                    onComplete?.invoke(true)
                } else {
                    _courts.value = emptyList()
                    _isLoaded.value = true
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                _lastError.value = e.message ?: "Không thể kết nối đến máy chủ"
                // Thử khôi phục từ Room cache cục bộ (nếu có)
                if (context != null && _courts.value.isEmpty()) {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val cachedEntities = db.courtDao().getAllCourtsSync()
                        if (cachedEntities.isNotEmpty()) {
                            _courts.value = cachedEntities.map { it.toDomainModel().withAddressArea() }
                        }
                    } catch (_: Exception) {}
                }
                _isLoaded.value = true
                onComplete?.invoke(false)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
