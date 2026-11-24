package com.example.easysport.ui.map

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.Court
import com.example.easysport.data.repository.SampleData
import com.example.easysport.location.LocationHelper
import com.example.easysport.ui.common.EsBottomBar
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsLogoMark
import com.example.easysport.ui.common.oneDecimal
import com.example.easysport.ui.home.HomeTab
import com.example.easysport.util.CourtDistanceConstants
import com.example.easysport.util.CourtDistanceUtils
import com.example.easysport.util.CourtWithDistance
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun MapScreen(
    initialSport: String? = null,
    onTabSelected: (HomeTab) -> Unit = {},
    onOpenCourt: (String) -> Unit = {},
) {
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        MapScreenPreviewContent(onTabSelected = onTabSelected, onOpenCourt = onOpenCourt)
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Khởi tạo ViewModel
    val viewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key = "map:${initialSport.orEmpty()}", factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(initialSport) as T
        }
    })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Launcher xin quyền vị trí
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted, context)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            com.example.easysport.data.repository.CourtStore.refresh(context)
            viewModel.refreshLocation(context)
            kotlinx.coroutines.awaitCancellation()
        }
    }

    // Tọa độ tâm mặc định: ưu tiên sân đầu tiên hoặc Tân Bình, TP. HCM (10.8016, 106.6508)
    val defaultCenter = remember(uiState.filteredCourts) {
        val firstWithCoord = uiState.filteredCourts.firstOrNull { it.hasCoordinates }
        if (firstWithCoord != null) {
            LatLng(firstWithCoord.court.latitude!!, firstWithCoord.court.longitude!!)
        } else {
            LatLng(10.8016, 106.6508)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 13.5f)
    }

    // Trạng thái bản đồ đã sẵn sàng
    var isMapLoaded by remember { mutableStateOf(false) }

    // Di chuyển camera khi người dùng chủ động chọn sân (tap marker) hoặc khi bản đồ vừa load xong
    LaunchedEffect(uiState.userInitiatedCourtId, isMapLoaded) {
        if (!isMapLoaded) return@LaunchedEffect
        val courtId = uiState.userInitiatedCourtId ?: return@LaunchedEffect
        val selected = uiState.filteredCourts.firstOrNull { it.court.id == courtId }
        if (selected != null && selected.hasCoordinates) {
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(selected.court.latitude!!, selected.court.longitude!!),
                        15f
                    )
                )
            } catch (e: Exception) {
                // Bảo vệ chống hủy coroutine animation khi map đang thao tác
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        // 1. Header ứng dụng
        MapHeader(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

        // 2. Thanh tìm kiếm
        MapSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(12.dp))

        // 3. Thanh filter môn thể thao
        SportFilterChips(
            selectedSport = uiState.selectedSport,
            onSelectSport = viewModel::onSportSelected,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(10.dp))

        // 4. Khung bản đồ Google Maps và các lớp phủ thông tin
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Google Maps thật
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = PaddingValues(bottom = 220.dp),
                properties = MapProperties(
                    isMyLocationEnabled = uiState.isLocationPermissionGranted && uiState.hasUserLocation
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    mapToolbarEnabled = false
                ),
                onMapLoaded = {
                    isMapLoaded = true
                }
            ) {
                // Tạo và cache BitmapDescriptors bên trong GoogleMap khi đối tượng bản đồ đã sẵn sàng
                val markerIcons = remember(context) {
                    mapOf(
                        false to createCourtMarkerBitmap(context, isSelected = false),
                        true to createCourtMarkerBitmap(context, isSelected = true)
                    )
                }

                // Marker cho các sân có tọa độ hợp lệ
                uiState.filteredCourts.forEach { item ->
                    if (item.hasCoordinates) {
                        val pos = LatLng(item.court.latitude!!, item.court.longitude!!)
                        val isSelected = item.court.id == uiState.selectedCourtId
                        Marker(
                            state = rememberMarkerState(key = item.court.id, position = pos),
                            title = item.court.name,
                            snippet = "${item.court.sportType} · ${item.formattedDistance}",
                            icon = markerIcons[isSelected],
                            anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                            zIndex = if (isSelected) 2f else 1f,
                            onClick = {
                                viewModel.onCourtSelected(item.court.id)
                                true // Ngăn Google Maps tự xử lý thêm (mở info window, di chuyển camera riêng)
                            }
                        )
                    }
                }
            }

            // Banner cảnh báo nếu chưa cấu hình Google Maps API Key
            if (!uiState.hasMapsApiKey) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Chưa cấu hình Google Maps API Key. Thêm MAPS_API_KEY vào local.properties để tải bản đồ chuẩn xác.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Nhãn nổi góc trên: Trạng thái vị trí & Số lượng sân gần bạn
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = if (!uiState.hasMapsApiKey) 68.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nhãn vị trí người dùng
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (uiState.hasUserLocation) EsColors.Primary else Color(0xFF94A3B8),
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uiState.hasUserLocation) "Vị trí của bạn" else "Chưa xác định vị trí",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                // Nhãn số lượng sân tìm thấy
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = if (uiState.filteredCourts.isEmpty()) {
                            "Không tìm thấy sân ${uiState.selectedSport.lowercase()}"
                        } else if (uiState.hasUserLocation) {
                            "${uiState.nearbyCourtsCount} sân ${uiState.selectedSport.lowercase()} gần bạn"
                        } else {
                            "${uiState.nearbyCourtsCount} sân ${uiState.selectedSport.lowercase()} trong hệ thống"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Thông báo hỗ trợ định vị (nếu chưa có quyền hoặc GPS tắt)
                if (uiState.locationNotice != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = uiState.locationNotice!!,
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Nút định vị GPS góc dưới bên phải
            IconButton(
                onClick = {
                    if (!LocationHelper.hasLocationPermission(context)) {
                        locationPermissionLauncher.launch(LocationHelper.REQUIRED_PERMISSIONS)
                    } else if (!LocationHelper.isGpsEnabled(context)) {
                        Toast.makeText(context, "Dịch vụ định vị GPS đang tắt trên máy.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.refreshLocation(context)
                        if (uiState.hasUserLocation && isMapLoaded) {
                            coroutineScope.launch {
                                try {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(uiState.userLat!!, uiState.userLng!!),
                                            15f
                                        )
                                    )
                                } catch (e: Exception) {
                                    // Bảo vệ khi thao tác camera bị hủy hoặc bản đồ chưa sẵn sàng
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 226.dp)
                    .size(46.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                if (uiState.isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = EsColors.Primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.GpsFixed,
                        contentDescription = "Định vị vị trí hiện tại",
                        tint = if (uiState.hasUserLocation) EsColors.Primary else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Dòng ghi chú khoảng cách ước tính
            Text(
                text = "${CourtDistanceConstants.DISTANCE_ESTIMATE_NOTE} · Bán kính 5 km",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 200.dp)
            )

            // Thẻ xem trước sân bên dưới (hoặc Empty State khi không tìm thấy sân nào)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val selectedItem = uiState.selectedCourtWithDistance
                if (selectedItem != null) {
                    CourtMapPreviewCard(
                        item = selectedItem,
                        onViewCourt = { onOpenCourt(selectedItem.court.id) }
                    )
                } else {
                    EmptyCourtsCard(
                        sport = uiState.selectedSport,
                        query = uiState.searchQuery
                    )
                }
            }
        }

        // 5. Thanh điều hướng dưới cùng
        EsBottomBar(selected = HomeTab.MAP, onSelected = onTabSelected)
    }
}

// ---------------------------------------------------------------------
@Composable
private fun MapHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Bản đồ",
            color = EsColors.Title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            EsLogoMark(markSize = 30.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Easy Sport",
                color = Color(0xFF64748B),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun MapSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Tìm kiếm",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Tìm sân hoặc khu vực",
                        color = Color(0xFF94A3B8),
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = EsColors.Title, fontSize = 15.sp),
                    cursorBrush = SolidColor(EsColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Xóa",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun SportFilterChips(
    selectedSport: String,
    onSelectSport: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sports = listOf("Bóng đá", "Cầu lông", "Pickleball")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        sports.forEach { sport ->
            val isSelected = (sport == selectedSport)
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSelected) EsColors.Primary else Color(0xFFF1F5F9),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelectSport(sport) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = sport,
                        color = if (isSelected) Color.White else Color(0xFF334155),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun CourtMapPreviewCard(
    item: CourtWithDistance,
    onViewCourt: () -> Unit,
) {
    val court = item.court
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(court.imageRes),
                    contentDescription = court.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = court.name,
                        color = EsColors.Title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = EsColors.Star,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        val distanceText = item.distanceKm?.let { " · ${item.formattedDistance}" } ?: ""
                        Text(
                            text = "${court.rating.oneDecimal()} (${court.reviewCount})$distanceText",
                            color = Color(0xFF475569),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${court.sportType} · ${if (court.indoor) "Trong nhà" else "Ngoài trời"}",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = court.address,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onViewCourt,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Xem sân",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun EmptyCourtsCard(
    sport: String,
    query: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Không tìm thấy sân phù hợp",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = EsColors.Title
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (query.isNotBlank()) "Không có sân $sport nào khớp từ khóa '$query'."
                else "Chưa có sân $sport nào trong bán kính tìm kiếm.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------
@Composable
private fun MapScreenPreviewContent(
    onTabSelected: (HomeTab) -> Unit = {},
    onOpenCourt: (String) -> Unit = {},
) {
    val sampleCourt = SampleData.courts.first()
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        MapHeader(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        MapSearchBar(query = "", onQueryChange = {}, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        SportFilterChips(selectedSport = "Cầu lông", onSelectSport = {}, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Text("Google Maps Preview Mode", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                CourtMapPreviewCard(
                    item = CourtWithDistance(sampleCourt, 1.2),
                    onViewCourt = {}
                )
            }
        }
        EsBottomBar(selected = HomeTab.MAP, onSelected = onTabSelected)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MapPreview() {
    MapScreen()
}

/**
 * Tạo BitmapDescriptor hình giọt nước cho ghim sân:
 * - Màu #3B82C4 (thường) và #1D4E89 (đang chọn)
 * - Viền trắng dày 2.5dp
 * - Biểu tượng thể thao màu trắng ở tâm
 * - Neo tại chóp dưới (0.5f, 1.0f)
 */
private fun createCourtMarkerBitmap(
    context: android.content.Context,
    isSelected: Boolean
): com.google.android.gms.maps.model.BitmapDescriptor {
    return try {
        val density = context.resources.displayMetrics.density
        val widthDp = if (isSelected) 42f else 36f
        val heightDp = if (isSelected) 56f else 48f
        val w = (widthDp * density).toInt().coerceAtLeast(1)
        val h = (heightDp * density).toInt().coerceAtLeast(1)

        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val strokeWidth = 2.5f * density
        val radius = (w - strokeWidth * 2) / 2f
        val centerX = w / 2f
        val centerY = radius + strokeWidth
        val tipX = w / 2f
        val tipY = h - strokeWidth

        // Đường nét hình giọt nước: vòng tròn phía trên kết nối tới chóp nhọn phía dưới
        val path = android.graphics.Path().apply {
            moveTo(tipX, tipY)
            arcTo(
                android.graphics.RectF(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius
                ),
                140f,
                260f,
                false
            )
            lineTo(tipX, tipY)
            close()
        }

        // Đổ bóng nhẹ phía dưới chóp
        val shadowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(45, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(tipX, tipY - 1f, 3f * density, shadowPaint)

        // Tô màu thân ghim: #1D4E89 khi chọn, #3B82C4 khi thường
        val fillColor = if (isSelected) 0xFF1D4E89.toInt() else 0xFF3B82C4.toInt()
        val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawPath(path, fillPaint)

        // Viền trắng bao quanh ghim
        val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        canvas.drawPath(path, strokePaint)

        // Biểu tượng thể thao trắng (hình quả bóng thể thao với đường rãnh trắng)
        val iconRadius = radius * 0.44f
        val iconStrokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = 1.8f * density
        }
        canvas.drawCircle(centerX, centerY, iconRadius, iconStrokePaint)

        val seamPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = 1.2f * density
        }
        // Đường chỉ dọc
        canvas.drawLine(centerX, centerY - iconRadius, centerX, centerY + iconRadius, seamPaint)
        // Đường cong bán nguyệt ngang tạo họa tiết quả bóng
        val seamRect = android.graphics.RectF(
            centerX - iconRadius,
            centerY - iconRadius * 0.55f,
            centerX + iconRadius,
            centerY + iconRadius * 0.55f
        )
        canvas.drawArc(seamRect, 0f, 180f, false, seamPaint)

        com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        val hue = if (isSelected) {
            com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
        } else {
            com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
        }
        com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hue)
    }
}
