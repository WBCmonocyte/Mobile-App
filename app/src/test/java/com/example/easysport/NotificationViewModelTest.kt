package com.example.easysport

import com.example.easysport.data.repository.NotificationRepository
import com.example.easysport.ui.home.AppNotification
import com.example.easysport.ui.home.NotifUiState
import com.example.easysport.ui.home.NotificationType
import com.example.easysport.ui.home.NotificationViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNotificationRepository(
    var items: MutableList<AppNotification> = mutableListOf(),
    var shouldFail: Boolean = false,
) : NotificationRepository {

    override suspend fun getNotifications(): Result<List<AppNotification>> {
        return if (shouldFail) Result.failure(Exception("Lỗi kết nối mạng"))
        else Result.success(items.toList())
    }

    override suspend fun markRead(id: String): Result<Unit> {
        val idx = items.indexOfFirst { it.id == id }
        if (idx != -1) {
            items[idx] = items[idx].copy(isRead = true)
        }
        return Result.success(Unit)
    }

    override suspend fun markAllRead(): Result<Unit> {
        items = items.map { it.copy(isRead = true) }.toMutableList()
        return Result.success(Unit)
    }

    override suspend fun getUnreadCount(): Result<Int> {
        return Result.success(items.count { !it.isRead })
    }
}

class NotificationViewModelTest {

    @Test
    fun load_emptyList_returnsEmptyState() {
        val repo = FakeNotificationRepository(mutableListOf())
        val vm = NotificationViewModel(repo, Dispatchers.Unconfined)

        assertTrue(vm.uiState.value is NotifUiState.Empty)
        assertEquals(0, vm.unreadCount.value)
    }

    @Test
    fun load_hasNotifications_returnsSuccessStateWithCorrectUnreadCount() {
        val notifs = mutableListOf(
            AppNotification("n1", "T1", "B1", "1p", "", NotificationType.BOOKING, isRead = false),
            AppNotification("n2", "T2", "B2", "2p", "", NotificationType.PAYMENT, isRead = true),
            AppNotification("n3", "T3", "B3", "3p", "", NotificationType.REMINDER, isRead = false),
        )
        val repo = FakeNotificationRepository(notifs)
        val vm = NotificationViewModel(repo, Dispatchers.Unconfined)

        val state = vm.uiState.value
        assertTrue(state is NotifUiState.Success)
        assertEquals(3, (state as NotifUiState.Success).items.size)
        assertEquals(2, vm.unreadCount.value)
    }

    @Test
    fun load_networkError_returnsErrorState() {
        val repo = FakeNotificationRepository(shouldFail = true)
        val vm = NotificationViewModel(repo, Dispatchers.Unconfined)

        val state = vm.uiState.value
        assertTrue(state is NotifUiState.Error)
        assertEquals("Lỗi kết nối mạng", (state as NotifUiState.Error).message)
    }

    @Test
    fun markRead_updatesItemLocallyAndUnreadCount() {
        val notifs = mutableListOf(
            AppNotification("n1", "T1", "B1", "1p", "", NotificationType.BOOKING, isRead = false),
        )
        val repo = FakeNotificationRepository(notifs)
        val vm = NotificationViewModel(repo, Dispatchers.Unconfined)

        vm.markRead("n1")

        val state = vm.uiState.value as NotifUiState.Success
        assertTrue(state.items.first().isRead)
        assertEquals(0, vm.unreadCount.value)
    }

    @Test
    fun markAllRead_marksAllItemsReadAndZeroUnread() {
        val notifs = mutableListOf(
            AppNotification("n1", "T1", "B1", "1p", "", NotificationType.BOOKING, isRead = false),
            AppNotification("n2", "T2", "B2", "2p", "", NotificationType.REMINDER, isRead = false),
        )
        val repo = FakeNotificationRepository(notifs)
        val vm = NotificationViewModel(repo, Dispatchers.Unconfined)

        vm.markAllRead()

        val state = vm.uiState.value as NotifUiState.Success
        assertTrue(state.items.all { it.isRead })
        assertEquals(0, vm.unreadCount.value)
    }
}
