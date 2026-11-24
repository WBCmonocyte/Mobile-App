package com.example.easysport.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.InventoryItemModel
import com.example.easysport.data.model.OwnerInventoryStatsData
import com.example.easysport.data.model.OwnerOverviewData
import com.example.easysport.ui.common.ProCard
import com.example.easysport.ui.common.ProColors
import com.example.easysport.ui.common.ProHeroBrush
import com.example.easysport.ui.common.ProKpiCard
import com.example.easysport.ui.common.ProSectionHeader
import com.example.easysport.ui.common.ProStatusDotPill
import com.example.easysport.ui.common.vnd
import com.example.easysport.ui.staff.EmptyStateCard
import java.text.NumberFormat
import java.util.Locale

private fun Long.vndFormat(): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(this)
}

@Composable
internal fun OwnerAnalyticsScreen(
    overview: OwnerOverviewData?,
    inventoryStats: OwnerInventoryStatsData?,
    inventoryItems: List<InventoryItemModel>,
    selectedPeriod: String,
    onSelectPeriod: (String) -> Unit,
    selectedBranchId: String?,
    onSelectBranch: (String?) -> Unit,
    onQuickReorder: (InventoryItemModel, Int) -> Unit,
    onGoToInventory: () -> Unit,
) {
    if (overview == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ProColors.Primary)
        }
        return
    }

    var reportSection by remember { mutableStateOf("REVENUE") } // REVENUE | INVENTORY
    var statsDimension by remember { mutableStateOf("BRANCH") } // BRANCH | FIELD | SLOT
    var invStatsFocus by remember { mutableStateOf("ALL") } // ALL | BRANCH | CATEGORY | TOP | REORDER

    val periodLabel = when (selectedPeriod) {
        "TODAY" -> "Hôm nay"
        "WEEK" -> "Tuần này"
        "MONTH" -> "Tháng này"
        else -> "Tất cả thời gian"
    }

    val cashVal = overview.cashRevenue.coerceAtLeast(0L)
    val qrVal = overview.qrRevenue.coerceAtLeast(0L)
    val sumChannels = (cashVal + qrVal).coerceAtLeast(1L)
    val cashPct = ((cashVal * 100.0) / sumChannels).toInt()
    val qrPct = (100 - cashPct).coerceAtLeast(0)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Global Period Filter
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val periods = listOf(
                    "ALL" to "Tất cả",
                    "TODAY" to "Hôm nay",
                    "WEEK" to "Tuần này",
                    "MONTH" to "Tháng này",
                )
                items(periods) { (key, label) ->
                    FilterChip(
                        selected = selectedPeriod == key,
                        onClick = { onSelectPeriod(key) },
                        label = { Text(label, fontSize = 12.5.sp) }
                    )
                }
            }
        }

        // Top Segmented Switcher: Doanh thu Sân vs Lợi nhuận & Kho
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ProColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "REVENUE" to "Doanh thu Sân & POS",
                        "INVENTORY" to "Lãi lỗ & Kho hàng",
                    ).forEach { (code, label) ->
                        val active = reportSection == code
                        Surface(
                            color = if (active) ProColors.Primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { reportSection = code }
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else ProColors.SubInk,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        if (reportSection == "REVENUE") {
            // Hero Revenue Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(ProHeroBrush)
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tổng doanh thu ($periodLabel)", fontSize = 13.sp, color = Color.White.copy(alpha = 0.82f))
                            ProStatusDotPill(
                                text = "${overview.totalBookings} đơn",
                                dotColor = Color(0xFF34D399),
                                textColor = Color.White,
                                bgColor = Color.White.copy(alpha = 0.16f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${overview.totalRevenue.toInt().vnd()} đ",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            if (cashVal > 0L) {
                                Box(
                                    Modifier
                                        .weight(cashVal.toFloat().coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(Color(0xFF60A5FA))
                                )
                            }
                            if (qrVal > 0L) {
                                Box(
                                    Modifier
                                        .weight(qrVal.toFloat().coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(Color(0xFF34D399))
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF60A5FA)))
                                    Text("Tiền mặt ($cashPct%)", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                                Text("${cashVal.toInt().vnd()} đ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34D399)))
                                    Text("VietQR ($qrPct%)", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.8f))
                                }
                                Text("${qrVal.toInt().vnd()} đ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                            }
                        }
                    }
                }
            }

            // Revenue Source Breakdown Card
            item {
                val sumSrc = (overview.courtRevenue + overview.posRevenue).coerceAtLeast(1L)
                val courtPct = ((overview.courtRevenue * 100.0) / sumSrc).toInt()
                val posPct = (100 - courtPct).coerceAtLeast(0)
                ProCard {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tỷ trọng nguồn thu", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Ink)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Tiền thuê sân ($courtPct%)", fontSize = 12.sp, color = ProColors.SubInk)
                                Text("${overview.courtRevenue.toInt().vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ProColors.Primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Dịch vụ & Quầy POS ($posPct%)", fontSize = 12.sp, color = ProColors.SubInk)
                                Text("${overview.posRevenue.toInt().vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ProColors.Emerald)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (overview.courtRevenue.toFloat() / sumSrc.toFloat()).coerceIn(0f, 1f) },
                            color = ProColors.Primary,
                            trackColor = ProColors.Emerald,
                            modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(999.dp))
                        )
                    }
                }
            }

            // Multi-dimensional Analysis Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProSectionHeader("Phân tích doanh thu theo chiều")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val dimensions = listOf(
                            "BRANCH" to "Theo chi nhánh",
                            "FIELD" to "Theo sân con",
                            "SLOT" to "Theo khung giờ",
                        )
                        items(dimensions) { (code, label) ->
                            FilterChip(
                                selected = statsDimension == code,
                                onClick = { statsDimension = code },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            when (statsDimension) {
                "BRANCH" -> {
                    val sortedBranches = overview.branchRevenues.sortedByDescending { it.revenue }
                    val maxRev = sortedBranches.maxOfOrNull { it.revenue }.takeIf { (it ?: 0L) > 0L } ?: 1L
                    if (sortedBranches.isEmpty()) {
                        item {
                            EmptyStateCard(title = "Chưa có chi nhánh", subtitle = "Thêm chi nhánh sân để theo dõi doanh thu.")
                        }
                    } else {
                        items(sortedBranches, key = { "an_br_${it.courtId}" }) { br ->
                            val ratio = (br.revenue.toFloat() / maxRev.toFloat()).coerceIn(0f, 1f)
                            ProCard {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(br.courtName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${br.sportType} · ${br.fieldCount} sân con · ${br.bookingCount} đơn", fontSize = 11.5.sp, color = ProColors.Muted)
                                        }
                                        Text("${br.revenue.toInt().vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = ProColors.Primary)
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        color = ProColors.Primary,
                                        trackColor = ProColors.Surface,
                                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ProColors.Surface)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Tiền sân: ${br.courtRevenue.toInt().vnd()} đ", fontSize = 11.5.sp, color = ProColors.SubInk, fontWeight = FontWeight.Medium)
                                        Text("Dịch vụ/POS: ${br.posRevenue.toInt().vnd()} đ", fontSize = 11.5.sp, color = ProColors.Emerald, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                "FIELD" -> {
                    val fields = overview.fieldRevenues
                    val maxFieldRev = fields.maxOfOrNull { it.totalRevenue }.takeIf { (it ?: 0L) > 0L } ?: 1L
                    if (fields.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "Chưa có dữ liệu doanh thu theo sân con",
                                subtitle = "Khi có đơn đặt sân trong kỳ được chọn, hệ thống sẽ xếp hạng từng sân con tại đây."
                            )
                        }
                    } else {
                        items(fields.withIndex().toList(), key = { "an_fld_${it.value.courtId}_${it.value.fieldName}_${it.index}" }) { (idx, f) ->
                            val ratio = (f.totalRevenue.toFloat() / maxFieldRev.toFloat()).coerceIn(0f, 1f)
                            ProCard {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (idx == 0) ProColors.AmberBg else ProColors.PrimarySoft),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("#${idx + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (idx == 0) ProColors.Amber else ProColors.Primary)
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(f.fieldName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${f.courtName} · ${f.bookingCount} lượt đặt", fontSize = 11.5.sp, color = ProColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Text("${f.totalRevenue.toInt().vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ProColors.Primary)
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        color = ProColors.Emerald,
                                        trackColor = ProColors.Surface,
                                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Tiền thuê sân: ${f.courtRevenue.toInt().vnd()} đ", fontSize = 11.5.sp, color = ProColors.SubInk)
                                        Text("Dịch vụ: ${f.serviceRevenue.toInt().vnd()} đ", fontSize = 11.5.sp, color = ProColors.Emerald, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    val slots = overview.timeSlotRevenues
                    val totalSlotRev = slots.sumOf { it.revenue }.coerceAtLeast(1L)
                    item {
                        ProCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (slots.isEmpty()) {
                                    Text("Chưa có dữ liệu khung giờ", fontSize = 13.sp, color = ProColors.Muted)
                                } else {
                                    slots.forEach { s ->
                                        val pct = ((s.revenue * 100.0) / totalSlotRev).toInt()
                                        val ratio = (s.revenue.toFloat() / totalSlotRev.toFloat()).coerceIn(0f, 1f)
                                        val barColor = when (s.slot) {
                                            "MORNING" -> ProColors.Amber
                                            "AFTERNOON" -> ProColors.Sky
                                            else -> ProColors.Primary
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(s.label, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = ProColors.Ink)
                                                Text("${s.revenue.toInt().vnd()} đ ($pct%)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = barColor)
                                            }
                                            LinearProgressIndicator(
                                                progress = { ratio },
                                                color = barColor,
                                                trackColor = ProColors.Surface,
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp))
                                            )
                                            Text("${s.bookingCount} đơn đặt sân trong khung giờ này", fontSize = 11.5.sp, color = ProColors.SubInk)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // INVENTORY & PROFIT TAB
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedBranchId == null,
                            onClick = { onSelectBranch(null) },
                            label = { Text("Tất cả chi nhánh", fontSize = 12.sp) }
                        )
                    }
                    items(overview.branchRevenues) { b ->
                        FilterChip(
                            selected = selectedBranchId == b.courtId,
                            onClick = { onSelectBranch(b.courtId) },
                            label = { Text(b.courtName, fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to "Tổng hợp",
                        "BRANCH" to "Theo chi nhánh",
                        "CATEGORY" to "Theo nhóm hàng",
                        "TOP" to "Top bán chạy",
                        "REORDER" to "Cần nhập (${inventoryStats?.reorderSuggestions?.size ?: 0})",
                    ).forEach { (code, label) ->
                        item {
                            FilterChip(
                                selected = invStatsFocus == code,
                                onClick = { invStatsFocus = code },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            item {
                AnalyticsInventoryHeroCard(stats = inventoryStats)
            }

            if (invStatsFocus == "ALL") {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProKpiCard(
                            icon = Icons.Rounded.ShoppingBag,
                            iconTint = ProColors.Primary,
                            iconBg = ProColors.PrimarySoft,
                            value = "${inventoryStats?.totalSoldUnits ?: 0} sp",
                            label = "Đã xuất bán POS",
                            modifier = Modifier.weight(1f)
                        )
                        ProKpiCard(
                            icon = Icons.Rounded.LocalShipping,
                            iconTint = ProColors.Sky,
                            iconBg = ProColors.SkyBg,
                            value = "${(inventoryStats?.totalImportSpend ?: 0L).vndFormat()} đ",
                            label = "Chi nhập (${inventoryStats?.totalImportedUnits ?: 0} sp)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProKpiCard(
                            icon = Icons.Rounded.ReportProblem,
                            iconTint = ProColors.Rose,
                            iconBg = ProColors.RoseBg,
                            value = "${(inventoryStats?.totalWastageCost ?: 0L).vndFormat()} đ",
                            label = "Hao hụt (${inventoryStats?.totalWastageUnits ?: 0} sp)",
                            modifier = Modifier.weight(1f)
                        )
                        ProKpiCard(
                            icon = Icons.Rounded.SwapHoriz,
                            iconTint = ProColors.Amber,
                            iconBg = ProColors.AmberBg,
                            value = "${inventoryStats?.totalTransferCount ?: 0} phiếu",
                            label = "Điều chuyển kho",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val cats = inventoryStats?.categoryBreakdown ?: emptyList()
            if ((invStatsFocus == "ALL" || invStatsFocus == "CATEGORY") && cats.isNotEmpty()) {
                item { ProSectionHeader("Phân bổ tồn kho & doanh thu theo nhóm") }
                item {
                    val maxVal = cats.maxOfOrNull { it.stockValue }.takeIf { (it ?: 0L) > 0L } ?: 1L
                    ProCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            cats.forEach { c ->
                                val progress = (c.stockValue.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                                val barColor = when (c.category) {
                                    "EQUIPMENT" -> ProColors.Amber
                                    "ACCESSORY" -> ProColors.Sky
                                    else -> ProColors.Emerald
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${c.label} (${c.itemCount} mã · ${c.stockUnits} tồn)",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = ProColors.Ink
                                        )
                                        Text(
                                            "Vốn ${c.stockValue.vndFormat()} đ",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = barColor
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = barColor,
                                        trackColor = ProColors.Surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Text(
                                        "Đã bán: ${c.soldUnits} sp · Doanh thu: ${c.revenue.vndFormat()} đ",
                                        fontSize = 11.5.sp,
                                        color = ProColors.SubInk
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val topSelling = inventoryStats?.topSelling ?: emptyList()
            if (invStatsFocus == "ALL" || invStatsFocus == "TOP") {
                item { ProSectionHeader("Top sản phẩm bán chạy nhất") }
                if (topSelling.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Chưa có dữ liệu bán hàng trong kỳ",
                            subtitle = "Khi nhân viên bán hàng qua quầy POS, thống kê top bán chạy sẽ hiển thị tại đây."
                        )
                    }
                } else {
                    items(topSelling.withIndex().toList(), key = { "an_top_${it.value.itemId}" }) { (idx, top) ->
                        ProCard {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (idx == 0) ProColors.AmberBg else ProColors.PrimarySoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "#${idx + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (idx == 0) ProColors.Amber else ProColors.Primary
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        top.itemName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ProColors.Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${top.courtName} · Đã bán ${top.soldUnits} ${top.unit} · Tồn ${top.currentStock}",
                                        fontSize = 11.5.sp,
                                        color = ProColors.SubInk
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${top.revenue.vndFormat()} đ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = ProColors.Primary
                                    )
                                    Text(
                                        "Lãi +${top.profit.vndFormat()} đ",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ProColors.Emerald
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val branchStats = inventoryStats?.branchBreakdown ?: emptyList()
            if ((invStatsFocus == "ALL" || invStatsFocus == "BRANCH") && branchStats.isNotEmpty()) {
                item { ProSectionHeader("Hiệu quả kho theo chi nhánh") }
                items(branchStats, key = { "an_inv_br_${it.courtId}" }) { br ->
                    ProCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(br.courtName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Ink)
                                if (br.lowStockCount > 0) {
                                    ProStatusDotPill("${br.lowStockCount} sắp hết", ProColors.Rose, ProColors.Rose, ProColors.RoseBg)
                                } else {
                                    ProStatusDotPill("Ổn định", ProColors.Emerald, ProColors.Emerald, ProColors.EmeraldBg)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Tồn: ${br.stockUnits} sp (${br.itemCount} mã)", fontSize = 12.sp, color = ProColors.SubInk)
                                Text("Vốn lưu kho: ${br.stockValue.vndFormat()} đ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.Ink)
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ProColors.Surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Doanh thu: ${br.revenue.vndFormat()} đ", fontSize = 12.sp, color = ProColors.Primary, fontWeight = FontWeight.SemiBold)
                                Text("Lãi gộp: +${br.profit.vndFormat()} đ", fontSize = 12.sp, color = ProColors.Emerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            val suggestions = inventoryStats?.reorderSuggestions ?: emptyList()
            if (invStatsFocus == "ALL" || invStatsFocus == "REORDER") {
                if (suggestions.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProSectionHeader("Gợi ý nhập hàng (${suggestions.size})")
                            TextButton(onClick = onGoToInventory) {
                                Text("Mở Kho hàng →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProColors.Primary)
                            }
                        }
                    }
                    items(suggestions, key = { "an_sug_${it.itemId}" }) { sug ->
                        val matchedItem = inventoryItems.firstOrNull { it.id == sug.itemId }
                        ProCard(accentBorderColor = ProColors.Amber.copy(alpha = 0.4f)) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(sug.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ProColors.Ink)
                                    Text(
                                        "${sug.courtName} · Còn ${sug.currentStock} / Ngưỡng ${sug.minAlertThreshold} ${sug.unit}",
                                        fontSize = 12.sp,
                                        color = ProColors.Rose
                                    )
                                    Text(
                                        "Đề xuất nhập +${sug.suggestedQty} ${sug.unit} (dự kiến ${sug.estimatedCost.vndFormat()} đ)",
                                        fontSize = 12.sp,
                                        color = ProColors.SubInk,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (matchedItem != null) {
                                    Button(
                                        onClick = { onQuickReorder(matchedItem, sug.suggestedQty) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                                    ) {
                                        Text("+ Nhập", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else if (invStatsFocus == "REORDER") {
                    item {
                        EmptyStateCard(
                            title = "Tất cả mặt hàng đều đủ tồn kho",
                            subtitle = "Chưa có sản phẩm nào chạm ngưỡng cảnh báo nhập thêm."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsInventoryHeroCard(stats: OwnerInventoryStatsData?) {
    val revenue = stats?.totalRevenue ?: 0L
    val cogs = stats?.totalCogs ?: 0L
    val profit = stats?.grossProfit ?: 0L
    val margin = stats?.profitMarginPercent ?: 0.0
    val potentialRev = stats?.totalPotentialRevenue ?: 0L

    ProCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ProHeroBrush)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Lợi nhuận gộp hàng hóa", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.85f))
                        Text(
                            "${profit.vndFormat()} đ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                            Text(
                                "Biên $margin%",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Doanh thu bán", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                        Text("${revenue.vndFormat()} đ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Giá vốn (COGS)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                        Text("${cogs.vndFormat()} đ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Dự thu hàng tồn", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                        Text("${potentialRev.vndFormat()} đ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                    }
                }
            }
        }
    }
}
