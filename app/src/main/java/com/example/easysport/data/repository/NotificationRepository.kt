package com.example.easysport.data.repository

import com.example.easysport.data.remote.PaymentApiClient
import com.example.easysport.ui.home.AppNotification

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<AppNotification>>
    suspend fun markRead(id: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    suspend fun getUnreadCount(): Result<Int>
}

object NetworkNotificationRepository : NotificationRepository {
    override suspend fun getNotifications(): Result<List<AppNotification>> =
        PaymentApiClient.getNotifications()

    override suspend fun markRead(id: String): Result<Unit> =
        PaymentApiClient.markNotificationRead(id)

    override suspend fun markAllRead(): Result<Unit> =
        PaymentApiClient.markAllNotificationsRead()

    override suspend fun getUnreadCount(): Result<Int> =
        PaymentApiClient.getUnreadNotificationCount()
}
