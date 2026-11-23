package com.example.easysport.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object SportIcons {
    val Badminton = ImageVector.Builder("Cầu lông", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
            moveTo(5f, 3f); lineTo(19f, 3f); lineTo(15f, 16f); lineTo(9f, 16f); close()
            moveTo(9f, 16f); lineTo(9f, 18f); curveTo(9f, 22f, 15f, 22f, 15f, 18f); lineTo(15f, 16f)
            moveTo(9f, 3f); lineTo(11f, 16f); moveTo(15f, 3f); lineTo(13f, 16f)
            moveTo(7f, 9f); lineTo(17f, 9f)
        }
    }.build()
    val Pickleball = ImageVector.Builder("Pickleball", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f) {
            moveTo(7f, 2f); lineTo(14f, 2f); quadTo(18f, 2f, 18f, 6f); lineTo(18f, 11f)
            quadTo(18f, 15f, 13f, 16f); lineTo(13f, 22f); lineTo(9f, 22f); lineTo(9f, 16f)
            quadTo(4f, 15f, 4f, 11f); lineTo(4f, 6f); quadTo(4f, 2f, 7f, 2f); close()
            moveTo(8f, 6f); lineTo(14f, 6f); moveTo(8f, 10f); lineTo(14f, 10f)
        }
    }.build()
}
