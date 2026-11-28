package com.example.easysport.ui.recurring

import android.net.Uri
import android.os.SystemClock
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.easysport.data.model.RecurringCheckoutResult
import com.example.easysport.data.remote.PaymentApiClient
import com.example.easysport.data.remote.RecurringApiClient
import com.example.easysport.data.repository.BookingStore
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.common.EsTopBar
import com.example.easysport.ui.common.vnd
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

@Composable
fun RecurringPaymentScreen(
    packageId: String,
    initialResult: RecurringCheckoutResult? = null,
    onBack: () -> Unit,
    onPaymentSuccess: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var holdId by remember { mutableStateOf(initialResult?.holdId) }
    var packageStatus by remember { mutableStateOf(initialResult?.status ?: "AWAITING_PAYMENT") }
    var holdPhase by remember { mutableStateOf(initialResult?.holdStatus ?: "AWAITING_PAYMENT") }
    var invoiceNumber by remember { mutableStateOf(initialResult?.orderInvoiceNumber) }
    var checkoutUrl by remember { mutableStateOf(initialResult?.checkoutUrl) }
    var totalPrice by remember { mutableIntStateOf(initialResult?.totalPrice ?: 0) }
    var paymentMethod by remember { mutableStateOf(initialResult?.paymentMethod ?: "QR_TRANSFER") }
    var hasLaunchedCheckout by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var paymentDeadlineAtMs by remember { mutableStateOf<Long?>(null) }
    var verificationDeadlineAtMs by remember { mutableStateOf<Long?>(null) }
    var nowMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    fun syncDeadlines(pDeadIso: String?, vDeadIso: String?, sNowIso: String?) {
        if (pDeadIso.isNullOrBlank()) return
        try {
            val sNow = if (!sNowIso.isNullOrBlank()) OffsetDateTime.parse(sNowIso).toInstant() else Instant.now()
            val pEnd = OffsetDateTime.parse(pDeadIso).toInstant()
            val vEnd = if (!vDeadIso.isNullOrBlank()) OffsetDateTime.parse(vDeadIso).toInstant() else pEnd.plusSeconds(60)
            val base = SystemClock.elapsedRealtime()
            paymentDeadlineAtMs = base + Duration.between(sNow, pEnd).toMillis().coerceAtLeast(0L)
            verificationDeadlineAtMs = base + Duration.between(sNow, vEnd).toMillis().coerceAtLeast(0L)
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(initialResult) {
        if (initialResult != null) {
            syncDeadlines(initialResult.paymentDeadline, initialResult.verificationDeadline, initialResult.serverNow)
        }
    }

    fun openCheckoutUrl(pathOrUrl: String) {
        val fullUrl = PaymentApiClient.resolveUrl(pathOrUrl)
        hasLaunchedCheckout = true
        runCatching {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(fullUrl))
        }.onFailure {
            errorMessage = "Không mở được trình duyệt SePay: ${it.localizedMessage}"
        }
    }

    fun checkStatus(isManual: Boolean) {
        scope.launch {
            isChecking = true
            if (isManual) errorMessage = null
            RecurringApiClient.getPackagePaymentStatus(packageId).onSuccess { st ->
                packageStatus = st.bookingStatus
                if (st.holdStatus != null) holdPhase = st.holdStatus
                if (st.orderInvoiceNumber != null) invoiceNumber = st.orderInvoiceNumber
                if (st.checkoutUrl != null) checkoutUrl = st.checkoutUrl
                if (st.totalPrice > 0) totalPrice = st.totalPrice
                paymentMethod = st.paymentMethod
                syncDeadlines(st.paymentDeadline, st.verificationDeadline, st.serverNow)

                when {
                    st.isPaid || st.bookingStatus == "ACTIVE" -> {
                        BookingStore.refresh()
                        onPaymentSuccess(packageId)
                    }
                    st.paymentStatus == "REVIEW_REQUIRED" || st.bookingStatus == "REVIEW_REQUIRED" -> {
                        holdPhase = "REVIEW_REQUIRED"
                        statusMessage = "Giao dịch thanh toán sau khi hết giữ chỗ nên được chuyển sang Cần đối soát. 12 buổi không bị chiếm giữ trái phép."
                    }
                    st.isExpired || st.holdStatus == "EXPIRED" || st.bookingStatus == "EXPIRED" -> {
                        holdPhase = "EXPIRED"
                        statusMessage = "Hết thời gian giữ chỗ 12 buổi. Vui lòng chọn lại ngày bắt đầu."
                    }
                }
            }.onFailure {
                if (isManual) errorMessage = it.localizedMessage ?: "Không kiểm tra được trạng thái"
            }
            isChecking = false
        }
    }

    LaunchedEffect(Unit) { checkStatus(isManual = false) }

    LaunchedEffect(packageStatus, holdPhase, hasLaunchedCheckout) {
        while (packageStatus != "ACTIVE" && holdPhase != "EXPIRED" && holdPhase != "REVIEW_REQUIRED") {
            delay(if (hasLaunchedCheckout || holdPhase == "VERIFYING_PAYMENT") 2_500L else 5_000L)
            nowMs = SystemClock.elapsedRealtime()
            checkStatus(isManual = false)
        }
    }

    LaunchedEffect(packageStatus, holdPhase) {
        while (packageStatus != "ACTIVE" && holdPhase != "EXPIRED" && holdPhase != "REVIEW_REQUIRED") {
            delay(1_000L)
            nowMs = SystemClock.elapsedRealtime()
            val pEnd = paymentDeadlineAtMs
            val vEnd = verificationDeadlineAtMs
            if (pEnd != null && vEnd != null) {
                holdPhase = when {
                    nowMs < pEnd -> "AWAITING_PAYMENT"
                    nowMs < vEnd -> "VERIFYING_PAYMENT"
                    else -> "EXPIRED"
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && packageStatus != "ACTIVE") {
                checkStatus(isManual = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val remainingSeconds = remember(nowMs, paymentDeadlineAtMs, verificationDeadlineAtMs, holdPhase) {
        val target = if (holdPhase == "VERIFYING_PAYMENT") verificationDeadlineAtMs else paymentDeadlineAtMs
        if (target == null) 300 else ((target - nowMs).coerceAtLeast(0L) / 1000L).toInt()
    }
    val countdownText = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60)

    fun handleCancelBack() {
        if (packageStatus != "ACTIVE" && holdPhase == "AWAITING_PAYMENT") {
            scope.launch {
                RecurringApiClient.releaseHold(packageId)
                onBack()
            }
        } else {
            onBack()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            EsTopBar(title = "Thanh toán lịch cố định", onBack = { handleCancelBack() })

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                Surface(shape = RoundedCornerShape(16.dp), color = EsColors.Surface) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(initialResult?.packageTitle ?: "3 tháng · 12 buổi · 1 buổi/tuần", fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = EsColors.Title)
                        if (initialResult != null && initialResult.startDate.isNotBlank()) {
                            Text("Bắt đầu: ${initialResult.weekdayLabel}, ${initialResult.startDate} (${initialResult.timeRange})", fontSize = 13.sp, color = EsColors.Body)
                        }
                        if (!invoiceNumber.isNullOrBlank()) {
                            Text("Mã hóa đơn: $invoiceNumber", fontSize = 12.5.sp, color = EsColors.Body)
                        }
                        HorizontalDivider(color = EsColors.Border, modifier = Modifier.padding(vertical = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tổng thanh toán 12 buổi", color = EsColors.Body, fontSize = 13.5.sp)
                            Text("${totalPrice.vnd()} đ", color = EsColors.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }

                val isSuccess = packageStatus == "ACTIVE"
                val isVerifying = holdPhase == "VERIFYING_PAYMENT"
                val isExpired = holdPhase == "EXPIRED" || packageStatus == "EXPIRED"
                val isReview = holdPhase == "REVIEW_REQUIRED" || packageStatus == "REVIEW_REQUIRED"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isSuccess -> EsColors.SuccessBg
                            isExpired -> Color(0xFFFEF2F2)
                            isReview || isVerifying -> Color(0xFFFFFBEB)
                            else -> EsColors.WarningBg
                        }
                    )
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when {
                                    isSuccess -> Icons.Rounded.Check
                                    isExpired -> Icons.Rounded.Close
                                    isVerifying -> Icons.Outlined.HourglassBottom
                                    isReview -> Icons.Outlined.Info
                                    else -> Icons.Outlined.HourglassTop
                                },
                                contentDescription = null,
                                tint = when {
                                    isSuccess -> EsColors.Success
                                    isExpired -> EsColors.Danger
                                    else -> EsColors.Warning
                                }
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    isSuccess -> "Đã xác nhận gói 12 buổi!"
                                    isExpired -> "Hết thời gian giữ chỗ"
                                    isReview -> "Giao dịch cần đối soát"
                                    isVerifying -> "Đang xác minh giao dịch ($countdownText)"
                                    else -> "Đang giữ trọn 12 buổi ($countdownText)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = EsColors.Title
                            )
                            Text(
                                statusMessage ?: when {
                                    isSuccess -> if (paymentMethod == "WALLET") "Đã thanh toán từ Ví EasySport và kích hoạt đủ 12 buổi." else "SePay đã xác thực giao dịch và kích hoạt đủ 12 buổi."
                                    isVerifying -> "Đang chờ xác minh thêm tối đa 1 phút. 12 buổi vẫn được giữ an toàn cho bạn."
                                    isExpired -> "Chỗ giữ 12 buổi đã tự động giải phóng. Vui lòng quay lại để kiểm tra lịch mới."
                                    else -> "Toàn bộ 12 buổi đang được giữ trong 5 phút thanh toán + 1 phút xác minh."
                                },
                                fontSize = 12.5.sp,
                                color = EsColors.Body,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = EsColors.Danger, fontSize = 13.sp)
                }
            }

            Surface(color = Color.White, shadowElevation = 10.dp) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        packageStatus == "ACTIVE" -> {
                            EsPrimaryButton(
                                text = "Xem lịch cố định của tôi",
                                onClick = { onPaymentSuccess(packageId) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        holdPhase == "EXPIRED" || holdPhase == "REVIEW_REQUIRED" -> {
                            EsPrimaryButton(
                                text = "Quay lại",
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        holdPhase == "VERIFYING_PAYMENT" -> {
                            OutlinedButton(
                                onClick = { checkStatus(isManual = true) },
                                enabled = !isChecking,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Kiểm tra xác minh")
                            }
                        }
                        else -> {
                            if (hasLaunchedCheckout) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { checkStatus(isManual = true) },
                                        enabled = !isChecking,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, EsColors.Border)
                                    ) {
                                        Text("Kiểm tra")
                                    }
                                    EsPrimaryButton(
                                        text = "Mở lại SePay",
                                        trailingIcon = Icons.Rounded.OpenInBrowser,
                                        onClick = { checkoutUrl?.let { openCheckoutUrl(it) } },
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                            } else {
                                EsPrimaryButton(
                                    text = "Mở cổng thanh toán SePay",
                                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                                    onClick = {
                                        val url = checkoutUrl
                                        if (!url.isNullOrBlank()) openCheckoutUrl(url) else checkStatus(isManual = true)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
