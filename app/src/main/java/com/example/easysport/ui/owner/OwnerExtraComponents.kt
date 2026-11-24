package com.example.easysport.ui.owner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.easysport.BuildConfig
import com.example.easysport.data.model.BranchFieldItem
import com.example.easysport.data.model.BranchRevenueItem
import com.example.easysport.data.model.Court
import com.example.easysport.data.model.OwnerStaffAccount
import com.example.easysport.data.model.ScheduleDay
import com.example.easysport.data.model.StaffShiftItem
import com.example.easysport.location.LocationFetchResult
import com.example.easysport.location.LocationHelper
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.common.ProAvatar
import com.example.easysport.ui.common.ProCard
import com.example.easysport.ui.common.ProColors
import com.example.easysport.ui.common.ProStatusDotPill
import com.example.easysport.ui.common.vnd
import com.example.easysport.ui.staff.ShiftStatusBadge
import com.example.easysport.ui.staff.StatMiniBox
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker as OsmMarker
import java.io.File

private data class DistrictPreset(
    val label: String,
    val addressSuffix: String,
    val lat: Double,
    val lng: Double,
)

private val HCMC_PRESETS = listOf(
    DistrictPreset("Tân Bình", "Quận Tân Bình, TP. HCM", 10.8016, 106.6508),
    DistrictPreset("Quận 3", "Quận 3, TP. HCM", 10.7758, 106.6869),
    DistrictPreset("Quận 7", "Quận 7, TP. HCM", 10.7340, 106.7218),
    DistrictPreset("Bình Thạnh", "Quận Bình Thạnh, TP. HCM", 10.8106, 106.7091),
    DistrictPreset("Thủ Đức", "TP. Thủ Đức, TP. HCM", 10.8055, 106.7312),
)

