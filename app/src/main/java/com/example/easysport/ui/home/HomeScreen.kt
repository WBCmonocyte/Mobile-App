package com.example.easysport.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.Booking
import com.example.easysport.data.model.Court
import com.example.easysport.ui.common.CourtImage
import com.example.easysport.ui.common.EsBottomBar
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsLogoMark
import com.example.easysport.ui.common.RatingRow
import com.example.easysport.ui.common.icon
import com.example.easysport.ui.common.oneDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class HomeTab(val label: String, val icon: ImageVector) {
    HOME("Trang chủ", Icons.Outlined.Home),
    MAP("Bản đồ", Icons.Outlined.Map),
    NEWS("Tin nổi bật", Icons.Outlined.Newspaper),
    ACCOUNT("Tài khoản", Icons.Outlined.Person),
}

@Composable
fun HomeScreen(
    courts: List<Court> = emptyList(),
    upcoming: Booking? = null,
    allBookings: List<Booking> = emptyList(),
    bookingDates: Set<LocalDate> = emptySet(),
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onBookCourt: () -> Unit = {},
    onOpenBooking: (String) -> Unit = {},
    onCourtClick: (String) -> Unit = {},
    onNotifications: () -> Unit = {},
    onTabSelected: (HomeTab) -> Unit = {},
    onOpenMap: (String?) -> Unit = { onTabSelected(HomeTab.MAP) },
    onOpenProfile: () -> Unit = { onTabSelected(HomeTab.ACCOUNT) },
    onOpenWallet: () -> Unit = {},
    onOpenRecurring: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedSport by remember { mutableStateOf(SPORTS[0]) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Tìm booking tương ứng với ngày được chọn (nếu có)
    val selectedDateStr = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    val currentDayBooking = allBookings.firstOrNull { it.dateLabel.contains(selectedDateStr) && it.payment in listOf(com.example.easysport.data.model.PaymentStatus.PAID, com.example.easysport.data.model.PaymentStatus.DEPOSIT_PAID) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            HomeHeader(
                onNotifications = onNotifications,
                onOpenProfile = onOpenProfile,
            )

            Spacer(Modifier.height(20.dp))
            CalendarSection(
                bookingDates = bookingDates,
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    selectedDate = date
                    val dateFormatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    val match = allBookings.firstOrNull { it.dateLabel.contains(dateFormatted) }
                    // Nếu là ô màu xanh (có lịch đặt), mở ngay chi tiết lịch đặt
                    if (date in bookingDates && match != null) {
                        onOpenBooking(match.id)
                    }
                }
            )

            if (currentDayBooking != null) {
                Spacer(Modifier.height(16.dp))
                UpcomingCard(currentDayBooking) { onOpenBooking(currentDayBooking.id) }
            }

            Spacer(Modifier.height(24.dp))
            BookBanner(onBookCourt)

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = EsColors.SurfaceTint,
                    border = BorderStroke(1.dp, EsColors.Primary.copy(alpha = 0.2f)),
                    onClick = onOpenRecurring,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = EsColors.Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Lịch cố định", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = EsColors.Title)
                            Text("Gói 3 tháng · 12 buổi", fontSize = 11.5.sp, color = EsColors.Body)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = EsColors.SuccessBg,
                    border = BorderStroke(1.dp, EsColors.Success.copy(alpha = 0.25f)),
                    onClick = onOpenWallet,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = EsColors.Success, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Ví EasySport", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = EsColors.Title)
                            Text("Số dư & hoàn tiền", fontSize = 11.5.sp, color = EsColors.Body)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SportChips(
                selected = selectedSport,
                onSelect = { selectedSport = it }
            )

            Spacer(Modifier.height(16.dp))
            NearbyCard(
                courts = courts,
                selectedSport = selectedSport.label,
                onExploreMaps = { onOpenMap(selectedSport.label) },
                onCourtClick = onCourtClick,
                isRefreshing = isRefreshing,
                errorMessage = errorMessage,
                onRefresh = onRefresh,
            )
            Spacer(Modifier.height(24.dp))
        }
        HomeBottomBar(selected = HomeTab.HOME, onSelected = onTabSelected)
    }
}

// ---------------------------------------------------------------------
@Composable
private fun HomeHeader(
    onNotifications: () -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onOpenProfile)
        ) {
            EsLogoMark(markSize = 40.dp)
            Spacer(Modifier.width(12.dp))
            Text("Easy Sport", color = EsColors.Title, fontSize = 28.sp, fontWeight = FontWeight.Normal)
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EsColors.Surface)
                .clickable(onClick = onNotifications),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Notifications, "Thông báo", tint = EsColors.Title, modifier = Modifier.size(24.dp))
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 11.dp)
                    .size(8.dp)
                    .background(EsColors.Primary, CircleShape),
            )
        }
    }
}

