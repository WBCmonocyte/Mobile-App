package com.example.easysport.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.*
import com.example.easysport.ui.common.EsColors

private val DarkGrayColor = Color(0xFF334155) // Xám đen cho Đang giữ & Đã đặt
private val AvailableColor = Color(0xFFF1F5F9) // Xám trắng cho Ca trống

@Composable
fun FootballTimePicker(
    slots: List<TimeSlot>,
    selected: Set<String>,
    onSelect: (Set<String>) -> Unit,
) {
    var session by remember { mutableStateOf(Session.MORNING) }
    val currentGroup = slots.filter { it.session == session }

    // Tab chọn Buổi: Sáng / Chiều / Tối
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Session.entries.forEach { item ->
            val active = session == item
            val countInSession = slots.count { it.session == item && it.label in selected }

            Surface(
                onClick = { session = item },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (active) EsColors.Primary else EsColors.Surface,
                contentColor = if (active) Color.White else EsColors.Title,
                border = if (active) null else androidx.compose.foundation.BorderStroke(1.dp, EsColors.Border)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (countInSession > 0) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (active) Color.White else EsColors.Primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$countInSession",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) EsColors.Primary else Color.White
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.range,
                        fontSize = 11.sp,
                        color = if (active) Color.White.copy(alpha = 0.9f) else EsColors.Body
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Chọn khung giờ", color = EsColors.Title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (selected.isNotEmpty()) {
            Text(
                text = "Đã chọn: ${selected.size} ca",
                color = EsColors.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Chạm vào các ca bên dưới hoặc kéo trên thanh để chọn nhiều khung giờ",
        color = EsColors.Body,
        fontSize = 12.sp
    )

    Spacer(Modifier.height(12.dp))
    RentalTimeline(
        group = currentGroup,
        selected = selected,
        onToggleSlot = { slotLabel ->
            val next = if (slotLabel in selected) selected - slotLabel else selected + slotLabel
            onSelect(next)
        },
        onSelectRange = { rangeLabels ->
            onSelect(selected + rangeLabels)
        }
    )

    Spacer(Modifier.height(12.dp))
    // Chú thích màu sắc: Trống (xám trắng), Đang giữ (xám đen), Đã đặt (xám đen), Đang chọn (xanh)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(
            Triple(AvailableColor, "Ca trống", false),
            Triple(DarkGrayColor, "Đang giữ", false),
            Triple(DarkGrayColor, "Đã đặt", false),
            Triple(EsColors.Primary, "Đã chọn", false)
        ).forEach { (color, label, _) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(color)
                        .border(0.5.dp, EsColors.Border)
                )
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, color = EsColors.Body)
            }
        }
    }

    // Danh sách các ca chi tiết của buổi hiện tại
    Spacer(Modifier.height(16.dp))
    Text(
        "Các ca trong buổi ${session.title.lowercase()} (chạm để chọn/bỏ chọn):",
        color = EsColors.Title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        currentGroup.forEach { slot ->
            val isHeld = slot.status == SlotAvailabilityStatus.HELD
            val isBooked = slot.status == SlotAvailabilityStatus.BOOKED
            val isOccupied = isHeld || isBooked
            val isSelected = slot.label in selected && !isOccupied

            val bg = when {
                isOccupied -> DarkGrayColor
                isSelected -> EsColors.Primary
                else -> AvailableColor
            }
            val textColor = when {
                isOccupied -> Color(0xFFF1F5F9)
                isSelected -> Color.White
                else -> EsColors.Title
            }
            val borderColor = when {
                isSelected -> EsColors.Primary
                isOccupied -> Color.Transparent
                else -> EsColors.Border
            }
            val statusText = when {
                isHeld -> "Đang giữ"
                isBooked -> "Đã đặt"
                isSelected -> "✓ Đã chọn"
                else -> "Còn trống"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable(enabled = !isOccupied) {
                        val next = if (isSelected) selected - slot.label else selected + slot.label
                        onSelect(next)
                    }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = slot.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        color = if (isSelected || isOccupied) Color.White.copy(alpha = 0.9f) else textColor.copy(alpha = 0.8f),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }

    if (selected.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onSelect(emptySet()) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = EsColors.Danger)
                Spacer(Modifier.width(4.dp))
                Text("Xóa chọn tất cả các ca", fontSize = 12.sp, color = EsColors.Danger)
            }
        }
    }
}

@Composable
private fun RentalTimeline(
    group: List<TimeSlot>,
    selected: Set<String>,
    onToggleSlot: (String) -> Unit,
    onSelectRange: (Set<String>) -> Unit,
) {
    if (group.isEmpty()) return
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelectRange by rememberUpdatedState(onSelectRange)

    Column {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(EsColors.SurfaceTint)
        ) {
            val labels = group.map { it.start } + group.last().end
            labels.forEachIndexed { index, label ->
                val labelWidth = 48.dp
                val position = (maxWidth * (index.toFloat() / group.size) - labelWidth / 2)
                    .coerceIn(0.dp, (maxWidth - labelWidth).coerceAtLeast(0.dp))
                Text(
                    text = label,
                    color = EsColors.Title,
                    fontSize = 12.sp,
                    textAlign = when (index) {
                        0 -> TextAlign.Start
                        group.size -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier
                        .offset(x = position)
                        .width(labelWidth)
                        .padding(top = 5.dp)
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pointerInput(group) {
                    var anchor = 0
                    var enabled = false
                    detectDragGestures(
                        onDragStart = { point ->
                            val index = (point.x / size.width * group.size).toInt().coerceIn(group.indices)
                            anchor = index
                            val slot = group[index]
                            enabled = slot.isAvailable
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (enabled) {
                                val index = (change.position.x / size.width * group.size).toInt().coerceIn(group.indices)
                                val interval = group.subList(minOf(anchor, index), maxOf(anchor, index) + 1)
                                if (interval.all { it.isAvailable }) {
                                    currentOnSelectRange(interval.map { it.label }.toSet())
                                }
                            }
                        }
                    )
                }
        ) {
            group.forEach { slot ->
                val isOccupied = !slot.isAvailable
                val active = !isOccupied && slot.label in selected
                val color = when {
                    isOccupied -> DarkGrayColor
                    active -> EsColors.Primary
                    else -> AvailableColor
                }

                val statusLabel = when (slot.status) {
                    SlotAvailabilityStatus.HELD -> "Đang giữ"
                    SlotAvailabilityStatus.BOOKED -> "Đã đặt"
                    else -> if (active) "Đã chọn" else "Ca trống"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                        .drawBehind {
                            drawLine(EsColors.Border, Offset.Zero, Offset(0f, size.height), 1.dp.toPx())
                            if (active) {
                                drawLine(
                                    Color.White,
                                    Offset(4.dp.toPx(), size.height * 0.3f),
                                    Offset(4.dp.toPx(), size.height * 0.7f),
                                    2.dp.toPx()
                                )
                                drawLine(
                                    Color.White,
                                    Offset(size.width - 4.dp.toPx(), size.height * 0.3f),
                                    Offset(size.width - 4.dp.toPx(), size.height * 0.7f),
                                    2.dp.toPx()
                                )
                            }
                        }
                        .semantics {
                            contentDescription = "${slot.label}, $statusLabel"
                            this.selected = active
                        }
                        .clickable(enabled = !isOccupied) {
                            onToggleSlot(slot.label)
                        }
                )
            }
        }
    }
}
