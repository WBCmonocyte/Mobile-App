package com.example.easysport.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsTopBar

enum class NotificationType {
    BOOKING, REMINDER, PAYMENT, PROMOTION, SYSTEM
}

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val createdAt: String = "",
    val type: NotificationType,
    val isRead: Boolean = false,
    val targetBookingId: String? = null,
)

private val SAMPLE_NOTIFICATIONS = listOf<AppNotification>()

@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onOpenBooking: (String) -> Unit = {},
    viewModel: NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = NotificationViewModel.Factory()
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Trích xuất danh sách thông báo khi state là Success để tính hasUnread
    val notifications = (uiState as? NotifUiState.Success)?.items ?: emptyList()
    val hasUnread = notifications.any { !it.isRead }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            EsTopBar(
                title = "Thông báo",
                onBack = onBack,
                trailing = {
                    if (hasUnread) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(
                                Icons.Outlined.DoneAll,
                                contentDescription = "Đã đọc tất cả",
                                tint = EsColors.Primary
                            )
                        }
                    }
                }
            )

            when (val state = uiState) {
                is NotifUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EsColors.Primary)
                    }
                }

                is NotifUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = EsColors.Body,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                color = EsColors.Body,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Thử lại", color = Color.White)
                            }
                        }
                    }
                }

                is NotifUiState.Empty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = EsColors.Body,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Bạn chưa có thông báo",
                                color = EsColors.Body,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                is NotifUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.items, key = { it.id }) { notif ->
                            NotificationItem(
                                notification = notif,
                                onClick = {
                                    viewModel.markRead(notif.id)
                                    if (notif.targetBookingId != null) {
                                        onOpenBooking(notif.targetBookingId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    val (icon, iconBg, iconTint) = when (notification.type) {
        NotificationType.BOOKING -> Triple(Icons.Outlined.SportsSoccer, EsColors.SurfaceTint, EsColors.Primary)
        NotificationType.REMINDER -> Triple(Icons.Outlined.ConfirmationNumber, EsColors.WarningBg, EsColors.Warning)
        NotificationType.PAYMENT -> Triple(Icons.Outlined.Payments, EsColors.SuccessBg, EsColors.Success)
        NotificationType.PROMOTION -> Triple(Icons.Outlined.LocalOffer, Color(0xFFFEECEC), Color(0xFFE53935))
        NotificationType.SYSTEM -> Triple(Icons.Outlined.Notifications, EsColors.Surface, EsColors.Title)
    }

    val itemBg = if (notification.isRead) Color.White else EsColors.Surface.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(itemBg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    color = EsColors.Title,
                    fontSize = 15.sp,
                    fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(EsColors.Primary, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = notification.body,
                color = if (notification.isRead) EsColors.Body else EsColors.Title.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = notification.timeAgo,
                color = EsColors.Body.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NotificationPreview() {
    NotificationScreen(onBack = {})
}