// ---------------------------------------------------------------------
private val DAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

@Composable
private fun CalendarSection(
    bookingDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val weekStart = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var weekMode by remember { mutableStateOf(true) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (weekMode) {
            Text(
                text = "Tháng ${selectedDate.monthValue}, ${selectedDate.year}",
                color = EsColors.Title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.ChevronLeft, "Tháng trước", tint = EsColors.Title)
                }
                Text(
                    text = "Tháng ${currentYearMonth.monthValue}, ${currentYearMonth.year}",
                    color = EsColors.Title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.ChevronRight, "Tháng sau", tint = EsColors.Title)
                }
            }
        }
        WeekMonthToggle(weekMode) { weekMode = it }
    }

    Spacer(Modifier.height(14.dp))

    if (weekMode) {
        // Chế độ Tuần (7 ngày)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                DayCell(
                    label = DAY_LABELS[i],
                    day = date.dayOfMonth,
                    hasBooking = date in bookingDates,
                    isSelected = date == selectedDate,
                    onClick = { onDateSelected(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        // Chế độ Tháng (Lưới toàn bộ các ngày trong tháng)
        MonthCalendarGrid(
            yearMonth = currentYearMonth,
            bookingDates = bookingDates,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
private fun MonthCalendarGrid(
    yearMonth: YearMonth,
    bookingDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1) // Thứ 2 = 0, CN = 6
    val totalCells = dayOfWeekOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EsColors.Surface)
            .padding(12.dp)
    ) {
        // Hàng tiêu đề thứ
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = EsColors.Body,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Lưới các ngày trong tháng
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - dayOfWeekOffset + 1
                    if (day in 1..daysInMonth) {
                        val date = yearMonth.atDay(day)
                        val hasBooking = date in bookingDates
                        val isSelected = date == selectedDate
                        val shape = RoundedCornerShape(10.dp)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .then(if (isSelected) Modifier.border(2.dp, EsColors.Primary, shape) else Modifier)
                                .clip(shape)
                                .background(if (hasBooking) EsColors.Primary else Color.White)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$day",
                                    color = if (hasBooking) Color.White else EsColors.Title,
                                    fontSize = 14.sp,
                                    fontWeight = if (hasBooking || isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasBooking) {
                                    Spacer(Modifier.height(1.dp))
                                    Box(
                                        Modifier
                                            .size(4.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekMonthToggle(weekMode: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(EsColors.Surface)
            .border(1.dp, EsColors.Border, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        @Composable
        fun Segment(text: String, active: Boolean, onClick: () -> Unit) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = text,
                    color = if (active) EsColors.Primary else EsColors.Body,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
        Segment("Tuần", weekMode) { onChange(true) }
        Segment("Tháng", !weekMode) { onChange(false) }
    }
}

@Composable
private fun DayCell(
    label: String,
    day: Int,
    hasBooking: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = if (hasBooking) EsColors.Primary else EsColors.Body,
            fontSize = 13.sp,
            fontWeight = if (hasBooking) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .then(if (isSelected) Modifier.border(2.dp, Color(0xFFC3D6EC), shape) else Modifier)
                .clip(shape)
                .background(if (hasBooking) EsColors.Primary else EsColors.Surface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$day",
                    color = if (hasBooking) Color.White else EsColors.Title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (hasBooking) {
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.size(4.dp).background(Color.White, CircleShape))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun UpcomingCard(booking: Booking, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EsColors.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = booking.timeRange.substringBefore(" - ").substringBefore(","),
            color = EsColors.Primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(14.dp))
        Box(Modifier.width(1.dp).height(36.dp).background(EsColors.Border))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(booking.courtName, color = EsColors.Title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${booking.dateLabel} · ${booking.timeRange}",
                color = EsColors.Body,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = EsColors.Primary)
    }
}

@Composable
private fun BookBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EsColors.Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Đặt Sân", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Đặt sân · Sẵn sàng ra sân", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White)
    }
}

// ---------------------------------------------------------------------
data class Sport(val label: String, val icon: ImageVector)

val SPORTS = listOf(
    Sport("Bóng đá", Icons.Outlined.SportsSoccer),
    Sport("Cầu lông", com.example.easysport.ui.common.SportIcons.Badminton),
    Sport("Pickleball", com.example.easysport.ui.common.SportIcons.Pickleball),
    Sport("Tennis", Icons.Outlined.SportsTennis),
)

@Composable
private fun SportChips(
    selected: Sport,
    onSelect: (Sport) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SPORTS.forEach { sport ->
            val active = sport.label == selected.label
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) EsColors.Primary else EsColors.Surface)
                    .clickable { onSelect(sport) }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    sport.icon, sport.label,
                    tint = if (active) Color.White else EsColors.Title,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = sport.label,
                    color = if (active) Color.White else EsColors.Title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun NearbyCard(
    courts: List<Court>,
    selectedSport: String,
    onExploreMaps: () -> Unit,
    onCourtClick: (String) -> Unit,
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
) {
    val context = LocalContext.current
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    var userLocation by remember { mutableStateOf<com.example.easysport.location.LocationFetchResult.Success?>(null) }

    LaunchedEffect(Unit) {
        if (!isPreview && com.example.easysport.location.LocationHelper.hasLocationPermission(context) &&
            com.example.easysport.location.LocationHelper.isGpsEnabled(context)) {
            val res = com.example.easysport.location.LocationHelper.getCurrentLocation(context)
            if (res is com.example.easysport.location.LocationFetchResult.Success) {
                userLocation = res
            }
        }
    }

    val filtered = remember(courts, selectedSport, userLocation) {
        com.example.easysport.util.CourtDistanceUtils.filterAndSortCourts(
            courts = courts,
            userLat = userLocation?.latitude,
            userLng = userLocation?.longitude,
            selectedSport = selectedSport,
            applyRadiusFilter = true
        ).take(3)
    }

    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EsColors.SurfaceTint)
            .border(1.dp, EsColors.Primary, shape)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExploreMaps),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Outlined.LocationOn, null, tint = EsColors.Primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Khám phá các sân gần đây",
                    color = EsColors.Primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Xem bản đồ các sân $selectedSport quanh bạn",
                    color = EsColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(Icons.Outlined.NorthEast, "Mở bản đồ", tint = EsColors.Primary, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(if (userLocation != null) EsColors.Primary else Color(0xFF94A3B8), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (userLocation != null) "Vị trí của bạn · ${com.example.easysport.util.CourtDistanceConstants.DISTANCE_ESTIMATE_NOTE}"
                else "Bật định vị để tính khoảng cách sân gần bạn",
                color = EsColors.Body,
                fontSize = 12.sp
            )
        }

        if (isRefreshing && courts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EsColors.Primary, modifier = Modifier.size(32.dp))
            }
        } else if (errorMessage != null && courts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorMessage,
                    color = EsColors.Danger,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRefresh) {
                    Text("Thử lại", color = EsColors.Primary)
                }
            }
        } else if (filtered.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (userLocation != null) "Không có sân $selectedSport trong bán kính 5 km." else "Chưa có sân $selectedSport nào trong hệ thống.",
                color = EsColors.Body,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            filtered.forEach { item ->
                Spacer(Modifier.height(12.dp))
                NearbyCourtItem(item) { onCourtClick(item.court.id) }
            }
        }
    }
}

