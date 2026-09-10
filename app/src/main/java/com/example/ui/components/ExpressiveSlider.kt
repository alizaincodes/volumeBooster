package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.RiskRed
import kotlin.math.roundToInt

@Composable
fun ExpressiveSlider(
    value: Int, // 0 to 300
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    onRequestHeadphoneConfirm: ((targetPercent: Int) -> Unit)? = null
) {
    val view = LocalView.current
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var sliderWidthPx by remember { mutableFloatStateOf(1f) }
    var lastHapticValue by remember { mutableIntStateOf(value) }

    // Dynamic track color based on boost risk level
    val targetTrackColor = when {
        value <= 100 -> MaterialTheme.colorScheme.primary
        value <= 200 -> AmberWarning
        else -> RiskRed
    }
    val animatedTrackColor by animateColorAsState(
        targetValue = if (enabled) targetTrackColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        animationSpec = tween(250),
        label = "trackColor"
    )

    // Animated thumb scale on drag (Material 3 Expressive spring)
    val thumbSizeDp by animateDpAsState(
        targetValue = if (isDragging) 34.dp else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "thumbSize"
    )

    val currentFraction = (value.toFloat() / 300f).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = currentFraction,
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "sliderFraction"
    )

    fun triggerHaptics(newValue: Int) {
        if (!hapticsEnabled) return
        val step = 10
        val oldStep = lastHapticValue / step
        val newStep = newValue / step

        // Stronger impact when crossing 100% or 200% thresholds
        if ((lastHapticValue <= 100 && newValue > 100) || (lastHapticValue >= 100 && newValue < 100) ||
            (lastHapticValue <= 200 && newValue > 200) || (lastHapticValue >= 200 && newValue < 200)
        ) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } else if (newStep != oldStep) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        lastHapticValue = newValue
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Volume boost slider"
                stateDescription = "$value percent"
            }
    ) {
        // Floating/indicator bubble above slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            val thumbOffsetDp = with(density) {
                val clampedPx = (animatedFraction * sliderWidthPx).coerceIn(0f, sliderWidthPx)
                clampedPx.toDp()
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = animatedTrackColor,
                shadowElevation = if (isDragging) 4.dp else 1.dp,
                modifier = Modifier
                    .offset {
                        val bubbleWidthPx = 48.dp.toPx()
                        val rawXPx = (animatedFraction * sliderWidthPx) - (bubbleWidthPx / 2f)
                        val clampedXPx = rawXPx.coerceIn(0f, (sliderWidthPx - bubbleWidthPx).coerceAtLeast(0f))
                        IntOffset(clampedXPx.roundToInt(), 0)
                    }
            ) {
                Text(
                    text = "$value%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Custom thick, rounded-track canvas slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp) // Minimum 48dp touch target
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            sliderWidthPx = size.width.toFloat()
                            val frac = (offset.x / size.width).coerceIn(0f, 1f)
                            val newValue = (frac * 300f).roundToInt()
                            triggerHaptics(newValue)
                            if (newValue > 150 && onRequestHeadphoneConfirm != null) {
                                onRequestHeadphoneConfirm(newValue)
                            } else {
                                onValueChange(newValue)
                            }
                        }
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            sliderWidthPx = size.width.toFloat()
                            val frac = (offset.x / size.width).coerceIn(0f, 1f)
                            val newValue = (frac * 300f).roundToInt()
                            triggerHaptics(newValue)
                            if (newValue > 150 && onRequestHeadphoneConfirm != null) {
                                onRequestHeadphoneConfirm(newValue)
                            } else {
                                onValueChange(newValue)
                            }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            sliderWidthPx = size.width.toFloat()
                            val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                            val newValue = (frac * 300f).roundToInt()
                            triggerHaptics(newValue)
                            if (newValue > 150 && onRequestHeadphoneConfirm != null) {
                                onRequestHeadphoneConfirm(newValue)
                            } else {
                                onValueChange(newValue)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                sliderWidthPx = size.width
                val trackHeight = 16.dp.toPx()
                val trackCorner = trackHeight / 2f
                val activeWidth = (animatedFraction * size.width).coerceIn(0f, size.width)

                // Inactive track background
                drawRoundRect(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    topLeft = Offset(0f, (size.height - trackHeight) / 2f),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(trackCorner, trackCorner)
                )

                // Active track
                if (activeWidth > 0) {
                    drawRoundRect(
                        color = animatedTrackColor,
                        topLeft = Offset(0f, (size.height - trackHeight) / 2f),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = CornerRadius(trackCorner, trackCorner)
                    )
                }

                // Tick mark at 100% (Normal max)
                val tick100X = (100f / 300f) * size.width
                drawCircle(
                    color = if (value >= 100) Color.White.copy(alpha = 0.85f) else Color.Gray.copy(alpha = 0.5f),
                    radius = 3.dp.toPx(),
                    center = Offset(tick100X, size.height / 2f)
                )

                // Tick mark at 200% (High risk)
                val tick200X = (200f / 300f) * size.width
                drawCircle(
                    color = if (value >= 200) Color.White.copy(alpha = 0.85f) else Color.Gray.copy(alpha = 0.5f),
                    radius = 3.dp.toPx(),
                    center = Offset(tick200X, size.height / 2f)
                )

                // Thumb outer ring & center
                val thumbX = activeWidth.coerceIn(0f, size.width)
                val thumbRadiusPx = (thumbSizeDp / 2f).toPx()

                drawCircle(
                    color = Color.White,
                    radius = thumbRadiusPx,
                    center = Offset(thumbX, size.height / 2f)
                )
                drawCircle(
                    color = animatedTrackColor,
                    radius = thumbRadiusPx * 0.65f,
                    center = Offset(thumbX, size.height / 2f)
                )
            }
        }

        // Labels under slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = "0%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "100% Normal",
                style = MaterialTheme.typography.labelSmall,
                color = if (value in 95..105) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (value in 95..105) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "200%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "300% Max",
                style = MaterialTheme.typography.labelSmall,
                color = if (value >= 200) RiskRed else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (value >= 200) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
