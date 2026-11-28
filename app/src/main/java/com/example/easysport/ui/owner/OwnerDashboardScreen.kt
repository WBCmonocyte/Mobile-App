package com.example.easysport.ui.owner

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.BranchFieldItem
import com.example.easysport.data.model.BranchRevenueItem
import com.example.easysport.data.model.Court
import com.example.easysport.data.model.InventoryItemModel
import com.example.easysport.data.model.OwnerInventoryData
import com.example.easysport.data.model.OwnerInventoryStatsData
import com.example.easysport.data.model.OwnerOverviewData
import com.example.easysport.data.model.OwnerStaffAccount
import com.example.easysport.data.model.OwnerWeekSchedule
import com.example.easysport.data.model.ScheduleDay
import com.example.easysport.data.model.ScheduleSlot
import com.example.easysport.data.model.ShiftReportItem
import com.example.easysport.data.model.StaffMemberOption
import com.example.easysport.data.model.StaffShiftItem
import com.example.easysport.data.model.StaffWorkload
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
import com.example.easysport.ui.common.ProKpiCard
import com.example.easysport.ui.common.ProSectionHeader
import com.example.easysport.ui.common.ProStatusDotPill
import com.example.easysport.ui.common.ProTopBar
import com.example.easysport.ui.common.StaffOwnerProfileDialog
import com.example.easysport.ui.common.vnd
import com.example.easysport.ui.staff.BookingStatusBadge
import com.example.easysport.ui.staff.EmptyStateCard
import com.example.easysport.ui.staff.StatMiniBox
import com.example.easysport.ui.staff.StatusBadge
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private enum class OwnerScreen(val title: String) {
    HOME("Trang chủ Chủ Sân"),
    ANALYTICS("Báo cáo & Thống kê"),
    INVENTORY("Kho & Hàng hóa"),
    SHIFTS("Phân ca nhân viên"),
    REPORTS("Duyệt bàn giao két"),
    MANAGE("Quản lý Sân & NV"),
    BOOKINGS("Đơn đặt sân"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by UserStore.currentUser.collectAsState()
    val courts by CourtStore.courts.collectAsState()

    var currentScreen by remember { mutableStateOf(OwnerScreen.HOME) }
    var overview by remember { mutableStateOf<OwnerOverviewData?>(null) }
    var shifts by remember { mutableStateOf<List<StaffShiftItem>>(emptyList()) }
    var staffList by remember { mutableStateOf<List<StaffMemberOption>>(emptyList()) }
    var schedule by remember { mutableStateOf<OwnerWeekSchedule?>(null) }
    var weekStart by remember { mutableStateOf<String?>(null) }
    var reports by remember { mutableStateOf<List<ShiftReportItem>>(emptyList()) }
    var staffAccounts by remember { mutableStateOf<List<OwnerStaffAccount>>(emptyList()) }
    var selectedPeriod by remember { mutableStateOf("ALL") }
    var selectedBranchId by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAssignSheet by remember { mutableStateOf(false) }
    var assignInitialDate by remember { mutableStateOf<String?>(null) }
    var assignInitialSlot by remember { mutableStateOf<String?>(null) }
    var showAutoAssignConfirm by remember { mutableStateOf(false) }
    var pendingConfirmReport by remember { mutableStateOf<ShiftReportItem?>(null) }
    var pendingRejectReport by remember { mutableStateOf<ShiftReportItem?>(null) }
    var pendingDeleteShift by remember { mutableStateOf<StaffShiftItem?>(null) }
    var showBranchSheet by remember { mutableStateOf(false) }
    var editingBranchId by remember { mutableStateOf<String?>(null) }
    var quickAddFieldBranch by remember { mutableStateOf<BranchRevenueItem?>(null) }
    var pendingDeleteField by remember { mutableStateOf<Pair<BranchRevenueItem, BranchFieldItem>?>(null) }
    var selectedStaffDetailId by remember { mutableStateOf<String?>(null) }
    var showStaffFormDialog by remember { mutableStateOf(false) }
    var editingStaffAccount by remember { mutableStateOf<OwnerStaffAccount?>(null) }

    var inventoryData by remember { mutableStateOf<OwnerInventoryData?>(null) }
    var inventoryStats by remember { mutableStateOf<OwnerInventoryStatsData?>(null) }
    var inventoryBranchId by remember { mutableStateOf<String?>(null) }
    var inventoryStatsPeriod by remember { mutableStateOf("ALL") }
    var restockTarget by remember { mutableStateOf<InventoryItemModel?>(null) }
    var restockInitialQty by remember { mutableStateOf(24) }
    var adjustTarget by remember { mutableStateOf<InventoryItemModel?>(null) }
    var wastageTarget by remember { mutableStateOf<InventoryItemModel?>(null) }
    var transferTarget by remember { mutableStateOf<InventoryItemModel?>(null) }
    var showInventoryForm by remember { mutableStateOf(false) }
    var editingInventoryItem by remember { mutableStateOf<InventoryItemModel?>(null) }
    var pendingDeleteInventoryItem by remember { mutableStateOf<InventoryItemModel?>(null) }

    fun refreshAll(
        targetWeek: String? = weekStart,
        targetPeriod: String = selectedPeriod,
        targetBranch: String? = selectedBranchId,
        targetInvBranch: String? = inventoryBranchId,
        targetInvPeriod: String = inventoryStatsPeriod,
    ) {
        scope.launch {
            isRefreshing = true
            StaffOwnerApiClient.getOwnerOverview(targetPeriod).onSuccess { overview = it }
                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
            StaffOwnerApiClient.getOwnerShiftsAndStaff().onSuccess { (s, st) ->
                shifts = s
                staffList = st
            }
            StaffOwnerApiClient.getOwnerWeekSchedule(targetWeek, targetBranch).onSuccess { sc ->
                schedule = sc
                if (weekStart == null) weekStart = sc.weekStart
            }
            StaffOwnerApiClient.getOwnerReports().onSuccess { reports = it }
            StaffOwnerApiClient.getOwnerStaffList().onSuccess { staffAccounts = it }
            StaffOwnerApiClient.getOwnerInventory(targetInvBranch).onSuccess { inventoryData = it }
            StaffOwnerApiClient.getOwnerInventoryStats(targetInvBranch, targetInvPeriod).onSuccess { inventoryStats = it }
            isRefreshing = false
        }
    }

    fun shiftWeekBy(daysDelta: Long) {
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val base = weekStart?.let { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }
            ?: LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextWeek = base.plusDays(daysDelta).format(fmt)
        weekStart = nextWeek
        scope.launch {
            isRefreshing = true
            StaffOwnerApiClient.getOwnerWeekSchedule(nextWeek, selectedBranchId)
                .onSuccess { schedule = it }
                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { refreshAll() }

    BackHandler {
        if (currentScreen != OwnerScreen.HOME) {
            currentScreen = OwnerScreen.HOME
        } else {
            showLogoutConfirm = true
        }
    }

    val pendingReportCount = remember(reports, overview) {
        reports.count { it.status == "PENDING" }.takeIf { reports.isNotEmpty() } ?: (overview?.pendingReports ?: 0)
    }
    val lowStockCount = remember(inventoryData) {
        inventoryData?.items?.count { it.lowStock } ?: 0
    }

    Scaffold(
        containerColor = ProColors.Canvas,
        topBar = {
            ProTopBar(
                title = currentScreen.title,
                subtitle = currentUser?.name ?: "Chủ sân EasySport",
                roleBadge = "CHỦ SÂN",
                isRefreshing = isRefreshing,
                onRefresh = { refreshAll() },
                onLogout = { showLogoutConfirm = true },
                onBack = if (currentScreen != OwnerScreen.HOME) {
                    { currentScreen = OwnerScreen.HOME }
                } else null,
                onProfileClick = { showProfileDialog = true },
            )
        },
        bottomBar = {
            OwnerCompactBottomBar(
                currentScreen = currentScreen,
                pendingReportCount = pendingReportCount,
                lowStockCount = lowStockCount,
                onSelect = { currentScreen = it },
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
            when (currentScreen) {
                OwnerScreen.HOME -> OwnerHomeHub(
                    overview = overview,
                    pendingReports = pendingReportCount,
                    lowStockCount = lowStockCount,
                    onNavigate = { currentScreen = it },
                )
                OwnerScreen.ANALYTICS -> OwnerAnalyticsScreen(
                    overview = overview,
                    inventoryStats = inventoryStats,
                    inventoryItems = inventoryData?.items ?: emptyList(),
                    selectedPeriod = selectedPeriod,
                    onSelectPeriod = { p ->
                        selectedPeriod = p
                        inventoryStatsPeriod = p
                        refreshAll(targetPeriod = p, targetInvPeriod = p)
                    },
                    selectedBranchId = inventoryBranchId,
                    onSelectBranch = { bId ->
                        inventoryBranchId = bId
                        refreshAll(targetInvBranch = bId)
                    },
                    onQuickReorder = { item, suggestedQty ->
                        restockInitialQty = suggestedQty
                        restockTarget = item
                    },
                    onGoToInventory = { currentScreen = OwnerScreen.INVENTORY },
                )
                OwnerScreen.INVENTORY -> OwnerInventoryTab(
                    inventoryData = inventoryData,
                    branches = overview?.branchRevenues ?: emptyList(),
                    selectedBranchId = inventoryBranchId,
                    onSelectBranch = { bId ->
                        inventoryBranchId = bId
                        refreshAll(targetInvBranch = bId)
                    },
                    onAddItem = {
                        editingInventoryItem = null
                        showInventoryForm = true
                    },
                    onEditItem = { item ->
                        editingInventoryItem = item
                        showInventoryForm = true
                    },
                    onRestockItem = {
                        restockInitialQty = 24
                        restockTarget = it
                    },
                    onAdjustItem = { adjustTarget = it },
                    onWastageItem = { wastageTarget = it },
                    onTransferItem = { transferTarget = it },
                    onDeleteItem = { pendingDeleteInventoryItem = it }
                )
                OwnerScreen.SHIFTS -> OwnerShiftsTab(
                    schedule = schedule,
                    courts = courts,
                    selectedBranchId = selectedBranchId,
                    onSelectBranch = { branchId ->
                        selectedBranchId = branchId
                        refreshAll(targetBranch = branchId)
                    },
                    onPrevWeek = { shiftWeekBy(-7) },
                    onNextWeek = { shiftWeekBy(7) },
                    onAutoAssign = { showAutoAssignConfirm = true },
                    onOpenAssign = {
                        assignInitialDate = null
                        assignInitialSlot = null
                        showAssignSheet = true
                    },
                    onSlotClick = { date, slotLabel ->
                        assignInitialDate = date
                        assignInitialSlot = slotLabel
                        showAssignSheet = true
                    },
                    onDeleteShift = { pendingDeleteShift = it }
                )
                OwnerScreen.REPORTS -> OwnerReportsTab(
                    reports = reports,
                    onConfirmClick = { pendingConfirmReport = it },
                    onRejectClick = { pendingRejectReport = it }
                )
                OwnerScreen.MANAGE -> OwnerManageTab(
                    branches = overview?.branchRevenues ?: emptyList(),
                    staffAccounts = staffAccounts,
                    onEditPricing = {
                        editingBranchId = it.courtId
                        showBranchSheet = true
                    },
                    onCreateCourt = {
                        editingBranchId = null
                        showBranchSheet = true
                    },
                    onQuickAddField = { quickAddFieldBranch = it },
                    onQuickDeleteField = { branch, field -> pendingDeleteField = branch to field },
                    onStaffClick = { selectedStaffDetailId = it.id },
                    onCreateStaff = {
                        editingStaffAccount = null
                        showStaffFormDialog = true
                    },
                    onToggleLock = { acc ->
                        scope.launch {
                            StaffOwnerApiClient.toggleOwnerStaffLock(acc.id)
                                .onSuccess { updated ->
                                    val msg = if (updated.locked) "Đã khóa tài khoản ${updated.name}" else "Đã mở khóa ${updated.name}"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    refreshAll()
                                }
                                .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                        }
                    }
                )
                OwnerScreen.BOOKINGS -> OwnerBookingsScreen(overview = overview)
            }
        }
    }

    if (showProfileDialog) {
        StaffOwnerProfileDialog(
            user = currentUser,
            roleLabel = "CHỦ SÂN",
            onDismiss = { showProfileDialog = false },
            onRequestLogout = { showLogoutConfirm = true },
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = Color.White,
            title = { Text("Đăng xuất tài khoản Chủ sân?", fontWeight = FontWeight.Bold, color = EsColors.Title) },
            text = { Text("Bạn có muốn thoát phiên quản lý và quay về màn hình đăng nhập?", color = EsColors.Body) },
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

    pendingDeleteShift?.let { shift ->
        AlertDialog(
            onDismissRequest = { pendingDeleteShift = null },
            containerColor = Color.White,
            title = { Text("Hủy ca trực?", fontWeight = FontWeight.Bold, color = EsColors.Title) },
            text = { Text("Bạn chắc chắn muốn hủy ca ${shift.shiftSlot} (${shift.shiftDate}) của ${shift.staffName}?", color = EsColors.Body) },
            confirmButton = {
                TextButton(onClick = {
                    val id = shift.id
                    val name = shift.staffName
                    pendingDeleteShift = null
                    scope.launch {
                        StaffOwnerApiClient.deleteOwnerShift(id)
                            .onSuccess {
                                Toast.makeText(context, "Đã hủy ca của $name", Toast.LENGTH_SHORT).show()
                                refreshAll()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    }
                }) { Text("Hủy ca", color = EsColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteShift = null }) { Text("Đóng", color = EsColors.Primary) }
            }
        )
    }

    pendingConfirmReport?.let { rep ->
        AlertDialog(
            onDismissRequest = { pendingConfirmReport = null },
            containerColor = Color.White,
            title = { Text("Xác nhận thu két tiền", fontWeight = FontWeight.Bold, color = EsColors.Title) },
            text = {
                Text(
                    "Xác nhận đã nhận đủ tiền từ ${rep.staffName}:\n• Tiền mặt: ${rep.cashAmount.vnd()} đ\n• VietQR: ${rep.qrAmount.vnd()} đ",
                    color = EsColors.Body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = rep
                    pendingConfirmReport = null
                    scope.launch {
                        StaffOwnerApiClient.confirmOwnerReport(target.id)
                            .onSuccess {
                                Toast.makeText(context, "Đã xác nhận thu két của ${target.staffName}", Toast.LENGTH_SHORT).show()
                                refreshAll()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    }
                }) { Text("Đã nhận đủ tiền", color = EsColors.Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmReport = null }) { Text("Để sau", color = EsColors.Body) }
            }
        )
    }

    pendingRejectReport?.let { rep ->
        RejectReportDialog(
            report = rep,
            onDismiss = { pendingRejectReport = null },
            onConfirmReject = { note ->
                val target = rep
                pendingRejectReport = null
                scope.launch {
                    StaffOwnerApiClient.rejectOwnerReport(target.id, note)
                        .onSuccess {
                            Toast.makeText(context, "Đã đánh dấu lệch két ca của ${target.staffName}", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }

    if (showBranchSheet) {
        val liveBranch = editingBranchId?.let { id ->
            overview?.branchRevenues?.firstOrNull { it.courtId == id }
        }
        BranchFormBottomSheet(
            existingBranch = liveBranch,
            onDismiss = {
                showBranchSheet = false
                editingBranchId = null
            },
            onCreateBranch = { name, address, sportType, fieldCount, fieldTypeLabel, indoor, lat, lng, basePrice, morning, afternoon, evening ->
                scope.launch {
                    StaffOwnerApiClient.createOwnerCourt(
                        name = name,
                        address = address,
                        sportType = sportType,
                        fieldCount = fieldCount,
                        pricePerHour = basePrice,
                        morningPrice = morning,
                        afternoonPrice = afternoon,
                        eveningPrice = evening,
                        latitude = lat,
                        longitude = lng,
                        indoor = indoor,
                        fieldTypeLabel = fieldTypeLabel,
                    ).onSuccess {
                        showBranchSheet = false
                        editingBranchId = null
                        Toast.makeText(context, "Đã tạo chi nhánh $name", Toast.LENGTH_LONG).show()
                        CourtStore.refresh(context)
                        refreshAll()
                    }.onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            },
            onUpdateBranch = { courtId, name, address, sportType, indoor, lat, lng, basePrice, morning, afternoon, evening ->
                scope.launch {
                    StaffOwnerApiClient.updateOwnerCourt(
                        courtId = courtId,
                        name = name,
                        address = address,
                        pricePerHour = basePrice,
                        morningPrice = morning,
                        afternoonPrice = afternoon,
                        eveningPrice = evening,
                        sportType = sportType,
                        indoor = indoor,
                        latitude = lat,
                        longitude = lng,
                    ).onSuccess {
                        showBranchSheet = false
                        editingBranchId = null
                        Toast.makeText(context, "Đã cập nhật chi nhánh $name", Toast.LENGTH_SHORT).show()
                        CourtStore.refresh(context)
                        refreshAll()
                    }.onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            },
            onAddField = { courtId, label, detail, fieldType ->
                scope.launch {
                    StaffOwnerApiClient.addCourtField(courtId, label, detail, fieldType)
                        .onSuccess {
                            Toast.makeText(context, "Đã thêm $label", Toast.LENGTH_SHORT).show()
                            CourtStore.refresh(context)
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            },
            onDeleteField = { courtId, fieldId ->
                scope.launch {
                    StaffOwnerApiClient.deleteCourtField(courtId, fieldId)
                        .onSuccess {
                            Toast.makeText(context, "Đã xóa sân con", Toast.LENGTH_SHORT).show()
                            CourtStore.refresh(context)
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    quickAddFieldBranch?.let { targetBranch ->
        QuickAddFieldDialog(
            branch = targetBranch,
            onDismiss = { quickAddFieldBranch = null },
            onConfirm = { label, detail, fieldType ->
                quickAddFieldBranch = null
                scope.launch {
                    StaffOwnerApiClient.addCourtField(targetBranch.courtId, label, detail, fieldType)
                        .onSuccess {
                            Toast.makeText(context, "Đã thêm $label vào ${targetBranch.courtName}", Toast.LENGTH_SHORT).show()
                            CourtStore.refresh(context)
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    pendingDeleteField?.let { (branch, field) ->
        AlertDialog(
            onDismissRequest = { pendingDeleteField = null },
            containerColor = Color.White,
            title = { Text("Xóa sân con?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Bạn có chắc muốn xóa ${field.label} khỏi chi nhánh ${branch.courtName}?")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteField = null
                    scope.launch {
                        StaffOwnerApiClient.deleteCourtField(branch.courtId, field.id)
                            .onSuccess {
                                Toast.makeText(context, "Đã xóa ${field.label}", Toast.LENGTH_SHORT).show()
                                CourtStore.refresh(context)
                                refreshAll()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Xóa", color = EsColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteField = null }) { Text("Hủy", color = ProColors.Muted) }
            }
        )
    }

    selectedStaffDetailId?.let { sid ->
        staffAccounts.firstOrNull { it.id == sid }?.let { staffObj ->
            StaffDetailBottomSheet(
                staff = staffObj,
                staffShifts = shifts.filter { it.staffId == staffObj.id },
                onDismiss = { selectedStaffDetailId = null },
                onCall = { phone ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    }
                },
                onEditStaff = { target ->
                    editingStaffAccount = target
                    showStaffFormDialog = true
                },
                onToggleLock = { target ->
                    scope.launch {
                        StaffOwnerApiClient.toggleOwnerStaffLock(target.id)
                            .onSuccess { updated ->
                                val msg = if (updated.locked) "Đã khóa tài khoản ${updated.name}" else "Đã mở khóa ${updated.name}"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                refreshAll()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    }
                }
            )
        }
    }

    if (showStaffFormDialog) {
        StaffAccountFormDialog(
            existingStaff = editingStaffAccount,
            courts = courts,
            onDismiss = {
                showStaffFormDialog = false
                editingStaffAccount = null
            },
            onCreate = { name, email, phone, password, address ->
                scope.launch {
                    StaffOwnerApiClient.createOwnerStaff(name, email, phone, password, address)
                        .onSuccess { created ->
                            showStaffFormDialog = false
                            editingStaffAccount = null
                            Toast.makeText(context, "Đã tạo nhân viên ${created.name}", Toast.LENGTH_LONG).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            },
            onUpdate = { staffId, name, phone, address, newPw ->
                scope.launch {
                    StaffOwnerApiClient.updateOwnerStaff(staffId, name, phone, address, newPw)
                        .onSuccess { updated ->
                            showStaffFormDialog = false
                            editingStaffAccount = null
                            Toast.makeText(context, "Đã cập nhật ${updated.name}", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    if (showAutoAssignConfirm) {
        val targetWeek = schedule?.weekStart ?: weekStart ?: LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        val targetCourtObj = courts.firstOrNull { it.id == selectedBranchId } ?: courts.firstOrNull { it.id == "court_thanh_phat" }
        AlertDialog(
            onDismissRequest = { showAutoAssignConfirm = false },
            containerColor = Color.White,
            title = { Text("Tự động xếp ca tuần?", fontWeight = FontWeight.Bold, color = EsColors.Title) },
            text = {
                Text(
                    "Hệ thống sẽ tự động điền các vị trí ca còn thiếu trong tuần $targetWeek tại ${targetCourtObj?.name ?: "Sân bóng Thành Phát"} (ưu tiên NV ít giờ nhất, tối đa 48h/tuần, nghỉ tối thiểu 8h giữa ca).",
                    color = EsColors.Body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAutoAssignConfirm = false
                    scope.launch {
                        StaffOwnerApiClient.autoAssignWeek(targetWeek, targetCourtObj?.id, targetCourtObj?.name)
                            .onSuccess { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                refreshAll(targetWeek)
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Xếp ca ngay", color = EsColors.Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAutoAssignConfirm = false }) { Text("Để sau", color = EsColors.Body) }
            }
        )
    }

    if (showAssignSheet) {
        AssignShiftBottomSheet(
            staffList = staffList,
            courts = courts,
            schedule = schedule,
            initialDate = assignInitialDate,
            initialSlot = assignInitialSlot,
            initialCourtId = selectedBranchId,
            onDismiss = { showAssignSheet = false },
            onConfirm = { staffId, date, slot, duty, notes, courtId, courtName ->
                scope.launch {
                    StaffOwnerApiClient.assignOwnerShift(staffId, date, slot, duty, notes, courtId, courtName)
                        .onSuccess {
                            showAssignSheet = false
                            Toast.makeText(context, "Đã phân ca thành công", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    restockTarget?.let { target ->
        RestockInventoryDialog(
            item = target,
            initialQuantity = restockInitialQty,
            onDismiss = { restockTarget = null },
            onConfirm = { qty, cost, note, sup ->
                restockTarget = null
                scope.launch {
                    StaffOwnerApiClient.restockInventoryItem(target.id, qty, cost, note, sup)
                        .onSuccess {
                            Toast.makeText(context, "Đã nhập thêm $qty ${target.unit} ${target.name}", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    wastageTarget?.let { target ->
        WastageInventoryDialog(
            item = target,
            onDismiss = { wastageTarget = null },
            onConfirm = { qty, reasonType, note ->
                wastageTarget = null
                scope.launch {
                    StaffOwnerApiClient.wastageInventoryItem(target.id, qty, reasonType, note)
                        .onSuccess {
                            Toast.makeText(context, "Đã xuất hủy $qty ${target.unit} ${target.name}", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    transferTarget?.let { target ->
        TransferInventoryDialog(
            item = target,
            branches = overview?.branchRevenues ?: emptyList(),
            onDismiss = { transferTarget = null },
            onConfirm = { targetCourtId, qty, note ->
                transferTarget = null
                scope.launch {
                    StaffOwnerApiClient.transferInventoryItem(target.id, targetCourtId, qty, note)
                        .onSuccess { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    adjustTarget?.let { target ->
        AdjustInventoryDialog(
            item = target,
            onDismiss = { adjustTarget = null },
            onConfirm = { newStock, note ->
                adjustTarget = null
                scope.launch {
                    StaffOwnerApiClient.adjustInventoryItem(target.id, newStock, note)
                        .onSuccess {
                            Toast.makeText(context, "Đã cập nhật kiểm kê ${target.name}: $newStock ${target.unit}", Toast.LENGTH_SHORT).show()
                            refreshAll()
                        }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }
        )
    }

    if (showInventoryForm) {
        InventoryItemFormDialog(
            existing = editingInventoryItem,
            branches = overview?.branchRevenues ?: emptyList(),
            defaultBranchId = inventoryBranchId,
            onDismiss = {
                showInventoryForm = false
                editingInventoryItem = null
            },
            onSave = { courtId, name, category, unit, imp, sell, stk, minA, sku, barcode, sup ->
                val editing = editingInventoryItem
                showInventoryForm = false
                editingInventoryItem = null
                scope.launch {
                    val res = if (editing == null) {
                        StaffOwnerApiClient.createInventoryItem(courtId, name, category, unit, imp, sell, stk, minA, sku, barcode, sup)
                    } else {
                        StaffOwnerApiClient.updateInventoryItem(editing.id, name, category, unit, imp, sell, minA, sku, barcode, sup)
                    }
                    res.onSuccess {
                        Toast.makeText(context, if (editing == null) "Đã thêm $name vào kho" else "Đã cập nhật $name", Toast.LENGTH_SHORT).show()
                        refreshAll()
                    }.onFailure {
                        Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    pendingDeleteInventoryItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteInventoryItem = null },
            containerColor = Color.White,
            title = { Text("Xóa mặt hàng khỏi kho?", fontWeight = FontWeight.Bold, color = EsColors.Title) },
            text = { Text("Xác nhận xóa \"${item.name}\" tại ${item.courtName}?", color = EsColors.Body) },
            confirmButton = {
                TextButton(onClick = {
                    val target = item
                    pendingDeleteInventoryItem = null
                    scope.launch {
                        StaffOwnerApiClient.deleteInventoryItem(target.id)
                            .onSuccess {
                                Toast.makeText(context, "Đã xóa ${target.name}", Toast.LENGTH_SHORT).show()
                                refreshAll()
                            }
                            .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show() }
                    }
                }) { Text("Xóa", color = ProColors.Rose, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteInventoryItem = null }) { Text("Hủy", color = EsColors.Body) }
            }
        )
    }
}

private data class OwnerMenuOption(
    val screen: OwnerScreen,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBg: Color,
    val badgeCount: Int = 0,
    val badgeColor: Color = ProColors.Rose,
)

@Composable
private fun OwnerHomeHub(
    overview: OwnerOverviewData?,
    pendingReports: Int,
    lowStockCount: Int,
    onNavigate: (OwnerScreen) -> Unit,
) {
    val cashVal = (overview?.cashRevenue ?: 0L).coerceAtLeast(0L)
    val qrVal = (overview?.qrRevenue ?: 0L).coerceAtLeast(0L)
    val sumChannels = (cashVal + qrVal).coerceAtLeast(1L)
    val cashPct = ((cashVal * 100.0) / sumChannels).toInt()
    val qrPct = (100 - cashPct).coerceAtLeast(0)

    val menuOptions = listOf(
        OwnerMenuOption(
            screen = OwnerScreen.ANALYTICS,
            title = "Báo cáo Thống kê",
            subtitle = "Doanh thu, lãi lỗ, đa chiều",
            icon = Icons.Rounded.BarChart,
            iconColor = Color(0xFF4F46E5),
            iconBg = Color(0xFFEEF2FF),
        ),
        OwnerMenuOption(
            screen = OwnerScreen.INVENTORY,
            title = "Kho & Hàng hóa",
            subtitle = "Tồn kho, nhập xuất, chuyển",
            icon = Icons.Rounded.Inventory2,
            iconColor = ProColors.Emerald,
            iconBg = ProColors.EmeraldBg,
            badgeCount = lowStockCount,
            badgeColor = ProColors.Rose,
        ),
        OwnerMenuOption(
            screen = OwnerScreen.SHIFTS,
            title = "Phân ca Nhân viên",
            subtitle = "Lịch trực tuần, gán ca",
            icon = Icons.Rounded.Group,
            iconColor = ProColors.Sky,
            iconBg = ProColors.SkyBg,
        ),
        OwnerMenuOption(
            screen = OwnerScreen.REPORTS,
            title = "Bàn giao két",
            subtitle = "Duyệt thu chi, chốt ca",
            icon = Icons.Rounded.AccountBalanceWallet,
            iconColor = ProColors.Amber,
            iconBg = ProColors.AmberBg,
            badgeCount = pendingReports,
            badgeColor = ProColors.Amber,
        ),
        OwnerMenuOption(
            screen = OwnerScreen.MANAGE,
            title = "Quản lý Sân & NV",
            subtitle = "Chi nhánh, giá sân, nhân sự",
            icon = Icons.Rounded.Storefront,
            iconColor = Color(0xFF7C3AED),
            iconBg = Color(0xFFF3E8FF),
        ),
        OwnerMenuOption(
            screen = OwnerScreen.BOOKINGS,
            title = "Đơn đặt sân",
            subtitle = "${overview?.totalBookings ?: 0} đơn · ${overview?.activeBookings ?: 0} đang đá",
            icon = Icons.AutoMirrored.Rounded.EventNote,
            iconColor = ProColors.Primary,
            iconBg = ProColors.PrimarySoft,
        ),
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Financial Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ProHeroBrush)
                    .clickable { onNavigate(OwnerScreen.ANALYTICS) }
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tổng doanh thu hệ thống", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        ProStatusDotPill(
                            text = "Xem thống kê →",
                            dotColor = Color(0xFF34D399),
                            textColor = Color.White,
                            bgColor = Color.White.copy(alpha = 0.18f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${(overview?.totalRevenue ?: 0L).toInt().vnd()} đ",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
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
                        Text("Tiền mặt ($cashPct%): ${cashVal.toInt().vnd()} đ", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.9f))
                        Text("VietQR ($qrPct%): ${qrVal.toInt().vnd()} đ", fontSize = 11.5.sp, color = Color(0xFF6EE7B7), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Quick 3-stat mini strip
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProKpiCard(
                    icon = Icons.Outlined.SportsSoccer,
                    iconTint = ProColors.Emerald,
                    iconBg = ProColors.EmeraldBg,
                    value = "${overview?.activeBookings ?: 0} sân",
                    label = "Đang hoạt động",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(OwnerScreen.BOOKINGS) }
                )
                ProKpiCard(
                    icon = Icons.Rounded.Storefront,
                    iconTint = ProColors.Sky,
                    iconBg = ProColors.SkyBg,
                    value = "${overview?.totalCourts ?: 0} cụm",
                    label = "Chi nhánh sân",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(OwnerScreen.MANAGE) }
                )
            }
        }

        // Actionable Alerts
        if (pendingReports > 0) {
            item {
                ProCard(
                    accentBorderColor = ProColors.Amber.copy(alpha = 0.45f),
                    containerColor = ProColors.AmberBg,
                    onClick = { onNavigate(OwnerScreen.REPORTS) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(ProColors.Amber.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = ProColors.Amber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Có $pendingReports báo cáo két chờ duyệt", fontWeight = FontWeight.Bold, color = ProColors.Ink, fontSize = 14.sp)
                            Text("Bấm để kiểm tra tiền mặt & xác nhận đóng ca", color = ProColors.Amber, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = ProColors.Amber)
                    }
                }
            }
        }

        if (lowStockCount > 0) {
            item {
                ProCard(
                    accentBorderColor = ProColors.Rose.copy(alpha = 0.4f),
                    containerColor = ProColors.RoseBg,
                    onClick = { onNavigate(OwnerScreen.INVENTORY) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(ProColors.Rose.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = ProColors.Rose, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("$lowStockCount mặt hàng sắp hết / hết tồn", fontWeight = FontWeight.Bold, color = ProColors.Ink, fontSize = 14.sp)
                            Text("Bấm vào Kho hàng để nhập bổ sung kịp thời", color = ProColors.Rose, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = ProColors.Rose)
                    }
                }
            }
        }

        // Grid Menu Chức năng (Phone-style Dashboard Grid)
        item {
            ProSectionHeader("Danh mục chức năng")
        }

        val rows = menuOptions.chunked(2)
        items(rows) { rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { opt ->
                    OwnerHubTile(
                        option = opt,
                        onClick = { onNavigate(opt.screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OwnerCompactBottomBar(
    currentScreen: OwnerScreen,
    pendingReportCount: Int,
    lowStockCount: Int,
    onSelect: (OwnerScreen) -> Unit,
) {
    val navItems = listOf(
        Triple(OwnerScreen.HOME, "Trang chủ", Icons.Rounded.Home),
        Triple(OwnerScreen.ANALYTICS, "Thống kê", Icons.Rounded.BarChart),
        Triple(OwnerScreen.INVENTORY, "Kho hàng", Icons.Rounded.Inventory2),
        Triple(OwnerScreen.REPORTS, "Két tiền", Icons.Rounded.AccountBalanceWallet),
    )

    Surface(color = Color.White, shadowElevation = 10.dp) {
        Column {
            HorizontalDivider(color = ProColors.Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                navItems.forEach { (screen, label, icon) ->
                    val active = currentScreen == screen
                    val badgeNum = when (screen) {
                        OwnerScreen.REPORTS -> pendingReportCount
                        OwnerScreen.INVENTORY -> lowStockCount
                        else -> 0
                    }
                    val badgeColor = if (screen == OwnerScreen.REPORTS) ProColors.Amber else ProColors.Rose

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelect(screen) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) ProColors.PrimarySoft else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (badgeNum > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = badgeColor) {
                                            Text(badgeNum.toString(), color = Color.White)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (active) ProColors.Primary else ProColors.Muted,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (active) ProColors.Primary else ProColors.Muted,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = label,
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
private fun OwnerHubTile(
    option: OwnerMenuOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ProColors.Border),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(option.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.title,
                        tint = option.iconColor,
                        modifier = Modifier.size(27.dp)
                    )
                }
                if (option.badgeCount > 0) {
                    Surface(
                        color = option.badgeColor,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "${option.badgeCount}",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = option.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ProColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = option.subtitle,
                    fontSize = 12.sp,
                    color = ProColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun OwnerBookingsScreen(overview: OwnerOverviewData?) {
    if (overview == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ProColors.Primary)
        }
        return
    }
    var filter by remember { mutableStateOf("ALL") }
    val filteredBookings = remember(overview.bookings, filter) {
        when (filter) {
            "ACTIVE" -> overview.bookings.filter { it.status == "CHECKED_IN" }
            "PAID" -> overview.bookings.filter { it.status in listOf("PAID", "DEPOSIT_PAID") }
            "DONE" -> overview.bookings.filter { it.status == "COMPLETED" }
            else -> overview.bookings
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                ProSectionHeader("Danh sách đơn đặt sân (${filteredBookings.size})")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("ALL" to "Tất cả", "ACTIVE" to "Đang đá", "PAID" to "Đã đặt/cọc", "DONE" to "Hoàn tất")
                    items(filters) { (key, label) ->
                        FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
                    }
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            item {
                EmptyStateCard(title = "Chưa có đơn phù hợp", subtitle = "Chọn bộ lọc khác để xem danh sách đơn.")
            }
        } else {
            items(filteredBookings) { b ->
                val stripeColor = when (b.status.uppercase()) {
                    "CHECKED_IN", "COMPLETED" -> ProColors.Emerald
                    "PAID", "DEPOSIT_PAID" -> ProColors.Primary
                    else -> ProColors.Amber
                }
                ProCard {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(Modifier.width(5.dp).fillMaxHeight().background(stripeColor))
                        Row(
                            modifier = Modifier.weight(1f).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProAvatar(b.customerName, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        b.customerName,
                                        fontWeight = FontWeight.Bold,
                                        color = ProColors.Ink,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    BookingStatusBadge(b.status)
                                }
                                Spacer(Modifier.height(3.dp))
                                Text("${b.courtName} (${b.fieldDetail})", fontSize = 12.5.sp, color = ProColors.SubInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${b.dateLabel} · ${b.timeRange}", fontSize = 11.5.sp, color = ProColors.Muted, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${(b.totalPrice + b.serviceAmount).vnd()} đ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProColors.Primary
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

@Composable
private fun OwnerShiftsTab(
    schedule: OwnerWeekSchedule?,
    courts: List<Court>,
    selectedBranchId: String?,
    onSelectBranch: (String?) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onAutoAssign: () -> Unit,
    onOpenAssign: () -> Unit,
    onSlotClick: (date: String, slotLabel: String) -> Unit,
    onDeleteShift: (StaffShiftItem) -> Unit,
) {
    var rulesExpanded by remember { mutableStateOf(false) }

    if (schedule == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EsColors.Primary)
        }
        return
    }

    val missingTotal = (schedule.totalPositions - schedule.filledPositions).coerceAtLeast(0)
    val coverageRatio = if (schedule.totalPositions > 0) {
        (schedule.filledPositions.toFloat() / schedule.totalPositions.toFloat()).coerceIn(0f, 1f)
    } else 1f
    val shortStart = schedule.weekStart.take(5)
    val shortEnd = schedule.weekEnd.take(5)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (courts.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedBranchId == null,
                            onClick = { onSelectBranch(null) },
                            label = { Text("Tất cả chi nhánh") }
                        )
                    }
                    items(courts) { c ->
                        FilterChip(
                            selected = selectedBranchId == c.id,
                            onClick = { onSelectBranch(c.id) },
                            label = { Text(c.name) }
                        )
                    }
                }
            }
        }
        // a. Thanh chọn tuần & độ phủ
        item {
            ProCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ProColors.Surface)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onPrevWeek) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Tuần trước", tint = ProColors.Ink)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = ProColors.Primary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Tuần $shortStart – $shortEnd",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = ProColors.Ink
                            )
                        }
                        IconButton(onClick = onNextWeek) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = "Tuần sau", tint = ProColors.Ink)
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Đã phủ ${schedule.filledPositions}/${schedule.totalPositions} vị trí ca",
                            fontWeight = FontWeight.Bold,
                            color = ProColors.Ink,
                            fontSize = 14.sp
                        )
                        ProStatusDotPill(
                            text = if (missingTotal == 0) "Đủ nhân sự" else "Thiếu $missingTotal vị trí",
                            dotColor = if (missingTotal == 0) ProColors.Emerald else ProColors.Amber,
                            textColor = if (missingTotal == 0) ProColors.Emerald else ProColors.Amber,
                            bgColor = if (missingTotal == 0) ProColors.EmeraldBg else ProColors.AmberBg
                        )
                    }

                    LinearProgressIndicator(
                        progress = { coverageRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (missingTotal == 0) ProColors.Emerald else ProColors.Primary,
                        trackColor = ProColors.Surface
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAutoAssign,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(11.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                        ) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Tự động xếp ca", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onOpenAssign,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(11.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            border = BorderStroke(1.dp, ProColors.Primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ProColors.Primary, containerColor = Color.White)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Phân ca tay", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }

        // b. Khối lượng công việc tuần (Horizontal cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProSectionHeader("Giờ công nhân viên tuần này")
                if (schedule.staffStats.isEmpty()) {
                    ProCard {
                        Text(
                            "Chưa có tài khoản nhân viên (STAFF).",
                            fontSize = 13.sp,
                            color = ProColors.Muted,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(schedule.staffStats) { st ->
                            WorkloadCard(st, schedule.maxWeeklyHours)
                        }
                    }
                }
            }
        }

        // c. Card gập/mở Nguyên tắc phân ca
        item {
            ProCard(onClick = { rulesExpanded = !rulesExpanded }) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(ProColors.PrimarySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = ProColors.Primary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Nguyên tắc phân ca (Luật LĐ)", fontWeight = FontWeight.Bold, color = ProColors.Ink, fontSize = 13.5.sp)
                        }
                        Icon(
                            if (rulesExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = ProColors.Muted
                        )
                    }
                    AnimatedVisibility(visible = rulesExpanded) {
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("3 khung ca chuẩn (06:00 - 23:00):", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = ProColors.Ink)
                            Text("• Ca Sáng (06:00 - 12:00 · 6h): tối thiểu 1 NV", fontSize = 12.sp, color = ProColors.SubInk)
                            Text("• Ca Chiều (12:00 - 17:00 · 5h): tối thiểu 1 NV", fontSize = 12.sp, color = ProColors.SubInk)
                            Text("• Ca Tối (17:00 - 23:00 · 6h): 1 NV (T2–T5), 2 NV cao điểm (T6–CN)", fontSize = 12.sp, color = ProColors.SubInk)
                            Spacer(Modifier.height(4.dp))
                            Text("4 luật bảo vệ sức khỏe & công bằng:", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = ProColors.Ink)
                            Text("1. Không xếp trùng/chồng giờ cùng nhân viên trong ngày.", fontSize = 12.sp, color = ProColors.SubInk)
                            Text("2. Tổng giờ làm trong ngày tối đa 12h (tối đa 2 ca).", fontSize = 12.sp, color = ProColors.SubInk)
                            Text("3. Tổng giờ tuần (T2–CN) tối đa 48h theo luật lao động.", fontSize = 12.sp, color = ProColors.SubInk)
                            Text("4. Nghỉ tối thiểu 8h giữa 2 ca (tránh Ca Tối hôm trước + Ca Sáng hôm sau).", fontSize = 12.sp, color = ProColors.SubInk)
                        }
                    }
                }
            }
        }

        // d. Chế độ Lịch bấm / Cả tuần
        item {
            var calendarMode by remember { mutableStateOf(true) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EsSegmentedControl(
                    leftLabel = "Lịch bấm từng ngày",
                    rightLabel = "Xem cả tuần (7 ngày)",
                    isLeftSelected = calendarMode,
                    onSelectLeft = { calendarMode = true },
                    onSelectRight = { calendarMode = false },
                )
                if (calendarMode && schedule.days.isNotEmpty()) {
                    val todayFmt = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
                    var selectedDate by remember(schedule.weekStart) {
                        mutableStateOf(
                            schedule.days.firstOrNull { it.date == todayFmt }?.date
                                ?: schedule.days.firstOrNull { !it.isPast }?.date
                                ?: schedule.days.first().date
                        )
                    }
                    InteractiveWeekCalendarStrip(
                        days = schedule.days,
                        selectedDate = selectedDate,
                        onSelectDate = { selectedDate = it }
                    )
                    val selectedDay = schedule.days.firstOrNull { it.date == selectedDate } ?: schedule.days.first()
                    DayScheduleCard(
                        day = selectedDay,
                        onSlotClick = onSlotClick,
                        onDeleteShift = onDeleteShift
                    )
                } else {
                    ProSectionHeader("Lịch trực 7 ngày")
                    schedule.days.forEach { day ->
                        DayScheduleCard(
                            day = day,
                            onSlotClick = onSlotClick,
                            onDeleteShift = onDeleteShift
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkloadCard(stat: StaffWorkload, maxWeeklyHours: Int) {
    val limit = maxWeeklyHours.coerceAtLeast(1)
    val ratio = (stat.hours.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
    val hoursDisplay = if (stat.hours % 1.0 == 0.0) stat.hours.toInt().toString() else String.format("%.1f", stat.hours)
    val barColor = when {
        stat.hours >= limit -> ProColors.Rose
        stat.hours > 40.0 -> ProColors.Amber
        else -> ProColors.Primary
    }

    ProCard(modifier = Modifier.width(188.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProAvatar(stat.name, size = 32.dp)
                Column(Modifier.weight(1f)) {
                    Text(stat.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ProColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${stat.shiftCount} ca tuần", fontSize = 11.5.sp, color = ProColors.Muted)
                }
            }
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = barColor,
                trackColor = ProColors.Surface
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${hoursDisplay}h / ${maxWeeklyHours}h",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
                val attendanceLabel = when {
                    stat.lateCount > 0 -> "Muộn ${stat.lateCount} (${stat.totalLateMinutes}')"
                    stat.earlyCount > 0 -> "Về sớm ${stat.earlyCount}"
                    stat.shiftCount > 0 -> "Đúng giờ"
                    else -> ""
                }
                if (attendanceLabel.isNotEmpty()) {
                    Text(
                        attendanceLabel,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            stat.earlyCount > 0 -> ProColors.Rose
                            stat.lateCount > 0 -> ProColors.Amber
                            else -> ProColors.Emerald
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayScheduleCard(
    day: ScheduleDay,
    onSlotClick: (date: String, slotLabel: String) -> Unit,
    onDeleteShift: (StaffShiftItem) -> Unit,
) {
    ProCard(modifier = Modifier.alpha(if (day.isPast) 0.58f else 1f)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = ProColors.PrimarySoft, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            day.weekday,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = ProColors.Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(day.date, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Ink)
                }
                if (day.isPast) {
                    ProStatusDotPill("Đã qua", ProColors.Muted, ProColors.Muted, ProColors.Surface)
                }
            }

            day.slots.forEachIndexed { index, slot ->
                if (index > 0) HorizontalDivider(color = ProColors.Border)
                SlotRow(
                    slot = slot,
                    isPast = day.isPast,
                    onClick = { if (!day.isPast) onSlotClick(day.date, slot.label) },
                    onDeleteShift = onDeleteShift
                )
            }
        }
    }
}

@Composable
private fun SlotRow(
    slot: ScheduleSlot,
    isPast: Boolean,
    onClick: () -> Unit,
    onDeleteShift: (StaffShiftItem) -> Unit,
) {
    val missing = (slot.minStaff - slot.shifts.size).coerceAtLeast(0)
    val periodIcon = when {
        slot.label.contains("Sáng", ignoreCase = true) -> Icons.Rounded.WbSunny
        slot.label.contains("Tối", ignoreCase = true) -> Icons.Rounded.NightsStay
        else -> Icons.Rounded.WbTwilight
    }
    val periodColor = when {
        slot.label.contains("Sáng", ignoreCase = true) -> ProColors.Amber
        slot.label.contains("Tối", ignoreCase = true) -> ProColors.Violet
        else -> ProColors.Sky
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isPast, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(periodIcon, contentDescription = null, tint = periodColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(slot.label, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = ProColors.Ink)
                    Text("Tối thiểu ${slot.minStaff} NV", fontSize = 11.5.sp, color = ProColors.Muted)
                }
            }
            if (missing == 0) {
                ProStatusDotPill("Đủ", ProColors.Emerald, ProColors.Emerald, ProColors.EmeraldBg)
            } else {
                ProStatusDotPill("Thiếu $missing", ProColors.Rose, ProColors.Rose, ProColors.RoseBg)
            }
        }

        if (slot.shifts.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                items(slot.shifts) { sh ->
                    val chipColor = when (sh.status.uppercase()) {
                        "ACTIVE" -> ProColors.Emerald
                        "COMPLETED" -> ProColors.SubInk
                        else -> ProColors.Primary
                    }
                    val chipBg = when (sh.status.uppercase()) {
                        "ACTIVE" -> ProColors.EmeraldBg
                        "COMPLETED" -> ProColors.Surface
                        else -> ProColors.PrimarySoft
                    }
                    Surface(
                        color = chipBg,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 5.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            ProAvatar(sh.staffName, size = 20.dp, bgColor = chipColor.copy(alpha = 0.16f), textColor = chipColor)
                            val lateText = sh.lateMinutes?.takeIf { it > 0 }?.let { " · Muộn $it'" } ?: ""
                            val earlyTag = if (sh.notes.contains("Về sớm")) " · Về sớm" else ""
                            Text(
                                "${sh.staffName}$lateText$earlyTag",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (earlyTag.isNotEmpty()) ProColors.Rose else chipColor
                            )
                            if (sh.status.equals("UPCOMING", ignoreCase = true)) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Hủy ca",
                                    tint = ProColors.Rose,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable { onDeleteShift(sh) }
                                )
                            }
                        }
                    }
                }
                if (!isPast && missing > 0) {
                    item {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, ProColors.Primary.copy(alpha = 0.45f)),
                            modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onClick)
                        ) {
                            Text(
                                "+ Xếp thêm",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProColors.Primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        } else if (!isPast) {
            Text("+ Bấm để phân công ca này", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ProColors.Primary)
        }
    }
}

@Composable
private fun OwnerReportsTab(
    reports: List<ShiftReportItem>,
    onConfirmClick: (ShiftReportItem) -> Unit,
    onRejectClick: (ShiftReportItem) -> Unit,
) {
    val pendingCash = reports.filter { it.status == "PENDING" }.sumOf { it.cashAmount }
    val confirmedCash = reports.filter { it.status == "CONFIRMED" }.sumOf { it.cashAmount }
    val pendingCount = reports.count { it.status == "PENDING" }
    val confirmedCount = reports.count { it.status == "CONFIRMED" }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProKpiCard(
                    icon = Icons.Rounded.Schedule,
                    iconTint = ProColors.Amber,
                    iconBg = ProColors.AmberBg,
                    value = "${pendingCash.vnd()} đ",
                    label = "Chờ duyệt ($pendingCount ca)",
                    modifier = Modifier.weight(1f)
                )
                ProKpiCard(
                    icon = Icons.Rounded.Payments,
                    iconTint = ProColors.Emerald,
                    iconBg = ProColors.EmeraldBg,
                    value = "${confirmedCash.vnd()} đ",
                    label = "Đã về két ($confirmedCount ca)",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (reports.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Chưa có báo cáo bàn giao",
                    subtitle = "Khi nhân viên kết thúc ca và nộp két, báo cáo sẽ hiển thị tại đây."
                )
            }
        } else {
            items(reports) { r ->
                val isConfirmed = r.status == "CONFIRMED"
                val isDiscrepancy = r.status == "DISCREPANCY"
                val stripeColor = when {
                    isConfirmed -> ProColors.Emerald
                    isDiscrepancy -> ProColors.Rose
                    else -> ProColors.Amber
                }
                val pillBg = when {
                    isConfirmed -> ProColors.EmeraldBg
                    isDiscrepancy -> ProColors.RoseBg
                    else -> ProColors.AmberBg
                }
                val pillLabel = when {
                    isConfirmed -> "Đã thu két"
                    isDiscrepancy -> "Đã báo lệch"
                    else -> "Chờ duyệt"
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
                                    ProAvatar(r.staffName, size = 36.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(r.staffName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ProColors.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${r.courtName} · ${r.totalOrders} đơn", fontSize = 12.sp, color = ProColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                ProStatusDotPill(
                                    text = pillLabel,
                                    dotColor = stripeColor,
                                    textColor = stripeColor,
                                    bgColor = pillBg
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(r.shiftDate, fontSize = 12.sp, color = ProColors.SubInk)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ProColors.Surface)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${r.cashAmount.vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Primary)
                                    Text("Tiền mặt két", fontSize = 11.sp, color = ProColors.Muted)
                                }
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${r.qrAmount.vnd()} đ", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Emerald)
                                    Text("VietQR", fontSize = 11.sp, color = ProColors.Muted)
                                }
                            }
                            if (r.handoverNote.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Ghi chú: ${r.handoverNote}", fontSize = 12.5.sp, color = ProColors.SubInk)
                            }
                            if (isDiscrepancy && r.confirmedBy != null) {
                                Spacer(Modifier.height(4.dp))
                                Text("Phản hồi: ${r.confirmedBy}", fontSize = 12.5.sp, color = ProColors.Rose, fontWeight = FontWeight.SemiBold)
                            }
                            if (!isConfirmed && !isDiscrepancy) {
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onRejectClick(r) },
                                        modifier = Modifier.weight(0.75f).height(42.dp),
                                        shape = RoundedCornerShape(11.dp),
                                        border = BorderStroke(1.dp, ProColors.Rose.copy(alpha = 0.5f))
                                    ) {
                                        Text("Báo lệch", color = ProColors.Rose, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                    }
                                    Button(
                                        onClick = { onConfirmClick(r) },
                                        modifier = Modifier.weight(1.25f).height(42.dp),
                                        shape = RoundedCornerShape(11.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Emerald)
                                    ) {
                                        Text("Xác nhận thu két", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    }
                                }
                            } else if (isDiscrepancy) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { onConfirmClick(r) },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(11.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Emerald)
                                ) {
                                    Text("Xác nhận đã đối soát & Thu két", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignShiftBottomSheet(
    staffList: List<StaffMemberOption>,
    courts: List<Court>,
    schedule: OwnerWeekSchedule?,
    initialDate: String?,
    initialSlot: String?,
    initialCourtId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (staffId: String, date: String, slot: String, duty: String, notes: String, courtId: String, courtName: String) -> Unit,
) {
    val fmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val today = remember { LocalDate.now() }
    val dateOptions = remember(schedule) {
        val fromSchedule = schedule?.days
            ?.filter { !it.isPast }
            ?.map { it.date to "${it.weekday} (${it.date.take(5)})" }
            .orEmpty()
        fromSchedule.ifEmpty {
            (0L..6L).map { offset ->
                val d = today.plusDays(offset)
                val ds = d.format(fmt)
                val label = when (offset) {
                    0L -> "Hôm nay (${ds.take(5)})"
                    1L -> "Ngày mai (${ds.take(5)})"
                    else -> ds.take(5)
                }
                ds to label
            }
        }
    }
    val slotOptions = remember(schedule) {
        schedule?.templates?.map { it.label }?.takeIf { it.isNotEmpty() }
            ?: listOf(
                "Ca Sáng (06:00 - 12:00)",
                "Ca Chiều (12:00 - 17:00)",
                "Ca Tối (17:00 - 23:00)"
            )
    }

    var selectedDate by remember(initialDate, dateOptions) {
        mutableStateOf(initialDate?.takeIf { d -> dateOptions.any { it.first == d } } ?: dateOptions.first().first)
    }
    var selectedSlot by remember(initialSlot, slotOptions) {
        mutableStateOf(initialSlot?.takeIf { s -> s in slotOptions } ?: slotOptions.first())
    }
    val hoursMap = remember(schedule) {
        schedule?.staffStats?.associate { it.staffId to it.hours } ?: emptyMap()
    }
    val assignedStaffInSelectedSlot = remember(schedule, selectedDate, selectedSlot) {
        schedule?.days
            ?.firstOrNull { it.date == selectedDate }
            ?.slots
            ?.firstOrNull { it.label == selectedSlot }
            ?.shifts
            ?.map { it.staffId }
            ?.toSet()
            ?: emptySet()
    }

    var selectedStaffId by remember {
        mutableStateOf(
            staffList.firstOrNull { it.id !in assignedStaffInSelectedSlot }?.id
                ?: staffList.firstOrNull()?.id
                ?: "user_staff_1"
        )
    }
    var selectedCourt by remember(courts, initialCourtId) {
        mutableStateOf(
            initialCourtId?.let { id -> courts.firstOrNull { it.id == id } }
                ?: courts.firstOrNull { it.id == "court_thanh_phat" }
                ?: courts.firstOrNull()
        )
    }
    var duty by remember { mutableStateOf("Lễ tân & Thu ngân") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Sắp Xếp Ca Làm Việc Mới", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EsColors.Title)
            Spacer(Modifier.height(10.dp))

            if (courts.isNotEmpty()) {
                Text("Chi nhánh / Cụm sân trực:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    items(courts) { c ->
                        FilterChip(
                            selected = selectedCourt?.id == c.id,
                            onClick = { selectedCourt = c },
                            label = { Text(c.name) }
                        )
                    }
                }
            }

            Text("1. Ngày làm việc:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                items(dateOptions) { (d, label) ->
                    FilterChip(selected = selectedDate == d, onClick = { selectedDate = d }, label = { Text(label) })
                }
            }

            Text("2. Khung ca trực:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                items(slotOptions) { slot ->
                    FilterChip(selected = selectedSlot == slot, onClick = { selectedSlot = slot }, label = { Text(slot) })
                }
            }

            Text("3. Chọn nhân viên:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                items(staffList) { st ->
                    val alreadyAssigned = st.id in assignedStaffInSelectedSlot
                    val h = hoursMap[st.id] ?: 0.0
                    val hStr = if (h % 1.0 == 0.0) "${h.toInt()}h" else String.format("%.1fh", h)
                    val label = if (alreadyAssigned) "${st.name} (đã có ca)" else "${st.name} ($hStr)"
                    FilterChip(
                        selected = selectedStaffId == st.id && !alreadyAssigned,
                        enabled = !alreadyAssigned,
                        onClick = { selectedStaffId = st.id },
                        label = { Text(label) }
                    )
                }
            }

            Text("4. Vị trí phụ trách:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                items(listOf("Lễ tân & Thu ngân", "Trực sân & POS", "Vệ sinh & Bảo trì")) { preset ->
                    FilterChip(selected = duty == preset, onClick = { duty = preset }, label = { Text(preset) })
                }
            }
            OutlinedTextField(value = duty, onValueChange = { duty = it }, label = { Text("Vị trí phụ trách") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Ghi chú (tùy chọn)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(14.dp))

            EsPrimaryButton(
                text = "Xác nhận phân ca làm việc",
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedStaffId !in assignedStaffInSelectedSlot,
                onClick = {
                    onConfirm(
                        selectedStaffId,
                        selectedDate,
                        selectedSlot,
                        duty,
                        notes,
                        selectedCourt?.id ?: "court_thanh_phat",
                        selectedCourt?.name ?: "Sân bóng Thành Phát",
                    )
                }
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OwnerManageTab(
    branches: List<BranchRevenueItem>,
    staffAccounts: List<OwnerStaffAccount>,
    onEditPricing: (BranchRevenueItem) -> Unit,
    onCreateCourt: () -> Unit,
    onQuickAddField: (BranchRevenueItem) -> Unit,
    onQuickDeleteField: (BranchRevenueItem, BranchFieldItem) -> Unit,
    onStaffClick: (OwnerStaffAccount) -> Unit,
    onCreateStaff: () -> Unit,
    onToggleLock: (OwnerStaffAccount) -> Unit,
) {
    val context = LocalContext.current
    var showBranches by remember { mutableStateOf(true) }
    var selectedBranchId by remember { mutableStateOf<String?>(null) }
    var showMap by remember { mutableStateOf(true) }
    var staffQuery by remember { mutableStateOf("") }
    var staffFilter by remember { mutableStateOf("ALL") }

    val sortedBranches = remember(branches) {
        branches.sortedWith(
            compareByDescending<BranchRevenueItem> { it.revenue }
                .thenByDescending { it.fieldCount }
                .thenBy { it.courtName }
        )
    }

    val visibleBranches = remember(sortedBranches, selectedBranchId) {
        if (selectedBranchId == null) sortedBranches
        else sortedBranches.filter { it.courtId == selectedBranchId }.ifEmpty { sortedBranches }
    }

    val totalFields = remember(sortedBranches) { sortedBranches.sumOf { it.fields.size.coerceAtLeast(it.fieldCount) } }
    val totalActive = remember(sortedBranches) { sortedBranches.sumOf { it.activeCount } }

    val filteredStaff = remember(staffAccounts, staffQuery, staffFilter) {
        staffAccounts.filter { st ->
            val matchStatus = when (staffFilter) {
                "ACTIVE" -> !st.locked
                "LOCKED" -> st.locked
                else -> true
            }
            val q = staffQuery.trim().lowercase()
            val matchQ = q.isEmpty() || st.name.lowercase().contains(q) ||
                st.phone.contains(q) || st.email.lowercase().contains(q) || st.address.lowercase().contains(q)
            matchStatus && matchQ
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EsSegmentedControl(
                leftLabel = "Chi nhánh & Sân (${branches.size})",
                rightLabel = "Nhân viên (${staffAccounts.size})",
                isLeftSelected = showBranches,
                onSelectLeft = { showBranches = true },
                onSelectRight = { showBranches = false },
            )
        }

        if (showBranches) {
            item {
                ProSectionHeader(
                    title = "Hệ thống sân ($totalFields sân con)",
                    actionText = "+ Thêm chi nhánh",
                    onAction = onCreateCourt
                )
            }

            if (sortedBranches.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${sortedBranches.size} chi nhánh · $totalFields sân con · $totalActive đang đá",
                            fontSize = 12.5.sp,
                            color = ProColors.SubInk,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = { showMap = !showMap },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Map,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = ProColors.Primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (showMap) "Ẩn bản đồ" else "Hiện bản đồ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ProColors.Primary
                            )
                        }
                    }
                }

                if (showMap) {
                    item {
                        OwnerBranchesMapCard(
                            branches = sortedBranches,
                            selectedBranchId = selectedBranchId,
                            onSelectBranch = { clickedId ->
                                selectedBranchId = if (selectedBranchId == clickedId) null else clickedId
                            }
                        )
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedBranchId == null,
                                onClick = { selectedBranchId = null },
                                label = {
                                    Text(
                                        "Tất cả (${sortedBranches.size})",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selectedBranchId == null) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                        items(sortedBranches, key = { "chip_${it.courtId}" }) { br ->
                            val isSel = selectedBranchId == br.courtId
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedBranchId = if (isSel) null else br.courtId },
                                label = {
                                    Text(
                                        "${br.courtName} (${br.fields.size.coerceAtLeast(br.fieldCount)})",
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (sortedBranches.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Chưa có chi nhánh nào",
                        subtitle = "Bấm '+ Thêm chi nhánh' để tạo cụm sân mới kèm vị trí bản đồ."
                    )
                }
            } else {
                items(visibleBranches, key = { it.courtId }) { b ->
                    val isFootball = b.sportType.contains("Bóng đá", ignoreCase = true)
                    val sportIcon = if (isFootball) Icons.Outlined.SportsSoccer else Icons.Outlined.SportsTennis
                    val sportTint = when {
                        isFootball -> ProColors.Primary
                        b.sportType.contains("Cầu lông", ignoreCase = true) -> ProColors.Emerald
                        else -> ProColors.Amber
                    }
                    val sportBg = when {
                        isFootball -> ProColors.PrimarySoft
                        b.sportType.contains("Cầu lông", ignoreCase = true) -> ProColors.EmeraldBg
                        else -> ProColors.AmberBg
                    }

                    ProCard {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(sportBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(sportIcon, contentDescription = null, tint = sportTint, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            b.courtName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.5.sp,
                                            color = ProColors.Ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${b.sportType}${if (b.indoor) " · Trong nhà" else " · Ngoài trời"} · ${b.fields.size.coerceAtLeast(b.fieldCount)} sân con",
                                            fontSize = 12.sp,
                                            color = ProColors.Muted
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (b.revenue > 0) {
                                        Text(
                                            "${b.revenue.toInt().vnd()} đ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = ProColors.Primary
                                        )
                                    }
                                    if (b.activeCount > 0) {
                                        ProStatusDotPill("${b.activeCount} đang đá", ProColors.Emerald, ProColors.Emerald, ProColors.EmeraldBg)
                                    }
                                }
                            }

                            if (b.address.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = ProColors.Muted, modifier = Modifier.size(14.dp))
                                    Text(
                                        b.address,
                                        fontSize = 12.sp,
                                        color = ProColors.SubInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ProColors.Surface)
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Giá/giờ:", fontSize = 11.5.sp, color = ProColors.Muted, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Sáng ${b.morningPrice / 1000}k", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.SubInk)
                                    Text("Chiều ${b.afternoonPrice / 1000}k", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.Primary)
                                    Text("Tối ${b.eveningPrice / 1000}k", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.Amber)
                                }
                            }

                            HorizontalDivider(color = ProColors.Border.copy(alpha = 0.6f))

                            // Danh sách sân con thuộc chi nhánh này
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Danh sách sân con (${b.fields.size})",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProColors.Ink
                                    )
                                    Text(
                                        "+ Thêm sân con",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProColors.Primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onQuickAddField(b) }
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                if (b.fields.isEmpty()) {
                                    Text(
                                        "Chưa có sân con nào. Bấm '+ Thêm sân con' để tạo.",
                                        fontSize = 12.sp,
                                        color = ProColors.Muted
                                    )
                                } else {
                                    b.fields.forEach { field ->
                                        val typeBadge = when (field.fieldType.uppercase()) {
                                            "5_VS_5" -> "Sân 5"
                                            "7_VS_7" -> "Sân 7"
                                            "11_VS_11" -> "Sân 11"
                                            "BADMINTON", "BADMINTON_VIP" -> "Cầu lông"
                                            "PICKLEBALL", "PICKLEBALL_INDOOR" -> "Pickleball"
                                            else -> b.sportType
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(ProColors.Surface)
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        field.label,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.5.sp,
                                                        color = ProColors.Ink
                                                    )
                                                    Surface(
                                                        color = sportBg,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            typeBadge,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = sportTint,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                if (field.detail.isNotBlank()) {
                                                    Text(
                                                        field.detail,
                                                        fontSize = 11.5.sp,
                                                        color = ProColors.Muted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            if (b.fields.size > 1) {
                                                IconButton(
                                                    onClick = { onQuickDeleteField(b, field) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.DeleteOutline,
                                                        contentDescription = "Xóa sân con",
                                                        tint = ProColors.Muted,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val lat = b.latitude ?: 10.8016
                                        val lng = b.longitude ?: 106.6508
                                        val label = Uri.encode(b.courtName)
                                        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                            .onFailure { Toast.makeText(context, "Không tìm thấy ứng dụng bản đồ", Toast.LENGTH_SHORT).show() }
                                    },
                                    modifier = Modifier.weight(0.9f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, ProColors.Border)
                                ) {
                                    Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(16.dp), tint = ProColors.Primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Chỉ đường", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.Primary)
                                }
                                Button(
                                    onClick = { onEditPricing(b) },
                                    modifier = Modifier.weight(1.1f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sửa chi nhánh & giá", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                ProSectionHeader(
                    title = "Đội ngũ nhân viên (${staffAccounts.size})",
                    actionText = "+ Thêm nhân viên",
                    onAction = onCreateStaff
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = staffQuery,
                        onValueChange = { staffQuery = it },
                        placeholder = { Text("Tìm tên, SĐT, email, chi nhánh...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ProColors.Muted) },
                        trailingIcon = {
                            if (staffQuery.isNotEmpty()) {
                                IconButton(onClick = { staffQuery = "" }) { Icon(Icons.Rounded.Close, contentDescription = "Xóa") }
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf(
                            "ALL" to "Tất cả (${staffAccounts.size})",
                            "ACTIVE" to "Hoạt động (${staffAccounts.count { !it.locked }})",
                            "LOCKED" to "Đã khóa (${staffAccounts.count { it.locked }})"
                        )
                        items(filters) { (key, label) ->
                            FilterChip(selected = staffFilter == key, onClick = { staffFilter = key }, label = { Text(label) })
                        }
                    }
                }
            }
            if (filteredStaff.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Không tìm thấy nhân viên",
                        subtitle = "Thử đổi từ khóa tìm kiếm hoặc bấm '+ Thêm nhân viên' để tạo tài khoản mới."
                    )
                }
            } else {
                items(filteredStaff) { st ->
                    val hoursStr = if (st.totalHours % 1.0 == 0.0) "${st.totalHours.toInt()}h" else String.format("%.1fh", st.totalHours)
                    ProCard(onClick = { onStaffClick(st) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProAvatar(st.name, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        st.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = ProColors.Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    ProStatusDotPill(
                                        text = if (st.locked) "Đã khóa" else "Hoạt động",
                                        dotColor = if (st.locked) ProColors.Rose else ProColors.Emerald,
                                        textColor = if (st.locked) ProColors.Rose else ProColors.Emerald,
                                        bgColor = if (st.locked) ProColors.RoseBg else ProColors.EmeraldBg
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${st.address.ifBlank { "Chi nhánh chính" }} · ${st.phone.ifBlank { st.email }}",
                                    fontSize = 12.sp,
                                    color = ProColors.SubInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${st.shiftCount} ca ($hoursStr) · Muộn ${st.lateCount} · Sớm ${st.earlyCount}",
                                    fontSize = 12.sp,
                                    color = if (st.lateCount + st.earlyCount > 0) ProColors.Amber else ProColors.Muted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onToggleLock(st) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (st.locked) ProColors.EmeraldBg else ProColors.RoseBg)
                            ) {
                                Icon(
                                    imageVector = if (st.locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                    contentDescription = if (st.locked) "Mở khóa" else "Khóa",
                                    tint = if (st.locked) ProColors.Emerald else ProColors.Rose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onCreateStaff,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tạo tài khoản nhân viên mới", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun RejectReportDialog(
    report: ShiftReportItem,
    onDismiss: () -> Unit,
    onConfirmReject: (String) -> Unit,
) {
    var note by remember { mutableStateOf("Tiền mặt thực tế lệch so với báo cáo két") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Báo lệch két tiền?", fontWeight = FontWeight.Bold, color = EsColors.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Gửi yêu cầu kiểm tra lại báo cáo ca của ${report.staffName} (${report.cashAmount.vnd()} đ tiền mặt).",
                    fontSize = 13.sp,
                    color = EsColors.Body
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Lý do / Ghi chú cho nhân viên") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmReject(note) },
                enabled = note.trim().isNotEmpty()
            ) {
                Text("Gửi báo lệch", color = EsColors.Danger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = EsColors.Primary) }
        }
    )
}
