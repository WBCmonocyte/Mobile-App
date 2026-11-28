package com.example.easysport.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bộ thành phần giao diện dùng chung cho Nhân viên & Chủ sân,
 * đồng bộ hoàn toàn bảng màu EasySport (`EsColors`) và header nhẹ nhàng của giao diện Khách hàng.
 */
object ProColors {
    val Canvas = EsColors.Surface
    val HeroStart = EsColors.Primary
    val HeroMid = Color(0xFF4472B4)
    val HeroEnd = Color(0xFF5683BC)
    val Primary = EsColors.Primary
    val PrimarySoft = EsColors.SurfaceTint
    val Ink = EsColors.Title
    val SubInk = Color(0xFF4B5E75)
    val Muted = EsColors.Body
    val Border = EsColors.Border
    val Surface = EsColors.Surface
    val Emerald = EsColors.Success
    val EmeraldBg = EsColors.SuccessBg
    val Amber = EsColors.Warning
    val AmberBg = EsColors.WarningBg
    val Rose = EsColors.Danger
    val RoseBg = Color(0xFFFDECEC)
    val Sky = EsColors.Primary
    val SkyBg = EsColors.SurfaceTint
    val Violet = Color(0xFF5B6CA8)
    val VioletBg = EsColors.SurfaceTint
}

val ProHeroBrush: Brush = Brush.linearGradient(
    colors = listOf(ProColors.HeroStart, ProColors.HeroMid, ProColors.HeroEnd)
)

@Composable
fun ProCard(
    modifier: Modifier = Modifier,
    accentBorderColor: Color? = null,
    containerColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val baseModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = baseModifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 1.5.dp,
        border = BorderStroke(1.dp, accentBorderColor ?: ProColors.Border),
    ) {
        Column(content = content)
    }
}

@Composable
fun ProStatusDotPill(
    text: String,
    dotColor: Color,
    textColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    Surface(
        modifier = clickableModifier,
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = text,
                color = textColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ProAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    bgColor: Color = ProColors.PrimarySoft,
    textColor: Color = ProColors.Primary,
) {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val initials = when {
        parts.isEmpty() -> "ES"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = textColor,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun ProTopBar(
    title: String,
    subtitle: String,
    @Suppress("UNUSED_PARAMETER") roleBadge: String = "",
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (onBack != null) {
                    Surface(
                        color = EsColors.Surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onBack),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = EsColors.Title,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                } else {
                    EsLogoMark(markSize = 40.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = EsColors.Title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        color = EsColors.Body,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onProfileClick != null) {
                    Surface(
                        color = ProColors.PrimarySoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onProfileClick),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "Tài khoản cá nhân",
                                tint = ProColors.Primary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
                Surface(
                    color = EsColors.Surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isRefreshing, onClick = onRefresh),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = EsColors.Primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Làm mới",
                                tint = EsColors.Title,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
                Surface(
                    color = ProColors.RoseBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onLogout),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Đăng xuất",
                            tint = EsColors.Danger,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = EsColors.Border)
    }
}

@Composable
fun EsSegmentedControl(
    leftLabel: String,
    rightLabel: String,
    isLeftSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EsColors.Surface)
            .border(1.dp, EsColors.Border, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        @Composable
        fun Segment(text: String, active: Boolean, onClick: () -> Unit, segModifier: Modifier) {
            Box(
                modifier = segModifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    color = if (active) EsColors.Primary else EsColors.Body,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Segment(leftLabel, isLeftSelected, onSelectLeft, Modifier.weight(1f))
        Segment(rightLabel, !isLeftSelected, onSelectRight, Modifier.weight(1f))
    }
}

@Composable
fun ProKpiCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    ProCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                color = ProColors.Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = ProColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ProSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = ProColors.Ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!actionText.isNullOrBlank() && onAction != null) {
            Text(
                text = actionText,
                color = ProColors.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}
