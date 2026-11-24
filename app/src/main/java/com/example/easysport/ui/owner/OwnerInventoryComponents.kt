package com.example.easysport.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.BranchRevenueItem
import com.example.easysport.data.model.InventoryItemModel
import com.example.easysport.data.model.InventoryTransactionModel
import com.example.easysport.data.model.OwnerInventoryData
import com.example.easysport.data.model.OwnerInventoryStatsData
import com.example.easysport.ui.common.EsColors
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

private fun Long.vndLong(): String {
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(this)
}

@Composable
internal fun OwnerInventoryTab(
    inventoryData: OwnerInventoryData?,
    inventoryStats: OwnerInventoryStatsData?,
    branches: List<BranchRevenueItem>,
    selectedBranchId: String?,
    statsPeriod: String,
    onSelectBranch: (String?) -> Unit,
    onSelectStatsPeriod: (String) -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (InventoryItemModel) -> Unit,
    onRestockItem: (InventoryItemModel) -> Unit,
    onQuickReorder: (InventoryItemModel, Int) -> Unit,
    onAdjustItem: (InventoryItemModel) -> Unit,
    onWastageItem: (InventoryItemModel) -> Unit,
    onTransferItem: (InventoryItemModel) -> Unit,
    onDeleteItem: (InventoryItemModel) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("ALL") } // ALL | DRINK | EQUIPMENT | ACCESSORY | LOW
    var txTypeFilter by remember { mutableStateOf("ALL") } // ALL | IMPORT | SALE | TRANSFER | WASTAGE | ADJUST
    var statsFocus by remember { mutableStateOf("ALL") } // ALL | BRANCH | CATEGORY | TOP | REORDER
    var subMode by remember { mutableStateOf("STOCK") } // STOCK | STATS | HISTORY

    val items = inventoryData?.items ?: emptyList()
    val transactions = inventoryData?.transactions ?: emptyList()
    val lowStockItems = remember(items) { items.filter { it.lowStock } }

    val filteredItems = remember(items, searchQuery, categoryFilter) {
        val q = searchQuery.trim().lowercase()
        items.filter { item ->
            val catOk = when (categoryFilter) {
                "ALL" -> true
                "LOW" -> item.lowStock || item.stockQuantity <= 0
                else -> item.category.equals(categoryFilter, ignoreCase = true)
            }
            val searchOk = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.courtName.lowercase().contains(q) ||
                item.sku.lowercase().contains(q) ||
                item.barcode.lowercase().contains(q) ||
                item.supplierName.lowercase().contains(q) ||
                item.category.lowercase().contains(q)
            catOk && searchOk
        }
    }

    val filteredTransactions = remember(transactions, txTypeFilter) {
        when (txTypeFilter) {
            "ALL" -> transactions
            "TRANSFER" -> transactions.filter { it.type == "TRANSFER_OUT" || it.type == "TRANSFER_IN" }
            "WASTAGE" -> transactions.filter { it.type == "DAMAGE" || it.type == "EXPIRED" || it.type == "INTERNAL_USE" }
            else -> transactions.filter { it.type == txTypeFilter }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedBranchId == null,
                        onClick = { onSelectBranch(null) },
                        label = { Text("Tất cả chi nhánh", fontSize = 12.5.sp) }
                    )
                }
                items(branches) { b ->
                    FilterChip(
                        selected = selectedBranchId == b.courtId,
                        onClick = { onSelectBranch(b.courtId) },
                        label = { Text(b.courtName, fontSize = 12.5.sp) }
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProKpiCard(
                    icon = Icons.Rounded.Inventory2,
                    iconTint = ProColors.Primary,
                    iconBg = ProColors.PrimarySoft,
                    value = "${inventoryData?.totalItems ?: 0} / ${inventoryData?.totalUnits ?: 0}",
                    label = "Mặt hàng / Tồn",
                    modifier = Modifier.weight(1f)
                )
                ProKpiCard(
                    icon = Icons.Rounded.Payments,
                    iconTint = ProColors.Emerald,
                    iconBg = ProColors.EmeraldBg,
                    value = "${(inventoryData?.totalStockValue ?: 0L).vndLong()} đ",
                    label = "Giá trị tồn (vốn)",
                    modifier = Modifier.weight(1f)
                )
            }
        }

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
                        "STOCK" to "Tồn kho (${items.size})",
                        "STATS" to "Thống kê",
                        "HISTORY" to "Nhập/Xuất (${transactions.size})",
                    ).forEach { (code, label) ->
                        val active = subMode == code
                        Surface(
                            color = if (active) ProColors.Primary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { subMode = code }
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else ProColors.SubInk,
                                fontSize = 12.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }

        when (subMode) {
            "STATS" -> {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "ALL" to "Tất cả",
                            "TODAY" to "Hôm nay",
                            "WEEK" to "Tuần này",
                            "MONTH" to "Tháng này",
                        ).forEach { (code, label) ->
                            item {
                                FilterChip(
                                    selected = statsPeriod == code,
                                    onClick = { onSelectStatsPeriod(code) },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
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
                                    selected = statsFocus == code,
                                    onClick = { statsFocus = code },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    InventoryFinancialHeroCard(stats = inventoryStats)
                }

                if (statsFocus == "ALL") {
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
                                value = "${(inventoryStats?.totalImportSpend ?: 0L).vndLong()} đ",
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
                                value = "${(inventoryStats?.totalWastageCost ?: 0L).vndLong()} đ",
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
                if ((statsFocus == "ALL" || statsFocus == "CATEGORY") && cats.isNotEmpty()) {
                    item {
                        ProSectionHeader("Phân bổ tồn kho & doanh thu theo nhóm")
                    }
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
                                                "Vốn ${c.stockValue.vndLong()} đ",
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
                                            "Đã bán: ${c.soldUnits} sp · Doanh thu: ${c.revenue.vndLong()} đ",
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
                if (statsFocus == "ALL" || statsFocus == "TOP") {
                    item {
                        ProSectionHeader("Top sản phẩm bán chạy nhất")
                    }
                    if (topSelling.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "Chưa có dữ liệu bán hàng trong kỳ",
                                subtitle = "Khi nhân viên bán hàng qua quầy POS, thống kê top bán chạy sẽ hiển thị tại đây."
                            )
                        }
                    } else {
                        items(topSelling.withIndex().toList(), key = { it.value.itemId }) { (idx, top) ->
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
                                            "${top.revenue.vndLong()} đ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = ProColors.Primary
                                        )
                                        Text(
                                            "Lãi +${top.profit.vndLong()} đ",
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
                if ((statsFocus == "ALL" || statsFocus == "BRANCH") && branchStats.isNotEmpty()) {
                    item {
                        ProSectionHeader("Hiệu quả kho theo chi nhánh")
                    }
                    items(branchStats, key = { it.courtId }) { br ->
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
                                    Text("Vốn lưu kho: ${br.stockValue.vndLong()} đ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.Ink)
                                }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ProColors.Surface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Doanh thu: ${br.revenue.vndLong()} đ", fontSize = 12.sp, color = ProColors.Primary, fontWeight = FontWeight.SemiBold)
                                    Text("Lãi gộp: +${br.profit.vndLong()} đ", fontSize = 12.sp, color = ProColors.Emerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                val suggestions = inventoryStats?.reorderSuggestions ?: emptyList()
                if (statsFocus == "ALL" || statsFocus == "REORDER") {
                    if (suggestions.isNotEmpty()) {
                        item {
                            ProSectionHeader("Gợi ý nhập hàng bổ sung (${suggestions.size})")
                        }
                        items(suggestions, key = { "sug_${it.itemId}" }) { sug ->
                            val matchedItem = items.firstOrNull { it.id == sug.itemId }
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
                                            "Đề xuất nhập +${sug.suggestedQty} ${sug.unit} (dự kiến ${sug.estimatedCost.vndLong()} đ)",
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
                    } else if (statsFocus == "REORDER") {
                        item {
                            EmptyStateCard(
                                title = "Tất cả mặt hàng đều đủ tồn kho",
                                subtitle = "Chưa có sản phẩm nào chạm ngưỡng cảnh báo nhập thêm."
                            )
                        }
                    }
                }
            }

            "STOCK" -> {
                if (lowStockItems.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ProColors.RoseBg),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ProColors.Rose.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = ProColors.Rose, modifier = Modifier.size(18.dp))
                                    Text(
                                        "Cảnh báo sắp hết hàng (${lowStockItems.size} món)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = ProColors.Rose
                                    )
                                }
                                lowStockItems.take(4).forEach { low ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "• ${low.name} (${low.courtName}): còn ${low.stockQuantity} ${low.unit}",
                                            fontSize = 12.5.sp,
                                            color = ProColors.Ink,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        TextButton(
                                            onClick = { onRestockItem(low) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("+ Nhập", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProColors.Primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm tên, SKU, mã vạch, nhà cung cấp...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ProColors.Muted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Xóa")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ProColors.Primary,
                            unfocusedBorderColor = ProColors.Border,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "ALL" to "Tất cả",
                            "DRINK" to "Đồ uống",
                            "EQUIPMENT" to "Dụng cụ",
                            "ACCESSORY" to "Phụ kiện",
                            "LOW" to "Sắp hết (${lowStockItems.size})",
                        ).forEach { (code, label) ->
                            item {
                                FilterChip(
                                    selected = categoryFilter == code,
                                    onClick = { categoryFilter = code },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Danh sách mặt hàng (${filteredItems.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = ProColors.Ink
                        )
                        Button(
                            onClick = onAddItem,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Thêm món", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (filteredItems.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Chưa có mặt hàng phù hợp",
                            subtitle = "Thử đổi từ khóa tìm kiếm hoặc bấm '+ Thêm' để nhập mặt hàng mới."
                        )
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        val stockColor = when {
                            item.stockQuantity <= 0 -> ProColors.Rose
                            item.lowStock -> ProColors.Amber
                            else -> ProColors.Emerald
                        }
                        val stockBg = when {
                            item.stockQuantity <= 0 -> ProColors.RoseBg
                            item.lowStock -> ProColors.AmberBg
                            else -> ProColors.EmeraldBg
                        }
                        val stockLabel = when {
                            item.stockQuantity <= 0 -> "Hết hàng"
                            item.lowStock -> "Sắp hết: ${item.stockQuantity} ${item.unit}"
                            else -> "Còn ${item.stockQuantity} ${item.unit}"
                        }
                        val catLabel = when (item.category) {
                            "EQUIPMENT" -> "Dụng cụ"
                            "ACCESSORY" -> "Phụ kiện"
                            else -> "Đồ uống"
                        }
                        val unitProfit = item.sellPrice - item.importPrice

                        ProCard {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = ProColors.Ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${item.courtName} · $catLabel",
                                            fontSize = 12.sp,
                                            color = ProColors.SubInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    ProStatusDotPill(stockLabel, stockColor, stockColor, stockBg)
                                }

                                if (item.sku.isNotBlank() || item.supplierName.isNotBlank()) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (item.sku.isNotBlank()) {
                                            Row(
                                                Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(ProColors.PrimarySoft)
                                                    .padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = ProColors.Primary, modifier = Modifier.size(13.dp))
                                                Text(item.sku, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProColors.Primary)
                                            }
                                        }
                                        if (item.supplierName.isNotBlank()) {
                                            Text(
                                                "NCC: ${item.supplierName}",
                                                fontSize = 11.5.sp,
                                                color = ProColors.Muted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ProColors.Surface)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Nhập: ${item.importPrice.vnd()} đ", fontSize = 12.sp, color = ProColors.SubInk)
                                    Text("Bán: ${item.sellPrice.vnd()} đ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProColors.Primary)
                                    if (item.category == "DRINK" || item.category == "ACCESSORY") {
                                        Text(
                                            "Lãi: ${if (unitProfit >= 0) "+" else ""}${unitProfit.vnd()} đ",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (unitProfit >= 0) ProColors.Emerald else ProColors.Rose
                                        )
                                    }
                                }

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onRestockItem(item) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Nhập kho", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = { onTransferItem(item) },
                                        enabled = item.stockQuantity > 0 && branches.size > 1,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Chuyển kho", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onWastageItem(item) },
                                        enabled = item.stockQuantity > 0,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ProColors.Rose)
                                    ) {
                                        Icon(Icons.Rounded.ReportProblem, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Xuất hủy", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    OutlinedButton(
                                        onClick = { onAdjustItem(item) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Kiểm kê", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    IconButton(onClick = { onEditItem(item) }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Sửa", tint = ProColors.SubInk, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeleteItem(item) }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Xóa", tint = ProColors.Rose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                item {
                    ProSectionHeader("Nhật ký biến động kho")
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "ALL" to "Tất cả",
                            "IMPORT" to "Nhập kho",
                            "SALE" to "Bán POS",
                            "TRANSFER" to "Chuyển kho",
                            "WASTAGE" to "Xuất hủy",
                            "ADJUST" to "Kiểm kê",
                        ).forEach { (code, label) ->
                            item {
                                FilterChip(
                                    selected = txTypeFilter == code,
                                    onClick = { txTypeFilter = code },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
                if (filteredTransactions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "Chưa có giao dịch kho phù hợp",
                            subtitle = "Các lần nhập, bán POS, chuyển kho, xuất hủy hoặc kiểm kê sẽ tự động ghi lại tại đây."
                        )
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        val (typeLabel, typeColor, typeBg) = when (tx.type) {
                            "IMPORT" -> Triple("Nhập kho", ProColors.Emerald, ProColors.EmeraldBg)
                            "SALE" -> Triple("Bán POS", ProColors.Primary, ProColors.PrimarySoft)
                            "TRANSFER_OUT" -> Triple("Chuyển đi", ProColors.Amber, ProColors.AmberBg)
                            "TRANSFER_IN" -> Triple("Nhận chuyển", ProColors.Sky, ProColors.SkyBg)
                            "DAMAGE" -> Triple("Hàng hỏng", ProColors.Rose, ProColors.RoseBg)
                            "EXPIRED" -> Triple("Hết hạn", ProColors.Rose, ProColors.RoseBg)
                            "INTERNAL_USE" -> Triple("Dùng nội bộ", ProColors.Amber, ProColors.AmberBg)
                            else -> Triple("Kiểm kê", ProColors.Amber, ProColors.AmberBg)
                        }
                        val deltaStr = if (tx.quantityDelta > 0) "+${tx.quantityDelta}" else "${tx.quantityDelta}"
                        ProCard {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(typeBg)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(deltaStr, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = typeColor)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            tx.itemName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ProColors.Ink,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        ProStatusDotPill(typeLabel, typeColor, typeColor, typeBg)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text("Tồn sau GD: ${tx.stockAfter} · ${tx.createdBy}", fontSize = 12.sp, color = ProColors.SubInk)
                                    if (tx.note.isNotBlank()) {
                                        Text(tx.note, fontSize = 11.5.sp, color = ProColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryFinancialHeroCard(stats: OwnerInventoryStatsData?) {
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
                            "${profit.vndLong()} đ",
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
                        Text("${revenue.vndLong()} đ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Giá vốn (COGS)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                        Text("${cogs.vndLong()} đ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFDE68A))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Dự thu hàng tồn", fontSize = 11.sp, color = Color.White.copy(alpha = 0.78f))
                        Text("${potentialRev.vndLong()} đ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                    }
                }
            }
        }
    }
}

@Composable
internal fun RestockInventoryDialog(
    item: InventoryItemModel,
    initialQuantity: Int = 24,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, unitCost: Int, note: String, supplierName: String) -> Unit,
) {
    var qtyText by remember(item.id, initialQuantity) { mutableStateOf(initialQuantity.toString()) }
    var costText by remember(item.id) { mutableStateOf(item.importPrice.toString()) }
    var supplierName by remember(item.id) { mutableStateOf(item.supplierName) }
    var note by remember(item.id) { mutableStateOf("Nhập thêm hàng vào kho") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Nhập kho: ${item.name}", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tồn hiện tại: ${item.stockQuantity} ${item.unit} · ${item.courtName}", fontSize = 12.5.sp, color = ProColors.SubInk)
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter(Char::isDigit) },
                    label = { Text("Số lượng nhập thêm (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter(Char::isDigit) },
                    label = { Text("Giá vốn nhập (đ / ${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Nhà cung cấp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú phiếu nhập") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val qty = qtyText.toIntOrNull() ?: 0
            val cost = costText.toIntOrNull() ?: item.importPrice
            TextButton(
                enabled = qty > 0,
                onClick = { onConfirm(qty, cost, note.trim(), supplierName.trim()) }
            ) {
                Text("Xác nhận nhập", color = EsColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = EsColors.Body) }
        }
    )
}

@Composable
internal fun WastageInventoryDialog(
    item: InventoryItemModel,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, reasonType: String, note: String) -> Unit,
) {
    var qtyText by remember { mutableStateOf("1") }
    var reasonType by remember { mutableStateOf("DAMAGE") }
    var note by remember { mutableStateOf("Hư hỏng / rơi vỡ tại quầy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Xuất hủy / Tiêu hao: ${item.name}", fontWeight = FontWeight.Bold, color = ProColors.Rose, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tồn hiện tại: ${item.stockQuantity} ${item.unit} · Giá vốn: ${item.importPrice.vnd()} đ", fontSize = 12.5.sp, color = ProColors.SubInk)
                Text("Loại xuất kho", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.SubInk)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "DAMAGE" to "Hỏng hóc / Vỡ",
                        "EXPIRED" to "Hết hạn",
                        "INTERNAL_USE" to "Dùng nội bộ",
                    ).forEach { (code, label) ->
                        item {
                            FilterChip(
                                selected = reasonType == code,
                                onClick = {
                                    reasonType = code
                                    note = when (code) {
                                        "EXPIRED" -> "Hàng quá hạn sử dụng"
                                        "INTERNAL_USE" -> "Nhân viên dùng / Tài trợ giải sân"
                                        else -> "Hư hỏng / rơi vỡ tại quầy"
                                    }
                                },
                                label = { Text(label, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter(Char::isDigit) },
                    label = { Text("Số lượng xuất (tối đa ${item.stockQuantity})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Lý do chi tiết") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val qty = qtyText.toIntOrNull() ?: 0
            TextButton(
                enabled = qty in 1..item.stockQuantity,
                onClick = { onConfirm(qty, reasonType, note.trim()) }
            ) {
                Text("Xác nhận xuất hủy", color = ProColors.Rose, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = EsColors.Body) }
        }
    )
}

@Composable
internal fun TransferInventoryDialog(
    item: InventoryItemModel,
    branches: List<BranchRevenueItem>,
    onDismiss: () -> Unit,
    onConfirm: (targetCourtId: String, quantity: Int, note: String) -> Unit,
) {
    val targetBranches = remember(branches, item.courtId) {
        branches.filter { it.courtId != item.courtId }
    }
    var targetCourtId by remember(targetBranches) {
        mutableStateOf(targetBranches.firstOrNull()?.courtId ?: "")
    }
    var qtyText by remember { mutableStateOf(minOf(10, item.stockQuantity).coerceAtLeast(1).toString()) }
    var note by remember { mutableStateOf("Điều chuyển bổ sung hàng giữa chi nhánh") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Điều chuyển kho: ${item.name}", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Từ kho: ${item.courtName} (còn ${item.stockQuantity} ${item.unit})", fontSize = 12.5.sp, color = ProColors.SubInk)
                if (targetBranches.isEmpty()) {
                    Text("Chưa có chi nhánh khác để điều chuyển.", color = ProColors.Rose, fontSize = 13.sp)
                } else {
                    Text("Chuyển đến chi nhánh:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.SubInk)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(targetBranches) { b ->
                            FilterChip(
                                selected = targetCourtId == b.courtId,
                                onClick = { targetCourtId = b.courtId },
                                label = { Text(b.courtName, fontSize = 12.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter(Char::isDigit) },
                        label = { Text("Số lượng chuyển (${item.unit}, tối đa ${item.stockQuantity})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú điều chuyển") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            val qty = qtyText.toIntOrNull() ?: 0
            TextButton(
                enabled = targetCourtId.isNotBlank() && qty in 1..item.stockQuantity,
                onClick = { onConfirm(targetCourtId, qty, note.trim()) }
            ) {
                Text("Chuyển kho ngay", color = EsColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = EsColors.Body) }
        }
    )
}

@Composable
internal fun AdjustInventoryDialog(
    item: InventoryItemModel,
    onDismiss: () -> Unit,
    onConfirm: (newStock: Int, note: String) -> Unit,
) {
    var stockText by remember { mutableStateOf(item.stockQuantity.toString()) }
    var note by remember { mutableStateOf("Kiểm kê thực tế tại quầy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Kiểm kê: ${item.name}", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Số lượng trên hệ thống: ${item.stockQuantity} ${item.unit}", fontSize = 12.5.sp, color = ProColors.SubInk)
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it.filter(Char::isDigit) },
                    label = { Text("Số lượng thực tế đếm được") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Lý do điều chỉnh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val ns = stockText.toIntOrNull()
            TextButton(
                enabled = ns != null && ns >= 0,
                onClick = { if (ns != null) onConfirm(ns, note.trim()) }
            ) {
                Text("Lưu kiểm kê", color = EsColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = EsColors.Body) }
        }
    )
}

@Composable
internal fun InventoryItemFormDialog(
    existing: InventoryItemModel?,
    branches: List<BranchRevenueItem>,
    defaultBranchId: String?,
    onDismiss: () -> Unit,
    onSave: (
        courtId: String,
        name: String,
        category: String,
        unit: String,
        importPrice: Int,
        sellPrice: Int,
        stockQuantity: Int,
        minAlert: Int,
        sku: String,
        barcode: String,
        supplierName: String,
    ) -> Unit,
) {
    var selectedCourtId by remember {
        mutableStateOf(existing?.courtId ?: defaultBranchId ?: branches.firstOrNull()?.courtId ?: "court_thanh_phat")
    }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "DRINK") }
    var unit by remember { mutableStateOf(existing?.unit ?: "chai") }
    var importPriceText by remember { mutableStateOf((existing?.importPrice ?: 8000).toString()) }
    var sellPriceText by remember { mutableStateOf((existing?.sellPrice ?: 15000).toString()) }
    var stockText by remember { mutableStateOf((existing?.stockQuantity ?: 24).toString()) }
    var minAlertText by remember { mutableStateOf((existing?.minAlertThreshold ?: 10).toString()) }
    var sku by remember { mutableStateOf(existing?.sku ?: "") }
    var barcode by remember { mutableStateOf(existing?.barcode ?: "") }
    var supplierName by remember { mutableStateOf(existing?.supplierName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                if (existing == null) "Thêm mặt hàng vào kho" else "Sửa thông tin mặt hàng",
                fontWeight = FontWeight.Bold,
                color = EsColors.Title,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (existing == null && branches.isNotEmpty()) {
                    Text("Chi nhánh", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.SubInk)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(branches) { b ->
                            FilterChip(
                                selected = selectedCourtId == b.courtId,
                                onClick = { selectedCourtId = b.courtId },
                                label = { Text(b.courtName, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên mặt hàng (vd: Nước tăng lực Sting)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DRINK" to "Đồ uống", "EQUIPMENT" to "Dụng cụ", "ACCESSORY" to "Phụ kiện").forEach { (code, label) ->
                        FilterChip(
                            selected = category == code,
                            onClick = { category = code },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Đơn vị") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minAlertText,
                        onValueChange = { minAlertText = it.filter(Char::isDigit) },
                        label = { Text("Cảnh báo <=") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importPriceText,
                        onValueChange = { importPriceText = it.filter(Char::isDigit) },
                        label = { Text("Giá nhập (đ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = { sellPriceText = it.filter(Char::isDigit) },
                        label = { Text("Giá bán (đ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (existing == null) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter(Char::isDigit) },
                        label = { Text("Số lượng tồn ban đầu") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Mã SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Mã vạch") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Nhà cung cấp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val imp = importPriceText.toIntOrNull() ?: 0
            val sell = sellPriceText.toIntOrNull() ?: 0
            val stk = stockText.toIntOrNull() ?: 0
            val minA = minAlertText.toIntOrNull() ?: 10
            TextButton(
                enabled = name.trim().isNotEmpty() && unit.trim().isNotEmpty(),
                onClick = {
                    onSave(
                        selectedCourtId, name.trim(), category, unit.trim(),
                        imp, sell, stk, minA, sku.trim(), barcode.trim(), supplierName.trim()
                    )
                }
            ) {
                Text(if (existing == null) "Thêm vào kho" else "Lưu thay đổi", color = EsColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = EsColors.Body) }
        }
    )
}