@Composable
internal fun OwnerBranchesMapCard(
    branches: List<BranchRevenueItem>,
    selectedBranchId: String?,
    onSelectBranch: (String?) -> Unit,
) {
    if (branches.isEmpty()) return
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val selectedBranch = remember(branches, selectedBranchId) {
        selectedBranchId?.let { id -> branches.firstOrNull { it.courtId == id } }
    }
    val defaultLatLng = remember(branches) {
        val withCoords = branches.filter { it.latitude != null && it.longitude != null }
        if (withCoords.isNotEmpty()) {
            LatLng(
                withCoords.map { it.latitude!! }.average(),
                withCoords.map { it.longitude!! }.average()
            )
        } else {
            LatLng(10.8016, 106.6508)
        }
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 12.6f)
    }

    LaunchedEffect(selectedBranchId) {
        val target = selectedBranch
        if (target?.latitude != null && target.longitude != null) {
            runCatching {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), 15f))
            }
        } else {
            runCatching {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12.6f))
            }
        }
    }

    ProCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = ProColors.Primary, modifier = Modifier.size(18.dp))
                    Text("Bản đồ vị trí chi nhánh", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ProColors.Ink)
                }
                ProStatusDotPill(
                    if (BuildConfig.HAS_MAPS_API_KEY) "Google Maps" else "OpenStreetMap",
                    ProColors.Emerald,
                    ProColors.Emerald,
                    ProColors.EmeraldBg
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, ProColors.Border, RoundedCornerShape(14.dp))
                    .background(ProColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                if (isPreview) {
                    Text("Bản đồ Preview", color = ProColors.Muted)
                } else if (BuildConfig.HAS_MAPS_API_KEY) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = false,
                            zoomControlsEnabled = true,
                            compassEnabled = true,
                            mapToolbarEnabled = false
                        )
                    ) {
                        branches.forEach { br ->
                            val lat = br.latitude ?: 10.8016
                            val lng = br.longitude ?: 106.6508
                            val pos = LatLng(lat, lng)
                            Marker(
                                state = rememberMarkerState(key = "${br.courtId}_${lat}_${lng}", position = pos),
                                title = br.courtName,
                                snippet = "${br.sportType} · ${br.fieldCount} sân con",
                                onClick = {
                                    onSelectBranch(br.courtId)
                                    false
                                }
                            )
                        }
                    }
                } else {
                    OsmBranchesMapView(
                        branches = branches,
                        selectedBranchId = selectedBranchId,
                        onSelectBranch = { onSelectBranch(it.courtId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (selectedBranch != null) {
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
                        Text(
                            selectedBranch.courtName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ProColors.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedBranch.address.isNotBlank()) {
                            Text(
                                selectedBranch.address,
                                fontSize = 11.5.sp,
                                color = ProColors.SubInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val lat = selectedBranch.latitude ?: 10.8016
                            val lng = selectedBranch.longitude ?: 106.6508
                            val label = Uri.encode(selectedBranch.courtName)
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                .onFailure { Toast.makeText(context, "Không tìm thấy ứng dụng bản đồ", Toast.LENGTH_SHORT).show() }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Chỉ đường", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Text(
                    "Chạm vào ghim trên bản đồ hoặc chọn chi nhánh bên dưới để xem chi tiết từng sân.",
                    fontSize = 11.5.sp,
                    color = ProColors.Muted
                )
            }
        }
    }
}

@Composable
internal fun MapLocationPickerDialog(
    initialLat: Double,
    initialLng: Double,
    onDismiss: () -> Unit,
    onPicked: (Double, Double) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf(LatLng(initialLat, initialLng)) }
    var isFetchingGps by remember { mutableStateOf(false) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(initialLat, initialLng), 14.5f)
    }
    val markerState = rememberMarkerState(position = picked)

    LaunchedEffect(picked) {
        markerState.position = picked
        runCatching { cameraState.animate(CameraUpdateFactory.newLatLng(picked)) }
    }

    val fetchGps: () -> Unit = {
        scope.launch {
            isFetchingGps = true
            when (val res = LocationHelper.getCurrentLocation(context)) {
                is LocationFetchResult.Success -> {
                    picked = LatLng(res.latitude, res.longitude)
                    Toast.makeText(context, "Đã lấy vị trí GPS hiện tại", Toast.LENGTH_SHORT).show()
                }
                is LocationFetchResult.GpsDisabled -> {
                    Toast.makeText(context, "Vui lòng bật GPS trên thiết bị", Toast.LENGTH_SHORT).show()
                }
                is LocationFetchResult.PermissionDenied -> {
                    Toast.makeText(context, "Chưa có quyền truy cập vị trí", Toast.LENGTH_SHORT).show()
                }
                is LocationFetchResult.Error -> {
                    Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                }
            }
            isFetchingGps = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchGps()
        else Toast.makeText(context, "Bạn đã từ chối quyền vị trí", Toast.LENGTH_SHORT).show()
    }
    val isPreview = LocalInspectionMode.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Chọn vị trí chi nhánh", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EsColors.Title)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = ProColors.Muted)
                    }
                }
                Text("Chạm lên bản đồ để ghim vị trí hoặc chọn nhanh khu vực:", fontSize = 12.5.sp, color = ProColors.SubInk)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(HCMC_PRESETS) { p ->
                        FilterChip(
                            selected = kotlin.math.abs(picked.latitude - p.lat) < 0.004 && kotlin.math.abs(picked.longitude - p.lng) < 0.004,
                            onClick = { picked = LatLng(p.lat, p.lng) },
                            label = { Text(p.label, fontSize = 12.sp) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ProColors.Border, RoundedCornerShape(14.dp))
                ) {
                    if (BuildConfig.HAS_MAPS_API_KEY && !isPreview) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraState,
                            onMapClick = { latLng -> picked = latLng },
                            uiSettings = MapUiSettings(
                                myLocationButtonEnabled = false,
                                zoomControlsEnabled = true,
                                compassEnabled = true
                            )
                        ) {
                            Marker(
                                state = markerState,
                                title = "Vị trí chi nhánh",
                                snippet = String.format("%.5f, %.5f", picked.latitude, picked.longitude)
                            )
                        }
                    } else {
                        OsmLocationPickerMapView(
                            currentLat = picked.latitude,
                            currentLng = picked.longitude,
                            onLocationPicked = { lat, lng -> picked = LatLng(lat, lng) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            if (LocationHelper.hasLocationPermission(context)) fetchGps()
                            else permissionLauncher.launch(LocationHelper.REQUIRED_PERMISSIONS)
                        },
                        containerColor = Color.White,
                        contentColor = ProColors.Primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ProColors.Primary)
                        } else {
                            Icon(Icons.Rounded.GpsFixed, contentDescription = "GPS")
                        }
                    }
                }

                Surface(color = ProColors.Surface, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = String.format("Tọa độ đã chọn: %.5f, %.5f", picked.latitude, picked.longitude),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ProColors.Primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = { onPicked(picked.latitude, picked.longitude) },
                        modifier = Modifier.weight(1.2f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                    ) {
                        Text("Xác nhận vị trí", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BranchFormBottomSheet(
    existingBranch: BranchRevenueItem?,
    onDismiss: () -> Unit,
    onCreateBranch: (
        name: String,
        address: String,
        sportType: String,
        fieldCount: Int,
        fieldTypeLabel: String,
        indoor: Boolean,
        latitude: Double,
        longitude: Double,
        pricePerHour: Int,
        morningPrice: Int,
        afternoonPrice: Int,
        eveningPrice: Int,
    ) -> Unit,
    onUpdateBranch: (
        courtId: String,
        name: String,
        address: String,
        sportType: String,
        indoor: Boolean,
        latitude: Double,
        longitude: Double,
        pricePerHour: Int,
        morningPrice: Int,
        afternoonPrice: Int,
        eveningPrice: Int,
    ) -> Unit,
    onAddField: (courtId: String, label: String, detail: String, fieldType: String) -> Unit,
    onDeleteField: (courtId: String, fieldId: String) -> Unit,
) {
    val isEdit = existingBranch != null
    var name by remember(existingBranch?.courtId) { mutableStateOf(existingBranch?.courtName ?: "") }
    var address by remember(existingBranch?.courtId) { mutableStateOf(existingBranch?.address ?: "") }
    var sportType by remember(existingBranch?.courtId) { mutableStateOf(existingBranch?.sportType ?: "Bóng đá") }
    var indoor by remember(existingBranch?.courtId) { mutableStateOf(existingBranch?.indoor ?: false) }
    var lat by remember(existingBranch?.courtId) { mutableDoubleStateOf(existingBranch?.latitude ?: 10.8016) }
    var lng by remember(existingBranch?.courtId) { mutableDoubleStateOf(existingBranch?.longitude ?: 106.6508) }
    var hasCustomLocation by remember(existingBranch?.courtId) { mutableStateOf(existingBranch?.latitude != null) }

    var fieldCount by remember { mutableIntStateOf(2) }
    var fieldTypeLabel by remember { mutableStateOf("Sân 5 người") }

    var newFieldLabel by remember(existingBranch?.fields?.size) {
        mutableStateOf("Sân ${(existingBranch?.fields?.size ?: 0) + 1}")
    }
    var newFieldDetail by remember { mutableStateOf("Sân 5 người") }

    var baseStr by remember(existingBranch?.courtId) { mutableStateOf((existingBranch?.pricePerHour ?: 300000).toString()) }
    var morningStr by remember(existingBranch?.courtId) { mutableStateOf((existingBranch?.morningPrice ?: 180000).toString()) }
    var afternoonStr by remember(existingBranch?.courtId) { mutableStateOf((existingBranch?.afternoonPrice ?: 250000).toString()) }
    var eveningStr by remember(existingBranch?.courtId) { mutableStateOf((existingBranch?.eveningPrice ?: 350000).toString()) }

    var showMapPicker by remember { mutableStateOf(false) }

    val base = baseStr.toIntOrNull() ?: 0
    val morning = morningStr.toIntOrNull() ?: 0
    val afternoon = afternoonStr.toIntOrNull() ?: 0
    val evening = eveningStr.toIntOrNull() ?: 0
    val valid = name.trim().length >= 2 && address.trim().length >= 2 &&
        base >= 10000 && morning >= 10000 && afternoon >= 10000 && evening >= 10000

    val existingFields: List<BranchFieldItem> = existingBranch?.fields ?: emptyList()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (isEdit) "Quản lý chi nhánh · ${existingBranch?.courtName}" else "Thêm Chi Nhánh / Cụm Sân Mới",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = EsColors.Title
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên chi nhánh / cụm sân") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Khu vực chi nhánh:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ProColors.Ink)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(HCMC_PRESETS) { preset ->
                    FilterChip(
                        selected = address.contains(preset.label, ignoreCase = true),
                        onClick = {
                            if (address.isBlank()) {
                                address = preset.addressSuffix
                            } else if (!address.contains(preset.label, ignoreCase = true)) {
                                address = "${address.trim().trimEnd(',')}, ${preset.addressSuffix}"
                            }
                            if (!hasCustomLocation) {
                                lat = preset.lat
                                lng = preset.lng
                            }
                        },
                        label = { Text(preset.label, fontSize = 12.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Địa chỉ chi tiết (số nhà, đường, quận)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Bộ môn & Mái che:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ProColors.Ink)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Bóng đá", "Cầu lông", "Pickleball", "Tennis")) { sp ->
                    FilterChip(
                        selected = sportType == sp,
                        onClick = {
                            sportType = sp
                            fieldTypeLabel = when (sp) {
                                "Bóng đá" -> "Sân 5 người"
                                "Cầu lông" -> "Sân thảm đôi"
                                else -> "Sân tiêu chuẩn"
                            }
                        },
                        label = { Text(sp) }
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ProColors.Surface)
                    .clickable { indoor = !indoor }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sân trong nhà / Có mái che", fontSize = 13.5.sp, color = ProColors.Ink, fontWeight = FontWeight.Medium)
                Switch(checked = indoor, onCheckedChange = { indoor = it })
            }

            Surface(
                color = ProColors.PrimarySoft,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ProColors.Primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Vị trí trên bản đồ", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ProColors.Primary)
                        Text(String.format("%.5f, %.5f", lat, lng), fontSize = 12.sp, color = ProColors.SubInk)
                    }
                    OutlinedButton(
                        onClick = { showMapPicker = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ProColors.Primary)
                    ) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = ProColors.Primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Chọn trên bản đồ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ProColors.Primary)
                    }
                }
            }

            if (!isEdit) {
                Surface(color = ProColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Số lượng sân con:", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = ProColors.Ink)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { if (fieldCount > 1) fieldCount-- },
                                    modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White)
                                ) { Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                Text("$fieldCount sân", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ProColors.Primary)
                                IconButton(
                                    onClick = { if (fieldCount < 10) fieldCount++ },
                                    modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White)
                                ) { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val typeOptions = if (sportType == "Bóng đá") {
                                listOf("Sân 5 người", "Sân 7 người", "Sân 11 người")
                            } else {
                                listOf("Sân tiêu chuẩn", "Sân VIP có điều hòa")
                            }
                            items(typeOptions) { t ->
                                FilterChip(selected = fieldTypeLabel == t, onClick = { fieldTypeLabel = t }, label = { Text(t, fontSize = 12.sp) })
                            }
                        }
                        Text(
                            "Sẽ tạo: ${(1..fieldCount).joinToString(", ") { "Sân $it ($fieldTypeLabel)" }}",
                            fontSize = 11.5.sp,
                            color = ProColors.Muted
                        )
                    }
                }
            } else {
                Surface(color = ProColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Danh sách sân con (${existingFields.size})", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = ProColors.Ink)
                        existingFields.forEach { f ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(f.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ProColors.Ink)
                                    Text(f.detail, fontSize = 11.5.sp, color = ProColors.Muted)
                                }
                                if (existingFields.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteField(existingBranch!!.courtId, f.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Xóa sân con", tint = ProColors.Rose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = ProColors.Border)
                        Text("+ Thêm sân con mới vào cụm", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = ProColors.Primary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newFieldLabel,
                                onValueChange = { newFieldLabel = it },
                                label = { Text("Tên sân (VD: Sân 5C)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val code = when {
                                        newFieldDetail.contains("7") -> "7_VS_7"
                                        newFieldDetail.contains("11") -> "11_VS_11"
                                        else -> "5_VS_5"
                                    }
                                    onAddField(existingBranch!!.courtId, newFieldLabel.trim(), newFieldDetail, code)
                                },
                                enabled = newFieldLabel.trim().isNotEmpty(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("Thêm", fontWeight = FontWeight.Bold)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Sân 5 người", "Sân 7 người", "Sân 11 người", "Sân tiêu chuẩn")) { opt ->
                                FilterChip(selected = newFieldDetail == opt, onClick = { newFieldDetail = opt }, label = { Text(opt, fontSize = 11.5.sp) })
                            }
                        }
                    }
                }
            }

            Text("Bảng giá thuê theo khung giờ:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ProColors.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = baseStr,
                    onValueChange = { baseStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Mặc định (đ/h)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = morningStr,
                    onValueChange = { morningStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Sáng 06-12h") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = afternoonStr,
                    onValueChange = { afternoonStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Chiều 12-17h") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = eveningStr,
                    onValueChange = { eveningStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Tối 17-23h") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))
            EsPrimaryButton(
                text = if (isEdit) "Lưu thông tin chi nhánh" else "Tạo chi nhánh & cụm sân mới",
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (existingBranch != null) {
                        onUpdateBranch(
                            existingBranch.courtId,
                            name.trim(),
                            address.trim(),
                            sportType,
                            indoor,
                            lat,
                            lng,
                            base,
                            morning,
                            afternoon,
                            evening,
                        )
                    } else {
                        onCreateBranch(
                            name.trim(),
                            address.trim(),
                            sportType,
                            fieldCount,
                            fieldTypeLabel,
                            indoor,
                            lat,
                            lng,
                            base,
                            morning,
                            afternoon,
                            evening,
                        )
                    }
                }
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showMapPicker) {
        MapLocationPickerDialog(
            initialLat = lat,
            initialLng = lng,
            onDismiss = { showMapPicker = false },
            onPicked = { pickedLat, pickedLng ->
                lat = pickedLat
                lng = pickedLng
                hasCustomLocation = true
                showMapPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StaffDetailBottomSheet(
    staff: OwnerStaffAccount,
    staffShifts: List<StaffShiftItem>,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onEditStaff: (OwnerStaffAccount) -> Unit,
    onToggleLock: (OwnerStaffAccount) -> Unit,
) {
    val hoursDisplay = if (staff.totalHours % 1.0 == 0.0) "${staff.totalHours.toInt()}h" else String.format("%.1fh", staff.totalHours)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProAvatar(staff.name, size = 50.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ProColors.Ink, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        ProStatusDotPill(
                            text = if (staff.locked) "Đã khóa" else "Hoạt động",
                            dotColor = if (staff.locked) ProColors.Rose else ProColors.Emerald,
                            textColor = if (staff.locked) ProColors.Rose else ProColors.Emerald,
                            bgColor = if (staff.locked) ProColors.RoseBg else ProColors.EmeraldBg
                        )
                    }
                    Text(staff.email, fontSize = 12.5.sp, color = ProColors.SubInk)
                    Text(
                        "${staff.phone.ifBlank { "Chưa có SĐT" }} · ${staff.address.ifBlank { "Chưa gán chi nhánh" }}",
                        fontSize = 12.sp,
                        color = ProColors.Muted
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (staff.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onCall(staff.phone) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = ProColors.Primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Gọi điện", fontSize = 12.5.sp, color = ProColors.Primary, maxLines = 1)
                    }
                }
                Button(
                    onClick = { onEditStaff(staff) },
                    modifier = Modifier.weight(1.1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sửa & Đổi MK", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onToggleLock(staff) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    border = BorderStroke(1.dp, if (staff.locked) ProColors.Emerald else ProColors.Rose)
                ) {
                    Icon(
                        if (staff.locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (staff.locked) ProColors.Emerald else ProColors.Rose
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (staff.locked) "Mở khóa" else "Khóa TK",
                        fontSize = 12.sp,
                        color = if (staff.locked) ProColors.Emerald else ProColors.Rose,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMiniBox("Tổng ca", "${staff.shiftCount}", ProColors.Primary, Modifier.weight(1f))
                StatMiniBox("Hoàn thành", "${staff.completedShifts}", ProColors.Emerald, Modifier.weight(1f))
                StatMiniBox("Tổng giờ", hoursDisplay, ProColors.Ink, Modifier.weight(1f))
                StatMiniBox("Muộn / Sớm", "${staff.lateCount} / ${staff.earlyCount}", if (staff.lateCount + staff.earlyCount > 0) ProColors.Rose else ProColors.Emerald, Modifier.weight(1f))
            }

            Text("Lịch sử ca trực gần đây (${staffShifts.size})", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = ProColors.Ink)
            if (staffShifts.isEmpty()) {
                Surface(color = ProColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Nhân viên này chưa được xếp ca làm việc nào.", fontSize = 13.sp, color = ProColors.Muted, modifier = Modifier.padding(14.dp))
                }
            } else {
                staffShifts.take(8).forEach { s ->
                    Surface(color = ProColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${s.shiftDate} · ${s.shiftSlot}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = ProColors.Ink, modifier = Modifier.weight(1f))
                                ShiftStatusBadge(s.status)
                            }
                            Text("${s.courtName} · ${s.duty}", fontSize = 12.sp, color = ProColors.SubInk)
                            if (s.lateMinutes != null && s.lateMinutes > 0) {
                                Text("Đi muộn ${s.lateMinutes} phút (vào lúc ${s.checkInTime ?: "--"})", fontSize = 11.5.sp, color = ProColors.Amber, fontWeight = FontWeight.SemiBold)
                            }
                            if (s.notes.contains("Về sớm")) {
                                Text(s.notes, fontSize = 11.5.sp, color = ProColors.Rose, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun StaffAccountFormDialog(
    existingStaff: OwnerStaffAccount?,
    courts: List<Court>,
    onDismiss: () -> Unit,
    onCreate: (name: String, email: String, phone: String, password: String, address: String) -> Unit,
    onUpdate: (staffId: String, name: String, phone: String, address: String, newPassword: String?) -> Unit,
) {
    val isEdit = existingStaff != null
    var name by remember(existingStaff?.id) { mutableStateOf(existingStaff?.name ?: "") }
    var email by remember(existingStaff?.id) { mutableStateOf(existingStaff?.email ?: "") }
    var phone by remember(existingStaff?.id) { mutableStateOf(existingStaff?.phone ?: "") }
    var address by remember(existingStaff?.id) {
        mutableStateOf(existingStaff?.address?.ifBlank { courts.firstOrNull()?.name ?: "" } ?: (courts.firstOrNull()?.name ?: "Sân bóng Thành Phát"))
    }
    var password by remember(existingStaff?.id) { mutableStateOf(if (isEdit) "" else "123456") }

    val valid = name.trim().length >= 2 &&
        (isEdit || (email.trim().contains("@") && password.length >= 6)) &&
        (!isEdit || password.isEmpty() || password.length >= 6)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(if (isEdit) "Chỉnh sửa nhân viên" else "Thêm nhân viên mới", fontWeight = FontWeight.Bold, color = EsColors.Title)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isEdit) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email đăng nhập") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (courts.isNotEmpty()) {
                    Text("Chi nhánh làm việc chính:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = ProColors.Ink)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(courts) { c ->
                            FilterChip(
                                selected = address == c.name,
                                onClick = { address = c.name },
                                label = { Text(c.name, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Chi nhánh / Khu vực") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(if (isEdit) "Đặt lại mật khẩu mới (bỏ trống nếu giữ nguyên)" else "Mật khẩu khởi tạo (>= 6 ký tự)")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (existingStaff != null) {
                        onUpdate(existingStaff.id, name.trim(), phone.trim(), address.trim(), password.takeIf { it.isNotBlank() })
                    } else {
                        onCreate(name.trim(), email.trim(), phone.trim(), password, address.trim())
                    }
                }
            ) {
                Text(if (isEdit) "Lưu thay đổi" else "Tạo tài khoản", color = EsColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = EsColors.Body) }
        }
    )
}

@Composable
internal fun InteractiveWeekCalendarStrip(
    days: List<ScheduleDay>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        days.forEach { day ->
            val selected = day.date == selectedDate
            val dayNum = day.date.take(2)
            val missingCount = day.slots.sumOf { (it.minStaff - it.shifts.size).coerceAtLeast(0) }
            val hasAnyShift = day.slots.any { it.shifts.isNotEmpty() }
            val dotColor = when {
                day.isPast -> ProColors.Muted
                missingCount == 0 -> ProColors.Emerald
                hasAnyShift -> ProColors.Amber
                else -> ProColors.Rose
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) ProColors.Primary else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (selected) ProColors.Primary else ProColors.Border,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectDate(day.date) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.weekday,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) Color.White.copy(alpha = 0.85f) else ProColors.Muted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dayNum,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else ProColors.Ink
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else dotColor)
                )
            }
        }
    }
}

private val OSM_STREET_TILE_SOURCE = XYTileSource(
    "OsmEasySport",
    1,
    19,
    256,
    ".png",
    arrayOf(
        "https://tile.openstreetmap.org/",
    ),
    "© OpenStreetMap contributors"
)

private fun ensureOsmConfigured(context: Context) {
    val cfg = Configuration.getInstance()
    cfg.userAgentValue = "EasySportVenueManager/1.0 (Android)"
    val basePath = File(context.cacheDir, "osmdroid")
    val tileCache = File(basePath, "tiles")
    if (!tileCache.exists()) tileCache.mkdirs()
    cfg.osmdroidBasePath = basePath
    cfg.osmdroidTileCache = tileCache
}

private fun createPinDrawable(context: Context, fillColorInt: Int, isSelected: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = ((if (isSelected) 34f else 24f) * density).toInt().coerceAtLeast(24)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    if (isSelected) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColorInt
            alpha = 65
        }
        c.drawCircle(cx, cy, sizePx * 0.48f, haloPaint)
    }

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    val outerRadius = if (isSelected) sizePx * 0.34f else sizePx * 0.42f
    c.drawCircle(cx, cy, outerRadius, borderPaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColorInt
    }
    c.drawCircle(cx, cy, outerRadius - 2.2f * density, fillPaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    c.drawCircle(cx, cy, outerRadius * 0.34f, dotPaint)

    return BitmapDrawable(context.resources, bmp)
}

@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun OsmBranchesMapView(
    branches: List<BranchRevenueItem>,
    selectedBranchId: String?,
    onSelectBranch: (BranchRevenueItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember { ensureOsmConfigured(context); true }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val onSelectState by rememberUpdatedState(onSelectBranch)

    val centerAll = remember(branches) {
        val valid = branches.filter { it.latitude != null && it.longitude != null }
        if (valid.isNotEmpty()) {
            GeoPoint(valid.map { it.latitude!! }.average(), valid.map { it.longitude!! }.average())
        } else {
            GeoPoint(10.8065, 106.6610)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onPause()
            mapViewRef?.onDetach()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                ensureOsmConfigured(ctx)
                MapView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setTileSource(OSM_STREET_TILE_SOURCE)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(13.2)
                    controller.setCenter(centerAll)
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                    onResume()
                    mapViewRef = this
                }
            },
            update = { mv ->
                mapViewRef = mv
                mv.overlays.clear()

                var selectedMarker: OsmMarker? = null
                branches.forEach { br ->
                    val lat = br.latitude ?: 10.8016
                    val lng = br.longitude ?: 106.6508
                    val isSel = br.courtId == selectedBranchId
                    val m = OsmMarker(mv).apply {
                        position = GeoPoint(lat, lng)
                        setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_CENTER)
                        title = br.courtName
                        snippet = "${br.sportType} · ${br.fieldCount} sân con"
                        icon = createPinDrawable(
                            context,
                            if (isSel) 0xFF1B8057.toInt() else 0xFF1E3A8A.toInt(),
                            isSel
                        )
                        setOnMarkerClickListener { marker, _ ->
                            onSelectState(br)
                            marker.showInfoWindow()
                            true
                        }
                    }
                    mv.overlays.add(m)
                    if (isSel) selectedMarker = m
                }

                val selBranch = selectedBranchId?.let { id -> branches.firstOrNull { it.courtId == id } }
                if (selBranch?.latitude != null && selBranch.longitude != null) {
                    mv.controller.animateTo(GeoPoint(selBranch.latitude, selBranch.longitude), 15.5, 450L)
                    selectedMarker?.showInfoWindow()
                } else {
                    mv.controller.animateTo(centerAll, 13.0, 450L)
                }
                mv.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { mapViewRef?.controller?.zoomIn() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = "Phóng to", modifier = Modifier.size(18.dp), tint = ProColors.Primary)
                }
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { mapViewRef?.controller?.zoomOut() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ZoomOut, contentDescription = "Thu nhỏ", modifier = Modifier.size(18.dp), tint = ProColors.Ink)
                }
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(34.dp)
                    .clickable {
                        mapViewRef?.controller?.animateTo(centerAll, 13.0, 400L)
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.CenterFocusStrong, contentDescription = "Xem tất cả", modifier = Modifier.size(16.dp), tint = ProColors.Primary)
                }
            }
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun OsmLocationPickerMapView(
    currentLat: Double,
    currentLng: Double,
    onLocationPicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember { ensureOsmConfigured(context); true }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val onPickedState by rememberUpdatedState(onLocationPicked)

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onPause()
            mapViewRef?.onDetach()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                ensureOsmConfigured(ctx)
                MapView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setTileSource(OSM_STREET_TILE_SOURCE)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(15.2)
                    controller.setCenter(GeoPoint(currentLat, currentLng))
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                    onResume()
                    mapViewRef = this
                }
            },
            update = { mv ->
                mapViewRef = mv
                mv.overlays.clear()

                val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        onPickedState(p.latitude, p.longitude)
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean {
                        onPickedState(p.latitude, p.longitude)
                        return true
                    }
                })
                mv.overlays.add(tapOverlay)

                val pin = OsmMarker(mv).apply {
                    position = GeoPoint(currentLat, currentLng)
                    setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_CENTER)
                    title = "Vị trí chi nhánh"
                    snippet = String.format("%.5f, %.5f", currentLat, currentLng)
                    icon = createPinDrawable(context, 0xFFDC2626.toInt(), true)
                }
                mv.overlays.add(pin)
                mv.controller.animateTo(GeoPoint(currentLat, currentLng))
                mv.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { mapViewRef?.controller?.zoomIn() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ZoomIn, contentDescription = "Phóng to", modifier = Modifier.size(18.dp), tint = ProColors.Primary)
                }
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(34.dp)
                    .clickable { mapViewRef?.controller?.zoomOut() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ZoomOut, contentDescription = "Thu nhỏ", modifier = Modifier.size(18.dp), tint = ProColors.Ink)
                }
            }
        }
    }
}

