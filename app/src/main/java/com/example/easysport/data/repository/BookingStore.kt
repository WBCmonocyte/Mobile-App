package com.example.easysport.data.repository

import com.example.easysport.data.model.Booking
import com.example.easysport.data.remote.PaymentApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Kho lịch đặt sân kết nối backend API, có reactive StateFlow.
 * Không dùng dữ liệu mẫu tĩnh khi chạy thực tế.
 */
object BookingStore {

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun all(): List<Booking> = _bookings.value

    fun get(id: String): Booking? = _bookings.value.firstOrNull { it.id == id }

    fun add(booking: Booking) {
        _bookings.update { current -> current.filterNot { it.id == booking.id } + booking }
        // Sau khi thêm, đồng bộ lại từ backend
        refresh()
    }

    fun remove(id: String) {
        _bookings.update { current -> current.filterNot { it.id == id } }
    }

    fun clear() {
        _bookings.value = emptyList()
        _error.value = null
        _isLoading.value = false
    }

    /**
     * Tải danh sách đơn đặt từ backend API (GET /api/bookings)
     */
    fun refresh(onComplete: ((Boolean) -> Unit)? = null) {
        val token = PaymentApiClient.authToken ?: return
        CoroutineScope(Dispatchers.Main).launch {
            _isLoading.value = true
            _error.value = null
            val result = PaymentApiClient.getBookings()
            result.fold(
                onSuccess = { list ->
                    if (token != PaymentApiClient.authToken) return@launch
                    _bookings.value = list
                    _isLoading.value = false
                    onComplete?.invoke(true)
                },
                onFailure = { err ->
                    _error.value = err.message
                    _isLoading.value = false
                    onComplete?.invoke(false)
                }
            )
        }
    }

    /**
     * Hủy đơn đặt sân qua backend API (DELETE /api/bookings/{id})
     */
    fun cancelBooking(bookingId: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = PaymentApiClient.cancelBooking(bookingId)
            result.fold(
                onSuccess = {
                    remove(bookingId)
                    refresh()
                    onResult?.invoke(true, null)
                },
                onFailure = { err ->
                    onResult?.invoke(false, err.message)
                }
            )
        }
    }
}
