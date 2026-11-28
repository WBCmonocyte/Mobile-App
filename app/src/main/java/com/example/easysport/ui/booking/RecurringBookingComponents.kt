package com.example.easysport.ui.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.RecurringPackagePreview
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.vnd

@Composable
fun BookingModeSegmentedToggle(
    isRecurring: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EsColors.Surface)
            .border(1.dp, EsColors.Border, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        @Composable
        fun OptionTab(label: String, active: Boolean, onClick: () -> Unit) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) EsColors.Primary else Color.Transparent)
                    .clickable(onClick = onClick)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (active) Color.White else EsColors.Title,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        OptionTab("Đặt một buổi", !isRecurring) { onChange(false) }
        OptionTab("Đặt lịch cố định", isRecurring) { onChange(true) }
    }
}

@Composable
fun RecurringPackagePreviewPanel(
    preview: RecurringPackagePreview?,
    isLoading: Boolean,
    errorMessage: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = EsColors.SurfaceTint,
            border = BorderStroke(1.dp, EsColors.Primary.copy(alpha = 0.25f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Gói: 3 tháng · 12 buổi · 1 buổi/tuần",
                    color = EsColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Sinh đúng 12 ngày cùng thứ liên tiếp (giờ Việt Nam). Chỉ cho thanh toán khi đủ 12 buổi trống.",
                    color = EsColors.Body,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        if (isLoading) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = EsColors.Primary)
                Spacer(Modifier.width(10.dp))
                Text("Đang kiểm tra trống cả 12 buổi...", fontSize = 13.sp, color = EsColors.Body)
            }
        } else if (errorMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF2F2)
            ) {
                Text(errorMessage, color = EsColors.Danger, fontSize = 12.5.sp, modifier = Modifier.padding(10.dp))
            }
        } else if (preview != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (preview.allAvailable) EsColors.Surface else Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, if (preview.allAvailable) EsColors.Border else EsColors.Danger.copy(alpha = 0.45f))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${preview.fieldDetail} · ${preview.weekdayLabel}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = EsColors.Title)
                        Text("${preview.unitPrice.vnd()} đ/buổi", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EsColors.Primary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng gói (${preview.totalSessions} buổi)", fontSize = 13.sp, color = EsColors.Body)
                        Text("${preview.totalPrice.vnd()} đ", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = EsColors.Title)
                    }

                    if (!preview.allAvailable) {
                        val firstBad = preview.conflictingSessions.firstOrNull()
                        Text(
                            "Có ${preview.conflictingSessions.size} buổi bị trùng lịch"
                                + (firstBad?.let { " (ví dụ ${it.dateLabel} · ${it.slot})" } ?: "")
                                + ". Vui lòng đổi ngày bắt đầu hoặc khung giờ.",
                            color = EsColors.Danger,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text("✓ Cả 12 buổi đều trống, sẵn sàng thanh toán trọn gói!", color = EsColors.Success, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(color = EsColors.Border, modifier = Modifier.padding(vertical = 4.dp))
                    preview.sessions.forEach { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    if (s.available) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (s.available) EsColors.Success else EsColors.Danger,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Buổi ${s.sessionIndex}: ${s.sessionDate} · ${s.timeRange}",
                                    fontSize = 12.5.sp,
                                    color = if (s.available) EsColors.Title else EsColors.Danger,
                                    fontWeight = if (s.available) FontWeight.Normal else FontWeight.SemiBold
                                )
                            }
                            Text(
                                if (s.available) "${s.unitPrice.vnd()} đ" else (s.conflictStatus ?: "Trùng lịch"),
                                fontSize = 12.sp,
                                color = if (s.available) EsColors.Body else EsColors.Danger,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