@Composable
internal fun QuickAddFieldDialog(
    branch: BranchRevenueItem,
    onDismiss: () -> Unit,
    onConfirm: (label: String, detail: String, fieldType: String) -> Unit,
) {
    val typeOptions = remember(branch.sportType) {
        when (branch.sportType) {
            "Bóng đá" -> listOf("5_VS_5" to "Sân 5 người", "7_VS_7" to "Sân 7 người", "11_VS_11" to "Sân 11 người")
            "Cầu lông" -> listOf("BADMINTON" to "Sân thảm đôi", "BADMINTON_VIP" to "Sân VIP máy lạnh")
            "Pickleball" -> listOf("PICKLEBALL" to "Sân ngoài trời", "PICKLEBALL_INDOOR" to "Sân trong nhà")
            else -> listOf("STANDARD" to "Sân tiêu chuẩn", "VIP" to "Sân VIP")
        }
    }
    var label by remember { mutableStateOf("Sân ${branch.fields.size + 1}") }
    var selectedType by remember { mutableStateOf(typeOptions.first()) }
    var detail by remember { mutableStateOf(typeOptions.first().second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column {
                Text("Thêm sân con", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ProColors.Ink)
                Text(branch.courtName, fontSize = 12.5.sp, color = ProColors.Primary, fontWeight = FontWeight.Medium)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Tên sân (VD: Sân 5C, Sân số 4)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Loại sân:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = ProColors.Ink)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(typeOptions) { opt ->
                        FilterChip(
                            selected = selectedType.first == opt.first,
                            onClick = {
                                selectedType = opt
                                if (detail.isBlank() || typeOptions.any { it.second == detail }) {
                                    detail = opt.second
                                }
                            },
                            label = { Text(opt.second, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Mô tả ngắn (mặt sân, mái che, đèn...)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label.trim(), detail.trim().ifBlank { selectedType.second }, selectedType.first) },
                enabled = label.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Thêm sân", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = ProColors.Muted) }
        }
    )
}
