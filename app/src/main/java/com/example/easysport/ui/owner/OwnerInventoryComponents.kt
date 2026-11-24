package com.example.easysport.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.BranchRevenueItem
import com.example.easysport.data.model.InventoryItemModel
import com.example.easysport.data.model.InventoryTransactionModel
import com.example.easysport.data.model.OwnerInventoryData
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.ProCard
import com.example.easysport.ui.common.ProColors
import com.example.easysport.ui.common.ProKpiCard
import com.example.easysport.ui.common.ProSectionHeader
import com.example.easysport.ui.common.ProStatusDotPill
import com.example.easysport.ui.common.vnd
import com.example.easysport.ui.staff.EmptyStateCard

@Composable
internal fun OwnerInventoryTab(
    inventoryData: OwnerInventoryData?,
    branches: List<BranchRevenueItem>,
    selectedBranchId: String?,
    onSelectBranch: (String?) -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (InventoryItemModel) -> Unit,
    onRestockItem: (InventoryItemModel) -> Unit,
    onAdjustItem: (InventoryItemModel) -> Unit,
    onDeleteItem: (InventoryItemModel) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var subMode by remember { mutableStateOf("STOCK") } // STOCK | HISTORY

    val items = inventoryData?.items ?: emptyList()
    val transactions = inventoryData?.transactions ?: emptyList()
    val lowStockItems = remember(items) { items.filter { it.lowStock } }

    val filteredItems = remember(items, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) items
        else items.filter {
            it.name.lowercase().contains(q) ||
                it.courtName.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
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
                    value = "${(inventoryData?.totalStockValue ?: 0L).toInt().vnd()} đ",
                    label = "Giá trị tồn (vốn)",
                    modifier = Modifier.weight(1f)
                )
            }
        }

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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = subMode == "STOCK",
                        onClick = { subMode = "STOCK" },
                        label = { Text("Tồn kho (${items.size})") }
                    )
                    FilterChip(
                        selected = subMode == "HISTORY",
                        onClick = { subMode = "HISTORY" },
                        label = { Text("Nhập / Xuất (${transactions.size})") }
                    )
                }
                Button(
                    onClick = onAddItem,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thêm món", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (subMode == "STOCK") {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm tên nước, áo bib, chi nhánh...") },
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

            if (filteredItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Chưa có mặt hàng trong kho",
                        subtitle = "Bấm '+ Thêm món' để thêm nước uống hoặc dụng cụ cho chi nhánh."
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

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ProColors.Surface)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Giá nhập: ${item.importPrice.vnd()} đ", fontSize = 12.sp, color = ProColors.SubInk)
                                Text("Giá bán: ${item.sellPrice.vnd()} đ", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ProColors.Primary)
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onRestockItem(item) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Nhập kho", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { onAdjustItem(item) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Kiểm kê", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
        } else {
            item {
                ProSectionHeader("Lịch sử nhập / xuất kho gần đây")
            }
            if (transactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Chưa có giao dịch kho",
                        subtitle = "Các lần nhập hàng, bán POS hoặc kiểm kê sẽ tự động ghi lại tại đây."
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    val (typeLabel, typeColor, typeBg) = when (tx.type) {
                        "IMPORT" -> Triple("Nhập kho", ProColors.Emerald, ProColors.EmeraldBg)
                        "SALE" -> Triple("Bán POS", ProColors.Primary, ProColors.PrimarySoft)
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
                                    Text(tx.itemName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ProColors.Ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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

@Composable
internal fun RestockInventoryDialog(
    item: InventoryItemModel,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, unitCost: Int, note: String) -> Unit,
) {
    var qtyText by remember { mutableStateOf("24") }
    var costText by remember { mutableStateOf(item.importPrice.toString()) }
    var note by remember { mutableStateOf("Nhập thêm hàng vào kho") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Nhập kho: ${item.name}", fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                onClick = { onConfirm(qty, cost, note.trim()) }
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
                    label = { Text("Lý do điều chỉnh (vd: vỡ, hết hạn...)") },
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        label = { Text("Đơn vị (chai/bộ)") },
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
                    onSave(selectedCourtId, name.trim(), category, unit.trim(), imp, sell, stk, minA)
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
