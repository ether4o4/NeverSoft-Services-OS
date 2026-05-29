package com.ether4o4.morsvitaest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogoAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    Box(
        modifier = modifier.size(size).clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = this.size.minDimension
            val radius = diameter / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val ringStroke = (diameter * 0.035f).coerceAtLeast(2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB91C1C),
                        Color(0xFF1A0000),
                        Color(0xFF050505),
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius * 0.84f,
                center = center,
            )
            drawCircle(
                color = Color(0xFFE5484D).copy(alpha = 0.55f),
                radius = radius * 0.96f,
                center = center,
                style = Stroke(width = ringStroke),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = radius * 0.68f,
                center = center,
                style = Stroke(width = ringStroke * 0.72f),
            )

            val lineWidth = diameter * 0.44f
            drawLine(
                color = Color(0xFFE5484D),
                start = Offset(center.x - lineWidth / 2f, center.y + radius * 0.34f),
                end = Offset(center.x + lineWidth / 2f, center.y + radius * 0.34f),
                strokeWidth = ringStroke,
            )
        }

        Text(
            text = "MVE",
            style = TextStyle(
                color = Color(0xFFFFF2F2),
                fontSize = (size.value * 0.26f).sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}
