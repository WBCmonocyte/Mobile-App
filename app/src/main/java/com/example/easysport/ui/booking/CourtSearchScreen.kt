package com.example.easysport.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

@Composable
fun CourtSearchScreen(
    courts: List<Court> = emptyList(),
    onBack: () -> Unit = {},
    onFilter: () -> Unit = {},
    onSearchNearby: () -> Unit = {},
    onCourtSelected: (String) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var applied by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val results = courts.filter { it.name.contains(applied.trim(), ignoreCase = true) || it.address.contains(applied.trim(), ignoreCase = true) }

    fun search() {
        applied = query
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
                .padding(bottom = 16.dp),
        ) {
            EsTopBar(
                title = "Đặt Sân",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = onFilter) {
                        Icon(Icons.Outlined.Tune, "Bộ lọc", tint = EsColors.Title)
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
                    placeholder = { Text("Nhập tên sân bóng...", color = EsColors.Body, fontSize = 16.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = EsColors.Body) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
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
                    onClick = ::search,
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
            }
        }

        // ----- Danh sách kết quả -----
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
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
                    Text(
                        text = "Không tìm thấy sân phù hợp.",
                        color = EsColors.Body,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
            items(results, key = { it.id }) { court ->
                CourtCard(court) { onCourtSelected(court.id) }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
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
                    withStyle(SpanStyle(color = EsColors.Body, fontSize = 13.sp)) { append("Từ ") }
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
