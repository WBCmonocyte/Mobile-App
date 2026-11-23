package com.example.easysport.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easysport.data.model.Amenity
import java.util.Locale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.HorizontalDivider
import com.example.easysport.ui.home.HomeTab

object EsColors {
    val Primary = Color(0xFF3B6698)
    val Title = Color(0xFF1A2E46)
    val Body = Color(0xFF748398)
    val Surface = Color(0xFFF4F7FB)
    val SurfaceTint = Color(0xFFEBF2FA)
    val Border = Color(0xFFE2E8F0)
    val Disabled = Color(0xFFA5AFBD)
    val Star = Color(0xFFD19A3D)
    val Danger = Color(0xFFC34E50)
    val DangerBorder = Color(0xFFE8BDBF)
    val Success = Color(0xFF1E8E3E)
    val SuccessBg = Color(0xFFE6F4EA)
    val Warning = Color(0xFFB26A00)
    val WarningBg = Color(0xFFFFF3DC)
}

// ---------- Định dạng ----------
/** 300000 -> "300.000" */
fun Int.vnd(): String = "%,d".format(Locale.GERMANY, this)

/** 1.2 -> "1.2" (luôn dùng dấu chấm như thiết kế). */
fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)

fun Amenity.icon(): ImageVector = when (this) {
    Amenity.LIGHT -> Icons.Outlined.Lightbulb
    Amenity.PARKING -> Icons.Outlined.DirectionsCar
    Amenity.WIFI -> Icons.Outlined.Wifi
    Amenity.DRINKS -> Icons.Outlined.LocalDrink
}

// ---------- Thành phần ----------
@Composable
fun EsTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backTint: Color = EsColors.Title,
    trailing: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 12.dp),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại", tint = backTint)
            }
        }
        Text(
            text = title,
            color = EsColors.Title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

@Composable
fun EsSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = EsColors.Title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

@Composable
fun EsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    height: Dp = 52.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EsColors.Primary,
            contentColor = Color.White,
            disabledContainerColor = EsColors.Primary.copy(alpha = 0.5f),
            disabledContentColor = Color.White,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CourtImage(@DrawableRes res: Int, modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(corner)),
    )
}

/** filled = true: sao đặc màu chủ đạo (màn tìm kiếm); false: sao viền vàng (màn Home). */
@Composable
fun RatingRow(
    rating: Double,
    reviewCount: Int,
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (filled) Icons.Rounded.Star else Icons.Outlined.StarBorder,
            contentDescription = null,
            tint = if (filled) EsColors.Primary else EsColors.Star,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$rating",
            color = if (filled) EsColors.Primary else EsColors.Title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        Text("($reviewCount)", color = EsColors.Body, fontSize = 14.sp)
    }
}
/** Logo sân bóng (ô vuông xanh, nét trắng). */
@Composable
fun EsLogoMark(modifier: Modifier = Modifier, markSize: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(markSize)
            .background(EsColors.Primary, RoundedCornerShape(markSize * 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(markSize * 0.58f)) {
            val w = size.width
            val h = size.height
            val sw = 1.6.dp.toPx()
            drawRoundRect(Color.White, cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(sw))
            drawLine(Color.White, Offset(w / 2, 0f), Offset(w / 2, h), sw)
            drawCircle(Color.White, radius = size.minDimension * 0.17f, style = Stroke(sw))
            val bw = w * 0.16f
            val bh = h * 0.36f
            drawRect(Color.White, Offset(0f, (h - bh) / 2), Size(bw, bh), style = Stroke(sw))
            drawRect(Color.White, Offset(w - bw, (h - bh) / 2), Size(bw, bh), style = Stroke(sw))
        }
    }
}

val EsCardShape = RoundedCornerShape(16.dp)

@Composable
fun EsBottomBar(selected: HomeTab, onSelected: (HomeTab) -> Unit) {
    androidx.compose.foundation.layout.Column(Modifier.background(Color.White)) {
        HorizontalDivider(color = EsColors.Border)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
        ) {
            HomeTab.entries.forEach { tab ->
                val active = tab == selected
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        tab.icon, tab.label,
                        tint = if (active) EsColors.Primary else EsColors.Body,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        color = if (active) EsColors.Primary else EsColors.Body,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

