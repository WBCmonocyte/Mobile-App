package com.example.easysport.ui.booking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.BookingRequest
import com.example.easysport.data.model.Court
import com.example.easysport.data.model.PaymentMethod
import com.example.easysport.data.model.RecurringCheckoutResult
import com.example.easysport.data.model.RecurringPackagePreview
import com.example.easysport.data.model.Session
import com.example.easysport.data.model.TimeSlot
import com.example.easysport.data.model.SlotAvailabilityStatus
import com.example.easysport.data.remote.PaymentApiClient
import com.example.easysport.data.remote.RecurringApiClient
import com.example.easysport.data.remote.WalletApiClient
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.easysport.data.repository.UserStore
import com.example.easysport.data.repository.SampleData
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.common.EsSectionTitle
import com.example.easysport.ui.common.EsTopBar
import com.example.easysport.ui.common.vnd

private const val DEPOSIT_PERCENT = 30

@Composable
fun CourtDetailScreen(
    court: Court,
    onBack: () -> Unit,
    onConfirm: (BookingRequest) -> Unit,
    onRecurringConfirm: (RecurringCheckoutResult) -> Unit = {},
    defaultName: String = "",
    defaultPhone: String = "",
    /** Các ca chọn sẵn (theo label, ví dụ "18:00 - 19:30"). */
    initialSelected: Set<String> = emptySet(),
) {
    val scope = rememberCoroutineScope()
    var favorite by remember { mutableStateOf(false) }
    val fieldOptions = remember(court) {
        court.fields
    }
    var fieldIndex by remember { mutableIntStateOf(0) }
    var selectedLabels by remember { mutableStateOf(initialSelected) }
    val currentUser by UserStore.currentUser.collectAsStateWithLifecycle()
    val initialName = currentUser?.name?.takeIf { it.isNotBlank() } ?: defaultName
    val initialPhone = currentUser?.phone?.takeIf { it.isNotBlank() } ?: defaultPhone
    var name by remember(initialName) { mutableStateOf(initialName) }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var method by remember { mutableStateOf(PaymentMethod.QR_TRANSFER) }
    var isRecurring by remember { mutableStateOf(false) }
    var walletBalance by remember { mutableStateOf<Int?>(null) }
    var recurringPreview by remember { mutableStateOf<RecurringPackagePreview?>(null) }
    var recurringPreviewLoading by remember { mutableStateOf(false) }
    var recurringPreviewError by remember { mutableStateOf<String?>(null) }
    var isStartingRecurring by remember { mutableStateOf(false) }
    var recurringCheckoutError by remember { mutableStateOf<String?>(null) }

    var dateEpoch by androidx.compose.runtime.saveable.rememberSaveable { mutableLongStateOf(java.time.LocalDate.now().toEpochDay()) }
    val rentalDate = java.time.LocalDate.ofEpochDay(dateEpoch)
    val dateLabel = rentalDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    val recurringIdemKey = remember(court.id, fieldIndex, dateEpoch, selectedLabels, method, isRecurring) {
        UUID.randomUUID().toString()
    }

    LaunchedEffect(Unit) {
        WalletApiClient.getMyWallet().onSuccess { walletBalance = it.balance }
    }

    LaunchedEffect(isRecurring) {
        if (isRecurring && method == PaymentMethod.CASH_ON_SITE) {
            method = PaymentMethod.QR_TRANSFER
        }
    }

    // ----- Tính tiền theo ca đã chọn -----
    var liveSlots by remember { mutableStateOf(emptyList<TimeSlot>()) }
    var availabilityReady by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(court.id, fieldIndex, rentalDate, owner) {
        availabilityReady = false
        liveSlots = emptyList()
        selectedLabels = emptySet()
        val field = fieldOptions.getOrNull(fieldIndex) ?: return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            while (true) {
                PaymentApiClient.getCourtAvailability(court.id, field.id.ifBlank { field.detail }, dateLabel)
                    .onSuccess { list ->
                        liveSlots = list.map { item ->
                            val hour = item.slot.substringBefore(":").toIntOrNull() ?: 0
                            val session = if (hour < 12) Session.MORNING else if (hour < 18) Session.AFTERNOON else Session.EVENING
                            val status = when (item.status) {
                                "AVAILABLE" -> SlotAvailabilityStatus.AVAILABLE
                                "HELD" -> SlotAvailabilityStatus.HELD
                                else -> SlotAvailabilityStatus.BOOKED
                            }
                            TimeSlot(item.slot, session, status, item.expiresAt)
                        }
                        availabilityReady = true
                        selectedLabels = selectedLabels.filter { label -> liveSlots.any { it.label == label && it.isAvailable } }.toSet()
                    }.onFailure {
                        availabilityReady = false
                        liveSlots = liveSlots.map { it.copy(status = SlotAvailabilityStatus.BOOKED) }
                        android.widget.Toast.makeText(context, it.message ?: "Không tải được lịch trống", android.widget.Toast.LENGTH_LONG).show()
                    }
                kotlinx.coroutines.delay(15000)
            }
        }
    }

    val selectedSlots = liveSlots.filter { it.label in selectedLabels && it.isAvailable }
    val shiftCount = selectedSlots.size
    val total = selectedSlots.sumOf { court.sessionPrices[it.session] ?: 0 }
    val deposit = total * DEPOSIT_PERCENT / 100
    val hoursText = if (shiftCount % 2 == 0) "${shiftCount * 3 / 2}h" else "${shiftCount * 3 / 2}.5h"
    val payableAmount = if (isRecurring) (recurringPreview?.totalPrice ?: 0) else if (method == PaymentMethod.CASH_ON_SITE) deposit else total
    val walletEnough = method != PaymentMethod.WALLET || (walletBalance != null && walletBalance!! >= payableAmount && payableAmount > 0)
    val canConfirm = !isStartingRecurring && availabilityReady && name.isNotBlank() && phone.matches(Regex("[+0-9]{9,15}")) && shiftCount > 0 && walletEnough && (!isRecurring || (recurringPreview?.allAvailable == true && !recurringPreviewLoading))

    LaunchedEffect(isRecurring, court.id, fieldIndex, dateLabel, selectedLabels, availabilityReady) {
        recurringCheckoutError = null
        if (!isRecurring) {
            recurringPreview = null
            recurringPreviewError = null
            return@LaunchedEffect
        }
        val field = fieldOptions.getOrNull(fieldIndex) ?: return@LaunchedEffect
        val chosen = liveSlots.filter { it.label in selectedLabels && it.isAvailable }.map { it.label }
        if (chosen.isEmpty()) {
            recurringPreview = null
            recurringPreviewError = null
            return@LaunchedEffect
        }
        recurringPreviewLoading = true
        recurringPreviewError = null
        RecurringApiClient.previewPackage(
            courtId = court.id,
            fieldDetail = field.detail.ifBlank { field.id },
            startDate = dateLabel,
            slots = chosen,
        ).onSuccess { res ->
            recurringPreview = res
            if (res.walletBalance != null) walletBalance = res.walletBalance
        }.onFailure { err ->
            recurringPreview = null
            recurringPreviewError = err.localizedMessage ?: "Không kiểm tra được lịch 12 buổi"
        }
        recurringPreviewLoading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        EsTopBar(
            title = "Chi tiết & Đặt sân",
            onBack = onBack,
            trailing = {
                IconButton(onClick = { favorite = !favorite }) {
                    Icon(
                        imageVector = if (favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = if (favorite) EsColors.Danger else EsColors.Title,
                    )
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            HeroImage(court)

            Spacer(Modifier.height(12.dp))
            CourtInfoCard(court)

            // ----- Chọn sân -----
            Spacer(Modifier.height(24.dp))
            EsSectionTitle("Chọn sân")
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                fieldOptions.forEachIndexed { index, field ->
                    val active = index == fieldIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) EsColors.Primary else EsColors.Surface)
                            .border(
                                width = 1.dp,
                                color = if (active) EsColors.Primary else EsColors.Border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { fieldIndex = index }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = field.label,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (active) Color.White else EsColors.Title,
                            fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            // ----- Giờ thuê: nhóm theo buổi, chọn nhiều ca -----
            Spacer(Modifier.height(24.dp))
            EsSectionTitle(if (isRecurring) "Chọn chế độ & lịch cố định" else "Giờ thuê")
            Spacer(Modifier.height(10.dp))
            BookingModeSegmentedToggle(isRecurring = isRecurring, onChange = { isRecurring = it })
            Spacer(Modifier.height(12.dp))
            RentalDateButton(rentalDate) { dateEpoch = it.toEpochDay(); selectedLabels = emptySet() }
            Spacer(Modifier.height(16.dp))
            FootballTimePicker(liveSlots, selectedLabels) { selectedLabels = it }
            if (isRecurring && shiftCount > 0) {
                Spacer(Modifier.height(16.dp))
                RecurringPackagePreviewPanel(
                    preview = recurringPreview,
                    isLoading = recurringPreviewLoading,
                    errorMessage = recurringPreviewError,
                )
            } else if (shiftCount > 0) {
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(EsColors.SurfaceTint).padding(16.dp)) {
                    val slotsSummary = selectedSlots.joinToString(", ") { it.label }
                    Text("${fieldOptions[fieldIndex].label} · $shiftCount ca · $hoursText\nKhung giờ: $slotsSummary", color = EsColors.Body, fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tiền sân", color = EsColors.Title, fontWeight = FontWeight.SemiBold)
                        Text("${total.vnd()} đ", color = EsColors.Primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            // ----- Thông tin người đặt -----
            Spacer(Modifier.height(24.dp))
            EsSectionTitle("Thông tin người đặt")
            Spacer(Modifier.height(12.dp))
            InfoField("Người đặt", name, { name = it }, Icons.Outlined.Person, KeyboardType.Text)
            Spacer(Modifier.height(12.dp))
            InfoField("SĐT", phone, { phone = it }, Icons.Outlined.Phone, KeyboardType.Phone)

            // ----- Thanh toán -----
            Spacer(Modifier.height(24.dp))
            EsSectionTitle("Thanh toán & Đặt cọc")
            Spacer(Modifier.height(6.dp))
            Text("Phương thức thanh toán", color = EsColors.Body, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            PaymentOption(
                icon = Icons.Outlined.QrCode2,
                text = if (isRecurring) "Thanh toán trọn gói 12 buổi qua mã QR (SePay)" else "Thanh toán tiền cọc và tiền sân qua mã QR",
                selected = method == PaymentMethod.QR_TRANSFER,
                onClick = { method = PaymentMethod.QR_TRANSFER },
            )
            if (!isRecurring) {
                Spacer(Modifier.height(12.dp))
                PaymentOption(
                    icon = Icons.Outlined.Payments,
                    text = "Thanh toán tiền cọc qua mã QR và tiền sân khi nhận sân",
                    selected = method == PaymentMethod.CASH_ON_SITE,
                    onClick = { method = PaymentMethod.CASH_ON_SITE },
                )
            }
            Spacer(Modifier.height(12.dp))
            PaymentOption(
                icon = Icons.Outlined.AccountBalanceWallet,
                text = "Ví EasySport · Số dư: ${(walletBalance ?: 0).vnd()} đ",
                selected = method == PaymentMethod.WALLET,
                onClick = { method = PaymentMethod.WALLET },
            )
            if (method == PaymentMethod.WALLET && !walletEnough && payableAmount > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Số dư Ví chưa đủ (${(walletBalance ?: 0).vnd()} đ / cần ${payableAmount.vnd()} đ). Vui lòng chọn SePay QR.",
                    color = EsColors.Danger,
                    fontSize = 12.sp,
                )
            }

            // ----- Tổng kết -----
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EsColors.SurfaceTint)
                    .padding(16.dp),
            ) {
                if (isRecurring) {
                    val recUnit = recurringPreview?.unitPrice ?: total
                    val recTotal = recurringPreview?.totalPrice ?: (total * 12)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Giá mỗi buổi ($shiftCount ca · $hoursText):", color = EsColors.Title, fontSize = 14.5.sp)
                        Text("${recUnit.vnd()} đ", color = EsColors.Title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = EsColors.Border)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Tổng thanh toán (12 buổi):", color = EsColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${recTotal.vnd()} đ", color = EsColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng tiền ($shiftCount ca · $hoursText):", color = EsColors.Title, fontSize = 15.sp)
                        Text("${total.vnd()} đ", color = EsColors.Title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = EsColors.Border)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (method == PaymentMethod.CASH_ON_SITE) "Tiền đặt cọc ($DEPOSIT_PERCENT%):" else "Thanh toán ngay:",
                            color = EsColors.Primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${(if (method == PaymentMethod.CASH_ON_SITE) deposit else total).vnd()} đ",
                            color = EsColors.Primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (recurringCheckoutError != null) {
                Spacer(Modifier.height(8.dp))
                Text(recurringCheckoutError ?: "", color = EsColors.Danger, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
            EsPrimaryButton(
                text = when {
                    isStartingRecurring -> "Đang giữ 12 buổi..."
                    isRecurring -> "Thanh toán gói cố định 12 buổi"
                    else -> "Thanh toán"
                },
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                enabled = canConfirm,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!isRecurring) {
                        onConfirm(
                            BookingRequest(
                                courtId = court.id,
                                field = fieldOptions[fieldIndex],
                                slots = selectedSlots,
                                dateLabel = dateLabel,
                                customerName = name.trim(),
                                phone = phone.trim(),
                                paymentMethod = method,
                                total = total,
                                deposit = if (method == PaymentMethod.CASH_ON_SITE) deposit else total,
                            )
                        )
                    } else {
                        scope.launch {
                            isStartingRecurring = true
                            recurringCheckoutError = null
                            RecurringApiClient.startRecurringCheckout(
                                courtId = court.id,
                                fieldDetail = fieldOptions[fieldIndex].detail.ifBlank { fieldOptions[fieldIndex].id },
                                startDate = dateLabel,
                                slots = selectedSlots.map { it.label },
                                paymentMethod = method.name,
                                customerName = name.trim(),
                                customerPhone = phone.trim(),
                                idempotencyKey = recurringIdemKey,
                            ).onSuccess { res ->
                                onRecurringConfirm(res)
                            }.onFailure { err ->
                                recurringCheckoutError = err.localizedMessage ?: "Không thể giữ gói cố định. Vui lòng kiểm tra lại lịch."
                            }
                            isStartingRecurring = false
                        }
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun HeroImage(court: Court) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Image(
            painter = painterResource(court.imageRes),
            contentDescription = court.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Star, null, tint = EsColors.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("${court.rating}", color = EsColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(" (${court.reviewCount} đánh giá)", color = EsColors.Title, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CourtInfoCard(court: Court) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(EsColors.Surface)
            .padding(16.dp),
    ) {
        Text(court.name, color = EsColors.Title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, null, tint = EsColors.Body, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(court.address, color = EsColors.Body, fontSize = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = court.amenities.joinToString(" • ") { it.label },
            color = EsColors.Primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(EsColors.SurfaceTint)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun InfoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    keyboardType: KeyboardType,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = EsColors.Body) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = EsColors.Surface,
            unfocusedContainerColor = EsColors.Surface,
            focusedBorderColor = EsColors.Primary,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = EsColors.Primary,
            unfocusedLabelColor = EsColors.Body,
            focusedTextColor = EsColors.Title,
            unfocusedTextColor = EsColors.Title,
            cursorColor = EsColors.Primary,
        ),
    )
}

@Composable
private fun PaymentOption(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) EsColors.SurfaceTint else EsColors.Surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) EsColors.Primary else EsColors.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) EsColors.Primary else EsColors.Body, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = if (selected) EsColors.Primary else EsColors.Title,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = EsColors.Primary,
                unselectedColor = EsColors.Disabled,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1700)
@Composable
private fun CourtDetailPreview() {
    CourtDetailScreen(
        court = SampleData.courts.first(),
        onBack = {},
        onConfirm = {},
        defaultName = "Nguyễn Văn A",
        defaultPhone = "0901 234 567",
        initialSelected = setOf("10:00 - 11:30"),
    )
}

