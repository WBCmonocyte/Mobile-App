package com.example.easysport.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.WalletSummary
import com.example.easysport.data.model.WalletTransactionItem
import com.example.easysport.data.remote.RecurringApiClient
import com.example.easysport.data.remote.WalletApiClient
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsTopBar
import com.example.easysport.ui.common.vnd
import kotlinx.coroutines.launch

@Composable
fun WalletScreen(
    onBack: () -> Unit,
    onOpenRecurring: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf<WalletSummary?>(null) }
    var availableCreditsCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            isLoading = true
            errorMsg = null
            WalletApiClient.getMyWallet()
                .onSuccess { summary = it }
                .onFailure { errorMsg = it.localizedMessage ?: "Không tải được Ví EasySport" }
            RecurringApiClient.getMyPackages()
                .onSuccess { list ->
                    availableCreditsCount = list.sumOf { it.pkg.availableCreditsCount }
                }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            EsTopBar(
                title = "Ví EasySport",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = { reload() }, enabled = !isLoading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Làm mới", tint = EsColors.Primary)
                    }
                }
            )

            if (isLoading && summary == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EsColors.Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorMsg != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = errorMsg ?: "",
                                    color = EsColors.Danger,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = EsColors.Primary)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Số dư khả dụng (VND)", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                                        Text(
                                            "${(summary?.balance ?: 0).vnd()} đ",
                                            color = Color.White,
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Dùng để thanh toán đơn đặt lẻ hoặc trọn gói lịch cố định 12 buổi khi số dư đủ.",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = EsColors.SurfaceTint,
                            border = BorderStroke(1.dp, EsColors.Primary.copy(alpha = 0.2f)),
                            onClick = onOpenRecurring
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = EsColors.Primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Buổi còn dư đặt bù: $availableCreditsCount buổi",
                                        color = EsColors.Title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Khác với số dư tiền Ví EasySport. Bấm để đổi slot đặt bù.",
                                        color = EsColors.Body,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = EsColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EsColors.Surface
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = EsColors.Body, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Tiền hoàn từ các buổi lịch cố định được Chủ sân phê duyệt sẽ cộng trực tiếp vào Ví này.",
                                    color = EsColors.Body,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Lịch sử biến động số dư",
                            color = EsColors.Title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    val txs = summary?.transactions ?: emptyList()
                    if (txs.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = EsColors.Surface
                            ) {
                                Text(
                                    "Chưa có giao dịch Ví EasySport nào.",
                                    color = EsColors.Body,
                                    fontSize = 13.5.sp,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(txs, key = { it.id }) { tx ->
                            WalletTxRow(tx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletTxRow(tx: WalletTransactionItem) {
    val isPositive = tx.isPositive
    val badgeColor = if (isPositive) EsColors.Success else EsColors.Danger
    val prefix = if (isPositive) "+" else "-"
    val absAmt = kotlin.math.abs(tx.amount).vnd()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, EsColors.Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(
                    tx.reason.ifBlank { tx.txType },
                    color = EsColors.Title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$prefix$absAmt đ",
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    tx.createdAt.replace("T", " ").take(16),
                    color = EsColors.Body,
                    fontSize = 12.sp
                )
                Text(
                    "Số dư sau GD: ${tx.balanceAfter.vnd()} đ",
                    color = EsColors.Body,
                    fontSize = 12.sp
                )
            }
        }
    }
}
