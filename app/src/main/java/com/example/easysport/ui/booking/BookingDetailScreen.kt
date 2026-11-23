package com.example.easysport.ui.booking

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.easysport.data.repository.CourtStore
import com.example.easysport.data.repository.SampleData
import com.example.easysport.util.CourtDistanceUtils
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.Booking
import com.example.easysport.data.model.PaymentStatus
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.common.EsSectionTitle
import com.example.easysport.ui.common.EsTopBar
import com.example.easysport.ui.common.vnd

@Composable
fun BookingDetailScreen(
    booking: Booking,
    onBack: () -> Unit = {},
    onSupport: (() -> Unit)? = null,
    onDirections: (() -> Unit)? = null,
    onContactOwner: (() -> Unit)? = null,
    onCancelBooking: () -> Unit = {},
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val court = remember(booking.courtId) {
        CourtStore.getCourtById(booking.courtId)
    }
    val hasValidCoord = court != null && CourtDistanceUtils.isValidCoordinate(court.latitude, court.longitude)

    val handleSupport = onSupport ?: {
        Toast.makeText(context, "Thông tin hỗ trợ chưa được cấu hình. Vui lòng liên hệ đơn vị vận hành.", Toast.LENGTH_SHORT).show()
    }
    val handleDirections = onDirections ?: {
        if (hasValidCoord) {
            val uri = Uri.parse("google.navigation:q=${court!!.latitude},${court.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            try {
                context.startActivity(mapIntent)
            } catch (_: Exception) {
                val geoUri = Uri.parse("geo:${court.latitude},${court.longitude}?q=${court.latitude},${court.longitude}(${Uri.encode(booking.courtName)})")
                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
            }
        } else {
            try {
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(booking.address)}")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                Toast.makeText(context, "Vị trí sân: ${booking.address}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val handleContact = onContactOwner ?: {
        Toast.makeText(context, "Sân chưa cung cấp số điện thoại liên hệ.", Toast.LENGTH_LONG).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        EsTopBar(
            title = "Chi tiết lịch đặt",
            onBack = onBack,
            backTint = EsColors.Primary,
            trailing = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EsColors.SurfaceTint),
                ) {
                    IconButton(onClick = handleSupport, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Outlined.SupportAgent, "Hỗ trợ", tint = EsColors.Primary)
                    }
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            BookingInfoCard(booking)

            Spacer(Modifier.height(24.dp))
            EsSectionTitle("Vị trí sân đấu")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = EsColors.Body, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(booking.address, color = EsColors.Body, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            CourtRealMapCard(
                courtName = booking.courtName,
                address = booking.address,
                latitude = court?.latitude,
                longitude = court?.longitude,
                onDirections = handleDirections,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ----- Thanh hành động cố định phía dưới -----
        Column(Modifier.background(Color.White)) {
            HorizontalDivider(color = EsColors.Border)
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EsColors.DangerBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EsColors.Danger),
                ) {
                    Text("Hủy lịch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                EsPrimaryButton(
                    text = "Liên hệ chủ sân",
                    leadingIcon = Icons.Rounded.Call,
                    onClick = handleContact,
                    modifier = Modifier.weight(1.7f),
                    height = 56.dp,
                )
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Hủy lịch đặt?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc muốn hủy lịch tại ${booking.courtName}?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    onCancelBooking()
                }) { Text("Hủy lịch", color = EsColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Giữ lịch") }
            },
        )
    }
}

// ---------------------------------------------------------------------
@Composable
private fun BookingInfoCard(booking: Booking) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EsColors.Surface)
            .border(1.dp, Color(0xFFE2E9F2), shape)
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = booking.courtName,
                color = EsColors.Title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Outlined.Verified, null, tint = EsColors.Primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFD9E1EC))
        Spacer(Modifier.height(18.dp))

        DetailRow(Icons.Outlined.SportsSoccer, "Sân số") {
            Text(booking.fieldDetail, color = EsColors.Title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(18.dp))
        DetailRow(Icons.Outlined.AccessTime, "Giờ thuê") {
            Text(
                "${booking.timeRange} | ${booking.dateLabel}",
                color = EsColors.Title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(18.dp))
        DetailRow(Icons.Outlined.LocalOffer, "Giá thuê") {
            Text("${booking.totalPrice.vnd()} đ", color = EsColors.Primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (booking.depositAmount > 0 || booking.remainingAmount > 0) {
                Spacer(Modifier.height(4.dp))
                if (booking.depositAmount > 0) {
                    Text(
                        text = "• Đã cọc online SePay: ${booking.depositAmount.vnd()} đ",
                        color = EsColors.Success,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (booking.remainingAmount > 0) {
                    Text(
                        text = "• Còn lại trả tại sân: ${booking.remainingAmount.vnd()} đ",
                        color = EsColors.Warning,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        DetailRow(Icons.Outlined.CreditCard, "Trạng thái thanh toán") {
            PaymentStatusPill(booking.payment)
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = EsColors.Primary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, color = EsColors.Body, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun PaymentStatusPill(status: PaymentStatus) {
    val (fg, bg, icon, text) = when (status) {
        PaymentStatus.PAID -> Quadruple(
            EsColors.Success,
            EsColors.SuccessBg,
            Icons.Rounded.Check,
            "Đã thanh toán đủ (100%)"
        )
        PaymentStatus.DEPOSIT_PAID -> Quadruple(
            EsColors.Primary,
            EsColors.SurfaceTint,
            Icons.Rounded.Check,
            "Đã đặt cọc 30%"
        )
        PaymentStatus.PENDING -> Quadruple(
            EsColors.Warning,
            EsColors.WarningBg,
            Icons.Rounded.AccessTimeFilled,
            "Chờ thanh toán"
        )
        PaymentStatus.EXPIRED -> Quadruple(
            EsColors.Disabled,
            Color(0xFFF1F5F9),
            Icons.Rounded.AccessTimeFilled,
            "Đã hết hạn"
        )
        PaymentStatus.FAILED, PaymentStatus.CANCELLED -> Quadruple(
            EsColors.Danger,
            Color(0xFFFEE2E2),
            Icons.Rounded.Close,
            if (status == PaymentStatus.CANCELLED) "Đã hủy" else "Thất bại"
        )
    }

    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ---------------------------------------------------------------------
/** Bản đồ thực tế hiển thị vị trí sân đấu qua Google Maps SDK. */
@Composable
private fun CourtRealMapCard(
    courtName: String,
    address: String,
    latitude: Double?,
    longitude: Double?,
    onDirections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    val hasCoordinates = CourtDistanceUtils.isValidCoordinate(latitude, longitude)
    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Box(Modifier.fillMaxSize()) {
            if (isPreview) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Bản đồ vị trí sân (Preview Mode)", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
            } else if (hasCoordinates) {
                val courtPosition = LatLng(latitude!!, longitude!!)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(courtPosition, 15f)
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = false,
                        mapToolbarEnabled = false
                    )
                ) {
                    Marker(
                        state = rememberMarkerState(position = courtPosition),
                        title = courtName,
                        snippet = address
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = EsColors.Primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Chưa có tọa độ vệ tinh cho sân này",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EsColors.Title
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Chỉ đường sẽ tìm kiếm theo địa chỉ: $address",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Nút chỉ đường nổi góc dưới
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, EsColors.Border),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clickable(onClick = onDirections)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Navigation,
                        contentDescription = "Chỉ đường",
                        tint = EsColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Chỉ đường",
                        color = EsColors.Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 952)
@Composable
private fun BookingDetailPreview() {
    BookingDetailScreen(booking = SampleData.upcomingBooking)
}

