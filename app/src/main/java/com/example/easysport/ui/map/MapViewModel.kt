package com.example.easysport.ui.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easysport.BuildConfig
import com.example.easysport.data.model.Court
import com.example.easysport.data.repository.CourtStore
import com.example.easysport.location.LocationFetchResult
import com.example.easysport.location.LocationHelper
import com.example.easysport.util.CourtDistanceConstants
import com.example.easysport.util.CourtDistanceUtils
import com.example.easysport.util.CourtWithDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MapUiState(
    val courts: List<Court> = emptyList(),
    val filteredCourts: List<CourtWithDistance> = emptyList(),
    val selectedSport: String = "Cầu lông",
    val searchQuery: String = "",
    val selectedCourtId: String? = null,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val isLoadingLocation: Boolean = false,
    val locationNotice: String? = null,
    val hasMapsApiKey: Boolean = BuildConfig.HAS_MAPS_API_KEY,
) {
    val selectedCourtWithDistance: CourtWithDistance?
        get() = filteredCourts.firstOrNull { it.court.id == selectedCourtId }
            ?: filteredCourts.firstOrNull()

    val nearbyCourtsCount: Int
        get() = filteredCourts.distinctBy { it.court.id }.size

    val hasUserLocation: Boolean
        get() = CourtDistanceUtils.isValidCoordinate(userLat, userLng)
}

class MapViewModel(
    initialSport: String? = null
) : ViewModel() {

    private val _selectedSport = MutableStateFlow(initialSport ?: "Tất cả")
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCourtId = MutableStateFlow<String?>(null)
    private val _userLocation = MutableStateFlow<LocationFetchResult.Success?>(null)
    private val _isPermissionGranted = MutableStateFlow(false)
    private val _isGpsEnabled = MutableStateFlow(true)
    private val _isLoadingLocation = MutableStateFlow(false)
    private val _locationNotice = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(MapUiState(selectedSport = initialSport ?: "Tất cả"))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                CourtStore.courts,
                _selectedSport,
                _searchQuery,
                _selectedCourtId,
                _userLocation,
                _isPermissionGranted,
                _isGpsEnabled,
                _isLoadingLocation,
                _locationNotice
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val courts = args[0] as List<Court>
                val sport = args[1] as String
                val query = args[2] as String
                val selectedId = args[3] as String?
                val userLoc = args[4] as LocationFetchResult.Success?
                val permGranted = args[5] as Boolean
                val gpsEnabled = args[6] as Boolean
                val loadingLoc = args[7] as Boolean
                val notice = args[8] as String?

                val effectiveCourts = courts

                // Lọc và sắp xếp theo môn thể thao, từ khóa và vị trí thực
                val filtered = CourtDistanceUtils.filterAndSortCourts(
                    courts = effectiveCourts,
                    userLat = userLoc?.latitude,
                    userLng = userLoc?.longitude,
                    selectedSport = sport,
                    searchQuery = query,
                    radiusKm = CourtDistanceConstants.DEFAULT_SEARCH_RADIUS_KM,
                    applyRadiusFilter = true // Hiển thị các sân trên bản đồ, sắp xếp gần nhất lên đầu
                )

                MapUiState(
                    courts = effectiveCourts,
                    filteredCourts = filtered,
                    selectedSport = sport,
                    searchQuery = query,
                    selectedCourtId = selectedId?.takeIf { id -> filtered.any { it.court.id == id } } ?: filtered.firstOrNull()?.court?.id,
                    userLat = userLoc?.latitude,
                    userLng = userLoc?.longitude,
                    isLocationPermissionGranted = permGranted,
                    isGpsEnabled = gpsEnabled,
                    isLoadingLocation = loadingLoc,
                    locationNotice = notice,
                    hasMapsApiKey = BuildConfig.HAS_MAPS_API_KEY
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onSportSelected(sport: String) {
        _selectedSport.value = sport
        _selectedCourtId.value = null // Để tự động chọn sân đầu tiên trong kết quả mới
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCourtSelected(courtId: String) {
        _selectedCourtId.value = courtId
    }

    fun onPermissionResult(granted: Boolean, context: Context) {
        _isPermissionGranted.value = granted
        if (granted) {
            refreshLocation(context)
        } else {
            _userLocation.value = null
            _locationNotice.value = "Chưa cấp quyền vị trí. Bật quyền để tính khoảng cách và định vị sân gần bạn."
        }
    }

    fun refreshLocation(context: Context) {
        if (_isLoadingLocation.value) return
        viewModelScope.launch {
            _isLoadingLocation.value = true
            val hasPerm = LocationHelper.hasLocationPermission(context)
            _isPermissionGranted.value = hasPerm

            if (!hasPerm) {
                _userLocation.value = null
                _isLoadingLocation.value = false
                _locationNotice.value = "Ứng dụng cần quyền vị trí để định vị sân gần bạn."
                return@launch
            }

            val gpsOn = LocationHelper.isGpsEnabled(context)
            _isGpsEnabled.value = gpsOn
            if (!gpsOn) {
                _userLocation.value = null
                _isLoadingLocation.value = false
                _locationNotice.value = "GPS đang tắt. Vui lòng bật vị trí trên máy để định vị chính xác."
                return@launch
            }

            when (val result = LocationHelper.getCurrentLocation(context)) {
                is LocationFetchResult.Success -> {
                    _userLocation.value = result
                    _locationNotice.value = null
                }
                is LocationFetchResult.GpsDisabled -> {
                    _isGpsEnabled.value = false
                    _locationNotice.value = "GPS đang tắt. Vui lòng bật dịch vụ vị trí trên thiết bị."
                }
                is LocationFetchResult.PermissionDenied -> {
                    _isPermissionGranted.value = false
                    _locationNotice.value = "Chưa có quyền vị trí."
                }
                is LocationFetchResult.Error -> {
                    _locationNotice.value = result.message
                }
            }
            _isLoadingLocation.value = false
        }
    }
}

