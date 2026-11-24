package com.example.easysport.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.easysport.data.remote.PaymentApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotifUiState {
    object Loading : NotifUiState
    data class Success(val items: List<AppNotification>) : NotifUiState
    object Empty : NotifUiState
    data class Error(val message: String) : NotifUiState
}

class NotificationViewModel(
    private val repository: com.example.easysport.data.repository.NotificationRepository =
        com.example.easysport.data.repository.NetworkNotificationRepository,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main.immediate,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotifUiState>(NotifUiState.Loading)
    val uiState: StateFlow<NotifUiState> = _uiState.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = NotifUiState.Loading
        viewModelScope.launch(dispatcher) {
            repository.getNotifications()
                .onSuccess { items ->
                    _uiState.value = if (items.isEmpty()) NotifUiState.Empty
                    else NotifUiState.Success(items)
                    _unreadCount.value = items.count { !it.isRead }
                }
                .onFailure { e ->
                    _uiState.value = NotifUiState.Error(
                        e.localizedMessage ?: "Không thể tải thông báo."
                    )
                }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch(dispatcher) {
            repository.markRead(id)
            // Cập nhật cục bộ ngay lập tức để UI phản hồi nhanh
            val current = _uiState.value
            if (current is NotifUiState.Success) {
                val updated = current.items.map {
                    if (it.id == id) it.copy(isRead = true) else it
                }
                _uiState.value = NotifUiState.Success(updated)
                _unreadCount.value = updated.count { !it.isRead }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch(dispatcher) {
            repository.markAllRead()
            val current = _uiState.value
            if (current is NotifUiState.Success) {
                val updated = current.items.map { it.copy(isRead = true) }
                _uiState.value = NotifUiState.Success(updated)
                _unreadCount.value = 0
            }
        }
    }

    fun retry() = load()

    class Factory(
        private val repository: com.example.easysport.data.repository.NotificationRepository =
            com.example.easysport.data.repository.NetworkNotificationRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(repository) as T
        }
    }
}
