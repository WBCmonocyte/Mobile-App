package com.example.easysport.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.OwnerRecurringSessionItem
import com.example.easysport.data.model.SessionChangeRequestItem
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.ProCard
import com.example.easysport.ui.common.ProColors
import com.example.easysport.ui.common.ProStatusDotPill
import com.example.easysport.ui.common.vnd

@Composable
fun OwnerRecurringRequestsSection(
    requests: List<SessionChangeRequestItem>,
    upcomingSessions: List<OwnerRecurringSessionItem>,
    isSubmitting: Boolean,
    onResolveRequest: (requestId: String, decision: String, refundAmount: Int?, ownerNote: String) -> Unit,
    onProactiveWeatherPostpone: (sessionId: String, ownerNote: String) -> Unit,
) {
    var refundTarget by remember { mutableStateOf<SessionChangeRequestItem?>(null) }
    var weatherSessionTarget by remember { mutableStateOf<OwnerRecurringSessionItem?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val pending = requests.filter { it.status == "PENDING" }
        val processed = requests.filter { it.status != "PENDING" }

        Text(
            "Yêu cầu chờ Chủ sân xử lý (${pending.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = ProColors.Ink
        )

        if (pending.isEmpty()) {
            ProCard {
                Text(
                    "Không có yêu cầu hoãn/hủy hoặc hoàn tiền nào đang chờ xử lý.",
                    color = ProColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            pending.forEach { req ->
                val isWeather = req.requestType.equals("WEATHER_POSTPONE", ignoreCase = true)
                val typeLabel = when (req.requestType.uppercase()) {
                    "WEATHER_POSTPONE" -> "Xin hoãn thời tiết"
                    "REFUND_WALLET" -> "Yêu cầu hoàn ví"
                    "EXCESSIVE_CANCEL_REVIEW" -> "Vượt ngưỡng hủy"
                    else -> "Hủy sát giờ / Cần duyệt"
                }
                ProCard(accentBorderColor = ProColors.Amber.copy(alpha = 0.45f)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${req.customerName} (${req.customerPhone})", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Ink)
                                Text("${req.courtName} · ${req.sessionDate} · ${req.timeRange}", fontSize = 12.5.sp, color = ProColors.SubInk)
                            }
                            ProStatusDotPill(typeLabel, ProColors.Amber, ProColors.Amber, ProColors.AmberBg)
                        }
                        if (req.reasonNote.isNotBlank()) {
                            Text("Lý do khách nêu: \"${req.reasonNote}\"", fontSize = 12.5.sp, color = ProColors.Ink)
                        }
                        if (req.maxRefundableAmount > 0) {
                            Text(
                                "Giới hạn hoàn vào Ví tối đa: ${req.maxRefundableAmount.vnd()} đ",
                                fontSize = 12.sp,
                                color = ProColors.Muted
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        if (isWeather) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onResolveRequest(req.id, "REJECT", null, "Không xác nhận thời tiết xấu") },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.weight(0.75f),
                                    border = BorderStroke(1.dp, ProColors.Rose.copy(alpha = 0.5f))
                                ) {
                                    Text("Từ chối", color = ProColors.Rose, fontSize = 12.5.sp)
                                }
                                Button(
                                    onClick = { onResolveRequest(req.id, "APPROVE_WEATHER", null, "Xác nhận hoãn do thời tiết") },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.weight(1.4f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                                ) {
                                    Text("Xác nhận mưa (Trượt/Buổi dư)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onResolveRequest(req.id, "APPROVE_CREDIT", null, "Chủ sân duyệt giữ 1 buổi dư") },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Cấp 1 buổi dư", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { refundTarget = req },
                                    enabled = !isSubmitting && req.maxRefundableAmount > 0,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Emerald),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Hoàn Ví", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onResolveRequest(req.id, "REJECT", null, "Giữ nguyên lịch buổi sân") },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.weight(0.75f),
                                    border = BorderStroke(1.dp, ProColors.Rose.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Từ chối", color = ProColors.Rose, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Hoãn chủ động do thời tiết (${upcomingSessions.size} buổi sắp tới)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = ProColors.Ink
        )

        if (upcomingSessions.isEmpty()) {
            ProCard {
                Text(
                    "Chưa có buổi cố định nào đang chờ diễn ra tại cụm sân của bạn.",
                    color = ProColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            upcomingSessions.take(15).forEach { sess ->
                ProCard {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = ProColors.Primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Buổi ${sess.sessionIndex}: ${sess.sessionDate} · ${sess.timeRange}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = ProColors.Ink)
                            Text("${sess.courtName} (${sess.fieldDetail}) · ${sess.customerName}", fontSize = 12.sp, color = ProColors.SubInk)
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { weatherSessionTarget = sess },
                            enabled = !isSubmitting && sess.status == "UPCOMING",
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Outlined.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (sess.status == "WEATHER_APPROVED") "Đã hoãn" else "Hoãn mưa", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (processed.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Lịch sử đã xử lý (${processed.size})", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.SubInk)
            processed.take(10).forEach { r ->
                ProCard {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${r.customerName} · ${r.courtName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ProColors.Ink)
                            Text(
                                r.resolvedOutcome ?: r.status,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (r.status == "APPROVED") ProColors.Emerald else ProColors.Rose
                            )
                        }
                        if (r.approvedRefundAmount != null && r.approvedRefundAmount > 0) {
                            Text("Đã hoàn vào Ví: ${r.approvedRefundAmount.vnd()} đ", fontSize = 12.sp, color = ProColors.Emerald)
                        }
                        if (!r.ownerNote.isNullOrBlank()) {
                            Text("Ghi chú: ${r.ownerNote}", fontSize = 12.sp, color = ProColors.Muted)
                        }
                    }
                }
            }
        }
    }

    refundTarget?.let { req ->
        var amountText by remember(req.id) { mutableStateOf(req.maxRefundableAmount.toString()) }
        var noteText by remember(req.id) { mutableStateOf("") }
        val entered = amountText.toIntOrNull() ?: 0
        val valid = entered in 1..req.maxRefundableAmount

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) refundTarget = null },
            containerColor = Color.White,
            title = { Text("Duyệt hoàn tiền vào Ví EasySport", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Khách: ${req.customerName} · Buổi ${req.sessionDate} (${req.timeRange})\nGiới hạn tối đa: ${req.maxRefundableAmount.vnd()} đ",
                        fontSize = 13.sp,
                        color = EsColors.Body
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { amountText = (req.maxRefundableAmount / 2).coerceAtLeast(1).toString() }) {
                            Text("50%", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { amountText = req.maxRefundableAmount.toString() }) {
                            Text("100% tối đa", fontSize = 12.sp)
                        }
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { s -> amountText = s.filter { it.isDigit() } },
                        label = { Text("Số tiền hoàn (VND)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = !valid,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!valid) {
                        Text("Số tiền phải từ 1 đ đến ${req.maxRefundableAmount.vnd()} đ.", color = EsColors.Danger, fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Ghi chú xử lý") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolveRequest(req.id, "APPROVE_REFUND", entered, noteText)
                        refundTarget = null
                    },
                    enabled = valid && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Emerald)
                ) {
                    Text("Xác nhận hoàn ${entered.vnd()} đ")
                }
            },
            dismissButton = {
                TextButton(onClick = { refundTarget = null }, enabled = !isSubmitting) { Text("Hủy") }
            }
        )
    }

    weatherSessionTarget?.let { sess ->
        var noteText by remember(sess.sessionId) { mutableStateOf("Chủ sân xác nhận hoãn do thời tiết xấu") }
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) weatherSessionTarget = null },
            containerColor = Color.White,
            title = { Text("Hoãn buổi do thời tiết?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Buổi ${sess.sessionIndex} ngày ${sess.sessionDate} (${sess.timeRange}) · ${sess.customerName}.\nKhách sẽ được mở quyền chọn Trượt lịch hoặc Giữ 1 buổi còn dư (không tính lần hủy).",
                        fontSize = 13.sp,
                        color = EsColors.Body
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Thông báo gửi khách") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onProactiveWeatherPostpone(sess.sessionId, noteText)
                        weatherSessionTarget = null
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                ) {
                    Text("Xác nhận hoãn mưa")
                }
            },
            dismissButton = {
                TextButton(onClick = { weatherSessionTarget = null }, enabled = !isSubmitting) { Text("Hủy") }
            }
        )
    }
}
