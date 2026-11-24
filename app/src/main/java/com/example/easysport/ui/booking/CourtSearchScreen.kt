package com.example.easysport.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.Court
import com.example.easysport.ui.common.CourtImage
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.common.EsTopBar
import com.example.easysport.ui.common.RatingRow
import com.example.easysport.ui.common.oneDecimal
import com.example.easysport.ui.common.vnd
import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Tiêu chí lọc tìm kiếm sân thể thao.
 */
data class CourtFilterCriteria(
    val city: String? = null,
    val district: String? = null,
    val sportType: String? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
) : java.io.Serializable {
    val isActive: Boolean
        get() = !city.isNullOrBlank() || !district.isNullOrBlank() || !sportType.isNullOrBlank() || minPrice != null || maxPrice != null
}

val CourtFilterCriteriaSaver = listSaver<CourtFilterCriteria, String>(
    save = {
        listOf(
            it.city ?: "",
            it.district ?: "",
            it.sportType ?: "",
            it.minPrice?.toString() ?: "",
            it.maxPrice?.toString() ?: "",
        )
    },
    restore = { list ->
        CourtFilterCriteria(
            city = list.getOrNull(0)?.takeIf { it.isNotEmpty() },
            district = list.getOrNull(1)?.takeIf { it.isNotEmpty() },
            sportType = list.getOrNull(2)?.takeIf { it.isNotEmpty() },
            minPrice = list.getOrNull(3)?.toIntOrNull(),
            maxPrice = list.getOrNull(4)?.toIntOrNull(),
        )
    }
)

/**
 * Chuẩn hóa chuỗi tiếng Việt: loại bỏ dấu thanh, chuyển đ/Đ thành d, lowercase để tìm kiếm không dấu.
 */
