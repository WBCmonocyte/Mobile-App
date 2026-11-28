package com.example.easysport.ui.booking

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import com.example.easysport.data.model.*
import com.example.easysport.data.remote.PaymentApiClient
import com.example.easysport.data.remote.SlotConflictException
import com.example.easysport.data.repository.BookingStore
import com.example.easysport.data.repository.CourtStore
import com.example.easysport.data.repository.SampleData
import com.example.easysport.data.repository.UserStore
import com.example.easysport.ui.common.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@Composable
fun PaymentScreen(
    request: BookingRequest?,
    court: Court? = request?.let { req -> CourtStore.getCourtById(req.courtId) },
    onBack: () -> Unit,
    onPaymentSuccess: (Booking) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State lưu trữ qua vòng đời / xoay màn hình (rememberSaveable)
    var savedBookingId by rememberSaveable { mutableStateOf<String?>(null) }
    var savedCheckoutUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var savedInvoiceNumber by rememberSaveable { mutableStateOf<String?>(null) }
    var paymentStatusStr by rememberSaveable { mutableStateOf("READY") }
    var holdPhaseStr by rememberSaveable { mutableStateOf("READY") }
    var hasLaunchedCheckout by rememberSaveable { mutableStateOf(false) }

    var serverTotal by rememberSaveable { mutableStateOf<Int?>(null) }
    var serverDeposit by rememberSaveable { mutableStateOf<Int?>(null) }
    var serverRemaining by rememberSaveable { mutableStateOf<Int?>(null) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // Idempotency key duy nhất cho phiên thanh toán này
    var idempotencyKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }

    // Mốc thời gian đồng bộ với backend & Monotonic Clock
    var paymentDeadlineEpochMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var verificationDeadlineEpochMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var syncElapsedRealtime by rememberSaveable { mutableStateOf<Long?>(null) }
    var syncServerEpochMs by rememberSaveable { mutableStateOf<Long?>(null) }

    var remainingSeconds by rememberSaveable { mutableIntStateOf(300) }
    var isCountdownActive by rememberSaveable { mutableStateOf(false) }

    // Dialog khi bị tranh chấp khung giờ (HTTP 409)
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictDialogMessage by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isCheckingStatus by remember { mutableStateOf(false) }

    fun parseIsoToEpochMs(isoStr: String?): Long? {
        if (isoStr.isNullOrBlank()) return null
        return try {
            Instant.parse(isoStr).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    fun updateDeadlines(serverNowIso: String?, payDeadlineIso: String?, verDeadlineIso: String?, holdStatus: String?) {
        val sNow = parseIsoToEpochMs(serverNowIso) ?: return
        val pDead = parseIsoToEpochMs(payDeadlineIso) ?: return
        val vDead = parseIsoToEpochMs(verDeadlineIso) ?: return

        syncServerEpochMs = sNow
        syncElapsedRealtime = SystemClock.elapsedRealtime()
        paymentDeadlineEpochMs = pDead
        verificationDeadlineEpochMs = vDead
        if (holdStatus != null) {
            holdPhaseStr = holdStatus
        }
    }

    // Quản lý đếm ngược bằng Monotonic Clock kết hợp đồng bộ mốc thời gian ISO
    LaunchedEffect(isCountdownActive, paymentDeadlineEpochMs, verificationDeadlineEpochMs, syncElapsedRealtime, syncServerEpochMs) {
        if (!isCountdownActive) return@LaunchedEffect
        val pDead = paymentDeadlineEpochMs ?: return@LaunchedEffect
        val vDead = verificationDeadlineEpochMs ?: return@LaunchedEffect
        val sNow = syncServerEpochMs ?: return@LaunchedEffect
        val syncElapsed = syncElapsedRealtime ?: return@LaunchedEffect

        while (isCountdownActive && paymentStatusStr !in listOf("PAID", "DEPOSIT_PAID", "EXPIRED", "REVIEW_REQUIRED")) {
            val currentServerNow = sNow + (SystemClock.elapsedRealtime() - syncElapsed)
            if (currentServerNow < pDead) {
                holdPhaseStr = "AWAITING_PAYMENT"
                val sec = ((pDead - currentServerNow + 999) / 1000).coerceAtLeast(0).toInt()
                remainingSeconds = sec
            } else if (currentServerNow < vDead) {
                holdPhaseStr = "VERIFYING_PAYMENT"
                val sec = ((vDead - currentServerNow + 999) / 1000).coerceAtLeast(0).toInt()
                remainingSeconds = sec
            } else {
                holdPhaseStr = "VERIFYING_PAYMENT"
                statusMessage = "Đang lấy kết quả cuối cùng từ máy chủ..."
                remainingSeconds = 0
                isCountdownActive = false
                break
            }
            delay(1000)
        }
    }

    // Hàm mở SePay Sandbox Checkout bằng Android Custom Tabs
    fun openCheckoutUrl(url: String) {
        val resolved = PaymentApiClient.resolveUrl(url)
        if (resolved.isBlank() || holdPhaseStr != "AWAITING_PAYMENT") return
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(resolved))
        } catch (_: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resolved))
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                statusMessage = "Không thể mở trình duyệt: ${e.localizedMessage}"
            }
        }
    }

    // Hàm kiểm tra trạng thái thanh toán từ backend
    fun checkStatus(isManual: Boolean = false) {
        val bId = savedBookingId ?: return
        if (isLoading || isCheckingStatus) return

        coroutineScope.launch {
            isCheckingStatus = true
            val result = PaymentApiClient.getPaymentStatus(bId)
            result.onSuccess { statusResp ->
                serverTotal = statusResp.totalPrice
                serverDeposit = statusResp.depositAmount
                serverRemaining = statusResp.remainingAmount
                updateDeadlines(
                    statusResp.serverNow,
                    statusResp.paymentDeadline,
                    statusResp.verificationDeadline,
                    statusResp.holdStatus
                )

                when {
                    statusResp.isPaid -> {
                        paymentStatusStr = "PAID"
                        holdPhaseStr = "CONFIRMED"
                        isCountdownActive = false
                        statusMessage = "Thanh toán toàn bộ 100% thành công!"
                        coroutineScope.launch {
                            BookingStore.refresh()
                            CourtStore.refresh(context)
                        }
                        if (court != null && request != null) {
                            val booking = PaymentApiClient.toDomainBooking(bId, court, statusResp, request)
                            delay(1200)
                            onPaymentSuccess(booking)
                        }
                    }
                    statusResp.isDepositPaid -> {
                        paymentStatusStr = "DEPOSIT_PAID"
                        holdPhaseStr = "CONFIRMED"
                        isCountdownActive = false
                        statusMessage = "Thanh toán tiền cọc 30% thành công!"
                        coroutineScope.launch {
                            BookingStore.refresh()
                            CourtStore.refresh(context)
                        }
                        if (court != null && request != null) {
                            val booking = PaymentApiClient.toDomainBooking(bId, court, statusResp, request)
                            delay(1200)
                            onPaymentSuccess(booking)
                        }
                    }
                    statusResp.holdStatus == "REVIEW_REQUIRED" || statusResp.paymentStatus == "REVIEW_REQUIRED" -> {
                        paymentStatusStr = "REVIEW_REQUIRED"
                        holdPhaseStr = "REVIEW_REQUIRED"
                        isCountdownActive = false
                        statusMessage = "Giao dịch cần đối soát: Thanh toán được ghi nhận sau khi hết hạn giữ sân. Sân không bị chiếm giữ và ban quản lý sẽ đối soát hỗ trợ hoàn tiền."
                    }
                    statusResp.isExpired || statusResp.holdStatus in listOf("EXPIRED", "RELEASED") || statusResp.bookingStatus == "CANCELLED" -> {
                        paymentStatusStr = "EXPIRED"
                        holdPhaseStr = "EXPIRED"
                        isCountdownActive = false
                        statusMessage = "Thời gian giữ sân và xác minh đã hết. Vui lòng kiểm tra và chọn lại khung giờ."
                    }
                    statusResp.paymentStatus == "FAILED" -> {
                        paymentStatusStr = "FAILED"
                        isCountdownActive = false
                        statusMessage = "Giao dịch thanh toán SePay thất bại hoặc đã bị hủy."
                    }
                    statusResp.holdStatus == "VERIFYING_PAYMENT" -> {
                        holdPhaseStr = "VERIFYING_PAYMENT"
                        paymentStatusStr = "PENDING"
                        if (isManual) {
                            snackbarHostState.showSnackbar("Đang xác minh giao dịch SePay. Sân vẫn được giữ tối đa 1 phút.")
                        }
                    }
                    else -> {
                        paymentStatusStr = "PENDING"
                        if (isManual) {
                            snackbarHostState.showSnackbar("Chưa nhận được thanh toán từ SePay. Vui lòng thử lại sau ít giây.")
                        }
                    }
                }
            }.onFailure { ex ->
                statusMessage = "Chưa xác minh được trạng thái. Kiểm tra kết nối và thử lại."
                if (isManual) {
                    snackbarHostState.showSnackbar(ex.message ?: "Lỗi kiểm tra trạng thái")
                }
            }
            isCheckingStatus = false
        }
    }

    val latestCheckStatus by rememberUpdatedState({ checkStatus(false) })
    LaunchedEffect(savedBookingId, lifecycleOwner) {
        if (savedBookingId == null) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (paymentStatusStr !in listOf("PAID", "DEPOSIT_PAID", "EXPIRED", "REVIEW_REQUIRED", "CANCELLED")) {
                latestCheckStatus()
                delay(3000)
            }
        }
    }

    // Xử lý nút bấm "Thanh toán qua SePay" (Giành giữ sân 5 phút nguyên tử)
    fun handleInitiatePayment() {
        if (request == null || court == null || isLoading || savedBookingId != null) return
        isLoading = true
        coroutineScope.launch {
            statusMessage = null

            try {
                // Gọi API startCheckout để giành giữ sân đúng 5 phút nguyên tử trong database
                val currentUserId = UserStore.currentUser.value?.id
                val checkoutResult = PaymentApiClient.startCheckout(
                    request = request,
                    court = court,
                    idempotencyKey = idempotencyKey,
                    userId = currentUserId
                ).getOrThrow()

                val resolvedUrl = PaymentApiClient.resolveUrl(checkoutResult.checkoutUrl)
                savedBookingId = checkoutResult.bookingId
                savedCheckoutUrl = resolvedUrl
                savedInvoiceNumber = checkoutResult.orderInvoiceNumber
                serverTotal = checkoutResult.totalPrice
                serverDeposit = checkoutResult.depositAmount
                serverRemaining = checkoutResult.remainingAmount

                if (checkoutResult.holdStatus == "CONFIRMED" || request.paymentMethod == PaymentMethod.WALLET) {
                    holdPhaseStr = "CONFIRMED"
                    paymentStatusStr = "PAID"
                    BookingStore.refresh()
                    onPaymentSuccess(
                        Booking(
                            id = checkoutResult.bookingId,
                            courtId = court.id,
                            courtName = court.name,
                            fieldDetail = request.field.detail,
                            timeRange = request.slots.joinToString(", ") { it.label },
                            dateLabel = request.dateLabel,
                            totalPrice = checkoutResult.totalPrice,
                            payment = PaymentStatus.PAID,
                            address = court.address,
                            depositAmount = checkoutResult.depositAmount,
                            remainingAmount = 0,
                            paymentMethod = PaymentMethod.WALLET,
                        )
                    )
                    return@launch
                }

                hasLaunchedCheckout = true
                paymentStatusStr = "PENDING"

                updateDeadlines(
                    checkoutResult.serverNow,
                    checkoutResult.paymentDeadline,
                    checkoutResult.verificationDeadline,
                    checkoutResult.holdStatus
                )
                isCountdownActive = true

                // Mở Custom Tab thanh toán
                openCheckoutUrl(resolvedUrl)
            } catch (ex: SlotConflictException) {
                // Tranh chấp khung giờ: dừng loading, không mở checkout, thông báo cho người dùng
                conflictDialogMessage = "Khung giờ này đã được người khác chọn hoặc đang thanh toán. Vui lòng chọn khung giờ khác."
                showConflictDialog = true
            } catch (e: Exception) {
                statusMessage = e.message ?: "Đã xảy ra lỗi khi tạo thanh toán."
                snackbarHostState.showSnackbar(statusMessage ?: "Lỗi kết nối")
            } finally {
                isLoading = false
            }
        }
    }

    // Dialog thông báo tranh chấp khung giờ (HTTP 409)
    if (showConflictDialog) {
        AlertDialog(
            onDismissRequest = {
                showConflictDialog = false
                onBack()
            },
            title = {
                Text(
                    text = "Khung giờ không khả dụng",
                    color = EsColors.Title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = conflictDialogMessage,
                    color = EsColors.Body,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                EsPrimaryButton(
                    text = "Quay lại chọn khung giờ khác",
                    onClick = {
                        showConflictDialog = false
                        onBack()
                    }
                )
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            EsTopBar(title = "Thanh toán SePay", onBack = onBack)

            if (request == null || court == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = EsColors.Warning,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Thông tin thanh toán không còn khả dụng.",
                        style = MaterialTheme.typography.titleMedium,
                        color = EsColors.Title,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Vui lòng chọn lại sân bóng từ danh sách.",
                        color = EsColors.Body,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    EsPrimaryButton(
                        text = "Quay lại",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    )
                }
            } else {
                val fullPayment = request.paymentMethod == PaymentMethod.QR_TRANSFER || request.paymentMethod == PaymentMethod.WALLET
                val effectiveTotal = serverTotal ?: request.total
                val effectiveDeposit = serverDeposit ?: request.deposit
                val effectiveRemaining = serverRemaining ?: if (fullPayment) 0 else (effectiveTotal - effectiveDeposit)
                val amountToPayOnline = if (fullPayment) effectiveTotal else effectiveDeposit

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Thẻ thông tin sân và lịch đặt
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = EsColors.Surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = court.name,
                                    color = EsColors.Title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = EsColors.SurfaceTint,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Giữ sân 5 phút + 1p xác minh",
                                        color = EsColors.Primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = EsColors.Body,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    court.address,
                                    color = EsColors.Body,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = EsColors.Border)
                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Sân: ${request.field.detail}",
                                color = EsColors.Title,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Thời gian: ${request.slots.mergedTimeRange()} | ${request.dateLabel}",
                                color = EsColors.Title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = EsColors.Body,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${request.customerName} - ${request.phone}",
                                    color = EsColors.Body,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Thẻ bảng kê thanh toán
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EsColors.Primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = EsColors.SurfaceTint)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "Chi tiết thanh toán",
                                color = EsColors.Title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))

                            PriceBreakdownRow(
                                label = "Tổng tiền thuê sân:",
                                value = "${effectiveTotal.vnd()} đ",
                                isBold = false
                            )

                            PriceBreakdownRow(
                                label = "Hình thức:",
                                value = if (fullPayment) "Thanh toán toàn bộ (100%)" else "Đặt cọc online (30%)",
                                isBold = false
                            )

                            Spacer(Modifier.height(6.dp))
                            HorizontalDivider(color = EsColors.Border)
                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (fullPayment) "Cần thanh toán qua SePay:" else "Tiền cọc thanh toán SePay:",
                                        color = EsColors.Title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Cổng SePay Sandbox (VietQR)",
                                        color = EsColors.Body,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "${amountToPayOnline.vnd()} đ",
                                    color = EsColors.Primary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!fullPayment) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Còn lại thanh toán tại sân:",
                                        color = EsColors.Body,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${effectiveRemaining.vnd()} đ",
                                        color = EsColors.Title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Card trạng thái giao dịch & Countdown
                    StatusCard(
                        statusStr = paymentStatusStr,
                        holdPhaseStr = holdPhaseStr,
                        invoiceNumber = savedInvoiceNumber,
                        hasLaunched = hasLaunchedCheckout,
                        isChecking = isCheckingStatus,
                        statusMessage = statusMessage,
                        remainingSeconds = remainingSeconds,
                        onReopenCheckout = {
                            savedCheckoutUrl?.let { openCheckoutUrl(it) }
                        }
                    )

                    // Lưu ý hệ thống SePay Sandbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = EsColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Hệ thống áp dụng chính sách giữ chỗ 5 phút khi bấm thanh toán, cùng tối đa 1 phút bổ sung để xác minh giao dịch SePay. Trong thời gian này, sân sẽ được bảo lưu an toàn.",
                            color = EsColors.Body,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // Thanh nút bấm cố định dưới đáy màn hình
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when {
                            paymentStatusStr in listOf("PAID", "DEPOSIT_PAID") -> {
                                EsPrimaryButton(
                                    text = "Đã xác nhận thanh toán",
                                    leadingIcon = Icons.Rounded.Check,
                                    onClick = { checkStatus(true) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            paymentStatusStr == "REVIEW_REQUIRED" || holdPhaseStr == "REVIEW_REQUIRED" -> {
                                EsPrimaryButton(
                                    text = "Giao dịch cần đối soát - Quay lại",
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            paymentStatusStr == "EXPIRED" || holdPhaseStr == "EXPIRED" -> {
                                EsPrimaryButton(
                                    text = "Thời gian giữ sân đã hết - Chọn lại",
                                    onClick = onBack,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            holdPhaseStr == "VERIFYING_PAYMENT" -> {
                                // Trong phút thứ 6: Đang xác minh, KHÔNG cho mở lại hay bắt đầu thanh toán mới
                                OutlinedButton(
                                    onClick = { checkStatus(isManual = true) },
                                    enabled = !isCheckingStatus,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isCheckingStatus) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = EsColors.Primary
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Kiểm tra trạng thái xác minh")
                                    }
                                }
                            }
                            hasLaunchedCheckout -> {
                                // Đang trong 5 phút thanh toán -> Nút Kiểm tra và Mở lại SePay
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { checkStatus(isManual = true) },
                                        enabled = !isCheckingStatus,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isCheckingStatus) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = EsColors.Primary
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Kiểm tra")
                                        }
                                    }

                                    EsPrimaryButton(
                                        text = "Mở lại SePay",
                                        trailingIcon = Icons.Rounded.OpenInBrowser,
                                        onClick = { savedCheckoutUrl?.let { openCheckoutUrl(it) } },
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }
                            }
                            else -> {
                                // Chưa mở SePay -> Nút chính giành giữ sân & mở Checkout
                                EsPrimaryButton(
                                    text = if (isLoading) "Đang xử lý thanh toán..." else if (request.paymentMethod == PaymentMethod.WALLET) "Thanh toán bằng Ví EasySport" else "Thanh toán qua SePay",
                                    trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { handleInitiatePayment() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    statusStr: String,
    holdPhaseStr: String,
    invoiceNumber: String?,
    hasLaunched: Boolean,
    isChecking: Boolean,
    statusMessage: String?,
    remainingSeconds: Int,
    onReopenCheckout: () -> Unit
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val countdownText = String.format("%02d:%02d", minutes, seconds)

    val (bgColor, fgColor, icon, title, description) = when {
        statusStr == "PAID" -> StatusVisual(
            EsColors.SuccessBg,
            EsColors.Success,
            Icons.Rounded.Check,
            "Đã thanh toán đủ (100%)",
            "Giao dịch đã được hệ thống SePay xác thực hoàn tất."
        )
        statusStr == "DEPOSIT_PAID" -> StatusVisual(
            EsColors.SurfaceTint,
            EsColors.Primary,
            Icons.Rounded.Check,
            "Đã đặt cọc 30% thành công",
            "Đơn đặt sân đã được chuyển sang ĐÃ ĐẶT thành công. Phần còn lại sẽ thanh toán trực tiếp tại sân."
        )
        statusStr == "REVIEW_REQUIRED" || holdPhaseStr == "REVIEW_REQUIRED" -> StatusVisual(
            Color(0xFFFFFBEB),
            Color(0xFFD97706),
            Icons.Outlined.Info,
            "Giao dịch cần đối soát",
            statusMessage ?: "Thanh toán được ghi nhận sau khi hết hạn giữ sân. Sân không bị chiếm giữ và nhân viên sẽ liên hệ đối soát hoàn tiền."
        )
        statusStr == "EXPIRED" || holdPhaseStr == "EXPIRED" -> StatusVisual(
            Color(0xFFFEF2F2),
            Color(0xFFDC2626),
            Icons.Rounded.Close,
            "Thời gian giữ sân đã hết",
            "Thời gian giữ sân và xác minh đã kết thúc. Vui lòng kiểm tra và chọn lại khung giờ."
        )
        statusStr == "FAILED" -> StatusVisual(
            Color(0xFFFEF2F2),
            Color(0xFFDC2626),
            Icons.Rounded.Close,
            "Thanh toán thất bại",
            statusMessage ?: "Giao dịch không thành công hoặc người dùng đã hủy."
        )
        holdPhaseStr == "VERIFYING_PAYMENT" -> StatusVisual(
            Color(0xFFFFFBEB),
            Color(0xFFD97706),
            Icons.Outlined.HourglassBottom,
            "Đang xác minh thanh toán ($countdownText)",
            "Đã hết 5 phút thanh toán. Đang chờ xác minh giao dịch từ ngân hàng/SePay (sân vẫn được giữ thêm tối đa 1 phút). Không thể tạo giao dịch mới."
        )
        holdPhaseStr == "AWAITING_PAYMENT" || (statusStr == "PENDING" && hasLaunched) -> StatusVisual(
            EsColors.WarningBg,
            EsColors.Warning,
            Icons.Outlined.HourglassTop,
            "Đang giữ sân ($countdownText)",
            "Sân đang được giữ cho bạn trong đúng 5 phút. Vui lòng hoàn tất thanh toán trước khi hết hạn."
        )
        else -> StatusVisual(
            Color(0xFFF8FAFC),
            EsColors.Title,
            Icons.Outlined.Payment,
            "Sẵn sàng thanh toán",
            "Khi bấm nút 'Thanh toán qua SePay', hệ thống sẽ giành giữ sân trong 5 phút."
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(fgColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = fgColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (invoiceNumber != null) {
                        Text(
                            text = "Hóa đơn: $invoiceNumber",
                            color = EsColors.Body,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = fgColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                color = EsColors.Body,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            if (statusStr == "PENDING" && hasLaunched && holdPhaseStr != "VERIFYING_PAYMENT") {
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onReopenCheckout,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Mở lại trang thanh toán SePay →",
                        color = fgColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private data class StatusVisual(
    val bg: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val desc: String
)

@Composable
private fun PriceBreakdownRow(label: String, value: String, isBold: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = EsColors.Body, fontSize = 13.sp)
        Text(
            value,
            color = EsColors.Title,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PaymentPreview() {
    val sampleCourt = SampleData.courts.first()
    PaymentScreen(
        request = BookingRequest(
            courtId = sampleCourt.id,
            field = sampleCourt.fields.first(),
            slots = listOf(SampleData.timeSlots.first()),
            dateLabel = "Hôm nay, 05/11/2026",
            customerName = "Nguyễn Văn A",
            phone = "0901234567",
            paymentMethod = PaymentMethod.QR_TRANSFER,
            total = 450_000,
            deposit = 135_000,
        ),
        onBack = {},
        onPaymentSuccess = {}
    )
}
