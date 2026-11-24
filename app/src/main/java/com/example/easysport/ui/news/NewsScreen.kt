package com.example.easysport.ui.news

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.easysport.data.model.NewsItem
import com.example.easysport.data.model.TournamentItem
import com.example.easysport.ui.common.EsBottomBar
import com.example.easysport.ui.common.EsCardShape
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.common.EsTopBar
import com.example.easysport.ui.home.HomeTab
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NewsScreen(
    onTabSelected: (HomeTab) -> Unit,
    onCourtClick: (String) -> Unit,
    viewModel: NewsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val newsState by viewModel.newsState.collectAsStateWithLifecycle()
    val tournamentsState by viewModel.tournamentsState.collectAsStateWithLifecycle()

    val openBrowser: (String) -> Unit = { url ->
        try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Không thể mở trình duyệt trên thiết bị.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            EsTopBar(
                title = "Tin nổi bật",
                onBack = null,
                trailing = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Làm mới",
                            tint = EsColors.Title
                        )
                    }
                }
            )
        },
        bottomBar = {
            EsBottomBar(selected = HomeTab.NEWS, onSelected = onTabSelected)
        },
        containerColor = EsColors.Surface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // =================================================================
            // 1. MỤC GIẢI ĐẤU SẮP TỚI
            // =================================================================
            item {
                SectionHeader(
                    title = "Giải đấu sắp tới",
                    icon = Icons.Outlined.EmojiEvents,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                )
            }

            item {
                when (val state = tournamentsState) {
                    is TournamentUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = EsColors.Primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    is TournamentUiState.Empty -> {
                        Surface(
                            shape = EsCardShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, EsColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.EventBusy,
                                    contentDescription = null,
                                    tint = EsColors.Disabled,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Hiện chưa có giải đấu sắp tới nào.",
                                    color = EsColors.Body,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    is TournamentUiState.Error -> {
                        Surface(
                            shape = EsCardShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, EsColors.DangerBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.message,
                                    color = EsColors.Danger,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.loadTournaments() },
                                    border = BorderStroke(1.dp, EsColors.Primary)
                                ) {
                                    Text("Thử lại giải đấu", color = EsColors.Primary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    is TournamentUiState.Success -> {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.items, key = { it.id }) { tournament ->
                                TournamentCard(
                                    item = tournament,
                                    onCourtClick = { onCourtClick(tournament.courtId) }
                                )
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 2. MỤC TIN TỨC THỂ THAO (RSS)
            // =================================================================
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(
                    title = "Tin tức thể thao",
                    icon = Icons.Outlined.Newspaper,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            when (val state = newsState) {
                is NewsUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = EsColors.Primary)
                                Spacer(Modifier.height(12.dp))
                                Text("Đang tải tin tức thể thao...", color = EsColors.Body, fontSize = 13.sp)
                            }
                        }
                    }
                }

                is NewsUiState.Empty -> {
                    item {
                        Surface(
                            shape = EsCardShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, EsColors.Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Article,
                                    contentDescription = null,
                                    tint = EsColors.Disabled,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Hiện chưa có bài báo thể thao nào.", color = EsColors.Body, fontSize = 14.sp)
                            }
                        }
                    }
                }

                is NewsUiState.Error -> {
                    item {
                        Surface(
                            shape = EsCardShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, EsColors.DangerBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.message,
                                    color = EsColors.Danger,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(14.dp))
                                EsPrimaryButton(
                                    text = "Thử lại",
                                    onClick = { viewModel.loadNews() },
                                    modifier = Modifier.width(160.dp)
                                )
                            }
                        }
                    }
                }

                is NewsUiState.Success -> {
                    items(state.items, key = { it.url }) { item ->
                        NewsCard(
                            item = item,
                            onClick = { openBrowser(item.url) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = EsColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = EsColors.Title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TournamentCard(
    item: TournamentItem,
    onCourtClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = EsCardShape,
        color = Color.White,
        border = BorderStroke(1.dp, EsColors.Border),
        shadowElevation = 1.dp,
        modifier = modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Huy hiệu Demo
            if (item.isDemo) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EsColors.WarningBg,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Giải minh họa",
                        color = EsColors.Warning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Tên giải đấu
            Text(
                text = item.title,
                color = EsColors.Title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Tên sân & địa chỉ
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    tint = EsColors.Primary,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = item.courtName.ifEmpty { "Sân bóng" },
                        color = EsColors.Title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.address.isNotEmpty()) {
                        Text(
                            text = item.address,
                            color = EsColors.Body,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Thời gian theo múi giờ thiết bị
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = EsColors.Body,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatTournamentTime(item.startsAt, item.endsAt),
                    color = EsColors.Body,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            // Số đội tối đa
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = EsColors.Body,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Quy mô: Tối đa ${item.maxTeams} đội",
                    color = EsColors.Body,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Nút Xem sân
            Button(
                onClick = onCourtClick,
                colors = ButtonDefaults.buttonColors(containerColor = EsColors.Primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Xem sân",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NewsCard(
    item: NewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = EsCardShape,
        color = Color.White,
        border = BorderStroke(1.dp, EsColors.Border),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(EsCardShape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Tiêu đề bài viết
            Text(
                text = item.title,
                color = EsColors.Title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Nguồn báo & Thời gian theo múi giờ thiết bị
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EsColors.SurfaceTint,
                ) {
                    Text(
                        text = item.source,
                        color = EsColors.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "•",
                    color = EsColors.Body,
                    fontSize = 12.sp
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = formatDeviceDateTime(item.publishedAt),
                    color = EsColors.Body,
                    fontSize = 12.sp
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "Mở bài gốc",
                    tint = EsColors.Body,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Định dạng thời gian giải đấu theo múi giờ thiết bị (SystemDefault)
 */
private fun formatTournamentTime(startIso: String, endIso: String): String {
    return try {
        val startOdt = OffsetDateTime.parse(startIso)
        val endOdt = OffsetDateTime.parse(endIso)
        val startLocal = startOdt.atZoneSameInstant(ZoneId.systemDefault())
        val endLocal = endOdt.atZoneSameInstant(ZoneId.systemDefault())

        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        "${startLocal.format(timeFmt)} - ${endLocal.format(timeFmt)}, ${startLocal.format(dateFmt)}"
    } catch (_: Exception) {
        "$startIso - $endIso"
    }
}

/**
 * Định dạng thời gian tin tức theo múi giờ thiết bị (SystemDefault)
 */
private fun formatDeviceDateTime(isoString: String): String {
    return try {
        val odt = OffsetDateTime.parse(isoString)
        val local = odt.atZoneSameInstant(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")
        local.format(formatter)
    } catch (_: Exception) {
        isoString
    }
}