fun normalizeVietnamese(input: String): String {
    val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(nfd)
        .replaceAll("")
        .replace('đ', 'd')
        .replace('Đ', 'd')
        .lowercase()
        .trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtSearchScreen(
    courts: List<Court> = emptyList(),
    onBack: () -> Unit = {},
    onFilter: () -> Unit = {},
    onSearchNearby: () -> Unit = {},
    onCourtSelected: (String) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var appliedQuery by rememberSaveable { mutableStateOf("") }
    var appliedFilters by rememberSaveable(stateSaver = CourtFilterCriteriaSaver) {
        mutableStateOf(CourtFilterCriteria())
    }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Lọc danh sách sân kết hợp từ khóa và bộ lọc bằng quan hệ AND
    val results = remember(courts, appliedQuery, appliedFilters) {
        val queryNorm = normalizeVietnamese(appliedQuery)
        courts.filter { court ->
            // 1. Khớp từ khóa: tên, địa chỉ, quận, thành phố
            val matchesQuery = if (queryNorm.isEmpty()) {
                true
            } else {
                val nameNorm = normalizeVietnamese(court.name)
                val addrNorm = normalizeVietnamese(court.address)
                val distNorm = normalizeVietnamese(court.district ?: "")
                val cityNorm = normalizeVietnamese(court.city ?: "")
                nameNorm.contains(queryNorm) || addrNorm.contains(queryNorm) ||
                        distNorm.contains(queryNorm) || cityNorm.contains(queryNorm)
            }

            // 2. Thành phố
            val matchesCity = appliedFilters.city.isNullOrBlank() ||
                    court.city.equals(appliedFilters.city, ignoreCase = true)

            // 3. Quận/Huyện
            val matchesDistrict = appliedFilters.district.isNullOrBlank() ||
                    court.district.equals(appliedFilters.district, ignoreCase = true)

            // 4. Môn thể thao
            val matchesSport = appliedFilters.sportType.isNullOrBlank() ||
                    court.sportType.equals(appliedFilters.sportType, ignoreCase = true)

            // 5. Khoảng giá theo giờ (inclusive boundaries)
            val matchesMinPrice = appliedFilters.minPrice == null || court.pricePerHour >= appliedFilters.minPrice!!
            val matchesMaxPrice = appliedFilters.maxPrice == null || court.pricePerHour <= appliedFilters.maxPrice!!

            matchesQuery && matchesCity && matchesDistrict && matchesSport && matchesMinPrice && matchesMaxPrice
        }
    }

    fun executeSearch() {
        appliedQuery = query
        focusManager.clearFocus()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(EsColors.Surface),
    ) {
        // ----- Vùng tìm kiếm (nền trắng) -----
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            EsTopBar(
                title = "Đặt Sân",
                onBack = onBack,
                trailing = {
                    Box {
                        IconButton(onClick = {
                            showFilterSheet = true
                            onFilter()
                        }) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = "Bộ lọc",
                                tint = if (appliedFilters.isActive) EsColors.Primary else EsColors.Title,
                            )
                        }
                        if (appliedFilters.isActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 10.dp)
                                    .clip(CircleShape)
                                    .background(EsColors.Primary),
                            )
                        }
                    }
                },
            )
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Nhập tên sân bóng, khu vực...", color = EsColors.Body, fontSize = 16.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = EsColors.Body) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                appliedQuery = ""
                            }) {
                                Icon(Icons.Outlined.Close, "Xóa từ khóa", tint = EsColors.Body, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { executeSearch() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = EsColors.Surface,
                        unfocusedContainerColor = EsColors.Surface,
                        focusedBorderColor = EsColors.Primary,
                        unfocusedBorderColor = EsColors.Border,
                        focusedTextColor = EsColors.Title,
                        unfocusedTextColor = EsColors.Title,
                        cursorColor = EsColors.Primary,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                EsPrimaryButton(
                    text = "Tìm kiếm",
                    onClick = ::executeSearch,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(EsColors.SurfaceTint)
                        .clickable(onClick = onSearchNearby),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.MyLocation, null, tint = EsColors.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tìm kiếm các sân gần bạn", color = EsColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                // Chips hiển thị các điều kiện lọc đang áp dụng
                if (appliedFilters.isActive) {
                    Spacer(Modifier.height(12.dp))
                    AppliedFilterChipsRow(
                        criteria = appliedFilters,
                        onRemoveCity = { appliedFilters = appliedFilters.copy(city = null, district = null) },
                        onRemoveDistrict = { appliedFilters = appliedFilters.copy(district = null) },
                        onRemoveSport = { appliedFilters = appliedFilters.copy(sportType = null) },
                        onRemovePrice = { appliedFilters = appliedFilters.copy(minPrice = null, maxPrice = null) },
                        onClearAll = { appliedFilters = CourtFilterCriteria() },
                    )
                }
            }
        }

        // ----- Danh sách kết quả -----
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Danh sách sân phù hợp", color = EsColors.Title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${results.size} sân", color = EsColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (results.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = EsColors.Body,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Không tìm thấy sân phù hợp",
                            color = EsColors.Title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Hãy thử thay đổi từ khóa hoặc điều chỉnh bộ lọc.",
                            color = EsColors.Body,
                            fontSize = 14.sp,
                        )
                        if (appliedFilters.isActive || appliedQuery.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    query = ""
                                    appliedQuery = ""
                                    appliedFilters = CourtFilterCriteria()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EsColors.SurfaceTint,
                                    contentColor = EsColors.Primary,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Xóa bộ lọc", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            items(results, key = { it.id }) { court ->
                CourtCard(court) { onCourtSelected(court.id) }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    // Modal Bottom Sheet cho bộ lọc
    if (showFilterSheet) {
        CourtFilterBottomSheet(
            courts = courts,
            currentCriteria = appliedFilters,
            onDismiss = { showFilterSheet = false },
            onApply = { newCriteria ->
                appliedFilters = newCriteria
                showFilterSheet = false
            },
        )
    }
}

/**
 * Thanh hiển thị các tiêu chí lọc đang kích hoạt dưới dạng Chip có nút xóa.
 */
@Composable
private fun AppliedFilterChipsRow(
    criteria: CourtFilterCriteria,
    onRemoveCity: () -> Unit,
    onRemoveDistrict: () -> Unit,
    onRemoveSport: () -> Unit,
    onRemovePrice: () -> Unit,
    onClearAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        criteria.city?.let { city ->
            FilterChipItem(
                label = "TP: $city",
                onRemove = onRemoveCity,
            )
        }
        criteria.district?.let { district ->
            FilterChipItem(
                label = "Quận: $district",
                onRemove = onRemoveDistrict,
            )
        }
        criteria.sportType?.let { sport ->
            FilterChipItem(
                label = sport,
                onRemove = onRemoveSport,
            )
        }
        if (criteria.minPrice != null || criteria.maxPrice != null) {
            val priceLabel = when {
                criteria.minPrice != null && criteria.maxPrice != null ->
                    "${criteria.minPrice.vnd()} - ${criteria.maxPrice.vnd()} đ"
                criteria.minPrice != null ->
                    "Từ ${criteria.minPrice.vnd()} đ"
                else ->
                    "Đến ${criteria.maxPrice!!.vnd()} đ"
            }
            FilterChipItem(
                label = priceLabel,
                onRemove = onRemovePrice,
            )
        }
        // Nút xóa tất cả
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFEE2E2))
                .clickable(onClick = onClearAll)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Xóa tất cả",
                color = EsColors.Danger,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(EsColors.SurfaceTint)
            .border(1.dp, EsColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = EsColors.Primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(EsColors.Primary.copy(alpha = 0.15f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Bỏ lọc",
                tint = EsColors.Primary,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/**
 * Bottom Sheet Bộ lọc Đặt Sân mang phong cách EasySport.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourtFilterBottomSheet(
    courts: List<Court>,
    currentCriteria: CourtFilterCriteria,
    onDismiss: () -> Unit,
    onApply: (CourtFilterCriteria) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Trạng thái nháp (draft state) trong BottomSheet
    var draftCity by remember { mutableStateOf(currentCriteria.city) }
    var draftDistrict by remember { mutableStateOf(currentCriteria.district) }
    var draftSportType by remember { mutableStateOf(currentCriteria.sportType) }
    var draftMinPriceText by remember { mutableStateOf(currentCriteria.minPrice?.toString() ?: "") }
    var draftMaxPriceText by remember { mutableStateOf(currentCriteria.maxPrice?.toString() ?: "") }

    // Danh sách thành phố có sẵn từ dữ liệu
    val availableCities = remember(courts) {
        courts.mapNotNull { it.city }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Danh sách quận/huyện phụ thuộc vào thành phố đang chọn
    val availableDistricts = remember(courts, draftCity) {
        val filtered = if (draftCity != null) {
            courts.filter { it.city.equals(draftCity, ignoreCase = true) }
        } else {
            courts
        }
        filtered.mapNotNull { it.district }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Danh sách môn thể thao có sẵn
    val availableSports = remember(courts) {
        courts.map { it.sportType }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Kiểm tra tính hợp lệ của khoảng giá
    val parsedMin = draftMinPriceText.trim().toIntOrNull()
    val parsedMax = draftMaxPriceText.trim().toIntOrNull()
    val isPriceError = parsedMin != null && parsedMax != null && parsedMin > parsedMax

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Bộ lọc tìm kiếm",
                    color = EsColors.Title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Đóng", tint = EsColors.Body)
                }
            }

            HorizontalDivider(color = EsColors.Border, modifier = Modifier.padding(vertical = 12.dp))

            // 1. Thành phố
            Text(
                text = "Thành phố",
                color = EsColors.Title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SelectableChip(
                        text = "Tất cả",
                        isSelected = draftCity == null,
                        onClick = {
                            draftCity = null
                        },
                    )
                }
                items(availableCities) { city ->
                    SelectableChip(
                        text = city,
                        isSelected = draftCity.equals(city, ignoreCase = true),
                        onClick = {
                            draftCity = city
                            // Khi đổi thành phố, nếu quận đã chọn không thuộc thành phố mới thì bỏ chọn quận
                            if (draftDistrict != null) {
                                val validInNewCity = courts
                                    .filter { it.city.equals(city, ignoreCase = true) }
                                    .mapNotNull { it.district }
                                if (draftDistrict !in validInNewCity) {
                                    draftDistrict = null
                                }
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 2. Quận / Huyện (phụ thuộc vào thành phố)
            Text(
                text = "Quận / Huyện",
                color = EsColors.Title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            if (availableDistricts.isEmpty()) {
                Text(
                    text = "Không có thông tin quận/huyện phù hợp.",
                    color = EsColors.Body,
                    fontSize = 14.sp,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        SelectableChip(
                            text = "Tất cả",
                            isSelected = draftDistrict == null,
                            onClick = { draftDistrict = null },
                        )
                    }
                    items(availableDistricts) { district ->
                        SelectableChip(
                            text = district,
                            isSelected = draftDistrict.equals(district, ignoreCase = true),
                            onClick = { draftDistrict = district },
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 3. Môn thể thao
            Text(
                text = "Môn thể thao",
                color = EsColors.Title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SelectableChip(
                        text = "Tất cả",
                        isSelected = draftSportType == null,
                        onClick = { draftSportType = null },
                    )
                }
                items(availableSports) { sport ->
                    SelectableChip(
                        text = sport,
                        isSelected = draftSportType.equals(sport, ignoreCase = true),
                        onClick = { draftSportType = sport },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 4. Khoảng giá theo giờ (VNĐ)
            Text(
                text = "Khoảng giá theo giờ (VNĐ)",
                color = EsColors.Title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draftMinPriceText,
                    onValueChange = { draftMinPriceText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Từ 0đ", color = EsColors.Body, fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isPriceError) EsColors.Danger else EsColors.Primary,
                        unfocusedBorderColor = if (isPriceError) EsColors.Danger else EsColors.Border,
                        focusedTextColor = EsColors.Title,
                        unfocusedTextColor = EsColors.Title,
                    ),
                )
                Text(
                    text = "—",
                    color = EsColors.Body,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = draftMaxPriceText,
                    onValueChange = { draftMaxPriceText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Đến ... đ", color = EsColors.Body, fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isPriceError) EsColors.Danger else EsColors.Primary,
                        unfocusedBorderColor = if (isPriceError) EsColors.Danger else EsColors.Border,
                        focusedTextColor = EsColors.Title,
                        unfocusedTextColor = EsColors.Title,
                    ),
                )
            }

            if (isPriceError) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Giá tối thiểu không được lớn hơn giá tối đa.",
                    color = EsColors.Danger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Footer hành động: Xóa bộ lọc & Áp dụng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        draftCity = null
                        draftDistrict = null
                        draftSportType = null
                        draftMinPriceText = ""
                        draftMaxPriceText = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EsColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EsColors.Title,
                    ),
                ) {
                    Text("Xóa bộ lọc", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                EsPrimaryButton(
                    text = "Áp dụng",
                    onClick = {
                        if (!isPriceError) {
                            onApply(
                                CourtFilterCriteria(
                                    city = draftCity,
                                    district = draftDistrict,
                                    sportType = draftSportType,
                                    minPrice = parsedMin,
                                    maxPrice = parsedMax,
                                )
                            )
                        }
                    },
                    enabled = !isPriceError,
                    modifier = Modifier.weight(1f),
                    height = 50.dp,
                )
            }

            Spacer(Modifier.navigationBarsPadding())
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Chip có thể chọn được trong ModalBottomSheet.
 */
@Composable
private fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (isSelected) EsColors.SurfaceTint else EsColors.Surface)
            .border(
                1.dp,
                if (isSelected) EsColors.Primary else EsColors.Border,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (isSelected) EsColors.Primary else EsColors.Title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CourtCard(court: Court, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, EsColors.Border, shape)
            .padding(16.dp),
    ) {
        Row {
            CourtImage(court.imageRes, Modifier.size(78.dp), corner = 14.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(court.name, color = EsColors.Title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.LocationOn, null, tint = EsColors.Body, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = court.address,
                        color = EsColors.Body,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RatingRow(court.rating, court.reviewCount, filled = true)
                    Text(
                        text = "Cách bạn ${court.distanceKm.oneDecimal()} km",
                        color = EsColors.Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EsColors.SurfaceTint)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = EsColors.Border)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = EsColors.Body, fontSize = 13.sp)) { append("Giá cơ bản ") }
                    withStyle(SpanStyle(color = EsColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                        append("${court.pricePerHour.vnd()}đ")
                    }
                    withStyle(SpanStyle(color = EsColors.Body, fontSize = 13.sp)) { append("/giờ") }
                },
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(EsColors.SurfaceTint)
                    .clickable(onClick = onSelect)
                    .padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Chọn sân", color = EsColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Rounded.ChevronRight, null, tint = EsColors.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 430, heightDp = 964)
@Composable
private fun CourtSearchPreview() {
    CourtSearchScreen()
}
