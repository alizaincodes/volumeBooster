package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun SoundwaveVisualizer(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    barCount: Int = 18,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .width(120.dp)
            .height(44.dp)
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barSpacing = totalWidth / (barCount * 1.6f)
        val barWidth = barSpacing * 0.7f

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val wave = if (isActive) {
                val primaryWave = sin(progress * 4f + phase)
                val secondaryWave = sin(progress * 8f - phase * 0.8f)
                val combined = (primaryWave * 0.6f + secondaryWave * 0.4f).coerceIn(-1f, 1f)
                0.25f + 0.75f * ((combined + 1f) / 2f)
            } else {
                0.15f
            }

            val currentBarHeight = (totalHeight * wave).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + barSpacing)
            val y = (totalHeight - currentBarHeight) / 2f

            val brush = Brush.verticalGradient(
                colors = listOf(primaryColor, accentColor),
                startY = y,
                endY = y + currentBarHeight
            )

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
