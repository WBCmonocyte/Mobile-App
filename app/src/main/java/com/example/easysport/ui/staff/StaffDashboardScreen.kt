package com.example.easysport.ui.staff

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.Court
import com.example.easysport.data.model.PosSaleItem
import com.example.easysport.data.model.ShiftReportItem
import com.example.easysport.data.model.StaffBookingItem
import com.example.easysport.data.model.StaffShiftItem
import com.example.easysport.data.model.StaffShiftSummary
import com.example.easysport.data.remote.StaffOwnerApiClient
import com.example.easysport.data.repository.CourtStore
import com.example.easysport.data.repository.UserStore
import com.example.easysport.ui.common.EsCardShape
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.common.EsSegmentedControl
import com.example.easysport.ui.common.ProAvatar
import com.example.easysport.ui.common.ProCard
import com.example.easysport.ui.common.ProColors
import com.example.easysport.ui.common.ProHeroBrush
import com.example.easysport.ui.common.ProSectionHeader
import com.example.easysport.ui.common.ProStatusDotPill
import com.example.easysport.ui.common.ProTopBar
import com.example.easysport.ui.common.vnd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class StaffTab(val label: String, val title: String, val icon: ImageVector) {
    PITCHES("Sân bóng", "Sơ đồ sân", Icons.Outlined.SportsSoccer),
    BOOKINGS("Đơn đặt", "Đơn đặt sân", Icons.AutoMirrored.Outlined.ReceiptLong),
    POS("Quầy POS", "Quầy dịch vụ", Icons.Outlined.LocalDrink),
    SHIFT("Ca & Két", "Ca trực & Két", Icons.Outlined.CalendarMonth),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDashboardScreen(
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by UserStore.currentUser.collectAsState()
    val courts by CourtStore.courts.collectAsState()

    var currentTab by remember { mutableStateOf(StaffTab.PITCHES) }
    var bookings by remember { mutableStateOf<List<StaffBookingItem>>(emptyList()) }
    var myShifts by remember { mutableStateOf<List<StaffShiftItem>>(emptyList()) }
    var myReports by remember { mutableStateOf<List<ShiftReportItem>>(emptyList()) }
    var shiftSummary by remember { mutableStateOf<StaffShiftSummary?>(null) }
    var retailSales by remember { mutableStateOf<List<PosSaleItem>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSubmittingReport by remember { mutableStateOf(false) }

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showWalkInSheet by remember { mutableStateOf(false) }
    var walkInPreselectedCourt by remember { mutableStateOf<Court?>(null) }
    var walkInPreselectedField by remember { mutableStateOf<String?>(null) }
    var checkoutTarget by remember { mutableStateOf<StaffBookingItem?>(null) }
    var extendTarget by remember { mutableStateOf<StaffBookingItem?>(null) }
    var cancelTarget by remember { mutableStateOf<StaffBookingItem?>(null) }
    var posPreselectedBookingId by remember { mutableStateOf<String?>(null) }
    var showOutOfShiftDialog by remember { mutableStateOf(false) }
    var pendingClockOutShift by remember { mutableStateOf<StaffShiftItem?>(null) }

    var pendingVietQrAmount by remember { mutableIntStateOf(0) }
    var pendingVietQrContent by remember { mutableStateOf("") }
    var pendingVietQrSummary by remember { mutableStateOf("") }
    var onVietQrConfirmed by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun refreshAll() {
        scope.launch {
            isRefreshing = true
            StaffOwnerApiClient.getStaffBookings()
                .onSuccess { bookings = it }
                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
            StaffOwnerApiClient.getMyShifts().onSuccess { myShifts = it }
            StaffOwnerApiClient.getMyReports().onSuccess { myReports = it }
            StaffOwnerApiClient.getShiftSummary().onSuccess { shiftSummary = it }
            StaffOwnerApiClient.getMyRetailSales().onSuccess { retailSales = it }
            isRefreshing = false
        }
    }

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
    val currentShiftForContext = remember(myShifts, todayStr) {
        myShifts.firstOrNull { it.status == "ACTIVE" }
            ?: myShifts.firstOrNull { it.status == "UPCOMING" && it.shiftDate == todayStr }
            ?: myShifts.lastOrNull { it.status == "COMPLETED" && it.shiftDate == todayStr }
    }
    val assignedCourt = remember(courts, currentShiftForContext) {
        courts.firstOrNull { it.id == currentShiftForContext?.courtId }
            ?: courts.firstOrNull { it.id == "court_thanh_phat" }
            ?: courts.firstOrNull()
    }

    fun dialCustomer(phone: String) {
        if (phone.isBlank()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        }
    }

    LaunchedEffect(Unit) { refreshAll() }

    BackHandler { showLogoutConfirm = true }

    val waitingCount = remember(bookings) {
        bookings.count { it.status in listOf("PAID", "DEPOSIT_PAID", "PENDING") }
    }

    Scaffold(
        containerColor = ProColors.Canvas,
        topBar = {
            ProTopBar(
                title = currentTab.title,
                subtitle = "${currentUser?.name ?: "Nhân viên"} · ${assignedCourt?.name ?: "Chi nhánh"}",
                roleBadge = "NHÂN VIÊN",
                isRefreshing = isRefreshing,
                onRefresh = { refreshAll() },
                onLogout = { showLogoutConfirm = true },
            )
        },
        bottomBar = {
            StaffBottomBar(
                selected = currentTab,
                waitingCount = waitingCount,
                onSelected = { currentTab = it }
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshAll() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ProColors.Canvas)
        ) {
            when (currentTab) {
                StaffTab.PITCHES -> StaffPitchesTab(
                    courts = courts,
                    bookings = bookings,
                    currentShift = currentShiftForContext,
                    assignedCourt = assignedCourt,
                    shiftSummary = shiftSummary,
                    onGoToShiftTab = { currentTab = StaffTab.SHIFT },
                    onWalkInClick = { court, fieldLabel ->
                        if (currentShiftForContext == null || currentShiftForContext.status != "ACTIVE") {
                            showOutOfShiftDialog = true
                        } else {
                            walkInPreselectedCourt = court ?: assignedCourt
                            walkInPreselectedField = fieldLabel
                            showWalkInSheet = true
                        }
                    },
                    onCall = { phone -> dialCustomer(phone) },
                    onCheckIn = { b ->
                        if (currentShiftForContext?.status != "ACTIVE") {
                            showOutOfShiftDialog = true
                        } else {
                            scope.launch {
                                StaffOwnerApiClient.checkInBooking(b.id)
                                    .onSuccess {
                                        Toast.makeText(context, "Check-in thành công cho ${b.customerName}", Toast.LENGTH_SHORT).show()
                                        refreshAll()
                                    }
                                    .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                            }
                        }
                    },
                    onCancelBooking = { b ->
                        if (currentShiftForContext?.status != "ACTIVE") showOutOfShiftDialog = true
                        else cancelTarget = b
                    },
                    onGoToPos = { bookingId ->
                        posPreselectedBookingId = bookingId
                        currentTab = StaffTab.POS
                    },
                    onExtendTime = { b ->
                        if (currentShiftForContext?.status != "ACTIVE") showOutOfShiftDialog = true
                        else extendTarget = b
                    },
                    onCheckOut = { b ->
                        if (currentShiftForContext?.status != "ACTIVE") showOutOfShiftDialog = true
                        else checkoutTarget = b
                    }
                )
                StaffTab.BOOKINGS -> StaffBookingsTab(
                    bookings = bookings,
                    currentShift = currentShiftForContext,
                    onWalkInClick = {
                        if (currentShiftForContext == null || currentShiftForContext.status != "ACTIVE") {
                            showOutOfShiftDialog = true
                        } else {
                            walkInPreselectedCourt = assignedCourt
                            walkInPreselectedField = null
                            showWalkInSheet = true
                        }
                    },
                    onCall = { phone -> dialCustomer(phone) },
                    onCheckIn = { b ->
                        if (currentShiftForContext?.status != "ACTIVE") {
                            showOutOfShiftDialog = true
                        } else {
                            scope.launch {
                                StaffOwnerApiClient.checkInBooking(b.id)
                                    .onSuccess {
                                        Toast.makeText(context, "Đã nhận sân: ${b.customerName}", Toast.LENGTH_SHORT).show()
                                        refreshAll()
                                    }
                                    .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                            }
                        }
                    },
                    onCancelBooking = { b ->
                        if (currentShiftForContext?.status != "ACTIVE") showOutOfShiftDialog = true
                        else cancelTarget = b
                    },
                    onExtendTime = { b ->
                        if (currentShiftForContext?.status != "ACTIVE") showOutOfShiftDialog = true
                        else extendTarget = b
                    },
                    onCheckOut = { b ->
                        if (currentShiftForContext?.status != "ACTIVE") showOutOfShiftDialog = true
                        else checkoutTarget = b
                    },
                    onAddPos = { bookingId ->
                        posPreselectedBookingId = bookingId
                        currentTab = StaffTab.POS
                    }
                )
                StaffTab.POS -> StaffPosTab(
                    bookings = bookings,
                    assignedCourt = assignedCourt,
                    initialSelectedBookingId = posPreselectedBookingId,
                    retailSales = retailSales,
                    onSubmitPos = { targetBookingId, note, amount, method, onReset ->
                        if (currentShiftForContext == null || currentShiftForContext.status != "ACTIVE") {
                            showOutOfShiftDialog = true
                            return@StaffPosTab
                        }
                        if (targetBookingId == null && method == "VietQR") {
                            pendingVietQrAmount = amount
                            pendingVietQrContent = "EASYSPORT POS ${System.currentTimeMillis() % 10000}"
                            pendingVietQrSummary = "Bán lẻ quầy POS: $note"
                            onVietQrConfirmed = {
                                scope.launch {
                                    StaffOwnerApiClient.createRetailPosSale(
                                        itemsNote = note,
                                        amount = amount,
                                        paymentMethod = "QR",
                                        courtId = assignedCourt?.id,
                                        courtName = assignedCourt?.name
                                    ).onSuccess {
                                        onReset()
                                        Toast.makeText(
                                            context,
                                            "Đã ghi nhận thu VietQR ${amount.vnd()} đ ($note)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        refreshAll()
                                    }.onFailure {
                                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            scope.launch {
                                if (targetBookingId == null) {
                                    StaffOwnerApiClient.createRetailPosSale(
                                        itemsNote = note,
                                        amount = amount,
                                        paymentMethod = "CASH",
                                        courtId = assignedCourt?.id,
                                        courtName = assignedCourt?.name
                                    ).onSuccess {
                                        onReset()
                                        Toast.makeText(
                                            context,
                                            "Đã thu Tiền mặt bán lẻ ${amount.vnd()} đ ($note)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        refreshAll()
                                    }.onFailure {
                                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    StaffOwnerApiClient.addPosServices(targetBookingId, note, amount)
                                        .onSuccess {
                                            onReset()
                                            Toast.makeText(
                                                context,
                                                "Đã ghi nhận +${amount.vnd()} đ ($note) vào sân",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            refreshAll()
                                        }
                                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                )
                StaffTab.SHIFT -> StaffShiftTab(
                    shifts = myShifts,
                    bookings = bookings,
                    myReports = myReports,
                    shiftSummary = shiftSummary,
                    isSubmittingReport = isSubmittingReport,
                    onClockIn = { shiftId ->
                        scope.launch {
                            StaffOwnerApiClient.clockInShift(shiftId)
                                .onSuccess {
                                    Toast.makeText(context, "Đã chấm công vào ca", Toast.LENGTH_SHORT).show()
                                    refreshAll()
                                }
                                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                        }
                    },
                    onClockOut = { shift ->
                        pendingClockOutShift = shift
                    },
                    onSubmitReport = { shift, cash, qr, count, note ->
                        if (isSubmittingReport) return@StaffShiftTab
                        scope.launch {
                            isSubmittingReport = true
                            StaffOwnerApiClient.submitShiftReport(
                                cashAmount = cash,
                                qrAmount = qr,
                                totalOrders = count,
                                handoverNote = note,
                                shiftId = shift.id,
                                courtId = shift.courtId,
                                courtName = shift.courtName,
                            )
                                .onSuccess {
                                    Toast.makeText(
                                        context,
                                        "Đã nộp báo cáo két (${cash.vnd()} đ) cho Chủ sân",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    refreshAll()
                                }
                                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                            isSubmittingReport = false
                        }
                    },
                    onResubmitReport = { reportId, cash, qr, note ->
                        if (isSubmittingReport) return@StaffShiftTab
                        scope.launch {
                            isSubmittingReport = true
                            StaffOwnerApiClient.resubmitShiftReport(reportId, cash, qr, note)
                                .onSuccess {
                                    Toast.makeText(context, "Đã gửi giải trình đối soát lại cho Chủ sân", Toast.LENGTH_LONG).show()
                                    refreshAll()
                                }
                                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                            isSubmittingReport = false
                        }
                    }
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = Color.White,
            title = { Text("Đăng xuất ca làm việc?", fontWeight = FontWeight.Bold, color = EsColors.Title) },
            text = { Text("Bạn có muốn thoát tài khoản nhân viên để quay về màn hình đăng nhập?", color = EsColors.Body) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
                }) { Text("Đăng xuất", color = EsColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Ở lại", color = EsColors.Primary) }
            }
        )
    }

    if (showWalkInSheet) {
        WalkInBottomSheet(
            courts = courts,
            assignedCourt = assignedCourt,
            initialCourt = walkInPreselectedCourt,
            initialField = walkInPreselectedField,
            onDismiss = { showWalkInSheet = false },
            onConfirm = { courtId, fieldDetail, slot, name, phone, price, deposit, remaining, paymentMethod ->
                val createAction = {
                    scope.launch {
                        StaffOwnerApiClient.createWalkInBooking(courtId, fieldDetail, slot, name, phone, price, deposit, remaining, paymentMethod)
                            .onSuccess {
                                showWalkInSheet = false
                                Toast.makeText(context, "Đã tạo lượt thuê trực tiếp cho $name", Toast.LENGTH_LONG).show()
                                refreshAll()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    }
                    Unit
                }
                if (paymentMethod == "VietQR" && deposit > 0) {
                    showWalkInSheet = false
                    pendingVietQrAmount = deposit
                    pendingVietQrContent = "EASYSPORT WALKIN ${System.currentTimeMillis() % 10000}"
                    pendingVietQrSummary = "Thuê trực tiếp $fieldDetail ($slot) - $name"
                    onVietQrConfirmed = createAction
                } else {
                    createAction()
                }
            }
        )
    }

    extendTarget?.let { target ->
        val courtHourly = courts.firstOrNull { it.id == target.courtId }?.pricePerHour ?: 300000
        ExtendTimeDialog(
            booking = target,
            baseHourlyPrice = courtHourly,
            onDismiss = { extendTarget = null },
            onConfirm = { minutes, extraAmount ->
                extendTarget = null
                scope.launch {
                    StaffOwnerApiClient.addPosServices(target.id, "Gia hạn thêm $minutes phút", extraAmount)
                        .onSuccess {
                            Toast.makeText(
                                context,
                                "Đã gia hạn +$minutes phút (+${extraAmount.vnd()} đ) cho ${target.customerName}",
                                Toast.LENGTH_LONG
                            ).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }

    checkoutTarget?.let { target ->
        CheckOutBottomSheet(
            booking = target,
            onDismiss = { checkoutTarget = null },
            onConfirmCheckOut = { paymentMethod ->
                val id = target.id
                val due = target.remainingAmount
                val apiMethod = if (paymentMethod == "VietQR") "QR" else "CASH"
                val finishCheckout = {
                    scope.launch {
                        StaffOwnerApiClient.checkOutBooking(id, apiMethod)
                            .onSuccess {
                                Toast.makeText(context, "Đã trả sân & thu đủ tiền đơn #$id", Toast.LENGTH_LONG).show()
                                refreshAll()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    }
                    Unit
                }
                checkoutTarget = null
                if (paymentMethod == "VietQR" && due > 0) {
                    pendingVietQrAmount = due
                    pendingVietQrContent = "EASYSPORT $id"
                    pendingVietQrSummary = "Quyết toán trả sân ${target.courtName} (${target.customerName})"
                    onVietQrConfirmed = finishCheckout
                } else {
                    finishCheckout()
                }
            }
        )
    }

    cancelTarget?.let { target ->
        CancelBookingDialog(
            booking = target,
            onDismiss = { cancelTarget = null },
            onConfirmCancel = { reason ->
                val targetId = target.id
                val customer = target.customerName
                cancelTarget = null
                scope.launch {
                    StaffOwnerApiClient.cancelBooking(targetId, reason)
                        .onSuccess {
                            Toast.makeText(context, "Đã hủy đơn của $customer", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    if (showOutOfShiftDialog) {
        AlertDialog(
            onDismissRequest = { showOutOfShiftDialog = false },
            containerColor = Color.White,
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = ProColors.Amber, modifier = Modifier.size(28.dp)) },
            title = { Text("Chưa Vào Ca Trực", fontWeight = FontWeight.Bold, color = EsColors.Title) },
            text = {
                Text(
                    "Bạn đang ngoài ca trực (chưa chấm công vào ca hoặc ca hôm nay đã kết thúc).\n\nĐể đảm bảo tiền mặt và doanh thu được hạch toán đúng vào két của bạn, bạn cần vào ca trực trước khi cho thuê sân hoặc bán lẻ thu tiền.",
                    fontSize = 13.5.sp,
                    color = EsColors.Body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showOutOfShiftDialog = false
                    currentTab = StaffTab.SHIFT
                }) {
                    Text("Đến tab Ca & Két", color = EsColors.Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOutOfShiftDialog = false }) {
                    Text("Đóng", color = EsColors.Body)
                }
            }
        )
    }

    pendingClockOutShift?.let { shift ->
        ClockOutConfirmDialog(
            shift = shift,
            activePlayingCount = bookings.count { it.status == "CHECKED_IN" },
            unpaidTotal = bookings.filter { it.status == "CHECKED_IN" }.sumOf { it.remainingAmount },
            onDismiss = { pendingClockOutShift = null },
            onConfirm = { reason ->
                val id = shift.id
                pendingClockOutShift = null
                scope.launch {
                    StaffOwnerApiClient.clockOutShift(id, reason)
                        .onSuccess {
                            Toast.makeText(context, "Đã kết thúc ca làm việc", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    if (onVietQrConfirmed != null) {
        VietQrPaymentDialog(
            amount = pendingVietQrAmount,
            transferContent = pendingVietQrContent,
            summary = pendingVietQrSummary,
            onDismiss = { onVietQrConfirmed = null },
            onConfirmReceived = {
                val action = onVietQrConfirmed
                onVietQrConfirmed = null
                action?.invoke()
            }
        )
    }
}

@Composable
private fun StaffBottomBar(
    selected: StaffTab,
    waitingCount: Int,
    onSelected: (StaffTab) -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 10.dp,
    ) {
        Column {
            HorizontalDivider(color = ProColors.Border)
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                StaffTab.entries.forEach { tab ->
                    val active = tab == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelected(tab) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) ProColors.PrimarySoft else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (tab == StaffTab.BOOKINGS && waitingCount > 0) {
                                BadgedBox(badge = { Badge(containerColor = ProColors.Rose) { Text(waitingCount.toString()) } }) {
                                    Icon(
                                        tab.icon, tab.label,
                                        tint = if (active) ProColors.Primary else ProColors.Muted,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            } else {
                                Icon(
                                    tab.icon, tab.label,
                                    tint = if (active) ProColors.Primary else ProColors.Muted,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            color = if (active) ProColors.Primary else ProColors.Muted,
                            fontSize = 11.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffPitchesTab(
    courts: List<Court>,
    bookings: List<StaffBookingItem>,
    currentShift: StaffShiftItem?,
    assignedCourt: Court?,
    shiftSummary: StaffShiftSummary?,
    onGoToShiftTab: () -> Unit,
    onWalkInClick: (court: Court?, fieldLabel: String?) -> Unit,
    onCall: (String) -> Unit,
    onCheckIn: (StaffBookingItem) -> Unit,
    onCancelBooking: (StaffBookingItem) -> Unit,
    onGoToPos: (String?) -> Unit,
    onExtendTime: (StaffBookingItem) -> Unit,
    onCheckOut: (StaffBookingItem) -> Unit,
) {
    var showAllBranches by remember { mutableStateOf(false) }

    val branchBookings = remember(bookings, assignedCourt, showAllBranches) {
        if (showAllBranches || assignedCourt == null) bookings
        else bookings.filter { it.courtId == assignedCourt.id }
    }
    val activePlaying = branchBookings.filter { it.status == "CHECKED_IN" }
    val waitingCheckIn = branchBookings.filter { it.status in listOf("PAID", "DEPOSIT_PAID", "PENDING") }
    val revenueBookings = remember(bookings, currentShift, assignedCourt, showAllBranches) {
        when {
            !showAllBranches && currentShift != null -> bookings.filter { bookingMatchesShift(it, currentShift) }
            else -> branchBookings
        }
    }
    val cashCollected = shiftSummary?.totalCash ?: revenueBookings
        .filter { it.status in listOf("CHECKED_IN", "COMPLETED", "PAID", "DEPOSIT_PAID") }
        .sumOf { (if (it.paymentMethod.contains("CASH")) it.depositAmount else 0) + it.serviceAmount }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Branch & Live Shift Card (Alobo / Playtomic style)
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
                        Column(Modifier.weight(1f)) {
                            Text(
                                assignedCourt?.name ?: "Chi nhánh trực quầy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (assignedCourt != null && assignedCourt.address.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    assignedCourt.address,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        val isShiftActive = currentShift?.status == "ACTIVE"
                        ProStatusDotPill(
                            text = when (currentShift?.status) {
                                "ACTIVE" -> "Đang trực ca"
                                "UPCOMING" -> "Chưa vào ca →"
                                else -> "Xem ca →"
                            },
                            dotColor = if (isShiftActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                            textColor = Color.White,
                            bgColor = Color.White.copy(alpha = 0.16f),
                            onClick = onGoToShiftTab,
                        )
                    }
                    if (currentShift != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${currentShift.shiftSlot} · ${currentShift.duty}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroGlassMetric("Đang đá", "${activePlaying.size} sân", Color(0xFF6EE7B7), Modifier.weight(1f))
                        HeroGlassMetric("Chờ nhận", "${waitingCheckIn.size} đơn", Color(0xFFFCD34D), Modifier.weight(1f))
                        HeroGlassMetric("Tiền mặt ca", "${cashCollected.vnd()}đ", Color.White, Modifier.weight(1.25f))
                    }
                }
            }
        }

        // Quick action row (replaces overlapping FAB)
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onWalkInClick(assignedCourt, null) },
                    modifier = Modifier.weight(1.15f).height(44.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cho thuê sân", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onGoToPos(null) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(1.dp, ProColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = ProColors.Ink)
                ) {
                    Icon(Icons.Outlined.LocalDrink, contentDescription = null, modifier = Modifier.size(17.dp), tint = ProColors.Primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Bán lẻ POS", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, maxLines = 1)
                }
            }
        }

        // Segmented pill toggle matching Customer WeekMonthToggle
        item {
            EsSegmentedControl(
                leftLabel = "Sân chi nhánh (${assignedCourt?.fields?.size ?: 0})",
                rightLabel = "Tất cả cụm (${courts.size})",
                isLeftSelected = !showAllBranches,
                onSelectLeft = { showAllBranches = false },
                onSelectRight = { showAllBranches = true },
            )
        }

        if (!showAllBranches && assignedCourt != null && assignedCourt.fields.isNotEmpty()) {
            item {
                ProSectionHeader(
                    title = "Sơ đồ sân · ${assignedCourt.name}",
                    actionText = "+ Thuê nhanh",
                    onAction = { onWalkInClick(assignedCourt, null) }
                )
            }
            items(assignedCourt.fields) { field ->
                val playing = activePlaying.firstOrNull {
                    it.fieldDetail.contains(field.label, ignoreCase = true)
                }
                val waiting = waitingCheckIn.firstOrNull {
                    it.fieldDetail.contains(field.label, ignoreCase = true)
                }

                PitchSlotCard(
                    title = field.label,
                    subtitle = field.detail,
                    emptySubtitle = "Giá ${assignedCourt.pricePerHour.vnd()} đ/giờ · Sẵn sàng đón khách",
                    playing = playing,
                    waiting = waiting,
                    onWalkIn = { onWalkInClick(assignedCourt, field.label) },
                    onCall = onCall,
                    onCheckIn = onCheckIn,
                    onCancel = onCancelBooking,
                    onGoToPos = { id -> onGoToPos(id) },
                    onExtendTime = onExtendTime,
                    onCheckOut = onCheckOut
                )
            }
        } else {
            item {
                ProSectionHeader(
                    title = "Tất cả cụm sân (${courts.size})",
                    actionText = "+ Thuê nhanh",
                    onAction = { onWalkInClick(assignedCourt, null) }
                )
            }
            items(courts) { court ->
                val playing = bookings.firstOrNull { it.status == "CHECKED_IN" && it.courtId == court.id }
                val waiting = bookings.firstOrNull { it.status in listOf("PAID", "DEPOSIT_PAID", "PENDING") && it.courtId == court.id }

                PitchSlotCard(
                    title = court.name,
                    subtitle = "${court.sportType} · ${court.fields.size} sân con",
                    emptySubtitle = "Giá ${court.pricePerHour.vnd()} đ/giờ · Sân trống sẵn sàng",
                    playing = playing,
                    waiting = waiting,
                    onWalkIn = { onWalkInClick(court, null) },
                    onCall = onCall,
                    onCheckIn = onCheckIn,
                    onCancel = onCancelBooking,
                    onGoToPos = { id -> onGoToPos(id) },
                    onExtendTime = onExtendTime,
                    onCheckOut = onCheckOut
                )
            }
        }
    }
}

@Composable
private fun HeroGlassMetric(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun SegmentedToggleItem(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = if (selected) ProColors.Primary else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else ProColors.SubInk,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun PitchSlotCard(
    title: String,
    subtitle: String,
    emptySubtitle: String,
    playing: StaffBookingItem?,
    waiting: StaffBookingItem?,
    onWalkIn: () -> Unit,
    onCall: (String) -> Unit,
    onCheckIn: (StaffBookingItem) -> Unit,
    onCancel: (StaffBookingItem) -> Unit,
    onGoToPos: (String) -> Unit,
    onExtendTime: (StaffBookingItem) -> Unit,
    onCheckOut: (StaffBookingItem) -> Unit,
) {
    val stripeColor = when {
        playing != null -> ProColors.Emerald
        waiting != null -> ProColors.Amber
        else -> ProColors.Primary
    }
    ProCard(
        accentBorderColor = when {
            playing != null -> ProColors.Emerald.copy(alpha = 0.35f)
            waiting != null -> ProColors.Amber.copy(alpha = 0.35f)
            else -> null
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
            )
            Column(Modifier.weight(1f).padding(15.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        playing != null -> ProColors.EmeraldBg
                                        waiting != null -> ProColors.AmberBg
                                        else -> ProColors.PrimarySoft
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.SportsSoccer,
                                contentDescription = null,
                                tint = stripeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = ProColors.Ink)
                            if (subtitle.isNotBlank()) {
                                Text(subtitle, fontSize = 12.sp, color = ProColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    when {
                        playing != null -> ProStatusDotPill("Đang đá", ProColors.Emerald, ProColors.Emerald, ProColors.EmeraldBg)
                        waiting != null -> ProStatusDotPill("Chờ nhận", ProColors.Amber, ProColors.Amber, ProColors.AmberBg)
                        else -> ProStatusDotPill("Sân trống", ProColors.Primary, ProColors.Primary, ProColors.PrimarySoft)
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    playing != null -> {
                        Surface(
                            color = ProColors.Surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "${playing.customerName} · ${playing.customerPhone}",
                                    fontSize = 13.sp,
                                    color = ProColors.Ink,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Khung giờ: ${playing.timeRange}", fontSize = 12.sp, color = ProColors.SubInk)
                                if (playing.serviceNote.isNotEmpty()) {
                                    Text(
                                        "Dịch vụ: ${formatServiceSummary(playing.serviceNote)} (+${playing.serviceAmount.vnd()} đ)",
                                        fontSize = 12.sp,
                                        color = ProColors.Primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (playing.remainingAmount > 0) {
                                    Text(
                                        "Còn thu: ${playing.remainingAmount.vnd()} đ",
                                        fontSize = 12.5.sp,
                                        color = ProColors.Amber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        CheckedInBookingActions(
                            onAddPos = { onGoToPos(playing.id) },
                            onExtendTime = { onExtendTime(playing) },
                            onCheckOut = { onCheckOut(playing) }
                        )
                    }
                    waiting != null -> {
                        Surface(
                            color = ProColors.AmberBg.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "Đặt trước: ${waiting.customerName} (${waiting.customerPhone})",
                                    fontSize = 13.sp,
                                    color = ProColors.Ink,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Khung giờ: ${waiting.timeRange}", fontSize = 12.sp, color = ProColors.SubInk)
                                Text(
                                    "Còn phải thu tại sân: ${waiting.remainingAmount.vnd()} đ",
                                    fontSize = 12.5.sp,
                                    color = ProColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (waiting.customerPhone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onCall(waiting.customerPhone) },
                                    modifier = Modifier.height(40.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    shape = RoundedCornerShape(11.dp),
                                    border = BorderStroke(1.dp, ProColors.Border)
                                ) {
                                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = ProColors.Primary)
                                }
                            }
                            OutlinedButton(
                                onClick = { onCancel(waiting) },
                                modifier = Modifier.height(40.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                shape = RoundedCornerShape(11.dp),
                                border = BorderStroke(1.dp, ProColors.Rose.copy(alpha = 0.45f))
                            ) {
                                Text("Hủy", color = ProColors.Rose, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { onCheckIn(waiting) },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                            ) {
                                Text("Nhận sân", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                    else -> {
                        Text(emptySubtitle, fontSize = 12.5.sp, color = ProColors.Muted)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onWalkIn,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(11.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProColors.PrimarySoft,
                                contentColor = ProColors.Primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cho thuê $title ngay", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckedInBookingActions(
    onAddPos: () -> Unit,
    onExtendTime: () -> Unit,
    onCheckOut: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            onClick = onAddPos,
            modifier = Modifier.weight(1f).height(38.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            border = BorderStroke(1.dp, ProColors.Border)
        ) {
            Text("+ Nước", color = ProColors.Primary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        OutlinedButton(
            onClick = onExtendTime,
            modifier = Modifier.weight(1f).height(38.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            border = BorderStroke(1.dp, ProColors.Primary.copy(alpha = 0.4f))
        ) {
            Text("Gia hạn", color = ProColors.Primary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Button(
            onClick = onCheckOut,
            modifier = Modifier.weight(1f).height(38.dp),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
        ) {
            Text("Trả sân", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun StaffBookingsTab(
    bookings: List<StaffBookingItem>,
    currentShift: StaffShiftItem?,
    onWalkInClick: () -> Unit,
    onCall: (String) -> Unit,
    onCheckIn: (StaffBookingItem) -> Unit,
    onCancelBooking: (StaffBookingItem) -> Unit,
    onExtendTime: (StaffBookingItem) -> Unit,
    onCheckOut: (StaffBookingItem) -> Unit,
    onAddPos: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember(currentShift?.id) { mutableStateOf(if (currentShift != null) "SHIFT" else "ALL") }

    val shiftMatchedCount = remember(bookings, currentShift) {
        if (currentShift == null) 0 else bookings.count { bookingMatchesShift(it, currentShift) }
    }

    val filtered = remember(bookings, query, filter, currentShift) {
        bookings.filter { b ->
            val matchStatus = when (filter) {
                "SHIFT" -> currentShift != null && bookingMatchesShift(b, currentShift)
                "WAITING" -> b.status in listOf("PAID", "DEPOSIT_PAID", "PENDING")
                "ACTIVE" -> b.status == "CHECKED_IN"
                "DONE" -> b.status == "COMPLETED"
                "CANCELLED" -> b.status == "CANCELLED"
                else -> true
            }
            val q = query.trim().lowercase()
            val matchQuery = q.isEmpty() || b.customerName.lowercase().contains(q) ||
                    b.customerPhone.contains(q) || b.id.lowercase().contains(q) || b.courtName.lowercase().contains(q)
            matchStatus && matchQuery
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (currentShift != null) ProColors.PrimarySoft else Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (currentShift != null) ProColors.Primary.copy(alpha = 0.25f) else ProColors.Border),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (currentShift != null) ProColors.Primary else ProColors.Muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (currentShift != null) {
                            "${currentShift.shiftSlot} · ${currentShift.courtName} (${currentShift.shiftDate})"
                        } else {
                            "Chưa có ca trực hôm nay"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (currentShift != null) ProColors.Primary else ProColors.SubInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Button(
                onClick = onWalkInClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text("Thuê sân", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tìm tên khách, SĐT, mã đơn...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ProColors.Muted) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
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
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = buildList {
                if (currentShift != null) {
                    add("SHIFT" to "Trong ca ($shiftMatchedCount)")
                }
                add("ALL" to "Tất cả (${bookings.size})")
                add("WAITING" to "Chờ nhận (${bookings.count { it.status in listOf("PAID", "DEPOSIT_PAID", "PENDING") }})")
                add("ACTIVE" to "Đang đá (${bookings.count { it.status == "CHECKED_IN" }})")
                add("DONE" to "Hoàn tất (${bookings.count { it.status == "COMPLETED" }})")
                add("CANCELLED" to "Đã hủy (${bookings.count { it.status == "CANCELLED" }})")
            }
            items(filters) { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            EmptyStateCard(
                title = "Không tìm thấy đơn đặt sân",
                subtitle = "Thử đổi bộ lọc hoặc bấm nút + Thuê sân ở phía trên để tạo đơn mới.",
                actionLabel = if (query.isNotEmpty() || filter != "ALL") "Xem tất cả đơn" else null,
                onAction = { query = ""; filter = "ALL" }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { b ->
                    val stripeColor = when (b.status.uppercase()) {
                        "CHECKED_IN" -> ProColors.Emerald
                        "COMPLETED" -> ProColors.Emerald
                        "PAID", "DEPOSIT_PAID", "PENDING" -> ProColors.Amber
                        else -> ProColors.Muted
                    }
                    ProCard {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(Modifier.width(5.dp).fillMaxHeight().background(stripeColor))
                            Column(Modifier.weight(1f).padding(15.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        ProAvatar(b.customerName, size = 34.dp)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                b.customerName,
                                                fontWeight = FontWeight.Bold,
                                                color = ProColors.Ink,
                                                fontSize = 14.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("#${b.id} · ${b.customerPhone}", fontSize = 12.sp, color = ProColors.Muted)
                                        }
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    BookingStatusBadge(b.status)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${b.courtName} (${b.fieldDetail})",
                                        fontSize = 13.sp,
                                        color = ProColors.Ink,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (currentShift != null && bookingMatchesShift(b, currentShift)) {
                                        ProStatusDotPill("Trong ca", ProColors.Primary, ProColors.Primary, ProColors.PrimarySoft)
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("${b.dateLabel} · ${b.timeRange}", fontSize = 12.5.sp, color = ProColors.SubInk)
                                if (b.serviceNote.isNotEmpty()) {
                                    Spacer(Modifier.height(3.dp))
                                    Text("POS: ${formatServiceSummary(b.serviceNote)} (+${b.serviceAmount.vnd()} đ)", fontSize = 12.sp, color = ProColors.Primary, fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ProColors.Surface)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tổng: ${(b.totalPrice + b.serviceAmount).vnd()} đ", fontSize = 12.5.sp, color = ProColors.SubInk)
                                    Text(
                                        "Còn thu: ${b.remainingAmount.vnd()} đ",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (b.remainingAmount > 0) ProColors.Amber else ProColors.Emerald
                                    )
                                }
                                val hasActions = b.customerPhone.isNotBlank() || b.status in listOf("PAID", "DEPOSIT_PAID", "PENDING", "CHECKED_IN")
                                if (hasActions) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (b.customerPhone.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = { onCall(b.customerPhone) },
                                                modifier = Modifier.height(38.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, ProColors.Border)
                                            ) {
                                                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = ProColors.Primary)
                                            }
                                        }
                                        if (b.status in listOf("PAID", "DEPOSIT_PAID", "PENDING")) {
                                            OutlinedButton(
                                                onClick = { onCancelBooking(b) },
                                                modifier = Modifier.height(38.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, ProColors.Rose.copy(alpha = 0.45f))
                                            ) {
                                                Text("Hủy", color = ProColors.Rose, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Button(
                                                onClick = { onCheckIn(b) },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                                            ) {
                                                Text("Nhận sân", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        } else if (b.status == "CHECKED_IN") {
                                            Box(Modifier.weight(1f)) {
                                                CheckedInBookingActions(
                                                    onAddPos = { onAddPos(b.id) },
                                                    onExtendTime = { onExtendTime(b) },
                                                    onCheckOut = { onCheckOut(b) }
                                                )
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
    }
}

@Composable
private fun StaffPosTab(
    bookings: List<StaffBookingItem>,
    assignedCourt: Court?,
    initialSelectedBookingId: String?,
    retailSales: List<PosSaleItem>,
    onSubmitPos: (targetBookingId: String?, note: String, amount: Int, method: String, onReset: () -> Unit) -> Unit,
) {
    val activeBookings = remember(bookings, assignedCourt) {
        bookings.filter { it.status == "CHECKED_IN" }
            .sortedByDescending { assignedCourt != null && it.courtId == assignedCourt.id }
    }
    var selectedBookingId by remember(initialSelectedBookingId, activeBookings) {
        mutableStateOf(
            if (initialSelectedBookingId != null && activeBookings.any { it.id == initialSelectedBookingId }) {
                initialSelectedBookingId
            } else {
                activeBookings.firstOrNull()?.id
            }
        )
    }
    var paymentMethod by remember { mutableStateOf("Tiền mặt") }

    var reviveCount by remember { mutableIntStateOf(0) }
    var waterCount by remember { mutableIntStateOf(0) }
    var bibCount by remember { mutableIntStateOf(0) }
    var redbullCount by remember { mutableIntStateOf(0) }

    val total = reviveCount * 15000 + waterCount * 10000 + bibCount * 50000 + redbullCount * 20000
    val itemCount = reviveCount + waterCount + bibCount + redbullCount
    val retailTotalToday = remember(retailSales) { retailSales.sumOf { it.amount } }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ProCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Chọn đối tượng tính tiền", fontWeight = FontWeight.Bold, color = ProColors.Ink, fontSize = 14.5.sp)
                            if (retailSales.isNotEmpty()) {
                                ProStatusDotPill(
                                    text = "Bán lẻ: ${retailSales.size} đơn · ${retailTotalToday.vnd()} đ",
                                    dotColor = ProColors.Emerald,
                                    textColor = ProColors.Emerald,
                                    bgColor = ProColors.EmeraldBg
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedBookingId == null,
                                    onClick = { selectedBookingId = null },
                                    label = { Text("Khách mua lẻ (Thu ngay)", maxLines = 1) }
                                )
                            }
                            items(activeBookings) { b ->
                                FilterChip(
                                    selected = selectedBookingId == b.id,
                                    onClick = { selectedBookingId = b.id },
                                    label = { Text("${b.fieldDetail} · ${b.customerName}", maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ProSectionHeader("2. Đồ uống & Dụng cụ ($itemCount món)")
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PosGridCard(
                        name = "Revive chanh muối",
                        price = 15000,
                        count = reviveCount,
                        icon = Icons.Outlined.LocalDrink,
                        tint = ProColors.Emerald,
                        bg = ProColors.EmeraldBg,
                        onMinus = { if (reviveCount > 0) reviveCount-- },
                        onPlus = { reviveCount++ },
                        modifier = Modifier.weight(1f)
                    )
                    PosGridCard(
                        name = "Nước khoáng 500ml",
                        price = 10000,
                        count = waterCount,
                        icon = Icons.Rounded.WaterDrop,
                        tint = ProColors.Sky,
                        bg = ProColors.SkyBg,
                        onMinus = { if (waterCount > 0) waterCount-- },
                        onPlus = { waterCount++ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PosGridCard(
                        name = "Thuê bộ áo bib (14c)",
                        price = 50000,
                        count = bibCount,
                        icon = Icons.Rounded.Checkroom,
                        tint = ProColors.Amber,
                        bg = ProColors.AmberBg,
                        onMinus = { if (bibCount > 0) bibCount-- },
                        onPlus = { bibCount++ },
                        modifier = Modifier.weight(1f)
                    )
                    PosGridCard(
                        name = "Redbull Thái 250ml",
                        price = 20000,
                        count = redbullCount,
                        icon = Icons.Rounded.Bolt,
                        tint = ProColors.Rose,
                        bg = ProColors.RoseBg,
                        onMinus = { if (redbullCount > 0) redbullCount-- },
                        onPlus = { redbullCount++ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Sticky bottom POS checkout bar
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, ProColors.Border)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (selectedBookingId == null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thu tiền lẻ:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ProColors.SubInk)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Tiền mặt", "VietQR").forEach { m ->
                                FilterChip(
                                    selected = paymentMethod == m,
                                    onClick = { paymentMethod = m },
                                    label = { Text(m) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tổng cộng ($itemCount món)", fontSize = 12.sp, color = ProColors.Muted)
                        Text("${total.vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ProColors.Primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val parts = buildList {
                                if (reviveCount > 0) add("${reviveCount}x Revive")
                                if (waterCount > 0) add("${waterCount}x Nước suối")
                                if (bibCount > 0) add("${bibCount}x Áo bib")
                                if (redbullCount > 0) add("${redbullCount}x Redbull")
                            }
                            onSubmitPos(selectedBookingId, parts.joinToString(", "), total, paymentMethod) {
                                reviveCount = 0; waterCount = 0; bibCount = 0; redbullCount = 0
                            }
                        },
                        enabled = total > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        if (selectedBookingId == null && paymentMethod == "VietQR") {
                            Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = when {
                                selectedBookingId != null -> "Ghi vào hóa đơn"
                                paymentMethod == "VietQR" -> "Hiện mã QR"
                                else -> "Thu Tiền mặt"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosGridCard(
    name: String,
    price: Int,
    count: Int,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProCard(
        modifier = modifier,
        accentBorderColor = if (count > 0) ProColors.Primary.copy(alpha = 0.6f) else null
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
                if (count > 0) {
                    Surface(color = ProColors.Primary, shape = CircleShape) {
                        Text(
                            "x$count",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = ProColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Text("${price.vnd()} đ", fontSize = 13.sp, color = ProColors.Primary, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ProColors.Surface)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onMinus, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Giảm", tint = ProColors.Ink, modifier = Modifier.size(16.dp))
                }
                Text(
                    count.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (count > 0) ProColors.Primary else ProColors.Muted
                )
                IconButton(onClick = onPlus, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = "Tăng", tint = ProColors.Primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StaffShiftTab(
    shifts: List<StaffShiftItem>,
    bookings: List<StaffBookingItem>,
    myReports: List<ShiftReportItem>,
    shiftSummary: StaffShiftSummary?,
    isSubmittingReport: Boolean,
    onClockIn: (String) -> Unit,
    onClockOut: (StaffShiftItem) -> Unit,
    onSubmitReport: (shift: StaffShiftItem, cash: Int, qr: Int, count: Int, note: String) -> Unit,
    onResubmitReport: (reportId: String, cash: Int, qr: Int, note: String) -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val today = remember { LocalDate.now() }
    val todayStr = remember(today) { today.format(dateFormatter) }

    val activeShift = remember(shifts, todayStr) {
        shifts.firstOrNull { it.status == "ACTIVE" }
            ?: shifts.firstOrNull { it.status == "UPCOMING" && it.shiftDate == todayStr }
            ?: shifts.firstOrNull {
                it.status == "UPCOMING" && runCatching {
                    !LocalDate.parse(it.shiftDate, dateFormatter).isBefore(today)
                }.getOrDefault(false)
            }
    }
    val handoverShift = remember(shifts, todayStr) {
        shifts.firstOrNull { it.status == "ACTIVE" }
            ?: shifts.lastOrNull { it.status == "COMPLETED" && it.shiftDate == todayStr }
    }
    val existingReport = remember(handoverShift, myReports) {
        handoverShift?.let { hs -> myReports.firstOrNull { it.shiftId == hs.id } }
    }
    val shiftCardTitle = when {
        activeShift == null -> ""
        activeShift.status == "ACTIVE" || activeShift.shiftDate == todayStr -> "Ca trực hôm nay"
        else -> "Ca sắp tới"
    }

    var note by remember { mutableStateOf("") }

    val shiftBookings = remember(bookings, handoverShift) {
        if (handoverShift != null) bookings.filter { bookingMatchesShift(it, handoverShift) } else emptyList()
    }
    val fallbackCash = shiftBookings.filter { it.status in listOf("CHECKED_IN", "COMPLETED", "PAID") }
        .sumOf { (if (it.paymentMethod.contains("CASH")) it.depositAmount else 0) + it.serviceAmount }
    val fallbackQr = shiftBookings.filter { !it.paymentMethod.contains("CASH") && it.status in listOf("PAID", "DEPOSIT_PAID", "CHECKED_IN", "COMPLETED") }
        .sumOf { it.depositAmount }
    val cash = shiftSummary?.totalCash ?: fallbackCash
    val qr = shiftSummary?.totalQr ?: fallbackQr
    val reportOrderCount = if (shiftSummary != null) {
        shiftSummary.orderCount + shiftSummary.retailCount
    } else {
        shiftBookings.size
    }

    val weeklyHours = remember(shifts, today) {
        val monday = today.minusDays(((today.dayOfWeek.value - 1).toLong()))
        val sunday = monday.plusDays(6)
        shifts.sumOf { s ->
            val d = runCatching { LocalDate.parse(s.shiftDate, dateFormatter) }.getOrNull()
            if (d != null && !d.isBefore(monday) && !d.isAfter(sunday)) {
                parseShiftHours(s.shiftSlot)
            } else 0.0
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (activeShift != null) {
            item {
                ProCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ProHeroBrush)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(shiftCardTitle, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                val isActive = activeShift.status == "ACTIVE"
                                ProStatusDotPill(
                                    text = if (isActive) "Đang trực ca" else "Sắp diễn ra",
                                    dotColor = if (isActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    textColor = Color.White,
                                    bgColor = Color.White.copy(alpha = 0.16f)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(activeShift.shiftSlot, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${activeShift.shiftDate} · ${activeShift.courtName}",
                                fontSize = 12.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Column(Modifier.padding(16.dp)) {
                        Text("Nhiệm vụ: ${activeShift.duty}", fontSize = 13.sp, color = ProColors.SubInk)
                        if (activeShift.checkInTime != null) {
                            Spacer(Modifier.height(4.dp))
                            Text("Đã chấm công vào: ${activeShift.checkInTime}", fontSize = 13.sp, color = ProColors.Emerald, fontWeight = FontWeight.Medium)
                        }
                        if (activeShift.lateMinutes != null && activeShift.lateMinutes > 0) {
                            Text("Vào muộn ${activeShift.lateMinutes} phút", fontSize = 12.5.sp, color = ProColors.Amber, fontWeight = FontWeight.SemiBold)
                        }
                        if (activeShift.checkOutTime != null) {
                            Text("Đã kết thúc ca: ${activeShift.checkOutTime}", fontSize = 13.sp, color = ProColors.Muted)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { onClockIn(activeShift.id) },
                                enabled = activeShift.status == "UPCOMING",
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Chấm công vào", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { onClockOut(activeShift) },
                                enabled = activeShift.status == "ACTIVE",
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                            ) {
                                Text("Kết thúc ca", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                EmptyStateCard(
                    title = "Hôm nay bạn không có ca trực",
                    subtitle = "Quản lý sẽ xếp lịch ca trực cho bạn tại mục Phân ca NV."
                )
            }
        }

        item {
            ProCard {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(ProColors.PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Payments, contentDescription = null, tint = ProColors.Primary, modifier = Modifier.size(18.dp))
                            }
                            Text("Bàn giao két tiền cuối ca", fontWeight = FontWeight.Bold, color = ProColors.Ink, fontSize = 15.5.sp)
                        }
                        if (existingReport != null) {
                            val (statusText, statusColor, statusBg) = when (existingReport.status) {
                                "CONFIRMED" -> Triple("Đã xác nhận", ProColors.Emerald, ProColors.EmeraldBg)
                                "DISCREPANCY" -> Triple("Cần kiểm lại", ProColors.Rose, ProColors.RoseBg)
                                else -> Triple("Chờ duyệt", ProColors.Amber, ProColors.AmberBg)
                            }
                            ProStatusDotPill(
                                text = statusText,
                                dotColor = statusColor,
                                textColor = statusColor,
                                bgColor = statusBg,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    when {
                        handoverShift == null -> {
                            Text(
                                "Bạn cần chấm công vào ca trực hôm nay trước khi nộp báo cáo bàn giao két.",
                                fontSize = 13.sp,
                                color = ProColors.Muted
                            )
                            Spacer(Modifier.height(12.dp))
                            EsPrimaryButton(
                                text = "Chưa có ca trực để bàn giao",
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {}
                            )
                        }
                        existingReport != null -> {
                            Text(
                                "${handoverShift.shiftSlot} · ${handoverShift.courtName} (${existingReport.shiftDate})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ProColors.Primary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CashTileBox("Tiền mặt đã nộp", "${existingReport.cashAmount.vnd()} đ", ProColors.Primary, Modifier.weight(1f))
                                CashTileBox("VietQR đã nộp", "${existingReport.qrAmount.vnd()} đ", ProColors.Emerald, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Số đơn trong ca: ${existingReport.totalOrders} · Ghi chú: ${existingReport.handoverNote}",
                                fontSize = 12.5.sp,
                                color = ProColors.SubInk
                            )
                            if (existingReport.confirmedBy != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    if (existingReport.status == "DISCREPANCY")
                                        "Phản hồi chủ sân: ${existingReport.confirmedBy}"
                                    else
                                        "Người duyệt: ${existingReport.confirmedBy}",
                                    fontSize = 12.5.sp,
                                    color = if (existingReport.status == "DISCREPANCY") ProColors.Rose else ProColors.Emerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            if (existingReport.status == "DISCREPANCY") {
                                var resubmitNote by remember(existingReport.id) { mutableStateOf("") }
                                OutlinedTextField(
                                    value = resubmitNote,
                                    onValueChange = { resubmitNote = it },
                                    placeholder = { Text("Giải trình chênh lệch / xác nhận đã bù đủ két...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2
                                )
                                Spacer(Modifier.height(10.dp))
                                EsPrimaryButton(
                                    text = if (isSubmittingReport) "Đang gửi..." else "Gửi lại giải trình đối soát",
                                    enabled = !isSubmittingReport && resubmitNote.trim().isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        onResubmitReport(
                                            existingReport.id,
                                            existingReport.cashAmount,
                                            existingReport.qrAmount,
                                            resubmitNote.trim()
                                        )
                                        resubmitNote = ""
                                    }
                                )
                            } else {
                                EsPrimaryButton(
                                    text = "Đã nộp báo cáo bàn giao ca này",
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {}
                                )
                            }
                        }
                        else -> {
                            Text(
                                "${handoverShift.shiftSlot} · ${handoverShift.courtName} · $reportOrderCount đơn (${shiftSummary?.retailCount ?: 0} POS lẻ)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ProColors.Primary
                            )
                            if ((shiftSummary?.unpaidActiveCount ?: 0) > 0) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = ProColors.AmberBg,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ProColors.Amber.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = ProColors.Amber, modifier = Modifier.size(17.dp))
                                        Text(
                                            "Còn ${shiftSummary?.unpaidActiveCount} sân đang đá chưa tất toán (${(shiftSummary?.unpaidTotal ?: 0).vnd()} đ)",
                                            fontSize = 12.sp,
                                            color = ProColors.Amber,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CashTileBox("Tiền mặt két", "${cash.vnd()} đ", ProColors.Primary, Modifier.weight(1f))
                                CashTileBox("VietQR", "${qr.vnd()} đ", ProColors.Emerald, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                placeholder = { Text("Ghi chú bàn giao két, bóng, lưới sân...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2
                            )
                            Spacer(Modifier.height(12.dp))
                            EsPrimaryButton(
                                text = if (isSubmittingReport) "Đang nộp báo cáo..." else "Nộp báo cáo két & Kết thúc ca",
                                enabled = !isSubmittingReport,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onSubmitReport(handoverShift, cash, qr, reportOrderCount, note)
                                    note = ""
                                }
                            )
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
                ProSectionHeader("Lịch làm việc (${shifts.size})", modifier = Modifier.weight(1f))
                val hoursStr = if (weeklyHours % 1.0 == 0.0) weeklyHours.toInt().toString() else String.format("%.1f", weeklyHours)
                ProStatusDotPill("Tuần này: ${hoursStr}h/48h", ProColors.Primary, ProColors.Primary, ProColors.PrimarySoft)
            }
        }

        if (shifts.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Chưa có ca trực được phân",
                    subtitle = "Quản lý sẽ xếp lịch ca trực cho bạn tại mục Phân ca NV."
                )
            }
        } else {
            items(shifts) { s ->
                ProCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ProColors.PrimarySoft)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(s.shiftDate.take(5), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ProColors.Primary)
                            Text(s.shiftDate.takeLast(4), fontSize = 10.5.sp, color = ProColors.Muted)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(s.shiftSlot, fontWeight = FontWeight.Bold, color = ProColors.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.width(6.dp))
                                ShiftStatusBadge(s.status)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text("${s.courtName} · ${s.duty}", fontSize = 12.5.sp, color = ProColors.SubInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (s.checkInTime != null) {
                                Text(
                                    "Chấm công: ${s.checkInTime}${s.checkOutTime?.let { " - $it" } ?: ""}",
                                    fontSize = 12.sp,
                                    color = ProColors.Emerald
                                )
                            }
                            if (s.lateMinutes != null && s.lateMinutes > 0) {
                                Text("Vào muộn ${s.lateMinutes} phút", fontSize = 12.sp, color = ProColors.Amber)
                            }
                            if (s.notes.isNotEmpty()) Text("Ghi chú: ${s.notes}", fontSize = 12.sp, color = ProColors.Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashTileBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ProColors.Surface)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.5.sp, color = ProColors.Muted, maxLines = 1)
    }
}

private fun parseShiftHours(slotLabel: String): Double {
    val match = Regex("(\\d{2}:\\d{2})\\s*-\\s*(\\d{2}:\\d{2})").find(slotLabel) ?: return 0.0
    return runCatching {
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val s = LocalTime.parse(match.groupValues[1], fmt)
        val e = LocalTime.parse(match.groupValues[2], fmt)
        Duration.between(s, e).toMinutes() / 60.0
    }.getOrDefault(0.0)
}

private fun bookingMatchesShift(booking: StaffBookingItem, shift: StaffShiftItem): Boolean {
    if (shift.courtId.isNotBlank() && booking.courtId.isNotBlank() && shift.courtId != booking.courtId) {
        return false
    }
    val bookingDate = Regex("(\\d{2}/\\d{2}/\\d{4})").find(booking.dateLabel)?.groupValues?.get(1) ?: return false
    if (bookingDate != shift.shiftDate) return false
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val shiftTimes = Regex("(\\d{2}:\\d{2})").findAll(shift.shiftSlot).mapNotNull {
        runCatching { LocalTime.parse(it.groupValues[1], fmt) }.getOrNull()
    }.toList()
    val bookingTimes = Regex("(\\d{2}:\\d{2})").findAll(booking.timeRange).mapNotNull {
        runCatching { LocalTime.parse(it.groupValues[1], fmt) }.getOrNull()
    }.toList()
    if (shiftTimes.size >= 2 && bookingTimes.isNotEmpty()) {
        val sStart = shiftTimes.first()
        val sEnd = shiftTimes.last()
        val bStart = bookingTimes.first()
        val bEnd = if (bookingTimes.size >= 2) bookingTimes.last() else bStart.plusHours(1)
        return bStart.isBefore(sEnd) && bEnd.isAfter(sStart)
    }
    return true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckOutBottomSheet(
    booking: StaffBookingItem,
    onDismiss: () -> Unit,
    onConfirmCheckOut: (paymentMethod: String) -> Unit,
) {
    var returnedBall by remember { mutableStateOf(true) }
    var returnedBib by remember { mutableStateOf(true) }
    var paymentMethod by remember { mutableStateOf("Tiền mặt") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Quyết Toán & Trả Sân", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EsColors.Title)
            Text("${booking.courtName} · ${booking.customerName}", fontSize = 13.sp, color = EsColors.Body)
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = EsColors.Surface), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tiền thuê sân", color = EsColors.Body, fontSize = 13.sp)
                        Text("${booking.totalPrice.vnd()} đ", fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Đã thanh toán / cọc", color = EsColors.Body, fontSize = 13.sp)
                        Text("- ${booking.depositAmount.vnd()} đ", color = EsColors.Success, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Dịch vụ POS thêm", color = EsColors.Body, fontSize = 13.sp)
                        Text("+ ${booking.serviceAmount.vnd()} đ", color = EsColors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = EsColors.Border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cần thu thêm", fontWeight = FontWeight.Bold, color = EsColors.Title)
                        Text("${booking.remainingAmount.vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EsColors.Primary)
                    }
                }
            }
            if (booking.remainingAmount > 0) {
                Spacer(Modifier.height(10.dp))
                Text("Hình thức thu phần còn lại:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EsColors.Title)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Tiền mặt", "VietQR").forEach { m ->
                        FilterChip(selected = paymentMethod == m, onClick = { paymentMethod = m }, label = { Text(m) })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { returnedBall = !returnedBall }) {
                Checkbox(checked = returnedBall, onCheckedChange = { returnedBall = it })
                Text("Đã thu hồi đủ bóng thi đấu", fontSize = 13.5.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { returnedBib = !returnedBib }) {
                Checkbox(checked = returnedBib, onCheckedChange = { returnedBib = it })
                Text("Đã thu hồi đủ áo bib & kiểm tra sân", fontSize = 13.5.sp)
            }
            Spacer(Modifier.height(12.dp))
            EsPrimaryButton(
                text = if (paymentMethod == "VietQR" && booking.remainingAmount > 0)
                    "Hiện mã VietQR (${booking.remainingAmount.vnd()} đ)"
                else
                    "Xác nhận đã thu ${booking.remainingAmount.vnd()} đ & Trả sân",
                enabled = returnedBall && returnedBib,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onConfirmCheckOut(paymentMethod) }
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalkInBottomSheet(
    courts: List<Court>,
    assignedCourt: Court?,
    initialCourt: Court?,
    initialField: String?,
    onDismiss: () -> Unit,
    onConfirm: (courtId: String, fieldDetail: String, slot: String, name: String, phone: String, price: Int, deposit: Int, remaining: Int, paymentMethod: String) -> Unit,
) {
    var selectedCourt by remember(initialCourt, assignedCourt, courts) {
        mutableStateOf(initialCourt ?: assignedCourt ?: courts.firstOrNull())
    }
    var showBranchSelector by remember(initialCourt, assignedCourt) {
        mutableStateOf(assignedCourt == null || (initialCourt != null && initialCourt.id != assignedCourt.id))
    }
    var selectedField by remember(selectedCourt, initialField) {
        mutableStateOf(
            initialField?.takeIf { f -> selectedCourt?.fields?.any { it.label == f } == true }
                ?: selectedCourt?.fields?.firstOrNull()?.label
                ?: "Sân 5 người"
        )
    }

    val now = remember { LocalTime.now() }
    val dynamicSlots = remember(now) {
        val list = mutableListOf<String>()
        val startH = now.hour.coerceIn(6, 22)
        list.add(String.format("%02d:00 - %02d:30 (Đá ngay)", startH, (startH + 1).coerceAtMost(23)))
        list.add(String.format("%02d:30 - %02d:00", startH, (startH + 2).coerceAtMost(23)))
        val nextH = (startH + 1).coerceAtMost(21)
        list.add(String.format("%02d:00 - %02d:30", nextH, nextH + 1))
        list.add(String.format("%02d:30 - %02d:00", nextH, nextH + 2))
        list.distinct()
    }
    var selectedSlot by remember(dynamicSlots) { mutableStateOf(dynamicSlots.firstOrNull() ?: "18:00 - 19:30 (Đá ngay)") }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var paymentOption by remember { mutableStateOf("Tiền mặt") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cho Khách Thuê Trực Tiếp", fontWeight = FontWeight.Bold, fontSize = 17.5.sp, color = EsColors.Title)
                AssistChip(
                    onClick = {
                        name = "Khách vãng lai"
                        phone = "0900000000"
                    },
                    label = { Text("⚡ Điền nhanh", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))

            if (!showBranchSelector && selectedCourt != null) {
                Surface(
                    color = EsColors.SurfaceTint,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EsColors.Primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Chi nhánh trực ca", fontSize = 11.5.sp, color = EsColors.Body)
                            Text(selectedCourt!!.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EsColors.Primary)
                        }
                        TextButton(onClick = { showBranchSelector = true }) {
                            Text("Đổi", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("1. Chọn sân con:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EsColors.Title)
            } else {
                Text("1. Chọn chi nhánh & sân con:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EsColors.Title)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    items(courts) { c ->
                        FilterChip(
                            selected = selectedCourt?.id == c.id,
                            onClick = {
                                selectedCourt = c
                                selectedField = c.fields.firstOrNull()?.label ?: "Sân 5 người"
                            },
                            label = { Text(c.name) }
                        )
                    }
                }
            }

            val fields = selectedCourt?.fields ?: emptyList()
            if (fields.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    items(fields) { f ->
                        FilterChip(
                            selected = selectedField == f.label,
                            onClick = { selectedField = f.label },
                            label = { Text(f.label) }
                        )
                    }
                }
            }

            Text("2. Khung giờ thuê:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EsColors.Title)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                items(dynamicSlots) { slot ->
                    FilterChip(
                        selected = selectedSlot == slot,
                        onClick = { selectedSlot = slot },
                        label = { Text(slot) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            Text("3. Thông tin khách nhận sân:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EsColors.Title)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên khách hàng") },
                placeholder = { Text("VD: Anh Tuấn, Đội FC...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Số điện thoại liên hệ") },
                placeholder = { Text("VD: 0912345678") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))

            Text("4. Hình thức thanh toán:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EsColors.Title)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tiền mặt", "VietQR", "Trả sau").forEach { opt ->
                    FilterChip(
                        selected = paymentOption == opt,
                        onClick = { paymentOption = opt },
                        label = { Text(opt, maxLines = 1) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            val price = selectedCourt?.pricePerHour ?: 300000
            val deposit = if (paymentOption == "Trả sau") 0 else price
            val remaining = if (paymentOption == "Trả sau") price else 0
            val apiPaymentMethod = when (paymentOption) {
                "VietQR" -> "VietQR"
                "Trả sau" -> "PAY_LATER"
                else -> "CASH_ON_SITE"
            }

            EsPrimaryButton(
                text = when (paymentOption) {
                    "VietQR" -> "Hiện mã VietQR (${price.vnd()} đ)"
                    "Trả sau" -> "Giao sân (Thu sau ${price.vnd()} đ)"
                    else -> "Thu Tiền mặt (${price.vnd()} đ) & Giao sân"
                },
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val c = selectedCourt
                    onConfirm(
                        c?.id ?: "court_thanh_phat",
                        selectedField,
                        selectedSlot,
                        name.ifBlank { "Khách vãng lai" },
                        phone.ifBlank { "0900000000" },
                        price,
                        deposit,
                        remaining,
                        apiPaymentMethod
                    )
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExtendTimeDialog(
    booking: StaffBookingItem,
    baseHourlyPrice: Int,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int, extraAmount: Int) -> Unit,
) {
    val options = remember(baseHourlyPrice) {
        listOf(
            30 to (baseHourlyPrice / 2),
            60 to baseHourlyPrice,
            90 to (baseHourlyPrice * 3 / 2),
        )
    }
    var selectedMinutes by remember { mutableIntStateOf(60) }
    val selectedAmount = options.firstOrNull { it.first == selectedMinutes }?.second ?: baseHourlyPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Gia hạn giờ đá", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EsColors.Title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${booking.courtName} (${booking.fieldDetail}) · ${booking.customerName}",
                    fontSize = 13.5.sp,
                    color = EsColors.Body
                )
                Text("Chọn thời gian đá thêm:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EsColors.Title)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (mins, _) ->
                        FilterChip(
                            selected = selectedMinutes == mins,
                            onClick = { selectedMinutes = mins },
                            label = { Text("+${mins}p", maxLines = 1) }
                        )
                    }
                }
                Surface(
                    color = EsColors.SurfaceTint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Phụ thu gia hạn:", fontSize = 13.sp, color = EsColors.Title)
                        Text("+${selectedAmount.vnd()} đ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EsColors.Primary)
                    }
                }
                Text(
                    "Khoản phí sẽ được cộng vào hóa đơn để thu khi khách trả sân.",
                    fontSize = 12.sp,
                    color = EsColors.Body
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMinutes, selectedAmount) }) {
                Text("Xác nhận gia hạn", color = EsColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = EsColors.Body)
            }
        }
    )
}

@Composable
private fun VietQrPaymentDialog(
    amount: Int,
    transferContent: String,
    summary: String,
    onDismiss: () -> Unit,
    onConfirmReceived: () -> Unit,
) {
    var qrBitmap by remember(amount, transferContent) { mutableStateOf<Bitmap?>(null) }
    var isLoadingQr by remember(amount, transferContent) { mutableStateOf(true) }

    LaunchedEffect(amount, transferContent) {
        isLoadingQr = true
        qrBitmap = withContext(Dispatchers.IO) {
            runCatching {
                val encodedInfo = URLEncoder.encode(transferContent, "UTF-8")
                val encodedName = URLEncoder.encode("SAN BONG EASYSPORT", "UTF-8")
                val url = "https://img.vietqr.io/image/MB-0987654321-compact2.png?amount=$amount&addInfo=$encodedInfo&accountName=$encodedName"
                URL(url).openStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
        }
        isLoadingQr = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = EsColors.Primary)
                Spacer(Modifier.width(8.dp))
                Text("Quét Mã VietQR Thanh Toán", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EsColors.Title)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(summary, fontSize = 12.5.sp, color = EsColors.Body, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EsColors.Surface)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingQr -> CircularProgressIndicator(color = EsColors.Primary)
                        qrBitmap != null -> Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Mã VietQR",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        else -> FallbackQrMatrix(seedText = "$amount|$transferContent")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = EsColors.Surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ngân hàng:", fontSize = 12.sp, color = EsColors.Body)
                            Text("MB Bank (Quân đội)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EsColors.Title)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Số tài khoản:", fontSize = 12.sp, color = EsColors.Body)
                            Text("0987654321", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EsColors.Title)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Số tiền:", fontSize = 12.sp, color = EsColors.Body)
                            Text("${amount.vnd()} đ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EsColors.Primary)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nội dung CK:", fontSize = 12.sp, color = EsColors.Body)
                            Text(transferContent, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EsColors.Success)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmReceived,
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary)
            ) {
                Text("Đã nhận tiền")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun FallbackQrMatrix(seedText: String) {
    Canvas(modifier = Modifier.fillMaxSize().background(Color.White).padding(8.dp)) {
        val grid = 21
        val cell = size.width / grid
        var hash = seedText.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isFinder = (r < 7 && c < 7) || (r < 7 && c >= grid - 7) || (r >= grid - 7 && c < 7)
                val drawCell = if (isFinder) {
                    val lr = if (r >= grid - 7) r - (grid - 7) else r
                    val lc = if (c >= grid - 7) c - (grid - 7) else c
                    lr == 0 || lr == 6 || lc == 0 || lc == 6 || (lr in 2..4 && lc in 2..4)
                } else {
                    hash = hash * 1664525 + 1013904223 + r * 31 + c
                    (hash and 1) == 0
                }
                if (drawCell) {
                    drawRect(
                        color = Color(0xFF1A2E46),
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell, cell)
                    )
                }
            }
        }
    }
}

@Composable
internal fun BookingStatusBadge(status: String) {
    val (label, accentColor, bgColor) = when (status.uppercase()) {
        "CHECKED_IN" -> Triple("Đang đá", ProColors.Emerald, ProColors.EmeraldBg)
        "PAID" -> Triple("Đã thanh toán", ProColors.Primary, ProColors.PrimarySoft)
        "DEPOSIT_PAID" -> Triple("Đã cọc 30%", ProColors.Amber, ProColors.AmberBg)
        "COMPLETED" -> Triple("Hoàn tất", ProColors.Emerald, ProColors.EmeraldBg)
        "CANCELLED" -> Triple("Đã hủy", ProColors.Rose, ProColors.RoseBg)
        else -> Triple("Chờ xử lý", ProColors.Amber, ProColors.AmberBg)
    }
    ProStatusDotPill(text = label, dotColor = accentColor, textColor = accentColor, bgColor = bgColor)
}

@Composable
internal fun ShiftStatusBadge(status: String) {
    val (label, accentColor, bgColor) = when (status.uppercase()) {
        "ACTIVE" -> Triple("Đang trực ca", ProColors.Emerald, ProColors.EmeraldBg)
        "COMPLETED" -> Triple("Đã hoàn thành", ProColors.SubInk, ProColors.Surface)
        else -> Triple("Sắp diễn ra", ProColors.Amber, ProColors.AmberBg)
    }
    ProStatusDotPill(text = label, dotColor = accentColor, textColor = accentColor, bgColor = bgColor)
}

@Composable
internal fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = EsCardShape,
        border = BorderStroke(1.dp, EsColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EsColors.Disabled, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = EsColors.Title, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = EsColors.Body, fontSize = 13.sp, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onAction, shape = RoundedCornerShape(10.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
internal fun StatMiniBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = valueColor, maxLines = 1)
        Text(label, fontSize = 11.5.sp, color = EsColors.Body, maxLines = 1)
    }
}

@Composable
internal fun StatusBadge(text: String, textColor: Color, bgColor: Color) {
    Text(
        text = text,
        color = textColor,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(bgColor).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun formatServiceSummary(rawNote: String): String {
    val parts = rawNote.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return ""
    val counts = LinkedHashMap<String, Int>()
    parts.forEach { part -> counts[part] = (counts[part] ?: 0) + 1 }
    return counts.entries.joinToString(", ") { (item, count) ->
        if (count > 1) "${count}x $item" else item
    }
}

@Composable
private fun CancelBookingDialog(
    booking: StaffBookingItem,
    onDismiss: () -> Unit,
    onConfirmCancel: (String) -> Unit,
) {
    val presets = listOf("Khách không đến (No-show)", "Khách báo hủy", "Đặt nhầm khung giờ")
    var selectedReason by remember { mutableStateOf(presets.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Hủy đơn đặt sân?", fontWeight = FontWeight.Bold, color = EsColors.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Đơn của ${booking.customerName} (${booking.fieldDetail} · ${booking.timeRange}) sẽ chuyển sang trạng thái Đã hủy để mở lại sân trống.",
                    fontSize = 13.sp,
                    color = EsColors.Body
                )
                presets.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = r }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
                        Text(r, fontSize = 13.sp, color = EsColors.Title)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmCancel(selectedReason) }) {
                Text("Xác nhận hủy", color = EsColors.Danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Đóng", color = EsColors.Primary) }
        }
    )
}

@Composable
private fun ClockOutConfirmDialog(
    shift: StaffShiftItem,
    activePlayingCount: Int,
    unpaidTotal: Int,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    val now = remember { LocalTime.now() }
    val times = remember(shift.shiftSlot) {
        val matches = Regex("(\\d{2}:\\d{2})").findAll(shift.shiftSlot).mapNotNull {
            runCatching { LocalTime.parse(it.value, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
        }.toList()
        if (matches.size >= 2) matches[0] to matches[matches.size - 1] else null
    }
    val isEarly = remember(now, times) {
        times != null && now.isBefore(times.second.minusMinutes(15))
    }
    val earlyMinutes = remember(now, times, isEarly) {
        if (isEarly && times != null) Duration.between(now, times.second).toMinutes() else 0L
    }

    var reason by remember { mutableStateOf("") }
    val isValid = !isEarly || reason.trim().length >= 3

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                if (isEarly) "Kết thúc ca sớm hơn lịch?" else "Xác nhận kết thúc ca trực?",
                fontWeight = FontWeight.Bold,
                color = if (isEarly) ProColors.Amber else EsColors.Title
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Ca trực: ${shift.shiftSlot} (${shift.courtName})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ProColors.Primary
                )
                if (isEarly && times != null) {
                    Surface(
                        color = ProColors.AmberBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ProColors.Amber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                "⚠️ Bạn đang kết thúc sớm $earlyMinutes phút (ca kết thúc lúc ${times.second.format(DateTimeFormatter.ofPattern("HH:mm"))}).",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProColors.Amber
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Vui lòng nhập lý do kết thúc sớm để lưu vào báo cáo ca:",
                                fontSize = 12.sp,
                                color = ProColors.Ink
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = { Text("Nhập lý do kết thúc sớm...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
                if (activePlayingCount > 0) {
                    Surface(
                        color = ProColors.RoseBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ProColors.Rose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                "⚠️ Lưu ý: Còn $activePlayingCount sân đang đá chưa trả sân (${unpaidTotal.vnd()} đ công nợ).",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProColors.Rose
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Hãy đảm bảo bạn đã bàn giao các sân này cho nhân viên ca sau trước khi rời quầy.",
                                fontSize = 11.5.sp,
                                color = ProColors.SubInk
                            )
                        }
                    }
                }
                if (!isEarly && activePlayingCount == 0) {
                    Text(
                        "Xác nhận kết thúc phiên làm việc lúc ${now.format(DateTimeFormatter.ofPattern("HH:mm"))}?",
                        fontSize = 13.sp,
                        color = EsColors.Body
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(if (isEarly) reason.trim() else null) },
                enabled = isValid
            ) {
                Text(
                    "Kết thúc ca",
                    color = if (isEarly) ProColors.Amber else ProColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = EsColors.Body) }
        }
    )
}
