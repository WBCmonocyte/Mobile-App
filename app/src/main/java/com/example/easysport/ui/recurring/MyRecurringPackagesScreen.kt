package com.example.easysport.ui.recurring

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
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
import com.example.easysport.data.model.*
import com.example.easysport.data.remote.CourtAvailabilitySlot
import com.example.easysport.data.remote.PaymentApiClient
import com.example.easysport.data.remote.RecurringApiClient
import com.example.easysport.data.remote.RecurringSlotConflictException
import com.example.easysport.data.repository.BookingStore
import com.example.easysport.data.repository.CourtStore
import com.example.easysport.data.repository.SampleData
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.common.EsTopBar
import com.example.easysport.ui.common.vnd
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MyRecurringPackagesScreen(
    onBack: () -> Unit,
    onContinuePayment: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var packages by remember { mutableStateOf<List<RecurringPackageDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var statusFeedback by remember { mutableStateOf<String?>(null) }

    var activeSessionForCancel by remember { mutableStateOf<RecurringSessionItem?>(null) }
    var activeCreditForRedeem by remember { mutableStateOf<RecurringCreditItem?>(null) }

    fun reload() {
        scope.launch {
            isLoading = true
            errorMsg = null
            RecurringApiClient.getMyPackages()
                .onSuccess { packages = it }
                .onFailure { errorMsg = it.localizedMessage ?: "Không tải được danh sách lịch cố định" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    val allCredits = remember(packages) {
        packages.flatMap { it.credits }.filter { it.status.equals("AVAILABLE", ignoreCase = true) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            EsTopBar(
                title = "Lịch cố định của tôi",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = { reload() }, enabled = !isLoading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Làm mới", tint = EsColors.Primary)
                    }
                }
            )

            if (isLoading && packages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EsColors.Primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (statusFeedback != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EsColors.SuccessBg,
                                border = BorderStroke(1.dp, EsColors.Success.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    statusFeedback ?: "",
                                    color = EsColors.Success,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    if (errorMsg != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF2F2)
                            ) {
                                Text(
                                    errorMsg ?: "",
                                    color = EsColors.Danger,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    if (allCredits.isNotEmpty()) {
                        item {
                            Text(
                                "Buổi còn dư có thể đặt bù (${allCredits.size})",
                                color = EsColors.Title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            )
                        }
                        items(allCredits, key = { it.id }) { credit ->
                            CreditCard(
                                credit = credit,
                                onRedeem = { activeCreditForRedeem = credit }
                            )
                        }
                    }

                    item {
                        Text(
                            "Các gói cố định đã đăng ký (${packages.size})",
                            color = EsColors.Title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp
                        )
                    }

                    if (packages.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = EsColors.Surface
                            ) {
                                Text(
                                    "Bạn chưa có gói lịch cố định nào. Hãy chọn một cụm sân và bật chế độ \"Đặt lịch cố định\" (3 tháng · 12 buổi).",
                                    color = EsColors.Body,
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(packages, key = { it.pkg.id }) { detail ->
                            RecurringPackageCard(
                                detail = detail,
                                onContinuePayment = { onContinuePayment(detail.pkg.id) },
                                onManageSession = { activeSessionForCancel = it },
                                onRedeemCredit = { activeCreditForRedeem = it }
                            )
                        }
                    }
                }
            }
        }
    }

    activeSessionForCancel?.let { sess ->
        SessionCancelOrPostponeDialog(
            session = sess,
            onDismiss = { activeSessionForCancel = null },
            onResolved = { msg ->
                activeSessionForCancel = null
                statusFeedback = msg
                scope.launch { BookingStore.refresh() }
                reload()
            }
        )
    }

    activeCreditForRedeem?.let { credit ->
        RedeemCreditDialog(
            credit = credit,
            onDismiss = { activeCreditForRedeem = null },
            onRedeemed = { msg ->
                activeCreditForRedeem = null
                statusFeedback = msg
                scope.launch { BookingStore.refresh() }
                reload()
            }
        )
    }
}

@Composable
private fun CreditCard(
    credit: RecurringCreditItem,
    onRedeem: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EsColors.SurfaceTint),
        border = BorderStroke(1.dp, EsColors.Primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = EsColors.Primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(credit.courtName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EsColors.Title)
                Text(
                    "${credit.fieldDetail} · ${credit.slotCount} ca/buổi" +
                        (credit.timeRange.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""),
                    fontSize = 12.5.sp,
                    color = EsColors.Body
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRedeem,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Đặt bù", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RecurringPackageCard(
    detail: RecurringPackageDetail,
    onContinuePayment: () -> Unit,
    onManageSession: (RecurringSessionItem) -> Unit,
    onRedeemCredit: (RecurringCreditItem) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val pkg = detail.pkg
    val (statusLabel, statusBg, statusFg) = packageStatusVisual(pkg.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, EsColors.Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(pkg.courtName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EsColors.Title, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                    Text(
                        statusLabel,
                        color = statusFg,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${pkg.packageTitle} · ${pkg.fieldDetail}", color = EsColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Lịch cố định: ${pkg.weekdayLabel}, ${pkg.timeRange} (từ ${pkg.startDate})", color = EsColors.Body, fontSize = 12.5.sp)
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(EsColors.Surface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Giá trị gói", fontSize = 11.sp, color = EsColors.Body)
                    Text("${pkg.totalPrice.vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = EsColors.Title)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Đã chơi / Sắp tới", fontSize = 11.sp, color = EsColors.Body)
                    Text("${pkg.playedCount} / ${pkg.upcomingCount} buổi", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = EsColors.Title)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Buổi còn dư", fontSize = 11.sp, color = EsColors.Body)
                    Text("${pkg.availableCreditsCount}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = EsColors.Primary)
                }
            }

            if (pkg.totalRefunded > 0) {
                Spacer(Modifier.height(6.dp))
                Text("Đã hoàn vào Ví EasySport: ${pkg.totalRefunded.vnd()} đ", color = EsColors.Success, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (pkg.status == "AWAITING_PAYMENT") {
                Spacer(Modifier.height(10.dp))
                EsPrimaryButton(
                    text = "Tiếp tục thanh toán SePay",
                    onClick = onContinuePayment,
                    modifier = Modifier.fillMaxWidth(),
                    height = 44.dp
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (expanded) "Thu gọn danh sách ${detail.sessions.size} buổi" else "Xem chi tiết ${detail.sessions.size} buổi & thao tác hoãn/hủy",
                    color = EsColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = EsColors.Primary
                )
            }

            if (expanded) {
                HorizontalDivider(color = EsColors.Border, modifier = Modifier.padding(vertical = 6.dp))
                detail.sessions.forEach { s ->
                    val canModify = pkg.status == "ACTIVE" && (s.status == "UPCOMING" || s.status == "WEATHER_APPROVED")
                    val (sLabel, sBg, sFg) = sessionStatusVisual(s.status)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Buổi ${s.sessionIndex}: ${s.sessionDate} · ${s.timeRange}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EsColors.Title
                            )
                            if (!s.rescheduledToDate.isNullOrBlank()) {
                                Text("→ Đã đổi sang ngày ${s.rescheduledToDate}", fontSize = 12.sp, color = EsColors.Primary)
                            }
                            if (!s.resolutionNote.isNullOrBlank()) {
                                Text(s.resolutionNote, fontSize = 11.5.sp, color = EsColors.Body)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(shape = RoundedCornerShape(6.dp), color = sBg) {
                                Text(
                                    sLabel,
                                    color = sFg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            if (canModify) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (s.status == "WEATHER_APPROVED") "Chọn Trượt / Buổi dư →" else "Hoãn / Hủy",
                                    color = EsColors.Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onManageSession(s) }
                                )
                            }
                        }
                    }
                }
                val pkgCredits = detail.credits.filter { it.status == "AVAILABLE" }
                if (pkgCredits.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    pkgCredits.forEach { cr ->
                        OutlinedButton(
                            onClick = { onRedeemCredit(cr) },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dùng 1 buổi còn dư đặt lịch bù")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCancelOrPostponeDialog(
    session: RecurringSessionItem,
    onDismiss: () -> Unit,
    onResolved: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val weatherLockedAlready = session.status == "WEATHER_APPROVED"
    var reasonType by remember { mutableStateOf(if (weatherLockedAlready) "WEATHER" else "PERSONAL") }
    var preview by remember { mutableStateOf<CancelPreviewResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadPreview(rType: String) {
        scope.launch {
            isLoading = true
            errorMsg = null
            RecurringApiClient.previewSessionAction(session.id, rType)
                .onSuccess { preview = it }
                .onFailure { errorMsg = it.localizedMessage ?: "Không tải được chính sách buổi sân" }
            isLoading = false
        }
    }

    LaunchedEffect(reasonType) { loadPreview(reasonType) }

    fun executeAction(chosenAction: String) {
        scope.launch {
            isSubmitting = true
            errorMsg = null
            RecurringApiClient.cancelOrPostponeSession(
                sessionId = session.id,
                reasonType = reasonType,
                chosenAction = chosenAction,
                expectedSlideDate = preview?.proposedSlideDate,
                note = note.trim().takeIf { it.isNotEmpty() },
            ).onSuccess { res ->
                onResolved(res.message)
            }.onFailure { err ->
                errorMsg = err.localizedMessage ?: "Xử lý thất bại"
                if (chosenAction == "SLIDE") {
                    loadPreview(reasonType)
                }
            }
            isSubmitting = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        containerColor = Color.White,
        title = { Text("Buổi ${session.sessionIndex}: ${session.sessionDate}", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Khung giờ: ${session.timeRange} · ${session.fieldDetail}", fontSize = 13.sp, color = EsColors.Body)

                if (!weatherLockedAlready) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reasonType == "PERSONAL",
                            onClick = { reasonType = "PERSONAL" },
                            label = { Text("Việc cá nhân") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = reasonType == "WEATHER",
                            onClick = { reasonType = "WEATHER" },
                            label = { Text("Thời tiết xấu") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = EsColors.Primary)
                    }
                } else preview?.let { p ->
                    Surface(shape = RoundedCornerShape(10.dp), color = EsColors.Surface) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(p.policyNotice, fontSize = 12.5.sp, color = EsColors.Title, lineHeight = 18.sp)
                            Text(
                                "Giá thực trả buổi này: ${p.paidAmountSnapshot.vnd()} đ · Hoàn tối đa: ${p.maxRefundableAmount.vnd()} đ",
                                fontSize = 12.sp,
                                color = EsColors.Body
                            )
                            if (p.proposedSlideDate != null && p.canSlide) {
                                Text(
                                    "Ngày trượt lịch trống kế tiếp: ${p.proposedSlideDate}",
                                    fontSize = 12.5.sp,
                                    color = EsColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ghi chú gửi Chủ sân (nếu cần)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = EsColors.Danger, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val actions = preview?.availableActions ?: emptyList()
                if (actions.contains("SLIDE")) {
                    Button(
                        onClick = { executeAction("SLIDE") },
                        enabled = !isSubmitting && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary)
                    ) {
                        Text("Trượt lịch sang ${preview?.proposedSlideDate ?: "tuần kế tiếp"}")
                    }
                }
                if (actions.contains("KEEP_CREDIT")) {
                    OutlinedButton(
                        onClick = { executeAction("KEEP_CREDIT") },
                        enabled = !isSubmitting && !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Giữ 1 buổi còn dư (dùng đặt bù sau)")
                    }
                }
                if (actions.contains("REFUND_WALLET")) {
                    OutlinedButton(
                        onClick = { executeAction("REFUND_WALLET") },
                        enabled = !isSubmitting && !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Yêu cầu hoàn tiền vào Ví EasySport")
                    }
                }
                if (actions.contains("REQUEST_WEATHER")) {
                    Button(
                        onClick = { executeAction("REQUEST_WEATHER") },
                        enabled = !isSubmitting && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EsColors.Warning)
                    ) {
                        Text("Gửi Chủ sân xác nhận hoãn do thời tiết")
                    }
                }
                if (actions.contains("REQUEST_REVIEW")) {
                    Button(
                        onClick = { executeAction("REQUEST_REVIEW") },
                        enabled = !isSubmitting && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EsColors.Warning)
                    ) {
                        Text("Gửi Chủ sân xem xét yêu cầu")
                    }
                }
                if (actions.contains("AUTO_REFUND")) {
                    Button(
                        onClick = { executeAction("AUTO_REFUND") },
                        enabled = !isSubmitting && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EsColors.Success)
                    ) {
                        Text("Hủy & nhận hoàn ${(preview?.estimatedWalletRefundAmount ?: 0).vnd()} đ vào Ví")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Đóng")
                }
            }
        }
    )
}

@Composable
private fun RedeemCreditDialog(
    credit: RecurringCreditItem,
    onDismiss: () -> Unit,
    onRedeemed: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val court = remember(credit.courtId) { CourtStore.getCourtById(credit.courtId) }
    val fields = court?.fields ?: emptyList()

    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var selectedFieldDetail by remember { mutableStateOf(credit.fieldDetail) }
    var availability by remember { mutableStateOf<Map<String, CourtAvailabilitySlot>>(emptyMap()) }
    var selectedSlots by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingSlots by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val dateStr = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    fun refreshAvailability() {
        scope.launch {
            loadingSlots = true
            selectedSlots = emptyList()
            PaymentApiClient.getCourtAvailability(credit.courtId, selectedFieldDetail, dateStr)
                .onSuccess { list -> availability = list.associateBy { it.slot } }
                .onFailure { errorMsg = it.localizedMessage }
            loadingSlots = false
        }
    }

    LaunchedEffect(dateStr, selectedFieldDetail) { refreshAvailability() }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = Color.White,
        title = { Text("Đặt bù 1 buổi còn dư", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${credit.courtName} · Chọn đúng ${credit.slotCount} ca chơi", fontSize = 13.sp, color = EsColors.Body)

                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d) },
                            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
                        ).apply {
                            datePicker.minDate = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }.show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ngày đặt bù: $dateStr")
                }

                if (fields.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        fields.forEach { f ->
                            FilterChip(
                                selected = selectedFieldDetail == f.detail,
                                onClick = { selectedFieldDetail = f.detail },
                                label = { Text(f.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                if (loadingSlots) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EsColors.Primary)
                } else {
                    SampleData.timeSlots.forEach { slot ->
                        val st = availability[slot.label]?.status ?: "AVAILABLE"
                        val isFree = st == "AVAILABLE"
                        val isSelected = selectedSlots.contains(slot.label)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isFree) {
                                    selectedSlots = when {
                                        isSelected -> selectedSlots - slot.label
                                        credit.slotCount == 1 -> listOf(slot.label)
                                        selectedSlots.size < credit.slotCount -> selectedSlots + slot.label
                                        else -> selectedSlots.drop(1) + slot.label
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isSelected -> EsColors.Primary
                                !isFree -> Color(0xFFF1F5F9)
                                else -> EsColors.Surface
                            }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    slot.label,
                                    color = if (isSelected) Color.White else if (isFree) EsColors.Title else EsColors.Disabled,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                Text(
                                    when {
                                        isSelected -> "Đã chọn"
                                        st == "HELD" -> "Đang giữ"
                                        st == "BOOKED" -> "Đã đặt"
                                        else -> "Trống"
                                    },
                                    color = if (isSelected) Color.White else if (isFree) EsColors.Success else EsColors.Body,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = EsColors.Danger, fontSize = 12.5.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        submitting = true
                        errorMsg = null
                        RecurringApiClient.redeemCredit(credit.id, dateStr, selectedFieldDetail, selectedSlots)
                            .onSuccess { onRedeemed(it) }
                            .onFailure { err ->
                                errorMsg = when (err) {
                                    is RecurringSlotConflictException -> "Khung giờ vừa có người đặt. Buổi còn dư vẫn nguyên vẹn!"
                                    else -> err.localizedMessage ?: "Không thể đặt bù"
                                }
                                refreshAvailability()
                            }
                        submitting = false
                    }
                },
                enabled = !submitting && selectedSlots.size == credit.slotCount,
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary)
            ) {
                Text(if (submitting) "Đang xử lý..." else "Xác nhận đặt bù")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("Hủy") }
        }
    )
}

private fun packageStatusVisual(status: String): Triple<String, Color, Color> = when (status.uppercase()) {
    "ACTIVE" -> Triple("Đang hiệu lực", EsColors.SuccessBg, EsColors.Success)
    "AWAITING_PAYMENT" -> Triple("Chờ thanh toán", EsColors.WarningBg, EsColors.Warning)
    "REVIEW_REQUIRED" -> Triple("Chờ đối soát", EsColors.WarningBg, EsColors.Warning)
    "EXPIRED" -> Triple("Hết hạn giữ chỗ", Color(0xFFFEF2F2), EsColors.Danger)
    "COMPLETED" -> Triple("Hoàn tất", EsColors.SurfaceTint, EsColors.Primary)
    else -> Triple(status, EsColors.Surface, EsColors.Body)
}

private fun sessionStatusVisual(status: String): Triple<String, Color, Color> = when (status.uppercase()) {
    "UPCOMING" -> Triple("Sắp tới", EsColors.SurfaceTint, EsColors.Primary)
    "CHECKED_IN" -> Triple("Đang đá", EsColors.WarningBg, EsColors.Warning)
    "COMPLETED" -> Triple("Đã chơi", EsColors.SuccessBg, EsColors.Success)
    "RESCHEDULED" -> Triple("Đã trượt lịch", EsColors.SuccessBg, EsColors.Success)
    "CREDIT_GRANTED" -> Triple("Đã giữ buổi dư", EsColors.SurfaceTint, EsColors.Primary)
    "REFUNDED" -> Triple("Đã hoàn ví", EsColors.SuccessBg, EsColors.Success)
    "WEATHER_PENDING" -> Triple("Chờ duyệt thời tiết", EsColors.WarningBg, EsColors.Warning)
    "WEATHER_APPROVED" -> Triple("Đã xác nhận mưa", EsColors.SuccessBg, EsColors.Success)
    "PENDING_OWNER_REVIEW" -> Triple("Chờ Chủ sân duyệt", EsColors.WarningBg, EsColors.Warning)
    "CANCELLED" -> Triple("Đã hủy", Color(0xFFFEF2F2), EsColors.Danger)
    else -> Triple(status, EsColors.Surface, EsColors.Body)
}