@Composable
private fun NearbyCourtItem(item: com.example.easysport.util.CourtWithDistance, onClick: () -> Unit) {
    val court = item.court
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CourtImage(court.imageRes, Modifier.size(68.dp), corner = 10.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(court.name, color = EsColors.Title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingRow(court.rating, court.reviewCount, filled = false)
                val distText = item.distanceKm?.let { "  ·  ${item.formattedDistance}" } ?: "  ·  Chưa xác định khoảng cách"
                Text(distText, color = EsColors.Body, fontSize = 14.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                court.amenities.take(2).forEach { amenity ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(amenity.icon(), null, tint = EsColors.Body, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(amenity.label, color = EsColors.Body, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun openGoogleMapsForSport(context: Context, sportName: String) {
    val query = when (sportName) {
        "Bóng đá" -> "sân bóng đá gần đây"
        "Cầu lông" -> "sân cầu lông gần đây"
        "Pickleball" -> "sân pickleball gần đây"
        "Tennis" -> "sân tennis gần đây"
        else -> "sân thể thao gần đây"
    }
    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

// ---------------------------------------------------------------------
@Composable
private fun HomeBottomBar(selected: HomeTab, onSelected: (HomeTab) -> Unit) {
    EsBottomBar(selected = selected, onSelected = onSelected)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 862)
@Composable
private fun HomePreview() {
    HomeScreen()
}
