package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VisualizerWave(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    barColor: Color = Color(0xFF00E5FF),
    accentColor: Color = Color(0xFF7C4DFF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val count = amplitudes.size.coerceAtLeast(1)
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (count - 1)
        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2.dp.toPx())
        val maxHeight = size.height

        val gradient = Brush.verticalGradient(
            colors = listOf(
                accentColor.copy(alpha = if (isPlaying) pulseAlpha else 0.4f),
                barColor.copy(alpha = if (isPlaying) 0.9f else 0.3f)
            )
        )

        amplitudes.forEachIndexed { index, amp ->
            val effectiveAmp = if (isPlaying) amp.coerceIn(0.1f, 1.0f) else 0.08f
            val barHeight = (maxHeight * effectiveAmp).coerceAtLeast(4.dp.toPx())
            val x = index * (barWidth + spacing)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
